package me.jellysquid.mods.sodium.mixin.core.matrix;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sun.misc.Unsafe;

import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;

@Mixin(Matrix4f.class)
public class MixinMatrix4f implements Matrix4fExtended {
    @Shadow
    protected float m00;

    @Shadow
    protected float m01;

    @Shadow
    protected float m02;

    @Shadow
    protected float m03;

    @Shadow
    protected float m10;

    @Shadow
    protected float m11;

    @Shadow
    protected float m12;

    @Shadow
    protected float m13;

    @Shadow
    protected float m20;

    @Shadow
    protected float m21;

    @Shadow
    protected float m22;

    @Shadow
    protected float m23;

    @Shadow
    protected float m30;

    @Shadow
    protected float m31;

    @Shadow
    protected float m32;

    @Shadow
    protected float m33;

    @Override
    public void translate(float x, float y, float z) {
        this.m03 = this.m00 * x + this.m01 * y + this.m02 * z + this.m03;
        this.m13 = this.m10 * x + this.m11 * y + this.m12 * z + this.m13;
        this.m23 = this.m20 * x + this.m21 * y + this.m22 * z + this.m23;
        this.m33 = this.m30 * x + this.m31 * y + this.m32 * z + this.m33;
    }

    @Override
    public float transformVecX(float x, float y, float z) {
        return (this.m00 * x) + (this.m01 * y) + (this.m02 * z) + (this.m03 * 1.0f);
    }

    @Override
    public float transformVecY(float x, float y, float z) {
        return (this.m10 * x) + (this.m11 * y) + (this.m12 * z) + (this.m13 * 1.0f);
    }

    @Override
    public float transformVecZ(float x, float y, float z) {
        return (this.m20 * x) + (this.m21 * y) + (this.m22 * z) + (this.m23 * 1.0f);
    }

    @Override
    public void rotate(Quaternion quaternion) {
        boolean x = quaternion.getX() != 0.0F;
        boolean y = quaternion.getY() != 0.0F;
        boolean z = quaternion.getZ() != 0.0F;

        // Try to determine if this is a simple rotation on one axis component only
        if (x) {
            if (!y && !z) {
                this.rotateX(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (y) {
            if (!z) {
                this.rotateY(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (z) {
            this.rotateZ(quaternion);
        }
    }

    private void rotateX(Quaternion quaternion) {
        float x = quaternion.getX();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;
        float ta11 = 1.0F - xx;
        float ta22 = 1.0F - xx;

        float xw = x * w;

        float ta21 = 2.0F * xw;
        float ta12 = 2.0F * -xw;

        float a01 = this.m01 * ta11 + this.m02 * ta21;
        float a02 = this.m01 * ta12 + this.m02 * ta22;
        float a11 = this.m11 * ta11 + this.m12 * ta21;
        float a12 = this.m11 * ta12 + this.m12 * ta22;
        float a21 = this.m21 * ta11 + this.m22 * ta21;
        float a22 = this.m21 * ta12 + this.m22 * ta22;
        float a31 = this.m31 * ta11 + this.m32 * ta21;
        float a32 = this.m31 * ta12 + this.m32 * ta22;

        this.m01 = a01;
        this.m02 = a02;
        this.m11 = a11;
        this.m12 = a12;
        this.m21 = a21;
        this.m22 = a22;
        this.m31 = a31;
        this.m32 = a32;
    }

    private void rotateY(Quaternion quaternion) {
        float y = quaternion.getY();
        float w = quaternion.getW();

        float yy = 2.0F * y * y;
        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;
        float yw = y * w;
        float ta20 = 2.0F * -yw;
        float ta02 = 2.0F * yw;

        float a00 = this.m00 * ta00 + this.m02 * ta20;
        float a02 = this.m00 * ta02 + this.m02 * ta22;
        float a10 = this.m10 * ta00 + this.m12 * ta20;
        float a12 = this.m10 * ta02 + this.m12 * ta22;
        float a20 = this.m20 * ta00 + this.m22 * ta20;
        float a22 = this.m20 * ta02 + this.m22 * ta22;
        float a30 = this.m30 * ta00 + this.m32 * ta20;
        float a32 = this.m30 * ta02 + this.m32 * ta22;

        this.m00 = a00;
        this.m02 = a02;
        this.m10 = a10;
        this.m12 = a12;
        this.m20 = a20;
        this.m22 = a22;
        this.m30 = a30;
        this.m32 = a32;
    }

    private void rotateZ(Quaternion quaternion) {
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float zz = 2.0F * z * z;
        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;
        float zw = z * w;
        float ta10 = 2.0F * zw;
        float ta01 = 2.0F * -zw;

        float a00 = this.m00 * ta00 + this.m01 * ta10;
        float a01 = this.m00 * ta01 + this.m01 * ta11;
        float a10 = this.m10 * ta00 + this.m11 * ta10;
        float a11 = this.m10 * ta01 + this.m11 * ta11;
        float a20 = this.m20 * ta00 + this.m21 * ta10;
        float a21 = this.m20 * ta01 + this.m21 * ta11;
        float a30 = this.m30 * ta00 + this.m31 * ta10;
        float a31 = this.m30 * ta01 + this.m31 * ta11;

        this.m00 = a00;
        this.m01 = a01;
        this.m10 = a10;
        this.m11 = a11;
        this.m20 = a20;
        this.m21 = a21;
        this.m30 = a30;
        this.m31 = a31;
    }

    private void rotateXYZ(Quaternion quaternion) {
        float x = quaternion.getX();
        float y = quaternion.getY();
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;
        float yy = 2.0F * y * y;
        float zz = 2.0F * z * z;
        float ta00 = 1.0F - yy - zz;
        float ta11 = 1.0F - zz - xx;
        float ta22 = 1.0F - xx - yy;
        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;
        float ta10 = 2.0F * (xy + zw);
        float ta01 = 2.0F * (xy - zw);
        float ta20 = 2.0F * (zx - yw);
        float ta02 = 2.0F * (zx + yw);
        float ta21 = 2.0F * (yz + xw);
        float ta12 = 2.0F * (yz - xw);

        float a00 = this.m00 * ta00 + this.m01 * ta10 + this.m02 * ta20;
        float a01 = this.m00 * ta01 + this.m01 * ta11 + this.m02 * ta21;
        float a02 = this.m00 * ta02 + this.m01 * ta12 + this.m02 * ta22;
        float a10 = this.m10 * ta00 + this.m11 * ta10 + this.m12 * ta20;
        float a11 = this.m10 * ta01 + this.m11 * ta11 + this.m12 * ta21;
        float a12 = this.m10 * ta02 + this.m11 * ta12 + this.m12 * ta22;
        float a20 = this.m20 * ta00 + this.m21 * ta10 + this.m22 * ta20;
        float a21 = this.m20 * ta01 + this.m21 * ta11 + this.m22 * ta21;
        float a22 = this.m20 * ta02 + this.m21 * ta12 + this.m22 * ta22;
        float a30 = this.m30 * ta00 + this.m31 * ta10 + this.m32 * ta20;
        float a31 = this.m30 * ta01 + this.m31 * ta11 + this.m32 * ta21;
        float a32 = this.m30 * ta02 + this.m31 * ta12 + this.m32 * ta22;

        this.m00 = a00;
        this.m01 = a01;
        this.m02 = a02;
        this.m10 = a10;
        this.m11 = a11;
        this.m12 = a12;
        this.m20 = a20;
        this.m21 = a21;
        this.m22 = a22;
        this.m30 = a30;
        this.m31 = a31;
        this.m32 = a32;
    }

    /**
     * @reason Optimize
     * @author JellySquid
     */
    @OnlyIn(Dist.CLIENT)
    @Overwrite
    public void write(FloatBuffer buf) {
        if (buf.remaining() < 16) {
            throw new BufferUnderflowException();
        }

        if (UnsafeUtil.isAvailable()) {
            this.writeToBufferUnsafe(buf);
        } else {
            this.writeToBufferSafe(buf);
        }
    }

    private void writeToBufferUnsafe(FloatBuffer buf) {
        long addr = MemoryUtil.memAddress(buf);

        Unsafe unsafe = UnsafeUtil.instance();
        unsafe.putFloat(addr + 0, this.m00);
        unsafe.putFloat(addr + 4, this.m10);
        unsafe.putFloat(addr + 8, this.m20);
        unsafe.putFloat(addr + 12, this.m30);
        unsafe.putFloat(addr + 16, this.m01);
        unsafe.putFloat(addr + 20, this.m11);
        unsafe.putFloat(addr + 24, this.m21);
        unsafe.putFloat(addr + 28, this.m31);
        unsafe.putFloat(addr + 32, this.m02);
        unsafe.putFloat(addr + 36, this.m12);
        unsafe.putFloat(addr + 40, this.m22);
        unsafe.putFloat(addr + 44, this.m32);
        unsafe.putFloat(addr + 48, this.m03);
        unsafe.putFloat(addr + 52, this.m13);
        unsafe.putFloat(addr + 56, this.m23);
        unsafe.putFloat(addr + 60, this.m33);
    }

    private void writeToBufferSafe(FloatBuffer buf) {
        buf.put(0, this.m00);
        buf.put(1, this.m10);
        buf.put(2, this.m20);
        buf.put(3, this.m30);
        buf.put(4, this.m01);
        buf.put(5, this.m11);
        buf.put(6, this.m21);
        buf.put(7, this.m31);
        buf.put(8, this.m02);
        buf.put(9, this.m12);
        buf.put(10, this.m22);
        buf.put(11, this.m32);
        buf.put(12, this.m03);
        buf.put(13, this.m13);
        buf.put(14, this.m23);
        buf.put(15, this.m33);
    }
}
