package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.IRenderable;

public class FlatButtonWidget extends AbstractWidget implements IRenderable {
    private final Dim2i dim;
    private final String label;
    private final Runnable action;

    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;

    public FlatButtonWidget(Dim2i dim, String label, Runnable action) {
        this.dim = dim;
        this.label = label;
        this.action = action;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        boolean hovered = this.dim.containsCursor(mouseX, mouseY);

        int backgroundColor = this.enabled ? (hovered ? 0xE0000000 : 0x90000000) : 0x60000000;
        int textColor = this.enabled ? 0xFFFFFFFF : 0x90FFFFFF;

        int strWidth = this.font.getStringWidth(this.label);

        this.drawRect(this.dim.getOriginX(), this.dim.getOriginY(), this.dim.getLimitX(), this.dim.getLimitY(), backgroundColor);
        this.drawString(matrixStack, this.label, this.dim.getCenterX() - (strWidth / 2), this.dim.getCenterY() - 4, textColor);

        if (this.enabled && this.selected) {
            this.drawRect(this.dim.getOriginX(), this.dim.getLimitY() - 1, this.dim.getLimitX(), this.dim.getLimitY(), 0xFF94E4D3);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled) {
            return false;
        }

        if (button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
            this.action.run();
            this.playClickSound();

            return true;
        }

        return false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visibel) {
        this.visible = visibel;
    }
}
