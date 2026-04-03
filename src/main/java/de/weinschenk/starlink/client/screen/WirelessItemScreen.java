package de.weinschenk.starlink.client.screen;

import de.weinschenk.starlink.menu.WirelessItemMenu;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.SetWirelessChannelPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;

public class WirelessItemScreen extends AbstractContainerScreen<WirelessItemMenu> {

    private EditBox channelBox;

    public WirelessItemScreen(WirelessItemMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth      = 176;
        this.imageHeight     = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        channelBox = new EditBox(font, leftPos + 100, topPos + 172, 68, 12, Component.literal("Kanal"));
        channelBox.setMaxLength(64);
        channelBox.setValue(menu.getInitialChannel());
        channelBox.setHint(Component.literal("öffentlich…"));
        addRenderableWidget(channelBox);
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

        // 3x3 handler slot backgrounds
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                g.fill(x + 7 + col * 18, y + 16 + row * 18,
                       x + 7 + col * 18 + 18, y + 16 + row * 18 + 18, 0xFF111111);

        // Energy bar  (x=64, y=16, w=14, h=52)
        drawBar(g, x + 64, y + 16, 14, 52, menu.getEnergy(), menu.getMaxEnergy(), 0xFFFF6A00);
        // Sat bar  (x=82, y=16, w=14, h=52)
        drawBar(g, x + 82, y + 16, 14, 52, menu.getSatCount(), 20, 0xFF00AAFF);

        // Player inventory areas
        g.fill(x + 7, y + 83, x + 169, y + 116, 0xFF151520);
        g.fill(x + 7, y + 120, x + 169, y + 143, 0xFF151520);

        // Channel row separator
        g.fill(x + 1, y + 163, x + imageWidth - 1, y + 164, 0xFF333355);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 5, 0xFFEECC, false);
        g.drawString(font, "RF",  64, 70, 0xFF6A00, false);
        g.drawString(font, "SAT", 81, 70, 0x00AAFF, false);

        g.drawString(font, "RF: " + formatRF(menu.getEnergy()), 100, 20, 0xFFFFFF, false);
        g.drawString(font, "Sats: " + menu.getSatCount(), 100, 32, 0x88CCFF, false);

        if (menu.isReceiver()) {
            Component link = menu.isLinked()
                    ? Component.literal("\u00a7aVerknüpft")
                    : Component.literal("\u00a7cNicht verknüpft");
            g.drawString(font, link, 100, 44, 0xFFFFFF, false);
        }

        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xAAAAAA, false);

        // Channel label
        int channelColor = menu.isPrivate() ? 0xFFAA44 : 0x88AA88;
        g.drawString(font, "Kanal:", 8, 166, channelColor, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (channelBox != null && channelBox.isFocused()) {
            if (key == 257) { // Enter
                sendChannel();
                channelBox.setFocused(false);
                return true;
            }
            return channelBox.keyPressed(key, scan, mod) || super.keyPressed(key, scan, mod);
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public void removed() {
        super.removed();
        sendChannel();
    }

    private void sendChannel() {
        if (channelBox == null) return;
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new SetWirelessChannelPacket(menu.getBlockPos(), channelBox.getValue().trim()));
    }

    private static void drawBar(GuiGraphics g, int bx, int by, int bw, int bh,
                                 int current, int max, int fillColor) {
        g.fill(bx, by, bx + bw, by + bh, 0xFF111111);
        if (max > 0) {
            int fill = (int)((float) current / max * bh);
            fill = Math.max(0, Math.min(fill, bh));
            g.fill(bx, by + bh - fill, bx + bw, by + bh, fillColor);
        }
    }

    private static String formatRF(int rf) {
        if (rf >= 1_000_000) return String.format("%.1fM", rf / 1_000_000f);
        if (rf >= 1_000)     return String.format("%.1fk", rf / 1_000f);
        return String.valueOf(rf);
    }
}
