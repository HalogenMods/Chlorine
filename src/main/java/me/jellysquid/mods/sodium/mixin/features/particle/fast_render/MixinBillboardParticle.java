package me.jellysquid.mods.sodium.mixin.features.particle.fast_render;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.consumer.ParticleVertexConsumer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TexturedParticle.class)
public abstract class MixinBillboardParticle extends Particle {
    @Shadow
    public abstract float getScale(float tickDelta);

    @Shadow
    protected abstract float getMinU();

    @Shadow
    protected abstract float getMaxU();

    @Shadow
    protected abstract float getMinV();

    @Shadow
    protected abstract float getMaxV();

    protected MixinBillboardParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /**
     * @reason Optimize function
     * @author JellySquid
     */
    @Overwrite
    public void renderParticle(IVertexBuilder vertexConsumer, ActiveRenderInfo camera, float tickDelta) {
        Vector3d vec3d = camera.getProjectedView();

        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.posX) - vec3d.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.posY) - vec3d.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.posZ) - vec3d.getZ());

        Quaternion quaternion;

        if (this.particleAngle == 0.0F) {
            quaternion = camera.getRotation();
        } else {
            float angle = MathHelper.lerp(tickDelta, this.prevParticleAngle, this.particleAngle);

            quaternion = new Quaternion(camera.getRotation());
            quaternion.multiply(Vector3f.ZP.rotation(angle));
        }

        float size = this.getScale(tickDelta);
        int light = this.getBrightnessForRender(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        int color = ColorABGR.pack(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);

        ParticleVertexConsumer vertices = (ParticleVertexConsumer) vertexConsumer;

        addVertex(vertices, quaternion,-1.0F, -1.0F, x, y, z, maxU, maxV, color, light, size);
        addVertex(vertices, quaternion,-1.0F, 1.0F, x, y, z, maxU, minV, color, light, size);
        addVertex(vertices, quaternion,1.0F, 1.0F, x, y, z, minU, minV, color, light, size);
        addVertex(vertices, quaternion,1.0F, -1.0F, x, y, z, minU, maxV, color, light, size);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static void addVertex(ParticleVertexConsumer vertices, Quaternion rotation,
                           float x, float y, float posX, float posY, float posZ, float u, float v, int color, int light, float size) {
        // Quaternion q0 = new Quaternion(rotation);
        float q0x = rotation.getX();
        float q0y = rotation.getY();
        float q0z = rotation.getZ();
        float q0w = rotation.getW();

        // q0.hamiltonProduct(x, y, 0.0f, 0.0f)
        float q1x = (q0w * x) - (q0z * y);
        float q1y = (q0w * y) + (q0z * x);
        float q1w = (q0x * y) - (q0y * x);
        float q1z = -(q0x * x) - (q0y * y);

        // Quaternion q2 = new Quaternion(rotation);
        // q2.conjugate()
        float q2x = -q0x;
        float q2y = -q0y;
        float q2z = -q0z;
        float q2w = q0w;

        // q2.hamiltonProduct(q1)
        float q3x = q1z * q2x + q1x * q2w + q1y * q2z - q1w * q2y;
        float q3y = q1z * q2y - q1x * q2z + q1y * q2w + q1w * q2x;
        float q3z = q1z * q2z + q1x * q2y - q1y * q2x + q1w * q2w;

        // Vector3f f = new Vector3f(q2.getX(), q2.getY(), q2.getZ())
        // f.multiply(size)
        // f.add(pos)
        float fx = (q3x * size) + posX;
        float fy = (q3y * size) + posY;
        float fz = (q3z * size) + posZ;

        vertices.vertexParticle(fx, fy, fz, u, v, color, light);
    }
}
