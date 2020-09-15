package me.jellysquid.mods.sodium.mixin.features.options;

import dev.hanetzer.chlorine.common.config.Config;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.CloudOption;
import net.minecraft.client.settings.GraphicsFanciness;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameSettings.class)
public class MixinGameOptions {
    @Shadow
    public int renderDistanceChunks;

    @Shadow
    public GraphicsFanciness graphicFanciness;

    /**
     * @author JellySquid
     * @reason Make the cloud render mode user-configurable
     */
    @Overwrite
    public CloudOption getCloudOption() {
        Config.Client options = Config.CLIENT;

        if (this.renderDistanceChunks < 4 || !options.enableClouds.get()) {
            return CloudOption.OFF;
        }

        return options.cloudQuality.get().isFancy(this.graphicFanciness) ? CloudOption.FANCY : CloudOption.FAST;
    }
}
