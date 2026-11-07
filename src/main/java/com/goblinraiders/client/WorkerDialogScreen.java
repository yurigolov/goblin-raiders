package com.goblinraiders.client;

import com.goblinraiders.entity.work.WorkJob;
import com.goblinraiders.entity.GoblinChild;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.OpenWorkerDialogPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The player's own worker child: what it does and where, what is wrong, and the orders - work here, follow me, farm,
 * mine, hand over what you carry. The server sends a fresh state after every order.
 */
public class WorkerDialogScreen extends GoblinDialogScreen {
    private static final String KEY = "goblinraiders.dialog.worker.";
    private static final int PROBLEM_COLOR = 0xFFFF8A7A;
    private OpenWorkerDialogPayload state;

    public WorkerDialogScreen(GoblinChild child, OpenWorkerDialogPayload state) {
        super(child);
        this.state = state;
    }

    public boolean isFor(GoblinChild child) {
        return this.goblin == child;
    }

    public void update(OpenWorkerDialogPayload state) {
        this.state = state;
        this.rebuild();
    }

    @Override
    protected void addButtons() {
        WorkJob job = WorkJob.byId(this.state.job());
        Button stay = this.button(Component.translatable(KEY + "stay"), 0, 0, 2, b -> this.tell(DialogChoicePayload.WORKER_STAY));
        Button follow = this.button(Component.translatable(KEY + "follow"), 1, 0, 2, b -> this.tell(DialogChoicePayload.WORKER_FOLLOW));
        follow.active = this.state.working();
        stay.active = true;   // "work here" also moves the workplace
        this.button(Component.translatable(KEY + "farm"), 0, 1, 2, b -> this.tell(DialogChoicePayload.WORKER_FARM)).active = job != WorkJob.FARMER;
        this.button(Component.translatable(KEY + "mine"), 1, 1, 2, b -> this.tell(DialogChoicePayload.WORKER_MINE)).active = job != WorkJob.MINER;
        this.button(Component.translatable(KEY + "hand_over"), 0, 2, 2, b -> this.tell(DialogChoicePayload.WORKER_HAND_OVER)).active =
                this.state.carried() > 0;
        this.button(Component.translatable(KEY + "bye"), 1, 2, 2, b -> this.onClose());
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        WorkJob job = WorkJob.byId(this.state.job());
        String jobKey = job == WorkJob.MINER ? "miner" : "farmer";
        int y = this.drawText(graphics, Component.translatable(KEY + "job." + jobKey), TEXT_Y, NAME_COLOR) + 4;
        y = this.drawText(graphics, Component.translatable(KEY + (this.state.working() ? "working." + jobKey : "following")), y, TEXT_COLOR) + 4;
        GoblinChild.Problem problem = GoblinChild.Problem.values()[Math.min(this.state.problem(), GoblinChild.Problem.values().length - 1)];
        if (problem != GoblinChild.Problem.NONE) {
            y = this.drawText(graphics, Component.translatable(KEY + "problem." + problem.name().toLowerCase(java.util.Locale.ROOT)), y, PROBLEM_COLOR) + 4;
        }
        this.drawText(graphics, Component.translatable(KEY + "carried", this.state.carried()), y, TEXT_COLOR);
    }
}
