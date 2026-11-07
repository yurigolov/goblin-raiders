package com.goblinraiders.client;

import com.goblinraiders.entity.GoblinTrader;
import com.goblinraiders.network.DialogChoicePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** The trader greets the player, shows his price list and asks whether they will trade. */
public class TraderDialogScreen extends GoblinDialogScreen {
    private static final int GREETINGS = 3;
    private static final int ROW_H = 18;
    private static final int COLUMNS = 2;

    private final MerchantOffers offers;
    private final Component greeting;

    public TraderDialogScreen(GoblinTrader trader, MerchantOffers offers) {
        super(trader);
        this.offers = offers;
        this.greeting = Component.translatable("goblinraiders.dialog.trader.greeting." + (1 + trader.getRandom().nextInt(GREETINGS)));
    }

    @Override
    protected void addButtons() {
        this.button(Component.translatable("goblinraiders.dialog.trader.accept"), 0, 2, 2, b -> {
            this.answer(DialogChoicePayload.ACCEPT);
            this.onClose();
        });
        this.button(Component.translatable("goblinraiders.dialog.trader.refuse"), 1, 2, 2, b -> {
            this.answer(DialogChoicePayload.REFUSE);
            this.onClose();
        });
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.drawText(graphics, this.greeting, TEXT_Y, TEXT_COLOR) + 3;
        int rows = (this.offers.size() + COLUMNS - 1) / COLUMNS;
        int colW = TEXT_W / COLUMNS;
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < this.offers.size(); i++) {
            MerchantOffer offer = this.offers.get(i);
            int x = this.left + TEXT_X + (i / rows) * colW;
            int rowY = this.top + y + (i % rows) * ROW_H;
            hovered = this.drawItem(graphics, offer.getCostA(), x, rowY, mouseX, mouseY, hovered);
            int arrowX = x + 20;
            if (!offer.getCostB().isEmpty()) {
                hovered = this.drawItem(graphics, offer.getCostB(), x + 18, rowY, mouseX, mouseY, hovered);
                arrowX = x + 38;
            }
            int color = offer.isOutOfStock() ? 0xFF8A7F6A : TEXT_COLOR;
            graphics.drawString(this.font, "→", arrowX, rowY + 5, color, false);
            hovered = this.drawItem(graphics, offer.getResult(), arrowX + 9, rowY, mouseX, mouseY, hovered);
            if (offer.isOutOfStock()) {
                graphics.drawString(this.font, Component.translatable("goblinraiders.dialog.trader.sold_out"), arrowX + 28, rowY + 5, 0xFFC05040, false);
            }
        }
        int questionY = this.top + y + rows * ROW_H + 4;
        graphics.drawString(this.font, Component.translatable("goblinraiders.dialog.trader.question"), this.left + TEXT_X, questionY, NAME_COLOR, false);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    private ItemStack drawItem(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY, ItemStack hovered) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(this.font, stack, x, y);
        boolean over = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        return over ? stack : hovered;
    }
}
