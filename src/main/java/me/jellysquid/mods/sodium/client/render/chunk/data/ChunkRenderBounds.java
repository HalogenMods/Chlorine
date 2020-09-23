package me.jellysquid.mods.sodium.client.render.chunk.data;


import net.minecraft.util.math.SectionPos;

public class ChunkRenderBounds {
    public final float x1, y1, z1;
    public final float x2, y2, z2;

    public ChunkRenderBounds(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;

        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public ChunkRenderBounds(SectionPos origin) {
        this.x1 = origin.getWorldStartX();
        this.y1 = origin.getWorldStartY();
        this.z1 = origin.getWorldStartZ();

        this.x2 = origin.getWorldEndX() + 1;
        this.y2 = origin.getWorldEndY() + 1;
        this.z2 = origin.getWorldEndZ() + 1;
    }

    public static class Builder {
        private int x1 = Integer.MAX_VALUE, y1 = Integer.MAX_VALUE, z1 = Integer.MAX_VALUE;
        private int x2 = Integer.MIN_VALUE, y2 = Integer.MIN_VALUE, z2 = Integer.MIN_VALUE;

        private boolean empty = true;

        public void addBlock(int x, int y, int z) {
            if (x < this.x1) {
                this.x1 = x;
            }

            if (x > this.x2) {
                this.x2 = x;
            }

            if (y < this.y1) {
                this.y1 = y;
            }

            if (y > this.y2) {
                this.y2 = y;
            }

            if (z < this.z1) {
                this.z1 = z;
            }

            if (z > this.z2) {
                this.z2 = z;
            }

            this.empty = false;
        }

        public ChunkRenderBounds build(SectionPos origin) {
            if (this.empty) {
                return new ChunkRenderBounds(origin);
            }

            // Expand the bounding box by 8 blocks (half a chunk) in order to deal with diagonal surfaces
            return new ChunkRenderBounds(
                    Math.max(this.x1, origin.getWorldStartX()) - 8.0f,
                    Math.max(this.y1, origin.getWorldStartY()) - 8.0f,
                    Math.max(this.z1, origin.getWorldStartZ()) - 8.0f,

                    Math.min(this.x2 + 1, origin.getWorldEndX()) + 8.0f,
                    Math.min(this.y2 + 1, origin.getWorldEndY()) + 8.0f,
                    Math.min(this.z2 + 1, origin.getWorldEndZ()) + 8.0f
            );
        }
    }
}
