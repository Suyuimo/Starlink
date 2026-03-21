package de.weinschenk.starlink.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.block.LaunchControllerV2BlockEntity;
import de.weinschenk.starlink.menu.LaunchControllerV2Menu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.gui.widget.ForgeSlider;

public class LaunchControllerV2Screen extends AbstractContainerScreen<LaunchControllerV2Menu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/gui/launch_controller_v2.png");

    private static final int MAX_ENERGY_KILO = LaunchControllerV2BlockEntity.MAX_ENERGY / 1000;
    private static final int COST_KILO       = LaunchControllerV2BlockEntity.LAUNCH_COST / 1000;

    private static final int[] ORBIT_LABEL_COLORS = {
        0x64B4FF, 0xFFB432, 0xB450FF, 0xFF5050,
        0x50FF50, 0xFFFF50, 0xFF96C8, 0x96FFE6
    };

    public LaunchControllerV2Screen(LaunchControllerV2Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        // Orbit-Auswahl: < Orbit X >
        addRenderableWidget(Button.builder(
                Component.literal("<"),
                btn -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        LaunchControllerV2Menu.BTN_ORBIT_PREV)
        ).bounds(x + 7, y + 54, 20, 16).build());

        addRenderableWidget(Button.builder(
                Component.literal(">"),
                btn -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        LaunchControllerV2Menu.BTN_ORBIT_NEXT)
        ).bounds(x + 63, y + 54, 20, 16).build());

        // Launch Button
        addRenderableWidget(Button.builder(
                Component.literal("§c§l🚀 START"),
                btn -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        LaunchControllerV2Menu.BTN_LAUNCH)
        ).bounds(x + 93, y + 54, 76, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Energie-Balken (grün, horizontal, 162px breit max)
        int energyKilo  = menu.getEnergyKilo();
        int barWidth    = (int) (162.0 * energyKilo / MAX_ENERGY_KILO);
        graphics.fill(x + 7, y + 20, x + 7 + barWidth, y + 30, 0xFF00AA00);
        graphics.fill(x + 7, y + 20, x + 169, y + 30, 0x3300AA00); // Hintergrund leicht sichtbar
        // Redraw filled part on top
        if (barWidth > 0)
            graphics.fill(x + 7, y + 20, x + 7 + barWidth, y + 30, 0xFF00CC44);

        // Kosten-Marker (rote Linie bei 50%)
        int costMarker = (int) (162.0 * COST_KILO / MAX_ENERGY_KILO);
        graphics.fill(x + 7 + costMarker, y + 18, x + 8 + costMarker, y + 32, 0xFFFF4444);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        int energyKilo = menu.getEnergyKilo();
        int satCount   = menu.getSatCount();
        int orbitId    = menu.getOrbitId();

        // Energie-Text
        graphics.drawString(font,
                "Energie: " + String.format("%,d", energyKilo * 1000L) + " / " +
                String.format("%,d", (long) MAX_ENERGY_KILO * 1000) + " FE",
                x + 7, y + 8, 0x404040, false);

        // Satelliten-Count
        graphics.drawString(font,
                "Satelliten: " + satCount + " / 20",
                x + 7, y + 34, 0x404040, false);

        // Orbit-Anzeige (zwischen den < >-Buttons)
        graphics.drawString(font,
                "Orbit " + orbitId,
                x + 30, y + 58, ORBIT_LABEL_COLORS[orbitId % ORBIT_LABEL_COLORS.length], false);

        // Kosten-Hinweis
        graphics.drawString(font,
                "Start kostet: 5.000.000 FE",
                x + 7, y + 74, 0x888888, false);
    }
}
