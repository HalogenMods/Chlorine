package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.IModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Predicate;

@Mixin(MultipartBakedModel.class)
public class MixinMultipartBakedModel {
    private Map<BlockState, List<IBakedModel>> stateCacheFast;

    @Shadow
    @Final
    private List<Pair<Predicate<BlockState>, IBakedModel>> selectors;


    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(List<Pair<Predicate<BlockState>, IBakedModel>> components, CallbackInfo ci) {
        this.stateCacheFast = new IdentityHashMap<>();
    }

    /**
     * @author JellySquid
     * @reason Avoid expensive allocations and replace bitfield indirection
     */
    @Overwrite(remap = false)
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random, IModelData modelData) {
        if (state == null) {
            return Collections.emptyList();
        }

        List<IBakedModel> models = this.stateCacheFast.get(state);

        if (models == null) {
            models = new ArrayList<>(this.selectors.size());

            for (Pair<Predicate<BlockState>, IBakedModel> pair : this.selectors) {
                if ((pair.getLeft()).test(state)) {
                    models.add(pair.getRight());
                }
            }

            this.stateCacheFast.put(state, models);
        }

        List<BakedQuad> list = new ArrayList<>();

        long seed = random.nextLong();

        for (IBakedModel model : models) {
            random.setSeed(seed);

            list.addAll(model.getQuads(state, face, random));
        }

        return list;
    }

}
