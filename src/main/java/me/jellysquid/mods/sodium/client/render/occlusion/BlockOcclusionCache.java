package me.jellysquid.mods.sodium.client.render.occlusion;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

public class BlockOcclusionCache {
    private static final byte UNCACHED_VALUE = (byte) 127;

    private final Object2ByteLinkedOpenHashMap<CachedOcclusionShapeTest> map;
    private final CachedOcclusionShapeTest cachedTest = new CachedOcclusionShapeTest();
    private final BlockPos.Mutable cpos = new BlockPos.Mutable();

    public BlockOcclusionCache() {
        this.map = new Object2ByteLinkedOpenHashMap<>(2048, 0.5F);
        this.map.defaultReturnValue(UNCACHED_VALUE);
    }

    /**
     * @param selfState The state of the block in the world
     * @param view The world view for this render context
     * @param pos The position of the block
     * @param facing The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockState selfState, IBlockReader view, BlockPos pos, Direction facing) {
        BlockPos.Mutable adjPos = this.cpos;
        adjPos.setPos(pos.getX() + facing.getXOffset(), pos.getY() + facing.getYOffset(), pos.getZ() + facing.getZOffset());

        BlockState adjState = view.getBlockState(adjPos);

        if (selfState.isSideInvisible(adjState, facing)) {
            return false;
        } else if (adjState.isSolid()) {
            VoxelShape selfShape = selfState.getFaceOcclusionShape(view, pos, facing);
            VoxelShape adjShape = adjState.getFaceOcclusionShape(view, adjPos, facing.getOpposite());

            if (selfShape == VoxelShapes.fullCube() && adjShape == VoxelShapes.fullCube()) {
                return false;
            }

            return this.calculate(selfShape, adjShape);
        } else {
            return true;
        }
    }

    private boolean calculate(VoxelShape selfShape, VoxelShape adjShape) {
        CachedOcclusionShapeTest cache = this.cachedTest;
        cache.a = selfShape;
        cache.b = adjShape;
        cache.updateHash();

        byte cached = this.map.getByte(cache);

        if (cached != UNCACHED_VALUE) {
            return cached == 1;
        }

        boolean ret = VoxelShapes.compare(selfShape, adjShape, IBooleanFunction.ONLY_FIRST);

        this.map.put(cache.copy(), (byte) (ret ? 1 : 0));

        if (this.map.size() > 2048) {
            this.map.removeLastByte();
        }

        return ret;
    }

    private static final class CachedOcclusionShapeTest {
        private VoxelShape a, b;
        private int hashCode;

        private CachedOcclusionShapeTest() {

        }

        private CachedOcclusionShapeTest(VoxelShape a, VoxelShape b, int hashCode) {
            this.a = a;
            this.b = b;
            this.hashCode = hashCode;
        }

        public void updateHash() {
            int result = System.identityHashCode(this.a);
            result = 31 * result + System.identityHashCode(this.b);

            this.hashCode = result;
        }

        public CachedOcclusionShapeTest copy() {
            return new CachedOcclusionShapeTest(this.a, this.b, this.hashCode);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CachedOcclusionShapeTest) {
                CachedOcclusionShapeTest that = (CachedOcclusionShapeTest) o;

                return this.a == that.a &&
                        this.b == that.b;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
