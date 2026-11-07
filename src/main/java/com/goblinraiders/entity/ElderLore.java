package com.goblinraiders.entity;

/**
 * Topics the goblin elder talks about. Each topic has a title {@code goblinraiders.dialog.elder.topic.<id>} and pages
 * {@code goblinraiders.dialog.elder.<id>.<page>} (from 1) in the language files.
 */
public final class ElderLore {
    public static final String[] TOPICS = {"goblins", "raids", "stash", "amulet", "giants", "trade", "crazy", "legend"};
    public static final int[] PAGES = {3, 2, 2, 2, 2, 2, 2, 3};
    public static final int GREETINGS = 3;

    private ElderLore() {
    }
}
