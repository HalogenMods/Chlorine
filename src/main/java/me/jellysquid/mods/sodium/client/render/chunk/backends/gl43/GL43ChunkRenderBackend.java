package me.jellysquid.mods.sodium.client.render.chunk.backends.gl43;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferRegion;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.GlVendorUtil;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawCallBatcher;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawParamsVector;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkRenderBackendMultiDraw;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.impl.MultiTextureRenderPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgramComponentBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.shader.texture.ChunkProgramMultiTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;
import java.util.List;

/**
 * Shader-based chunk renderer which makes use of a custom memory allocator on top of large buffer objects to allow
 * for draw call batching without buffer switching.
 *
 * The biggest bottleneck after setting up vertex attribute state is the sheer number of buffer switches and draw calls
 * being performed. In vanilla, the game uses one buffer for every chunk section, which means we need to bind, setup,
 * and draw every chunk individually.
 *
 * In order to reduce the number of these calls, we need to firstly reduce the number of buffer switches. We do this
 * through sub-dividing the world into larger "chunk regions" which then have one large buffer object in OpenGL. From
 * here, we can allocate slices of this buffer to each individual chunk and then only bind it once before drawing. Then,
 * our draw calls can simply point to individual sections within the buffer by manipulating the offset and count
 * parameters.
 *
 * However, an unfortunate consequence is that if we run out of space in a buffer, we need to re-allocate the entire
 * storage, which can take a ton of time! With old OpenGL 2.1 code, the only way to do this would be to copy the buffer's
 * memory from the graphics card over the host bus into CPU memory, allocate a new buffer, and then copy it back over
 * the bus and into graphics card. For reasons that should be obvious, this is extremely inefficient and requires the
 * CPU and GPU to be synchronized.
 *
 * If we make use of more modern OpenGL 3.0 features, we can avoid this transfer over the memory bus and instead just
 * perform the copy between buffers in GPU memory with the aptly named "copy buffer" function. It's still not blazing
 * fast, but it's much better than what we're stuck with in older versions. We can help prevent these re-allocations by
 * sizing our buffers to be a bit larger than what we expect all the chunk data to be, but this wastes memory.
 *
 * In the initial implementation, this solution worked fine enough, but the amount of time being spent on uploading
 * chunks to the large buffers was now a magnitude more than what it was before all of this and it made chunk updates
 * *very* slow. It took some tinkering to figure out what was going wrong here, but at least on the NVIDIA drivers, it
 * seems that updating sub-regions of buffer memory hits some kind of slow path. A workaround for this problem is to
 * create a scratch buffer object and upload the chunk data there *first*, re-allocating the storage each time. Then,
 * you can copy the contents of the scratch buffer into the chunk region buffer, rise and repeat. I'm not happy with
 * this solution, but it performs surprisingly well across all hardware I tried.
 *
 * With both of these changes, the amount of CPU time taken by rendering chunks linearly decreases with the reduction
 * in buffer bind/setup/draw calls. Using the default settings of 4x2x4 chunk region buffers, the number of calls can be
 * reduced up to a factor of ~32x.
 */
public class GL43ChunkRenderBackend extends ChunkRenderBackendMultiDraw<GL43GraphicsState> {
    private final BlockRenderPassManager renderPassManager;
    private final ChunkRegionManager<GL43GraphicsState> bufferManager;

    private final ObjectArrayFIFOQueue<ChunkRegion<GL43GraphicsState>> pendingBatches = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRegion<GL43GraphicsState>> pendingUploads = new ObjectArrayFIFOQueue<>();

    private final GlMutableBuffer uploadBuffer;
    private final GlMutableBuffer uniformBuffer;

    private final ChunkDrawParamsVector uniformBufferBuilder;
    private final MemoryTracker memoryTracker = new MemoryTracker();

    public GL43ChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);

        this.renderPassManager = MultiTextureRenderPipeline.create();
        this.bufferManager = new ChunkRegionManager<>(this.memoryTracker);
        this.uploadBuffer = new GlMutableBuffer(GL15.GL_STREAM_COPY);
        this.uniformBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);

        this.uniformBufferBuilder = ChunkDrawParamsVector.create(2048);
    }

    @Override
    protected void modifyProgram(GlProgram.Builder builder, ChunkProgramComponentBuilder components,
                                 GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        components.texture = ChunkProgramMultiTexture::new;
    }

    @Override
    protected void addShaderConstants(ShaderConstants.Builder builder) {
        super.addShaderConstants(builder);

        builder.define("USE_MULTITEX");
    }

    @Override
    public void uploadChunks(Iterator<ChunkBuildResult<GL43GraphicsState>> queue) {
        this.setupUploadBatches(queue);

        GlMutableBuffer uploadBuffer = this.uploadBuffer;
        uploadBuffer.bind(GL15.GL_ARRAY_BUFFER);

        while (!this.pendingUploads.isEmpty()) {
            ChunkRegion<GL43GraphicsState> region = this.pendingUploads.dequeue();

            GlBufferArena arena = region.getBufferArena();
            arena.bind();

            ObjectArrayList<ChunkBuildResult<GL43GraphicsState>> uploadQueue = region.getUploadQueue();
            arena.ensureCapacity(getUploadQueuePayloadSize(uploadQueue));

            for (ChunkBuildResult<GL43GraphicsState> result : uploadQueue) {
                ChunkRenderContainer<GL43GraphicsState> render = result.render;
                ChunkRenderData data = result.data;

                for (BlockRenderPass pass : this.renderPassManager.getSortedPasses()) {
                    GL43GraphicsState graphics = render.getGraphicsState(pass);

                    // De-allocate the existing buffer arena for this render
                    // This will allow it to be cheaply re-allocated just below
                    if (graphics != null) {
                        graphics.delete();
                    }

                    ChunkMeshData meshData = data.getMesh(pass);

                    if (meshData != null) {
                        VertexData upload = meshData.takeVertexData();
                        uploadBuffer.upload(GL15.GL_ARRAY_BUFFER, upload);

                        GlBufferRegion segment = arena.upload(GL15.GL_ARRAY_BUFFER, 0, upload.buffer.capacity());

                        render.setGraphicsState(pass, new GL43GraphicsState(render, region, segment, meshData, this.vertexFormat));
                    } else {
                        render.setGraphicsState(pass, null);
                    }
                }

                render.setData(data);
            }

            arena.unbind();
            uploadQueue.clear();
        }

        uploadBuffer.invalidate(GL15.GL_ARRAY_BUFFER);
        uploadBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    @Override
    public void renderChunks(MatrixStack matrixStack, BlockRenderPass pass, ChunkRenderListIterator<GL43GraphicsState> renders, ChunkCameraContext camera) {
        this.beginRender(matrixStack, pass);

        this.bufferManager.cleanup();
        this.setupDrawBatches(renders, camera);

        GlVertexArray prevVao = null;

        this.uniformBuffer.bind(GL15.GL_ARRAY_BUFFER);
        this.uniformBuffer.upload(GL15.GL_ARRAY_BUFFER, this.uniformBufferBuilder.getBuffer());

        while (!this.pendingBatches.isEmpty()) {
            ChunkRegion<?> region = this.pendingBatches.dequeue();

            GlVertexArray vao = region.getVertexArray();
            vao.bind();

            // Check if the VAO's bindings need to be updated
            // This happens whenever the backing buffer object for the arena changes
            if (region.isDirty()) {
                this.setupArrayBufferState(region.getBufferArena());
                this.setupUniformBufferState();

                region.markClean();
            }

            ChunkDrawCallBatcher batch = region.getDrawBatcher();
            batch.end();

            batch.upload();
            GlFunctions.INDIRECT_DRAW.glMultiDrawArraysIndirect(GL11.GL_QUADS, 0, batch.getCount(), 0 /* tightly packed */);

            prevVao = vao;
        }

        if (prevVao != null) {
            prevVao.unbind();
        }

        this.uniformBuffer.unbind(GL15.GL_ARRAY_BUFFER);

        this.endRender(matrixStack);
    }

    private void setupArrayBufferState(GlBufferArena arena) {
        GlBuffer vbo = arena.getBuffer();
        vbo.bind(GL15.GL_ARRAY_BUFFER);

        this.vertexFormat.bindVertexAttributes();
        this.vertexFormat.enableVertexAttributes();
    }

    private void setupUniformBufferState() {
        this.uniformBuffer.bind(GL15.GL_ARRAY_BUFFER);

        int index = this.activeProgram.getModelOffsetAttributeLocation();

        // Bind a packed array buffer containing model transformations for each chunk. This provides an alternative to
        // gl_DrawID in OpenGL 4.6 and should work on more hardware.
        //
        // The base instance value assigned to each indirect draw call decides the starting offset of vertices in a
        // "instanced" vertex attributes. By specifying a divisor of 1, an instanced vertex attribute will always point
        // to the starting element (the base instance) in the array buffer, thereby acting as a per-draw constant.
        //
        // This provides performance as good as a uniform array without the need to split draw call batches due to
        // uniform array size limits. All uniforms can be uploaded and bound in a single call.
        GL20.glVertexAttribPointer(index, 4, GL11.GL_FLOAT, false, 0, 0L);
        GlFunctions.INSTANCED_ARRAY.glVertexAttribDivisor(index, 1);

        GL20.glEnableVertexAttribArray(index);
    }

    private void setupUploadBatches(Iterator<ChunkBuildResult<GL43GraphicsState>> renders) {
        while (renders.hasNext()) {
            ChunkBuildResult<GL43GraphicsState> result = renders.next();
            ChunkRenderContainer<GL43GraphicsState> render = result.render;

            ChunkRegion<GL43GraphicsState> region = this.bufferManager.getRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());

            if (region == null) {
                if (result.data.getMeshSize() <= 0) {
                    render.setData(result.data);
                    continue;
                }

                region = this.bufferManager.getOrCreateRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());
            }

            ObjectArrayList<ChunkBuildResult<GL43GraphicsState>> uploadQueue = region.getUploadQueue();

            if (uploadQueue.isEmpty()) {
                this.pendingUploads.enqueue(region);
            }

            uploadQueue.add(result);
        }
    }

    private void setupDrawBatches(ChunkRenderListIterator<GL43GraphicsState> it, ChunkCameraContext camera) {
        this.uniformBufferBuilder.begin();

        int drawCount = 0;

        while (it.hasNext()) {
            GL43GraphicsState state = it.getGraphicsState();
            int visible = it.getVisibleFaces();

            int index = drawCount++;
            float x = camera.getChunkModelOffset(state.getX(), camera.blockOriginX, camera.originX);
            float y = camera.getChunkModelOffset(state.getY(), camera.blockOriginY, camera.originY);
            float z = camera.getChunkModelOffset(state.getZ(), camera.blockOriginZ, camera.originZ);

            this.uniformBufferBuilder.pushChunkDrawParams(x, y, z);

            ChunkRegion<GL43GraphicsState> region = state.getRegion();
            ChunkDrawCallBatcher batch = region.getDrawBatcher();

            if (!batch.isBuilding()) {
                batch.begin();

                this.pendingBatches.enqueue(region);
            }

            int mask = 0b1;

            for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
                if ((visible & mask) != 0) {
                    long part = state.getModelPart(i);

                    batch.addIndirectDrawCall(BufferSlice.unpackStart(part), BufferSlice.unpackLength(part), index, 1);
                }

                mask <<= 1;
            }

            it.advance();
        }

        this.uniformBufferBuilder.end();
    }

    private static int getUploadQueuePayloadSize(List<ChunkBuildResult<GL43GraphicsState>> queue) {
        int size = 0;

        for (ChunkBuildResult<GL43GraphicsState> result : queue) {
            size += result.data.getMeshSize();
        }

        return size;
    }

    @Override
    public void delete() {
        super.delete();

        this.bufferManager.delete();
        this.uploadBuffer.delete();
    }

    @Override
    public Class<GL43GraphicsState> getGraphicsStateType() {
        return GL43GraphicsState.class;
    }

    @Override
    public BlockRenderPassManager getRenderPassManager() {
        return this.renderPassManager;
    }

    public static boolean isSupported(boolean disableBlacklist) {
        if (!disableBlacklist) {
            // Blacklist proprietary AMD drivers. See: http://ati.cchtml.com/show_bug.cgi?id=1273
            // The open-source Mesa drivers identify using the vendor string "X.org", so this should still allow those
            // drivers to be used.
            if (GlVendorUtil.matches("ATI Technologies Inc.")) {
                return false;
            }
        }

        return GlFunctions.isVertexArraySupported() &&
                GlFunctions.isBufferCopySupported() &&
                GlFunctions.isIndirectMultiDrawSupported() &&
                GlFunctions.isInstancedArraySupported();
    }

    @Override
    public String getRendererName() {
        return "Multidraw (GL 4.3)";
    }

    @Override
    public MemoryTracker getMemoryTracker() {
        return this.memoryTracker;
    }

    @Override
    public List<String> getDebugStrings() {
        return Lists.newArrayList("Allocated Regions: " + this.bufferManager.getAllocatedRegionCount());
    }
}