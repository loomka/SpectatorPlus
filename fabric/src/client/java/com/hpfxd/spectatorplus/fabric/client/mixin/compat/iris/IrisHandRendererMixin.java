package com.hpfxd.spectatorplus.fabric.client.mixin.compat.iris;

import com.hpfxd.spectatorplus.fabric.client.util.SpectatedHands;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iris replaces the vanilla hand pass when a shader pack is active, and its own pass refuses to render while the
 * local player is a spectator. Let it render the spectated player's arms instead.
 */
@Mixin(value = HandRenderer.class, remap = false)
public abstract class IrisHandRendererMixin {
    @Inject(method = "canRender", at = @At("RETURN"), cancellable = true)
    private void spectatorplus$renderSpectatedHands(Camera camera, GameRenderer gameRenderer, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !camera.isDetached() && SpectatedHands.target(Minecraft.getInstance()) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isHandTranslucent", at = @At("HEAD"), cancellable = true)
    private void spectatorplus$spectatedHandTranslucency(InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        final AbstractClientPlayer spectated = SpectatedHands.target(Minecraft.getInstance());
        if (spectated != null) {
            cir.setReturnValue(spectated.getItemInHand(hand).getItem() instanceof final BlockItem blockItem
                    && ItemBlockRenderTypes.getChunkRenderType(blockItem.getBlock().defaultBlockState()) == RenderType.translucent());
        }
    }
}
