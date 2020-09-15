package dev.hanetzer.chlorine.common;

import dev.hanetzer.chlorine.common.config.Config;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Chlorine.modID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Chlorine {
    public static final String modID = "chlorine";
    public Chlorine() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
