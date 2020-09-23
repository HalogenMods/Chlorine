package me.jellysquid.mods.sodium.mixin.features.debug;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import net.minecraft.client.gui.overlay.DebugOverlayGui;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Mixin(DebugOverlayGui.class)
public abstract class MixinDebugHud {
    @Shadow
    private static long bytesToMb(long bytes) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "getDebugInfoRight", at = @At("RETURN"))
    private void appendRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> strings = cir.getReturnValue();

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());
                break;
            }
        }

        strings.add("");
        strings.addAll(getChunkRendererDebugStrings());

        if (SodiumClientMod.options().advanced.disableDriverBlacklist) {
            strings.add(TextFormatting.RED + "(!!) Driver blacklist ignored");
        }
    }

    private static List<String> getChunkRendererDebugStrings() {
        ChunkRenderBackend<?> backend = SodiumWorldRenderer.getInstance().getChunkRenderer();
        MemoryTracker memoryTracker = backend.getMemoryTracker();

        List<String> strings = new ArrayList<>(4);
        strings.add("Chunk Renderer: " + backend.getRendererName());

        if (memoryTracker != null) {
            int allocated = memoryTracker.getAllocatedMemory();
            int used = memoryTracker.getUsedMemory();

            int ratio = (int) Math.floor(((double) used / (double) allocated) * 100.0D);

            strings.add("VRAM: " + bytesToMb(used) + "/" + bytesToMb(allocated) + "MB (" + ratio + "%)");
        }

        strings.addAll(backend.getDebugStrings());

        return strings;
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + bytesToMb(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()) + "MB";
    }
}
