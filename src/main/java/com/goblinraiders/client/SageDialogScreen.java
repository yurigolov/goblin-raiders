package com.goblinraiders.client;

import com.goblinraiders.entity.GoblinSage;
import com.goblinraiders.entity.GoblinTalk;
import com.goblinraiders.network.DialogChoicePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The sage's window: a greeting, then words of wisdom (a random one each time), a prophecy of where the settlements
 * lie (told in the chat) and a blessing for a few coins.
 */
public class SageDialogScreen extends GoblinDialogScreen {
    private static final String KEY = "goblinraiders.dialog.sage.";
    private Component heading;
    private Component text;
    private String lastLine = "";

    public SageDialogScreen(GoblinSage sage) {
        super(sage);
        this.text = Component.translatable(GoblinTalk.line("sage", GoblinTalk.GREETING, sage.getRandom()));
    }

    @Override
    protected void addButtons() {
        this.button(Component.translatable(KEY + "wisdom"), 0, 0, 2, b -> this.wisdom());
        this.button(Component.translatable(KEY + "prophecy"), 1, 0, 2, b -> this.ask(DialogChoicePayload.SAGE_PROPHECY, "prophecy_wait"));
        this.button(Component.translatable(KEY + "blessing", GoblinSage.BLESSING_COST), 0, 1, 2,
                b -> this.ask(DialogChoicePayload.SAGE_BLESSING, "blessing_wait"));
        this.button(Component.translatable(KEY + "bye"), 1, 2, 2, b -> this.onClose());
    }

    private void wisdom() {
        String line;
        int tries = 0;
        do {
            line = GoblinTalk.line("sage", "wisdom", this.goblin.getRandom());
        } while (line.equals(this.lastLine) && ++tries < 5);
        this.lastLine = line;
        this.heading = Component.translatable(KEY + "wisdom");
        this.text = Component.translatable(line);
    }

    /** The answer comes in the chat; the window says what the sage is doing meanwhile. */
    private void ask(int choice, String waitKey) {
        this.tell(choice);
        this.heading = null;
        this.text = Component.translatable(KEY + waitKey);
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
