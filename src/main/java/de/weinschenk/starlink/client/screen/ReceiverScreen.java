package de.weinschenk.starlink.client.screen;

import de.weinschenk.starlink.menu.ReceiverMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ReceiverScreen extends AbstractContainerScreen<ReceiverMenu> {

    public ReceiverScreen(ReceiverMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 100;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A1A2E);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF252540);

        // Status indicator square (x=12, y=18, 30x30)
        int indicatorColor = menu.isActive() ? 0xFF00AA44 : 0xFF880000;
        g.fill(x + 12, y + 18, x + 42, y + 48, indicatorColor);
        int innerColor = menu.isActive() ? 0xFF44FF88 : 0xFFCC2222;
        g.fill(x + 16, y + 22, x + 38, y + 44, innerColor);

        // Satellite bar (x=50, y=18, w=16, h=46)
        int bx = x + 50, by = y + 18, bw = 16, bh = 46;
        g.fill(bx, by, bx + bw, by + bh, 0xFF111111);
        int satFill = (int)(Math.min(menu.getSatCount(), 20) / 20f * bh);
        satFill = Math.max(0, Math.min(satFill, bh));
        g.fill(bx, by + bh - satFill, bx + bw, by + bh, 0xFF00AAFF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 5, 0xFFEECC, false);

        Component statusText = menu.isActive()
                ? Component.literal("\u00a7aSignal aktiv")
                : Component.literal("\u00a7cKein Signal");
        g.drawString(font, statusText, 12, 52, 0xFFFFFF, false);

        g.drawString(font, "Satelliten: " + menu.getSatCount(), 70, 18, 0x88CCFF, false);
        g.drawString(font, "SAT", 51, 66, 0x00AAFF, false);
    }
}
