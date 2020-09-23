package me.jellysquid.mods.sodium.mixin.features.item;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.consumer.QuadVertexConsumer;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.client.world.biome.ItemColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    private final XoRoShiRoRandom random = new XoRoShiRoRandom();

    @Shadow
    @Final
    private ItemColors itemColors;

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    public void renderModel(IBakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrices, IVertexBuilder vertices) {
        XoRoShiRoRandom random = this.random;

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = model.getQuads(null, direction, random.setSeedAndReturn(42L));

            if (!quads.isEmpty()) {
                this.renderQuads(matrices, vertices, quads, stack, light, overlay);
            }
        }

        List<BakedQuad> quads = model.getQuads(null, null, random.setSeedAndReturn(42L));

        if (!quads.isEmpty()) {
            this.renderQuads(matrices, vertices, quads, stack, light, overlay);
        }
    }

    /**
     * @reason Use vertex building intrinsics
     * @author JellySquid
     */
    @Overwrite
    public void renderQuads(MatrixStack matrices, IVertexBuilder vertices, List<BakedQuad> quads, ItemStack stack, int light, int overlay) {
        MatrixStack.Entry entry = matrices.getLast();

        QuadVertexConsumer consumer = (QuadVertexConsumer) vertices;
        IItemColor colorProvider = null;

        for (BakedQuad bakedQuad : quads) {
            int color = 0xFFFFFFFF;

            if (!stack.isEmpty() && bakedQuad.hasTintIndex()) {
                if (colorProvider == null) {
                    colorProvider = ((ItemColorsExtended) this.itemColors).getColorProvider(stack);
                }

                color = ColorARGB.toABGR((colorProvider.getColor(stack, bakedQuad.getTintIndex())), 255);
            }

            ModelQuadView quad = ((ModelQuadView) bakedQuad);

            for (int i = 0; i < 4; i++) {
                consumer.vertexQuad(entry, quad.getX(i), quad.getY(i), quad.getZ(i), color, quad.getTexU(i), quad.getTexV(i),
                        light, overlay, ModelQuadUtil.getFacingNormal(bakedQuad.getFace()));
            }

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
