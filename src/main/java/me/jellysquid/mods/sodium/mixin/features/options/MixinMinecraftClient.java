package me.jellysquid.mods.sodium.mixin.features.options;

import dev.hanetzer.chlorine.common.config.Config;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {
    /**
     * @author JellySquid
     * @reason Make ambient occlusion user configurable
     */
    @Overwrite
    public static boolean isAmbientOcclusionEnabled() {
        return Config.CLIENT.smoothLighting.get() != SodiumGameOptions.LightingQuality.OFF;
    }
}
