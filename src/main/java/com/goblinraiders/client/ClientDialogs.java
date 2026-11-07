package com.goblinraiders.client;

import com.goblinraiders.network.OpenQuestDialogPayload;
import com.goblinraiders.entity.GoblinHeadman;
import com.goblinraiders.entity.GoblinSage;
import com.goblinraiders.network.OpenWorkerDialogPayload;
import com.goblinraiders.entity.GoblinChild;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.NeutralGoblin;
import com.goblinraiders.entity.GoblinElder;
import com.goblinraiders.entity.GoblinKing;
import com.goblinraiders.entity.GoblinTrader;
import com.goblinraiders.network.OpenDialogPayload;
import com.goblinraiders.network.OpenKingDialogPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client-only: opens the dialog window the server asked for. */
public final class ClientDialogs {
    public static void open(OpenDialogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (entity instanceof GoblinTrader trader) {
            minecraft.setScreen(new TraderDialogScreen(trader, payload.offers()));
        } else if (entity instanceof GoblinElder elder) {
            minecraft.setScreen(new ElderDialogScreen(elder));
        } else if (entity instanceof GoblinSage sage) {
            minecraft.setScreen(new SageDialogScreen(sage));
        } else if (entity instanceof AbstractGoblin goblin) {
            minecraft.setScreen(new GoblinTalkScreen(goblin));
        }
    }

    public static void openWorker(OpenWorkerDialogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.entityId()) instanceof GoblinChild child) {
            if (minecraft.screen instanceof WorkerDialogScreen screen && screen.isFor(child)) {
                screen.update(payload);   // the server refreshed the state after an answer
            } else {
                minecraft.setScreen(new WorkerDialogScreen(child, payload));
            }
        }
    }

    public static void openQuest(OpenQuestDialogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.entityId()) instanceof GoblinHeadman headman) {
            if (minecraft.screen instanceof QuestDialogScreen screen && screen.isFor(headman)) {
                screen.update(payload);
            } else {
                minecraft.setScreen(new QuestDialogScreen(headman, payload));
            }
        }
    }

    public static void openKing(OpenKingDialogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.entityId()) instanceof GoblinKing king) {
            minecraft.setScreen(new KingDialogScreen(king, payload));
        }
    }

    private ClientDialogs() {
    }
}
