package me.jellysquid.mods.sodium.client.model.quad.sink;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorU8;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;

/**
 * A fallback implementation of {@link ModelQuadSink} for when we're writing into an arbitrary {@link BufferBuilder}.
 * This implementation is considerably slower than other sinks as it must perform many matrix transformations for every
 * vertex and unpack values as assumptions can't be made about what the backing buffer type is.
 */
public class FallbackQuadSink implements ModelQuadSink, ModelQuadSinkDelegate {
    private final IVertexBuilder consumer;

    // Hoisted matrices to avoid lookups in peeking
    private final Matrix4f modelMatrix;
    private final Matrix3f normalMatrix;

    // Cached vectors to avoid allocations
    private final Vector4f vector;
    private final Vector3f normal;

    public FallbackQuadSink(IVertexBuilder consumer, MatrixStack matrixStack) {
        this.consumer = consumer;
        this.modelMatrix = matrixStack.getLast().getMatrix();
        this.normalMatrix = matrixStack.getLast().getNormal();
        this.vector = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        this.normal = new Vector3f(0.0f, 0.0f, 0.0f);
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        Vector4f posVec = this.vector;
        Vector3f normVec = this.normal;

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            posVec.set(x, y, z, 1.0F);
            posVec.transform(this.modelMatrix);

            int color = quad.getColor(i);

            float r = ColorU8.normalize(ColorABGR.unpackRed(color));
            float g = ColorU8.normalize(ColorABGR.unpackGreen(color));
            float b = ColorU8.normalize(ColorABGR.unpackBlue(color));
            float a = ColorU8.normalize(ColorABGR.unpackAlpha(color));

            float u = quad.getTexU(i);
            float v = quad.getTexV(i);

            int light = quad.getLight(i);
            int norm = quad.getNormal(i);

            float normX = Norm3b.unpackX(norm);
            float normY = Norm3b.unpackY(norm);
            float normZ = Norm3b.unpackZ(norm);

            normVec.set(normX, normY, normZ);
            normVec.transform(this.normalMatrix);

            this.consumer.addVertex(posVec.getX(), posVec.getY(), posVec.getZ(), r, g, b, a, u, v, OverlayTexture.NO_OVERLAY, light, normVec.getX(), normVec.getY(), normVec.getZ());
        }
    }

    @Override
    public ModelQuadSink get(ModelQuadFacing facing) {
        return this;
    }
}
