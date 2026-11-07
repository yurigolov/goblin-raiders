package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Goblin voices and the sounds of their settlements, defined in assets/goblinraiders/sounds.json (written by
 * tools/gen_data.py). Every one of them is a file made by tools/sounds/synth.py, the voices by its small speech
 * synthesizer; only the raid alarm is the user's own recording.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, GoblinRaiders.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_AMBIENT = register("entity.goblin.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_TALK = register("entity.goblin.talk");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_LAUGH = register("entity.goblin.laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_ANGRY = register("entity.goblin.angry");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_HURT = register("entity.goblin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_DEATH = register("entity.goblin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_YES = register("entity.goblin.yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_NO = register("entity.goblin.no");

    public static final DeferredHolder<SoundEvent, SoundEvent> RUTHLESS_AMBIENT = register("entity.ruthless_goblin.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUTHLESS_HURT = register("entity.ruthless_goblin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUTHLESS_DEATH = register("entity.ruthless_goblin.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_AMBIENT = register("entity.giant_goblin.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_ROAR = register("entity.giant_goblin.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_HURT = register("entity.giant_goblin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_DEATH = register("entity.giant_goblin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_ATTACK = register("entity.giant_goblin.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> TRADER_HAGGLE = register("entity.goblin_trader.haggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRADER_YELL = register("entity.goblin_trader.yell");

    public static final DeferredHolder<SoundEvent, SoundEvent> ELDER_AMBIENT = register("entity.goblin_elder.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ELDER_TALK = register("entity.goblin_elder.talk");

    public static final DeferredHolder<SoundEvent, SoundEvent> CRAZY_LAUGH = register("entity.crazy_goblin.laugh");

    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_AMBIENT = register("entity.goblin_shaman.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_SPELL = register("entity.goblin_shaman.spell");

    public static final DeferredHolder<SoundEvent, SoundEvent> KING_AMBIENT = register("entity.goblin_king.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> KING_ROAR = register("entity.goblin_king.roar");

    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_SNORE = register("entity.goblin.snore");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_ATTACK = register("entity.goblin.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_CELEBRATE = register("entity.goblin.celebrate");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBLIN_SCARED = register("entity.goblin.scared");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHILD_GIGGLE = register("entity.goblin_child.giggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOT_JINGLE = register("entity.goblin.loot_jingle");

    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_DRUMS = register("ambient.goblin.war_drums");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_HORN = register("ambient.goblin.war_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE = register("ambient.goblin.forge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARPEN = register("ambient.goblin.sharpen");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOP = register("ambient.goblin.chop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHATTER = register("ambient.goblin.chatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> COINS = register("ambient.goblin.coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE = register("ambient.goblin.pipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIRDS = register("ambient.goblin.birds");
    public static final DeferredHolder<SoundEvent, SoundEvent> FARM = register("ambient.goblin.farm");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL = register("ambient.goblin.bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRICKETS = register("ambient.goblin.crickets");
    public static final DeferredHolder<SoundEvent, SoundEvent> OWL = register("ambient.goblin.owl");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYSTIC = register("ambient.goblin.mystic");

    /** The alarm a player hears when goblins have just raided their base (sounds/raid_alarm.ogg). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RAID_ALARM = register("event.goblin_raid");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(GoblinRaiders.id(name)));
    }

    private ModSounds() {
    }
}
