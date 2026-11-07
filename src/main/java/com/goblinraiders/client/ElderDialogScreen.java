package com.goblinraiders.client;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.Minecraft;
import com.goblinraiders.entity.ElderLore;
import com.goblinraiders.entity.GoblinElder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The elder's stories. First he greets the player and offers topics; each topic is a few pages of lore
 * ({@code goblinraiders.dialog.elder.<topic>.<page>}).
 */
public class ElderDialogScreen extends GoblinDialogScreen {
    private final Component greeting;
    /** -1 while choosing a topic. */
    private int topic = -1;
    private int page;

    public ElderDialogScreen(GoblinElder elder) {
        super(elder);
        this.greeting = Component.translatable("goblinraiders.dialog.elder.greeting." + (1 + elder.getRandom().nextInt(ElderLore.GREETINGS)));
    }

    @Override
    protected void addButtons() {
        if (this.topic < 0) {
            for (int i = 0; i < ElderLore.TOPICS.length; i++) {
                int t = i;
                this.button(Component.translatable("goblinraiders.dialog.elder.topic." + ElderLore.TOPICS[i]), i % 3, i / 3, 3, b -> this.open(t, 0));
            }
            this.button(Component.translatable("goblinraiders.dialog.elder.bye"), 2, 2, 3, b -> this.onClose());
            return;
        }
        if (this.page + 1 < ElderLore.PAGES[this.topic]) {
            this.button(Component.translatable("goblinraiders.dialog.elder.next"), 0, 2, 3, b -> this.open(this.topic, this.page + 1));
        }
        this.button(Component.translatable("goblinraiders.dialog.elder.back"), 1, 2, 3, b -> this.open(-1, 0));
        this.button(Component.translatable("goblinraiders.dialog.elder.bye"), 2, 2, 3, b -> this.onClose());
    }

    private void open(int topic, int page) {
        this.topic = topic;
        this.page = page;
        this.rebuild();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.topic < 0) {
            int y = this.drawText(graphics, this.greeting, TEXT_Y, TEXT_COLOR) + 6;
            this.drawText(graphics, Component.translatable("goblinraiders.dialog.elder.ask"), y, NAME_COLOR);
            return;
        }
        String id = ElderLore.TOPICS[this.topic];
        int y = this.drawText(graphics, Component.translatable("goblinraiders.dialog.elder.topic." + id), TEXT_Y, NAME_COLOR) + 4;
        this.drawText(graphics, Component.translatable("goblinraiders.dialog.elder." + id + "." + (this.page + 1)), y, TEXT_COLOR);
        if (ElderLore.PAGES[this.topic] > 1) {
            String counter = (this.page + 1) + "/" + ElderLore.PAGES[this.topic];
            graphics.drawString(this.font, counter, this.left + PANEL_W - 12 - this.font.width(counter), this.top + 6, 0xFF8A7F6A, false);
        }
    }
}
