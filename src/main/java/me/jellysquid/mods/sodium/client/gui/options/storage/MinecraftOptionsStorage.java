package me.jellysquid.mods.sodium.client.gui.options.storage;

import dev.hanetzer.chlorine.common.Chlorine;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;

public class MinecraftOptionsStorage implements OptionStorage<GameSettings> {
    private final Minecraft client;

    public MinecraftOptionsStorage() {
        this.client = Minecraft.getInstance();
    }

    @Override
    public GameSettings getData() {
        return this.client.gameSettings;
    }

    @Override
    public void save() {
        this.getData().saveOptions();

        Chlorine.log.info("Flushed changes to Minecraft configuration");
    }
}
