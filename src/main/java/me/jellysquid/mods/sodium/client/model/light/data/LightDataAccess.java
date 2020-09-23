package me.jellysquid.mods.sodium.client.model.light.data;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

/**
 * The light data cache is used to make accessing the light data and occlusion properties of blocks cheaper. The data
 * for each block is stored as a long integer with packed fields in order to work around the lack of value types in Java.
 *
 * This code is not very pretty, but it does perform significantly faster than the vanilla implementation and has
 * good cache locality.
 *
 * Each long integer contains the following fields:
 * - OP: Block opacity test, true if opaque
 * - FO: Full block opaque test, true if opaque
 * - AO: Ambient occlusion, floating point value in the range of 0.0..1.0 encoded as an 12-bit unsigned integer
 * - LM: Light map texture coordinates, two packed UV shorts in an integer
 *
 * You can use the various static pack/unpack methods to extract these values in a usable format.
 */
public abstract class LightDataAccess {
    protected static final FluidState EMPTY_FLUID_STATE = Fluids.EMPTY.getDefaultState();

    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    protected IBlockDisplayReader world;

    public long get(int x, int y, int z, Direction d1, Direction d2) {
        return this.get(x + d1.getXOffset() + d2.getXOffset(),
                y + d1.getYOffset() + d2.getYOffset(),
                z + d1.getZOffset() + d2.getZOffset());
    }

    public long get(int x, int y, int z, Direction dir) {
        return this.get(x + dir.getXOffset(),
                y + dir.getYOffset(),
                z + dir.getZOffset());
    }

    public long get(BlockPos pos, Direction dir) {
        return this.get(pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    public long get(BlockPos pos) {
        return this.get(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Returns the light data for the block at the given position. The property fields can then be accessed using
     * the various unpack methods below.
     */
    public abstract long get(int x, int y, int z);

    protected long compute(int x, int y, int z) {
        BlockPos pos = this.pos.setPos(x, y, z);
        IBlockDisplayReader world = this.world;

        BlockState state = world.getBlockState(pos);

        float ao;

        if (state.getLightValue() == 0) {
            ao = state.getAmbientOcclusionLightValue(world, pos);
        } else {
            ao = 1.0f;
        }

        // FIX: Fluids are always non-translucent despite blocking light, so we need a special check here in order to
        // solve lighting issues underwater.
        boolean op = state.getFluidState() != EMPTY_FLUID_STATE || state.getOpacity(world, pos) == 0;
        boolean fo = state.isOpaqueCube(world, pos);

        // OPTIMIZE: Do not calculate lightmap data if the block is full and opaque
        int lm = fo ? 0 : WorldRenderer.getPackedLightmapCoords(world, state, pos);

        return packAO(ao) | packLM(lm) | packOP(op) | packFO(fo) | (1L << 60);
    }

    public static long packOP(boolean opaque) {
        return (opaque ? 1L : 0L) << 56;
    }

    public static boolean unpackOP(long word) {
        return ((word >>> 56) & 0b1) != 0;
    }

    public static long packFO(boolean opaque) {
        return (opaque ? 1L : 0L) << 57;
    }

    public static boolean unpackFO(long word) {
        return ((word >>> 57) & 0b1) != 0;
    }

    public static long packLM(int lm) {
        return (long) lm & 0xFFFFFFFFL;
    }

    public static int unpackLM(long word) {
        return (int) (word & 0xFFFFFFFFL);
    }

    public static long packAO(float ao) {
        int aoi = (int) (ao * 4096.0f);
        return ((long) aoi & 0xFFFFL) << 32;
    }

    public static float unpackAO(long word) {
        int aoi = (int) (word >>> 32 & 0xFFFFL);
        return aoi / 4096.0f;
    }

    public IBlockDisplayReader getWorld() {
        return this.world;
    }
}