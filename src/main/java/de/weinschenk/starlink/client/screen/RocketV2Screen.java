package de.weinschenk.starlink.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.menu.RocketV2Menu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RocketV2Screen extends AbstractContainerScreen<RocketV2Menu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/gui/rocket_v2.png");

    public RocketV2Screen(RocketV2Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = 90; // 12px above player inventory at y=102
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        // Satellitenanzahl anzeigen
        int satCount = menu.getSatelliteCount();
        graphics.drawString(font,
                Component.literal("Satelliten: " + satCount + " / 20"),
                x + 8, y + 6, 0x404040, false);
    }
}
