package com.goblinraiders.client;

import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.network.DialogChoicePayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A conversation window: the goblin's portrait on the left, its name and words on the right, answers as buttons
 * at the bottom. Subclasses fill in the text and the buttons.
 */
public abstract class GoblinDialogScreen extends Screen {
    protected static final int PANEL_W = 340;
    protected static final int PANEL_H = 236;
    protected static final int TEXT_X = 98;
    protected static final int TEXT_Y = 24;
    protected static final int TEXT_W = PANEL_W - TEXT_X - 12;
    protected static final int BUTTONS_Y = PANEL_H - 72;
    private static final int BORDER = 0xFF8A6A3A;
    private static final int FILL = 0xF02B2118;
    private static final int INNER = 0xFF3A2C1F;
    protected static final int TEXT_COLOR = 0xFFEDE0C4;
    protected static final int NAME_COLOR = 0xFFFFD27A;

    protected final AbstractGoblin goblin;
    protected int left;
    protected int top;
    private boolean answered;

    protected GoblinDialogScreen(AbstractGoblin goblin) {
        super(goblin.getDisplayName());
        this.goblin = goblin;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;
        this.addButtons();
    }

    /** Called from init and whenever the content changes (after clearWidgets). */
    protected abstract void addButtons();

    /** Draws the words of the goblin into the text area. */
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY);

    protected void rebuild() {
        this.clearWidgets();
        this.addButtons();
    }

    protected Button button(Component label, int column, int row, int columns, Button.OnPress onPress) {
        int gap = 6;
        int w = (PANEL_W - 24 - gap * (columns - 1)) / columns;
        int x = this.left + 12 + column * (w + gap);
        int y = this.top + BUTTONS_Y + row * 24;
        return this.addRenderableWidget(Button.builder(label, onPress).bounds(x, y, w, 20).build());
    }

    /** Sends the player's answer to the server; the window does not report a "close" after that. */
    protected void answer(int choice) {
        this.answered = true;
        PacketDistributor.sendToServer(new DialogChoicePayload(this.goblin.getId(), choice));
    }

    /** Sends an answer that keeps the conversation going (closing the window is still reported). */
    protected void tell(int choice) {
        PacketDistributor.sendToServer(new DialogChoicePayload(this.goblin.getId(), choice));
    }

    @Override
    public void removed() {
        if (!this.answered) {
            PacketDistributor.sendToServer(new DialogChoicePayload(this.goblin.getId(), DialogChoicePayload.CLOSE));
        }
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.goblin.isAlive() || this.minecraft.player == null || this.minecraft.player.distanceToSqr(this.goblin) > 100.0) {
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.left, y = this.top;
        graphics.fill(x - 2, y - 2, x + PANEL_W + 2, y + PANEL_H + 2, BORDER);
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, FILL);
        graphics.fill(x + 10, y + 18, x + 88, y + BUTTONS_Y - 8, INNER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = this.left, y = this.top;
        graphics.drawString(this.font, this.title, x + 12, y + 6, NAME_COLOR, false);
        float scale = 92.0F / Math.max(1.0F, this.goblin.getBbHeight());
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 10, y + 18, x + 88, y + BUTTONS_Y - 8, (int) scale, 0.0625F,
                mouseX, mouseY, this.goblin);
        this.renderContent(graphics, mouseX, mouseY);
    }

    /** Draws wrapped text into the text area starting at {@code yOffset}; returns the y below it. */
    protected int drawText(GuiGraphics graphics, Component text, int yOffset, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, TEXT_W);
        int y = this.top + yOffset;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, this.left + TEXT_X, y, color, false);
            y += this.font.lineHeight + 1;
        }
        return y - this.top;
    }
}
