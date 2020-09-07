package dev.hanetzer.chlorine.common;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Chlorine.modID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Chlorine {
    public static final String modID = "chlorine";
    public Chlorine() {
        MinecraftForge.EVENT_BUS.register(this);
    }
}
