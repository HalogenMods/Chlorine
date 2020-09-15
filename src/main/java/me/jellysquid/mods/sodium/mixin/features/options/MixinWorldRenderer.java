package me.jellysquid.mods.sodium.mixin.features.options;

import dev.hanetzer.chlorine.common.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Redirect(method = "renderRainSnow", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isFancyGraphicsEnabled()Z"))
    private boolean redirectGetFancyWeather() {
        return Config.CLIENT.weatherQuality.get().isFancy(Minecraft.getInstance().gameSettings.graphicFanciness);
    }
}
