package com.goblinraiders.client;

import com.goblinraiders.entity.GoblinVillager;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GoblinTalk;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Small talk with a goblin: how it lives, what is new, a tale, a piece of advice (a random answer each time, in the
 * goblin's own voice where it has one), and a treat - goblin grub makes it share where the village keeps its stash.
 */
public class GoblinTalkScreen extends GoblinDialogScreen {
    private static final String KEY = "goblinraiders.dialog.neutral.";
    private Component heading;
    private Component text;
    private String lastLine = "";

    public GoblinTalkScreen(AbstractGoblin goblin) {
        super(goblin);
        this.text = Component.translatable(GoblinTalk.line(goblin.voice(), GoblinTalk.GREETING, goblin.getRandom()));
    }

    @Override
    protected void addButtons() {
        boolean trades = this.goblin instanceof GoblinVillager;
        int columns = trades ? 3 : 2;
        int slot = 0;
        for (String topic : GoblinTalk.TOPICS) {
            this.button(Component.translatable(KEY + "topic." + topic), slot % columns, slot / columns, columns, b -> this.ask(topic));
            slot++;
        }
        Button treat = this.button(Component.translatable(KEY + "treat"), slot % columns, slot / columns, columns, b -> this.treat());
        slot++;
        treat.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(KEY + "treat.tip")));
        treat.active = this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.getInventory().contains(new ItemStack(ModItems.GOBLIN_GRUB.get()));
        if (trades) {
            this.button(Component.translatable(KEY + "trade"), slot % columns, slot / columns, columns, b -> {
                this.answer(DialogChoicePayload.ACCEPT);
                this.onClose();
            });
        }
        this.button(Component.translatable(KEY + "bye"), columns - 1, 2, columns, b -> this.onClose());
    }

    private void ask(String topic) {
        String line;
        int tries = 0;
        do {
            line = GoblinTalk.line(this.goblin.voice(), topic, this.goblin.getRandom());
        } while (line.equals(this.lastLine) && ++tries < 5);
        this.lastLine = line;
        this.heading = Component.translatable(KEY + "topic." + topic);
        this.text = Component.translatable(line);
    }

    private void treat() {
        this.tell(DialogChoicePayload.NEUTRAL_TREAT);
        this.heading = Component.translatable(KEY + "treat");
        this.text = Component.translatable(GoblinTalk.line(this.goblin.voice(), GoblinTalk.THANKS, this.goblin.getRandom()));
        this.rebuild();
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = TEXT_Y;
        if (this.heading != null) {
            y = this.drawText(graphics, this.heading, y, NAME_COLOR) + 4;
        }
        this.drawText(graphics, this.text, y, TEXT_COLOR);
    }
}
