package com.goblinraiders.client;

import net.minecraft.client.gui.components.Tooltip;
import com.goblinraiders.entity.GoblinKing;
import com.goblinraiders.item.VassalCharterItem;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.OpenKingDialogPayload;
import com.goblinraiders.raid.Horde;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Before the goblin king: "submit or die". Vassals instead see their tribute and can pay it or hire goblins
 * (the server re-sends the dialog after each purchase, so the numbers stay fresh).
 */
public class KingDialogScreen extends GoblinDialogScreen {
    private final OpenKingDialogPayload state;
    /** The vassal pressed "betray" and is asked to confirm. */
    private boolean confirmingBetrayal;

    public KingDialogScreen(GoblinKing king, OpenKingDialogPayload state) {
        super(king);
        this.state = state;
    }

    @Override
    protected void addButtons() {
        if (!this.state.vassal()) {
            this.button(Component.translatable("goblinraiders.dialog.king.submit"), 0, 2, 2, b -> this.choose(DialogChoicePayload.KING_SUBMIT, true));
            this.button(Component.translatable("goblinraiders.dialog.king.fight"), 1, 2, 2, b -> this.choose(DialogChoicePayload.KING_FIGHT, true));
            return;
        }
        if (this.confirmingBetrayal) {
            this.button(Component.translatable("goblinraiders.dialog.king.betray_yes"), 0, 2, 2, b -> this.choose(DialogChoicePayload.KING_BETRAY, true));
            this.button(Component.translatable("goblinraiders.dialog.king.betray_no"), 1, 2, 2, b -> {
                this.confirmingBetrayal = false;
                this.rebuild();
            });
            return;
        }
        this.button(Component.translatable("goblinraiders.dialog.king.pay", Horde.TRIBUTE_COST), 0, 0, 3,
                b -> this.choose(DialogChoicePayload.KING_PAY, false));
        this.button(Component.translatable("goblinraiders.dialog.king.hire_small", Horde.Hireling.SMALL.cost), 1, 0, 3,
                b -> this.choose(DialogChoicePayload.KING_HIRE_SMALL, false));
        this.button(Component.translatable("goblinraiders.dialog.king.hire_ruthless", Horde.Hireling.RUTHLESS.cost), 2, 0, 3,
                b -> this.choose(DialogChoicePayload.KING_HIRE_RUTHLESS, false));
        this.button(Component.translatable("goblinraiders.dialog.king.hire_giant", Horde.Hireling.GIANT.cost), 0, 1, 3,
                b -> this.choose(DialogChoicePayload.KING_HIRE_GIANT, false));
        this.button(Component.translatable("goblinraiders.dialog.king.buy_farmer", Horde.WORKER_COST), 1, 1, 3,
                b -> this.choose(DialogChoicePayload.KING_BUY_FARMER, false))
                .setTooltip(Tooltip.create(Component.translatable("goblinraiders.dialog.king.buy_farmer.tip")));
        this.button(Component.translatable("goblinraiders.dialog.king.buy_miner", Horde.WORKER_COST), 2, 1, 3,
                b -> this.choose(DialogChoicePayload.KING_BUY_MINER, false))
                .setTooltip(Tooltip.create(Component.translatable("goblinraiders.dialog.king.buy_miner.tip")));
        this.button(Component.translatable("goblinraiders.dialog.king.betray"), 0, 2, 3, b -> {
            this.confirmingBetrayal = true;
            this.rebuild();
        });
        this.button(Component.translatable("goblinraiders.dialog.king.bye"), 2, 2, 3, b -> this.onClose());
    }

    private void choose(int choice, boolean close) {
        this.answer(choice);
        if (close) {
            this.onClose();
        }
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.state.vassal()) {
            int y = this.drawText(graphics, Component.translatable("goblinraiders.dialog.king.greeting"), TEXT_Y, TEXT_COLOR) + 6;
            this.drawText(graphics, Component.translatable("goblinraiders.dialog.king.terms", Horde.TRIBUTE_COST), y, NAME_COLOR);
            return;
        }
        if (this.confirmingBetrayal) {
            this.drawText(graphics, Component.translatable("goblinraiders.dialog.king.betray_confirm"), TEXT_Y, 0xFFFF8A7A);
            return;
        }
        int y = this.drawText(graphics, Component.translatable("goblinraiders.dialog.king.vassal"), TEXT_Y, TEXT_COLOR) + 4;
        Component tribute = this.state.ticksToTribute() < 0
                ? Component.translatable("goblinraiders.dialog.king.overdue", Horde.TRIBUTE_COST)
                : Component.translatable("goblinraiders.dialog.king.due", Horde.TRIBUTE_COST, VassalCharterItem.formatTime(this.state.ticksToTribute()));
        y = this.drawText(graphics, tribute, y, NAME_COLOR) + 2;
        this.drawText(graphics, Component.translatable("goblinraiders.dialog.king.coins", this.state.coins()), y, TEXT_COLOR);
    }
}
