package de.weinschenk.starlink.client.screen;

import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import de.weinschenk.starlink.menu.LaunchControllerMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LaunchControllerScreen extends AbstractContainerScreen<LaunchControllerMenu> {

    // GUI-Abmessungen (kein Inventar-Bereich nötig — kein Item-Slot)
    private static final int GUI_W = 176;
    private static final int GUI_H = 80;

    private Button btnAxis;
    private Button btnLaunch;

    public LaunchControllerScreen(LaunchControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
        // Inventar-Label ausblenden
        this.inventoryLabelY = GUI_H + 10;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        // Achsen-Umschalt-Schaltfläche
        btnAxis = addRenderableWidget(Button.builder(
                        axisLabel(), btn -> sendMenuButton(LaunchControllerMenu.BTN_TOGGLE_AXIS))
                .bounds(x + 10, y + 34, 74, 20).build());

        // Start-Schaltfläche
        btnLaunch = addRenderableWidget(Button.builder(
                        Component.literal("STARTEN"), btn -> sendMenuButton(LaunchControllerMenu.BTN_LAUNCH))
                .bounds(x + 92, y + 34, 74, 20).build());
    }

    private Component axisLabel() {
        return Component.literal("Achse: " + (menu.isAxisX() ? "X  ►" : "Z  ►"));
    }

    /** Sendet einen Button-Klick an den Server (analog zu Vanilla clickMenuButton). */
    private void sendMenuButton(int buttonId) {
        assert this.minecraft != null;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Dunkler Hintergrund
        graphics.fill(x,     y,     x + GUI_W,     y + GUI_H,     0xFF2A2A2A);
        graphics.fill(x + 1, y + 1, x + GUI_W - 1, y + GUI_H - 1, 0xFF1A1A1A);

        // Aktive Achse grün hervorheben
        if (menu.isAxisX()) {
            graphics.fill(x + 10, y + 34, x + 84, y + 54, 0x6000CC00);
        } else {
            graphics.fill(x + 92, y + 34, x + 166, y + 54, 0x6000CC00);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Achsen-Button-Label aktuell halten
        btnAxis.setMessage(axisLabel());

        super.render(graphics, mouseX, mouseY, partialTick);

        int x = leftPos;
        int y = topPos;

        // Titel
        graphics.drawString(font, Component.literal("Startkontrolle"),
                x + 8, y + 8, 0xFFFFFF, false);

        // Treibstoff-Anzeige
        int fuel = menu.getFuel();
        String fuelText = "Treibstoff: " + fuel + " / " + LaunchControllerBlockEntity.FUEL_REQUIRED;
        graphics.drawString(font, Component.literal(fuelText), x + 8, y + 22, 0xAAAAAA, false);

        renderTooltip(graphics, mouseX, mouseY);
    }

    // Kein Spieler-Inventar-Rendering (imageHeight zu klein für die üblichen Slot-Zeilen)
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // absichtlich leer — kein title-/inventory-Label
    }
}
