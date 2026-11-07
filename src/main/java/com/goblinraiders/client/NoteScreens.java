package com.goblinraiders.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/** Client-only: shows a goblin note as a book page. Only called from the client side of item use. */
public final class NoteScreens {
    public static void open(Component page) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(List.of(page))));
    }

    private NoteScreens() {
    }
}
