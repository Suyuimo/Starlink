package de.weinschenk.starlink.client.screen;

import de.weinschenk.starlink.menu.WirelessFluidMenu;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.SetWirelessChannelPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;

public class WirelessFluidScreen extends AbstractContainerScreen<WirelessFluidMenu> {

    private EditBox channelBox;

    public WirelessFluidScreen(WirelessFluidMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 122;
    }

    @Override
    protected void init() {
        super.init();
        channelBox = new EditBox(font, leftPos + 86, topPos + 104, 82, 12, Component.literal("Kanal"));
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

        // Fluid bar  (x=12, y=16, w=22, h=68)
        drawBar(g, x + 12, y + 16, 22, 68, menu.getFluidAmount(), menu.getFluidCapacity(), 0xFF0066CC);
        // Energy bar  (x=40, y=16, w=16, h=68)
        drawBar(g, x + 40, y + 16, 16, 68, menu.getEnergy(), menu.getMaxEnergy(), 0xFFFF6A00);
        // Satellite bar  (x=62, y=16, w=16, h=68)
        drawBar(g, x + 62, y + 16, 16, 68, menu.getSatCount(), 20, 0xFF00AAFF);

        // Channel row separator
        g.fill(x + 1, y + 100, x + imageWidth - 1, y + 101, 0xFF333355);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 5, 0xFFEECC, false);

        g.drawString(font, "FL",  13, 86, 0x0066CC, false);
        g.drawString(font, "RF",  41, 86, 0xFF6A00, false);
        g.drawString(font, "SAT", 61, 86, 0x00AAFF, false);

        g.drawString(font, menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB", 86, 20, 0x88DDFF, false);
        g.drawString(font, formatRF(menu.getEnergy()) + " / " + formatRF(menu.getMaxEnergy()), 86, 32, 0xFFCC88, false);
        g.drawString(font, "Satelliten: " + menu.getSatCount(), 86, 44, 0x88CCFF, false);

        if (menu.isReceiver()) {
            Component link = menu.isLinked()
                    ? Component.literal("\u00a7aVerknüpft")
                    : Component.literal("\u00a7cNicht verknüpft");
            g.drawString(font, link, 86, 56, 0xFFFFFF, false);
        }

        // Channel label
        int channelColor = menu.isPrivate() ? 0xFFAA44 : 0x88AA88;
        g.drawString(font, "Kanal:", 8, 94, channelColor, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 257 && channelBox != null && channelBox.isFocused()) { // Enter
            sendChannel();
            channelBox.setFocused(false);
            return true;
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
        if (rf >= 1_000_000) return String.format("%.1fM RF", rf / 1_000_000f);
        if (rf >= 1_000)     return String.format("%.1fk RF", rf / 1_000f);
        return rf + " RF";
    }
}
