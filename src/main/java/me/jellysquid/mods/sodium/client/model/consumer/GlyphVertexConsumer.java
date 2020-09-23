package me.jellysquid.mods.sodium.client.model.consumer;

import net.minecraft.util.math.vector.Matrix4f;

public interface GlyphVertexConsumer {
    void vertexGlyph(Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light);
}
