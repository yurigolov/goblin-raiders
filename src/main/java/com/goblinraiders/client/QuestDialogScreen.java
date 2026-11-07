package com.goblinraiders.client;

import com.goblinraiders.entity.GoblinHeadman;
import com.goblinraiders.entity.GoblinTalk;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.OpenQuestDialogPayload;
import com.goblinraiders.quest.Quests;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The headman's window: without an errand he greets the player and offers one; with one he tells what it is, how far
 * along it is and what it pays, and takes the report or lets the player give it up.
 */
public class QuestDialogScreen extends GoblinDialogScreen {
    private static final String KEY = "goblinraiders.dialog.headman.";
    private static final int DONE_COLOR = 0xFF9BE07A;
    private OpenQuestDialogPayload state;
    private final Component greeting;

    public QuestDialogScreen(GoblinHeadman headman, OpenQuestDialogPayload state) {
        super(headman);
        this.state = state;
        this.greeting = Component.translatable(GoblinTalk.line("headman", GoblinTalk.GREETING, headman.getRandom()));
    }

    public boolean isFor(GoblinHeadman headman) {
        return this.goblin == headman;
    }

    public void update(OpenQuestDialogPayload state) {
        this.state = state;
        this.rebuild();
    }

    @Override
    protected void addButtons() {
        var quest = Quests.byId(this.state.quest());
        if (quest.isEmpty()) {
            this.button(Component.translatable(KEY + "take"), 0, 2, 2, b -> this.tell(DialogChoicePayload.QUEST_TAKE));
        } else {
            this.button(Component.translatable(KEY + "turn_in"), 0, 1, 2, b -> this.tell(DialogChoicePayload.QUEST_TURN_IN)).active =
                    this.state.progress() >= quest.get().count();
            this.button(Component.translatable(KEY + "drop"), 1, 1, 2, b -> this.tell(DialogChoicePayload.QUEST_DROP));
        }
        this.button(Component.translatable(KEY + "bye"), 1, 2, 2, b -> this.onClose());
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        var quest = Quests.byId(this.state.quest());
        if (quest.isEmpty()) {
            int y = this.drawText(graphics, this.greeting, TEXT_Y, TEXT_COLOR) + 6;
            y = this.drawText(graphics, Component.translatable(KEY + "offer"), y, NAME_COLOR) + 6;
            if (this.state.completed() > 0) {
                this.drawText(graphics, Component.translatable(KEY + "completed", this.state.completed()), y, TEXT_COLOR);
            }
            return;
        }
        Quests.Quest q = quest.get();
        String id = "goblinraiders.quest." + q.id();
        int y = this.drawText(graphics, Component.translatable(id + ".title"), TEXT_Y, NAME_COLOR) + 4;
        y = this.drawText(graphics, Component.translatable(id + ".text", q.count()), y, TEXT_COLOR) + 6;
        boolean done = this.state.progress() >= q.count();
        String progressKey = q.kind() == Quests.Kind.DELIVER ? "carried" : "progress";
        y = this.drawText(graphics, done ? Component.translatable(KEY + "ready")
                : Component.translatable(KEY + progressKey, this.state.progress(), q.count()), y, done ? DONE_COLOR : TEXT_COLOR) + 2;
        this.drawText(graphics, Component.translatable(KEY + "reward", q.coins()), y, NAME_COLOR);
    }
}
