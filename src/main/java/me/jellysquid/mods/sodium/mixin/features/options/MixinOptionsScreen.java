package me.jellysquid.mods.sodium.mixin.features.options;

import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class MixinOptionsScreen extends Screen {
    protected MixinOptionsScreen(ITextComponent title) {
        super(title);
    }

//    @Dynamic
//    @Inject(method = "func_213059_g(Lnet/minecraft/client/gui/widget/button/Button;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void open(Button widget, CallbackInfo ci) {
        this.minecraft.displayGuiScreen(new SodiumOptionsGUI(this));

        ci.cancel();
    }
}
