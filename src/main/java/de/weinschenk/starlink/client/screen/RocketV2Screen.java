package de.weinschenk.starlink.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.item.SatelliteItem;
import de.weinschenk.starlink.menu.RocketV2Menu;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.SetSatellitePinPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RocketV2Screen extends AbstractContainerScreen<RocketV2Menu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/gui/rocket_v2.png");

    // Config-Strip zwischen Slots und Spieler-Inventar
    private static final int CONFIG_Y      = 92;   // relativ zum GUI-Ursprung
    private static final int CONFIG_HEIGHT = 30;

    private Button  toggleBtn;
    private EditBox pinBox;
    private Button  savePinBtn;

    private int selectedSlot  = -1;
    private int prevSelected  = -2;

    public RocketV2Screen(RocketV2Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth      = 176;
        this.imageHeight     = 202;   // +22px gegenüber vorher für Config-Strip
        this.inventoryLabelY = 114;   // "Inventory"-Label über Spieler-Inventar
    }

    @Override
    protected void init() {
        super.init();

        // Toggle-Button: schaltet selektierten Slot zwischen Öffentlich / Privat
        toggleBtn = addRenderableWidget(Button.builder(
                Component.literal("🔓 Öffentlich"),
                btn -> onToggle())
                .bounds(leftPos + 8, topPos + CONFIG_Y + 14, 90, 14).build());

        // PIN-Eingabe
        pinBox = addRenderableWidget(new EditBox(
                font, leftPos + 104, topPos + CONFIG_Y + 14, 56, 12,
                Component.literal("PIN")));
        pinBox.setMaxLength(16);
        pinBox.setHint(Component.literal("PIN…"));

        // PIN speichern
        savePinBtn = addRenderableWidget(Button.builder(
                Component.literal("OK"),
                btn -> onSavePin())
                .bounds(leftPos + 162, topPos + CONFIG_Y + 13, 14, 14).build());

        refreshConfigStrip();
    }

    // -------------------------------------------------------------------------

    private void onToggle() {
        if (selectedSlot < 0) return;
        ItemStack stack = menu.getSlot(selectedSlot).getItem();
        if (stack.isEmpty()) return;
        // Server togglet den Slot; MC synct das geänderte ItemStack zurück
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selectedSlot);
    }

    private void onSavePin() {
        if (selectedSlot < 0) return;
        ItemStack stack = menu.getSlot(selectedSlot).getItem();
        if (stack.isEmpty() || !SatelliteItem.isPrivate(stack)) return;
        String pin = pinBox.getValue().trim();
        ModNetwork.CHANNEL.sendToServer(new SetSatellitePinPacket(selectedSlot, pin));
    }

    // -------------------------------------------------------------------------

    private void refreshConfigStrip() {
        if (toggleBtn == null) return;

        boolean hasSlot  = selectedSlot >= 0 && selectedSlot < 20;
        boolean occupied = hasSlot && !menu.getSlot(selectedSlot).getItem().isEmpty();
        boolean priv     = occupied && SatelliteItem.isPrivate(menu.getSlot(selectedSlot).getItem());

        toggleBtn.active  = occupied;
        toggleBtn.setMessage(Component.literal(priv ? "🔒 Privat" : "🔓 Öffentlich"));

        pinBox.setVisible(priv);
        pinBox.setEditable(priv);
        savePinBtn.visible = priv;

        // PIN-Wert aktualisieren wenn sich der Slot geändert hat (nicht beim Tippen)
        if (selectedSlot != prevSelected) {
            prevSelected = selectedSlot;
            String pin = occupied ? SatelliteItem.getPin(menu.getSlot(selectedSlot).getItem()) : "";
            pinBox.setValue(pin);
        }
    }

    // -------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, 180); // Originaltextur für obere 180px

        // Config-Strip-Hintergrund (zusätzliche 22px, programmatisch gezeichnet)
        graphics.fill(x + 1, y + 180, x + imageWidth - 1, y + 202, 0xFFD0D0D0);
        graphics.fill(x + 1, y + 180, x + imageWidth - 1, y + 181, 0xFF888888); // Trennlinie
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;

        // Satellitenanzahl
        int satCount = menu.getSatelliteCount();
        graphics.drawString(font,
                Component.literal("Satelliten: " + satCount + " / 20"),
                x + 8, y + 6, 0x404040, false);

        // Lock-Indikatoren auf den Slots zeichnen
        drawLockIndicators(graphics, x, y);

        // Config-Strip
        String slotLabel = selectedSlot >= 0
                ? "Slot " + (selectedSlot + 1) + ":"
                : "Slot hovern zum Konfigurieren";
        graphics.drawString(font, slotLabel, x + 8, y + CONFIG_Y + 3, 0x333333, false);

        refreshConfigStrip();
    }

    /** Zeichnet ein kleines farbiges Schloss-Symbol oben links auf jeden belegten Slot. */
    private void drawLockIndicators(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < 20; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty()) continue;

            int row = i / 5, col = i % 5;
            int sx = x + 8  + col * 18;
            int sy = y + 18 + row * 18;

            boolean priv = SatelliteItem.isPrivate(stack);
            // Kleines 5x5 Quadrat oben links auf dem Slot
            int color = priv ? 0xCCFF4444 : 0xCC44AA44;
            graphics.fill(sx, sy, sx + 5, sy + 5, color);

            // Ausgewählter Slot: Rahmen
            if (i == selectedSlot) {
                graphics.renderOutline(sx - 1, sy - 1, 18, 18, 0xFFFFFF00);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Maus-Klick: Slot auswählen durch Klick auf Lock-Indikator
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Lock-Indikator (5x5 oben links pro Slot) abfangen
        for (int i = 0; i < 20; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) continue;

            int row = i / 5, col = i % 5;
            int sx = leftPos + 8  + col * 18;
            int sy = topPos  + 18 + row * 18;

            if (mouseX >= sx && mouseX < sx + 5 && mouseY >= sy && mouseY < sy + 5) {
                selectedSlot = i;
                refreshConfigStrip();
                return true; // konsumiert, Slot-Interaktion verhindert
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // -------------------------------------------------------------------------
    // Tastatur-Weiterleitung an EditBox
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pinBox != null && pinBox.isFocused()) {
            return pinBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
