package me.jellysquid.mods.sodium.client.gui.options.storage;

import dev.hanetzer.chlorine.common.Chlorine;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;

public class SodiumOptionsStorage implements OptionStorage<SodiumGameOptions> {
//    private final SodiumGameOptions options;

    public SodiumOptionsStorage() {
//        this.options = SodiumClientMod.options();
    }

    @Override
    public SodiumGameOptions getData() {
//        return this.options;
        return null;
    }

    @Override
    public void save() {
//        this.options.writeChanges();
//        this.options.notifyListeners();

        Chlorine.log.info("Flushed changes to Sodium configuration");
    }
}
