package me.jellysquid.mods.sodium.client.render.chunk.passes.impl;

import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.WorldRenderPhase;
import net.minecraft.util.ResourceLocation;

public class MultiTextureRenderPipeline {
    public static final ResourceLocation SOLID_PASS = new ResourceLocation("sodium", "multi_texture/solid");
    public static final ResourceLocation TRANSLUCENT_PASS = new ResourceLocation("sodium", "multi_texture/translucent");

    public static BlockRenderPassManager create() {
        BlockRenderPassManager registry = new BlockRenderPassManager();
        registry.add(WorldRenderPhase.OPAQUE, SOLID_PASS, SolidRenderPass::new, BlockLayer.SOLID, BlockLayer.SOLID_MIPPED);
        registry.add(WorldRenderPhase.TRANSLUCENT, TRANSLUCENT_PASS, TranslucentRenderPass::new, BlockLayer.TRANSLUCENT_MIPPED);

        return registry;
    }
}
