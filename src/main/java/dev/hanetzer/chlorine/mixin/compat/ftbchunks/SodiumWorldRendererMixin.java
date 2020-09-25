package dev.hanetzer.chlorine.mixin.compat.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @Inject(method = "onChunkAdded", at = @At("TAIL"), remap = false)
    private void scheduleRebuildForChunkCFTBC(int x, int z, CallbackInfo ci) {
        FTBChunksClient.rerenderCache.add(new ChunkPos(x, z));
    }
}
