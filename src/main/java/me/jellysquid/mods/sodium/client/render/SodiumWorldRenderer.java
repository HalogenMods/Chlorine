package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.vertex.MatrixApplyingVertexBuilder;
import com.mojang.blaze3d.vertex.VertexBuilderUtils;
import dev.hanetzer.chlorine.common.config.Config;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.GlFogHelper;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl20.GL20ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl33.GL33ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl43.GL43ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.WorldRenderPhase;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import me.jellysquid.mods.sodium.common.util.ListUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Set;
import java.util.SortedSet;

/**
 * Provides an extension to vanilla's {@link WorldRenderer}.
 */
public class SodiumWorldRenderer implements ChunkStatusListener {
    private static SodiumWorldRenderer instance;

    private final Minecraft client;

    private ClientWorld world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;

    private boolean useEntityCulling;

    private final LongSet loadedChunkPositions = new LongOpenHashSet();
    private final Set<TileEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private ClippingHelper frustum;
    private ChunkRenderManager<?> chunkRenderManager;
    private ChunkRenderBackend<?> chunkRenderBackend;

    /**
     * Instantiates Sodium's world renderer. This should be called at the time of the world renderer initialization.
     */
    public static SodiumWorldRenderer create() {
        if (instance == null) {
            instance = new SodiumWorldRenderer(Minecraft.getInstance());
        }

        return instance;
    }

    /**
     * @throws IllegalStateException If the renderer has not yet been created
     * @return The current instance of this type
     */
    public static SodiumWorldRenderer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Renderer not initialized");
        }

        return instance;
    }

    private SodiumWorldRenderer(Minecraft client) {
        this.client = client;
    }

    public void setWorld(ClientWorld world) {
        this.world = world;
        this.loadedChunkPositions.clear();
        this.globalBlockEntities.clear();

        if (world == null) {
            if (this.chunkRenderManager != null) {
                this.chunkRenderManager.destroy();
                this.chunkRenderManager = null;
            }

            if (this.chunkRenderBackend != null) {
                this.chunkRenderBackend.delete();
                this.chunkRenderBackend = null;
            }

            this.loadedChunkPositions.clear();
        } else {
            this.initRenderer();

            ((ChunkStatusListenerManager) world.getChunkProvider()).setListener(this);
        }
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.chunkRenderManager.getVisibleChunkCount();
    }

    /**
     * Notifies the chunk renderer that the graph scene has changed and should be re-computed.
     */
    public void scheduleTerrainUpdate() {
        // BUG: seems to be called before init
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.markDirty();
        }
    }

    /**
     * @return True if no chunks are pending rebuilds
     */
    public boolean isTerrainRenderComplete() {
        return this.chunkRenderManager.isBuildComplete();
    }

    /**
     * Called prior to any chunk rendering in order to update necessary state.
     */
    public void updateChunks(ActiveRenderInfo camera, ClippingHelper frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.frustum = frustum;

        this.useEntityCulling = Config.CLIENT.useAdvancedEntityCulling.get();

        if (this.client.gameSettings.renderDistanceChunks != this.renderDistance) {
            this.reload();
        }

        IProfiler profiler = this.client.getProfiler();
        profiler.startSection("camera_setup");

        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        Vector3d cameraPos = camera.getProjectedView();

        this.chunkRenderManager.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);

        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        boolean dirty = cameraPos.x != this.lastCameraX || cameraPos.y != this.lastCameraY || cameraPos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw;

        if (dirty) {
            this.chunkRenderManager.markDirty();
        }

        this.lastCameraX = cameraPos.x;
        this.lastCameraY = cameraPos.y;
        this.lastCameraZ = cameraPos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;

        profiler.endStartSection("chunk_update");

        this.chunkRenderManager.updateChunks();

        if (!hasForcedFrustum && this.chunkRenderManager.isDirty()) {
            profiler.endStartSection("chunk_graph_rebuild");

            this.chunkRenderManager.updateGraph(camera, (FrustumExtended) frustum, frame, spectator);
        }

        profiler.endStartSection("visible_chunk_tick");

        this.chunkRenderManager.tickVisibleRenders();

        profiler.endSection();

        Entity.setRenderDistanceWeight(MathHelper.clamp((double) this.client.gameSettings.renderDistanceChunks / 8.0D, 1.0D, 2.5D));
    }


    public void drawChunkLayers(WorldRenderPhase phase, MatrixStack matrixStack, double x, double y, double z) {
        for (BlockRenderPass pass : this.chunkRenderBackend.getRenderPassManager().getPassesForPhase(phase)) {
            this.drawChunkLayer(pass, matrixStack, x, y, z);
        }
    }

    /**
     * Performs a render pass for the given {@link RenderType} and draws all visible chunks for it.
     */
    public void drawChunkLayer(BlockRenderPass pass, MatrixStack matrixStack, double x, double y, double z) {
        pass.beginRender();

        // We don't have a great way to check if underwater fog is being used, so assume that terrain will only ever
        // use linear fog. This will not disable fog in the Nether.
        if (!Config.CLIENT.enableFog.get() && GlFogHelper.isFogLinear()) {
            RenderSystem.disableFog();
        }

        this.chunkRenderManager.renderChunks(matrixStack, pass, x, y, z);

        pass.endRender();

        RenderSystem.clearCurrentColor();
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.initRenderer();
    }

    private void initRenderer() {
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.renderDistance = this.client.gameSettings.renderDistanceChunks;

        Config.Client opts = Config.CLIENT;

        final GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> vertexFormat;

        if (opts.useCompactVertexFormat.get()) {
            vertexFormat = SodiumVertexFormats.CHUNK_MESH_COMPACT;
        } else {
            vertexFormat = SodiumVertexFormats.CHUNK_MESH_FULL;
        }

        this.chunkRenderBackend = createChunkRenderBackend(opts.chunkRendererBackend.get(), vertexFormat);
        this.chunkRenderBackend.createShaders();

        this.chunkRenderManager = new ChunkRenderManager<>(this, this.chunkRenderBackend, this.world, this.renderDistance);
        this.chunkRenderManager.restoreChunks(this.loadedChunkPositions);
    }

    private static ChunkRenderBackend<?> createChunkRenderBackend(SodiumGameOptions.ChunkRendererBackendOption opt,
                                                           GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> vertexFormat) {
        boolean disableBlacklist = Config.CLIENT.disableDriverBlacklist.get();

        switch (opt) {
            case GL43:
                if (GL43ChunkRenderBackend.isSupported(disableBlacklist)) {
                    return new GL43ChunkRenderBackend(vertexFormat);
                }
            case GL33:
                if (GL33ChunkRenderBackend.isSupported(disableBlacklist)) {
                    return new GL33ChunkRenderBackend(vertexFormat);
                }
            case GL20:
                if (GL20ChunkRenderBackend.isSupported(disableBlacklist)) {
                    return new GL20ChunkRenderBackend(vertexFormat);
                }
            default:
                throw new IllegalArgumentException("No suitable chunk render backends exist");
        }
    }

    public void renderTileEntities(MatrixStack matrices, RenderTypeBuffers bufferBuilders, Long2ObjectMap<SortedSet<DestroyBlockProgress>> blockBreakingProgressions,
                                   ActiveRenderInfo camera, float tickDelta) {
        IRenderTypeBuffer.Impl immediate = bufferBuilders.getBufferSource();

        Vector3d cameraPos = camera.getProjectedView();
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        for (TileEntity blockEntity : this.chunkRenderManager.getVisibleBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            IRenderTypeBuffer consumer = immediate;
            SortedSet<DestroyBlockProgress> breakingInfos = blockBreakingProgressions.get(pos.toLong());

            if (breakingInfos != null && !breakingInfos.isEmpty()) {
                int stage = breakingInfos.last().getPartialBlockDamage();

                if (stage >= 0) {
                    MatrixStack.Entry entry = matrices.getLast();
                    IVertexBuilder transformer = new MatrixApplyingVertexBuilder(bufferBuilders.getCrumblingBufferSource().getBuffer(ModelBakery.DESTROY_RENDER_TYPES.get(stage)), entry.getMatrix(), entry.getNormal());
                    consumer = (layer) -> layer.isUseDelegate() ? VertexBuilderUtils.newDelegate(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
                }
            }

            TileEntityRendererDispatcher.instance.renderTileEntity(blockEntity, tickDelta, matrices, consumer);

            matrices.pop();
        }

        for (TileEntity blockEntity : this.globalBlockEntities) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            TileEntityRendererDispatcher.instance.renderTileEntity(blockEntity, tickDelta, matrices, immediate);

            matrices.pop();
        }
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.loadedChunkPositions.add(ChunkPos.asLong(x, z));
        this.chunkRenderManager.onChunkAdded(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.loadedChunkPositions.remove(ChunkPos.asLong(x, z));
        this.chunkRenderManager.onChunkRemoved(x, z);
    }

    public void onChunkRenderUpdated(ChunkRenderData meshBefore, ChunkRenderData meshAfter) {
        ListUtil.updateList(this.globalBlockEntities, meshBefore.getGlobalBlockEntities(), meshAfter.getGlobalBlockEntities());
    }

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!this.useEntityCulling) {
            return true;
        }

        AxisAlignedBB box = entity.getRenderBoundingBox();

        // Entities outside the valid world height will never map to a rendered chunk
        // Always render these entities or they'll be culled incorrectly!
        if (box.maxY < 0.5D || box.minY > 255.5D) {
            return true;
        }

        int minX = MathHelper.floor(box.minX - 0.5D) >> 4;
        int minY = MathHelper.floor(box.minY - 0.5D) >> 4;
        int minZ = MathHelper.floor(box.minZ - 0.5D) >> 4;

        int maxX = MathHelper.floor(box.maxX + 0.5D) >> 4;
        int maxY = MathHelper.floor(box.maxY + 0.5D) >> 4;
        int maxZ = MathHelper.floor(box.maxZ + 0.5D) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.chunkRenderManager.isChunkVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * @return The frustum of the current player's camera used to cull chunks
     */
    public ClippingHelper getFrustum() {
        return this.frustum;
    }

    public String getChunksDebugString() {
        // C: visible/total
        // TODO: add dirty and queued counts
        return String.format("C: %s/%s", this.chunkRenderManager.getVisibleChunkCount(), this.chunkRenderManager.getTotalSections());
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified block region.
     */
    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified chunk region.
     */
    public void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkY = minY; chunkY <= maxY; chunkY++) {
                for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                    this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
                }
            }
        }
    }

    /**
     * Schedules a chunk rebuild for the render belonging to the given chunk section position.
     */
    public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
        this.chunkRenderManager.scheduleRebuild(x, y, z, important);
    }

    public ChunkRenderBackend<?> getChunkRenderer() {
        return this.chunkRenderBackend;
    }
}
