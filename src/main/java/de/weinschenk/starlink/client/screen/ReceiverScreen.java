package de.weinschenk.starlink.client.screen;

import de.weinschenk.starlink.data.SignalFilterMode;
import de.weinschenk.starlink.menu.ReceiverMenu;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.SetBlockPrivacyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ReceiverScreen extends AbstractContainerScreen<ReceiverMenu> {

    private Button   btnAll;
    private Button   btnPublic;
    private Button   btnPrivate;
    private EditBox  pinBox;
    private Button   btnSavePin;

    public ReceiverScreen(ReceiverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 220;
        this.imageHeight = 100;
    }

    @Override
    protected void init() {
        super.init();

        btnAll = addRenderableWidget(Button.builder(
                Component.literal("Alle"),
                btn -> sendMode(SignalFilterMode.ALL))
                .bounds(leftPos + 8, topPos + 28, 60, 16).build());

        btnPublic = addRenderableWidget(Button.builder(
                Component.literal("Öffentlich"),
                btn -> sendMode(SignalFilterMode.PUBLIC_ONLY))
                .bounds(leftPos + 74, topPos + 28, 70, 16).build());

        btnPrivate = addRenderableWidget(Button.builder(
                Component.literal("Privat"),
                btn -> sendMode(SignalFilterMode.PRIVATE_ONLY))
                .bounds(leftPos + 150, topPos + 28, 62, 16).build());

        pinBox = addRenderableWidget(new EditBox(
                font, leftPos + 8, topPos + 56, 150, 14,
                Component.literal("PIN eingeben")));
        pinBox.setMaxLength(16);
        pinBox.setHint(Component.literal("PIN (max. 16 Zeichen)"));
        pinBox.setValue(menu.getClientPin());

        btnSavePin = addRenderableWidget(Button.builder(
                Component.literal("Speichern"),
                btn -> savePin())
                .bounds(leftPos + 164, topPos + 54, 48, 16).build());

        updateButtonStates();
    }

    private void sendMode(SignalFilterMode mode) {
        // Sofort lokal aktualisieren für flüssige UI
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode.ordinal());
        // Auch PIN gleich mitsenden (kann leer sein)
        String pin = pinBox != null ? pinBox.getValue().trim() : menu.getClientPin();
        ModNetwork.CHANNEL.sendToServer(new SetBlockPrivacyPacket(menu.getBlockPos(), mode.ordinal(), pin));
    }

    private void savePin() {
        String pin = pinBox.getValue().trim();
        menu.setClientPin(pin);
        SignalFilterMode mode = menu.getCurrentMode();
        ModNetwork.CHANNEL.sendToServer(new SetBlockPrivacyPacket(menu.getBlockPos(), mode.ordinal(), pin));
    }

    private void updateButtonStates() {
        if (btnAll == null) return;
        SignalFilterMode mode = menu.getCurrentMode();
        // Aktiver Button leicht hervorgehoben – einfachste Methode: Inaktiv-State für die anderen
        btnAll.active    = mode != SignalFilterMode.ALL;
        btnPublic.active = mode != SignalFilterMode.PUBLIC_ONLY;
        btnPrivate.active= mode != SignalFilterMode.PRIVATE_ONLY;

        boolean showPin = (mode == SignalFilterMode.PRIVATE_ONLY);
        pinBox.setVisible(showPin);
        pinBox.setEditable(showPin);
        btnSavePin.visible = showPin;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Dunkles Panel
        graphics.fill(leftPos, topPos, leftPos + imageWidth, leftPos + imageHeight, 0xE0001428);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF406080);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        updateButtonStates();

        // Titel
        graphics.drawString(font, "Empfänger-Einstellungen", leftPos + 8, topPos + 10, 0xFFAADD, false);

        // Modus-Label
        graphics.drawString(font, "Filter:", leftPos + 8, topPos + 16, 0xAAAAAA, false);

        // PIN-Label (nur wenn sichtbar)
        if (menu.getCurrentMode() == SignalFilterMode.PRIVATE_ONLY) {
            graphics.drawString(font, "PIN:", leftPos + 8, topPos + 44, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pinBox != null && pinBox.isFocused()) {
            return pinBox.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
