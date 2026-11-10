package com.hpfxd.spectatorplus.fabric.client.util;

import com.google.common.base.MoreObjects;
import com.hpfxd.spectatorplus.fabric.client.SpectatorClientMod;
import com.hpfxd.spectatorplus.fabric.client.mixin.ItemInHandRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Renders the first-person arms of the spectated player. Shared by the vanilla hand pass and by
 * shader pipelines (Iris) that render hands themselves through {@link ItemInHandRenderer#renderHandsWithItems}.
 */
public final class SpectatedHands {
    private static float xBob;
    private static float yBob;
    private static float xBobO;
    private static float yBobO;

    private SpectatedHands() {
    }

    /**
     * Returns the spectated player whose arms should be rendered this frame, or null.
     */
    public static AbstractClientPlayer target(Minecraft minecraft) {
        if (!SpectatorClientMod.config.renderArms || minecraft.player == null
                || !minecraft.options.getCameraType().isFirstPerson() || minecraft.options.hideGui) {
            return null;
        }
        final AbstractClientPlayer spectated = SpecUtil.getCameraPlayer(minecraft);
        if (spectated == null || spectated.isSpectator() || spectated.isSleeping()) {
            return null;
        }
        return spectated;
    }

    public static void tickSway(AbstractClientPlayer spectated) {
        if (spectated == null) {
            xBob = yBob = xBobO = yBobO = 0;
            return;
        }
        yBobO = yBob;
        xBobO = xBob;
        xBob += Mth.degreesDifference(xBob, spectated.getXRot()) * 0.5F;
        yBob += Mth.degreesDifference(yBob, spectated.getYRot()) * 0.5F;
    }

    public static void render(ItemInHandRenderer renderer, AbstractClientPlayer spectated, float partialTicks,
                              PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        final float attackAnim = spectated.getAttackAnim(partialTicks);
        final InteractionHand swingingHand = MoreObjects.firstNonNull(spectated.swingingArm, InteractionHand.MAIN_HAND);
        final float pitch = Mth.lerp(partialTicks, spectated.xRotO, spectated.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees((spectated.getViewXRot(partialTicks) - Mth.lerp(partialTicks, xBobO, xBob)) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.degreesDifference(Mth.lerp(partialTicks, yBobO, yBob), Mth.rotLerp(partialTicks, spectated.yRotO, spectated.getYRot())) * 0.1F));

        final ItemInHandRenderer.HandRenderSelection selection = evaluateWhichHandsToRender(spectated);
        final ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) renderer;

        if (selection.renderMainHand) {
            final float swingProgress = swingingHand == InteractionHand.MAIN_HAND ? attackAnim : 0.0F;
            final float equippedProgress = 1F - Mth.lerp(partialTicks, accessor.getOMainHandHeight(), accessor.getMainHandHeight());
            accessor.invokeRenderArmWithItem(spectated, partialTicks, pitch, InteractionHand.MAIN_HAND, swingProgress,
                    accessor.getMainHandItem(), equippedProgress, poseStack, buffer, packedLight);
        }

        if (selection.renderOffHand) {
            final float swingProgress = swingingHand == InteractionHand.OFF_HAND ? attackAnim : 0.0F;
            final float equippedProgress = 1F - Mth.lerp(partialTicks, accessor.getOOffHandHeight(), accessor.getOffHandHeight());
            accessor.invokeRenderArmWithItem(spectated, partialTicks, pitch, InteractionHand.OFF_HAND, swingProgress,
                    accessor.getOffHandItem(), equippedProgress, poseStack, buffer, packedLight);
        }

        poseStack.popPose();
    }

    private static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(AbstractClientPlayer player) {
        // see ItemInHandRenderer#evaluateWhichHandsToRender
        // could not just call into the original method due to the argument being LocalPlayer

        final ItemStack mainHandItem = player.getMainHandItem();
        final ItemStack offHandItem = player.getOffhandItem();
        final boolean bow = mainHandItem.is(Items.BOW) || offHandItem.is(Items.BOW);
        final boolean crossbow = mainHandItem.is(Items.CROSSBOW) || offHandItem.is(Items.CROSSBOW);
        if (!bow && !crossbow) {
            return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        } else if (player.isUsingItem()) {
            final ItemStack useItem = player.getUseItem();
            final InteractionHand usedHand = player.getUsedItemHand();
            if (!useItem.is(Items.BOW) && !useItem.is(Items.CROSSBOW)) {
                return usedHand == InteractionHand.MAIN_HAND && ItemInHandRendererAccessor.invokeIsChargedCrossbow(player.getOffhandItem())
                        ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
                        : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
            } else {
                return ItemInHandRenderer.HandRenderSelection.onlyForHand(usedHand);
            }
        } else {
            return ItemInHandRendererAccessor.invokeIsChargedCrossbow(mainHandItem)
                    ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
                    : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        }
    }
}
