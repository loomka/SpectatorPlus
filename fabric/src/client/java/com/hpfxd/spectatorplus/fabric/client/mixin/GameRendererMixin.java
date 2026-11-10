package com.hpfxd.spectatorplus.fabric.client.mixin;

import com.hpfxd.spectatorplus.fabric.client.util.SpecUtil;
import com.hpfxd.spectatorplus.fabric.client.util.SpectatedHands;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private LightTexture lightTexture;
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow @Final public ItemInHandRenderer itemInHandRenderer;

    @Unique private float bob;
    @Unique private float bobO;
    @Unique private float walkDist;
    @Unique private float walkDistO;

    @Inject(method = "renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
    public void spectatorplus$renderItemInHand(Camera camera, float partialTicks, Matrix4f matrix4f, CallbackInfo ci, @Local PoseStack poseStackIn) {
        // With a shader pack, Iris skips this pass and renders hands through ItemInHandRenderer#renderHandsWithItems,
        // which ItemInHandRendererMixin redirects to the spectated player.
        final AbstractClientPlayer spectated = SpectatedHands.target(this.minecraft);
        if (spectated != null) {
            this.lightTexture.turnOnLightLayer();
            final int packedLightCoords = this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(spectated, partialTicks);
            SpectatedHands.render(this.itemInHandRenderer, spectated, partialTicks, poseStackIn, this.renderBuffers.bufferSource(), packedLightCoords);
            this.lightTexture.turnOffLightLayer();
            this.renderBuffers.bufferSource().endBatch();
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void spectatorplus$tick(CallbackInfo ci) {
        final AbstractClientPlayer spectated = SpecUtil.getCameraPlayer(this.minecraft);
        if (spectated != null) {
            // View bobbing

            // get horizontal distance between current and last pos
            final Vec3 pos = spectated.position().with(Direction.Axis.Y, 0);
            final Vec3 posO = spectated.getPosition(0F).with(Direction.Axis.Y, 0);
            final float deltaMovement = (float) posO.distanceTo(pos);

            this.walkDistO = this.walkDist;
            this.walkDist += deltaMovement * 0.6F;

            this.bobO = this.bob;
            if (spectated.isPassenger() || !spectated.onGround() || spectated.isDeadOrDying() || spectated.isSwimming()) {
                this.bob = 0.0F;
            } else {
                this.bob += (Math.min(0.1F, deltaMovement) - this.bob) * 0.4F;
            }

        } else {
            this.bob = this.bobO = this.walkDist = this.walkDistO = 0;
        }
        SpectatedHands.tickSway(spectated);
    }

    @ModifyExpressionValue(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/AbstractClientPlayer;walkDist:F"))
    private float spectatorplus$modifyBobWalkDist(float original, @Local AbstractClientPlayer cameraPlayer) {
        return cameraPlayer == this.minecraft.player ? original : this.walkDist;
    }

    @ModifyExpressionValue(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/AbstractClientPlayer;walkDistO:F"))
    private float spectatorplus$modifyBobWalkDistO(float original, @Local AbstractClientPlayer cameraPlayer) {
        return cameraPlayer == this.minecraft.player ? original : this.walkDistO;
    }

    @ModifyExpressionValue(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/AbstractClientPlayer;bob:F"))
    private float spectatorplus$modifyBobValue(float original, @Local AbstractClientPlayer cameraPlayer) {
        return cameraPlayer == this.minecraft.player ? original : this.bob;
    }

    @ModifyExpressionValue(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/AbstractClientPlayer;oBob:F"))
    private float spectatorplus$modifyBobValueO(float original, @Local AbstractClientPlayer cameraPlayer) {
        return cameraPlayer == this.minecraft.player ? original : this.bobO;
    }

    @ModifyExpressionValue(method = "pick(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;blockInteractionRange()D"))
    private double spectatorplus$modifyBlockInteractionRange(double original) {
        final AbstractClientPlayer spectated = SpecUtil.getCameraPlayer(this.minecraft);
        if (spectated != null) {
            return spectated.blockInteractionRange();
        }

        return original;
    }

    @ModifyExpressionValue(method = "pick(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;entityInteractionRange()D"))
    private double spectatorplus$modifyEntityInteractionRange(double original) {
        final AbstractClientPlayer spectated = SpecUtil.getCameraPlayer(this.minecraft);
        if (spectated != null) {
            return spectated.entityInteractionRange();
        }

        return original;
    }
}
