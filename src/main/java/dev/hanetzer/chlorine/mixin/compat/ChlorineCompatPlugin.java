package dev.hanetzer.chlorine.mixin.compat;

import dev.hanetzer.chlorine.common.Chlorine;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class ChlorineCompatPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        try {
            if (hasClass("com.feed_the_beast.mods.ftbchunks.FTBChunks")) {
                Chlorine.log.info("Detected FTB-Chunks");
                Mixins.addConfiguration("chlorine.ftbchunks.json");
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    static boolean hasClass(String name) throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        if (stream != null) stream.close();
        return stream != null;
    }
}
