package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkRenderDispatcher.class)
public class MixinChunkBuilder {
    @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/WorldRenderer;Ljava/util/concurrent/Executor;ZLnet/minecraft/client/renderer/RegionRenderCacheBuilder;I)V",
            index = 10, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayListWithExpectedSize(I)Ljava/util/ArrayList;", remap = false))
    private int modifyThreadPoolSize(int prev) {
        // Do not allow any resources to be allocated
        return 0;
    }
}
