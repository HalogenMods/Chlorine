package me.jellysquid.mods.sodium.client.render.pipeline.context;

import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.cache.ArrayLightDataCache;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.sink.ModelQuadSinkDelegate;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.RenderContextCommon;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

public class ChunkRenderContext {
    private final ArrayLightDataCache lightDataCache;

    private final BlockRenderer blockRenderer;
    private final FluidRenderer fluidRenderer;

    private final BlockModelShapes models;

    public ChunkRenderContext(Minecraft client) {
        this.lightDataCache = new ArrayLightDataCache(WorldSlice.BLOCK_LENGTH);

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache);
        BiomeColorBlender biomeColorBlender = RenderContextCommon.createBiomeColorBlender();

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, biomeColorBlender);
        this.fluidRenderer = new FluidRenderer(client, lightPipelineProvider, biomeColorBlender);

        this.models = client.getModelManager().getBlockModelShapes();
    }

    public boolean renderBlock(IBlockDisplayReader world, BlockState state, BlockPos pos, ModelQuadSinkDelegate consumer, boolean cull) {
        IBakedModel model = this.models.getModel(state);
        long seed = state.getPositionRandom(pos);

        return this.blockRenderer.renderModel(world, state, pos, model, consumer, cull, seed);
    }

    public boolean renderFluid(IBlockDisplayReader world, FluidState fluidState, BlockPos.Mutable pos, ModelQuadSinkDelegate consumer) {
        return this.fluidRenderer.render(world, fluidState, pos, consumer);
    }

    public void init(IBlockDisplayReader world, int x, int y, int z) {
        this.lightDataCache.init(world, x, y, z);
    }
}
