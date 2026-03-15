package de.weinschenk.starlink.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.menu.DistillationChamberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DistillationChamberScreen extends AbstractContainerScreen<DistillationChamberMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/gui/distillation_chamber.png");

    // GUI-Größe (Texturgröße 176x166 wie Standard-Container)
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 166;

    // Positionen der Fortschritts-/Brennanzeige in der Textur
    private static final int FLAME_U    = 176;
    private static final int FLAME_V    = 0;
    private static final int FLAME_W    = 14;
    private static final int FLAME_H    = 14;
    private static final int ARROW_U    = 176;
    private static final int ARROW_V    = 14;
    private static final int ARROW_W    = 24;
    private static final int ARROW_H    = 17;

    public DistillationChamberScreen(DistillationChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = leftPos;
        int y = topPos;

        // Hintergrund
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Flammen-Anzeige (Brennstoff)
        if (menu.isActive()) {
            int flameHeight = menu.getBurnScaled(FLAME_H);
            graphics.blit(TEXTURE, x + 56, y + 36 + (FLAME_H - flameHeight),
                    FLAME_U, FLAME_V + (FLAME_H - flameHeight),
                    FLAME_W, flameHeight);
        }

        // Fortschrittspfeil
        int arrowWidth = menu.getProgressScaled(ARROW_W);
        graphics.blit(TEXTURE, x + 79, y + 34, ARROW_U, ARROW_V, arrowWidth, ARROW_H);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
