package com.goblinraiders.registry;

import com.goblinraiders.entity.GoblinHeadman;
import com.goblinraiders.entity.GoblinSage;
import com.goblinraiders.entity.GoblinVillager;
import com.goblinraiders.entity.GoblinChild;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.CrazyGoblin;
import com.goblinraiders.entity.GiantGoblin;
import com.goblinraiders.entity.GoblinElder;
import com.goblinraiders.entity.GoblinKing;
import com.goblinraiders.entity.GoblinShaman;
import com.goblinraiders.entity.GoblinTrader;
import com.goblinraiders.entity.NeutralGoblin;
import com.goblinraiders.entity.RuthlessGoblin;
import com.goblinraiders.entity.SmallGoblin;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, GoblinRaiders.MODID);

    // Sizes follow the player model scaled by GoblinRenderer: 1.8 blocks tall at scale 1.
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGoblin>> SMALL_GOBLIN = REGISTER.register("small_goblin",
            () -> EntityType.Builder.of(SmallGoblin::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.5F).eyeHeight(1.35F).clientTrackingRange(8).build("small_goblin"));

    public static final DeferredHolder<EntityType<?>, EntityType<RuthlessGoblin>> RUTHLESS_GOBLIN = REGISTER.register("ruthless_goblin",
            () -> EntityType.Builder.of(RuthlessGoblin::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).build("ruthless_goblin"));

    public static final DeferredHolder<EntityType<?>, EntityType<NeutralGoblin>> NEUTRAL_GOBLIN = REGISTER.register("neutral_goblin",
            () -> EntityType.Builder.of(NeutralGoblin::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).build("neutral_goblin"));

    public static final DeferredHolder<EntityType<?>, EntityType<GiantGoblin>> GIANT_GOBLIN = REGISTER.register("giant_goblin",
            () -> EntityType.Builder.of(GiantGoblin::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.6F).eyeHeight(3.24F).clientTrackingRange(10).build("giant_goblin"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinTrader>> GOBLIN_TRADER = REGISTER.register("goblin_trader",
            () -> EntityType.Builder.of(GoblinTrader::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).build("goblin_trader"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinElder>> GOBLIN_ELDER = REGISTER.register("goblin_elder",
            () -> EntityType.Builder.of(GoblinElder::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.5F).eyeHeight(1.35F).clientTrackingRange(8).build("goblin_elder"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrazyGoblin>> CRAZY_GOBLIN = REGISTER.register("crazy_goblin",
            () -> EntityType.Builder.of(CrazyGoblin::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.5F).eyeHeight(1.35F).clientTrackingRange(8).build("crazy_goblin"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinShaman>> GOBLIN_SHAMAN = REGISTER.register("goblin_shaman",
            () -> EntityType.Builder.of(GoblinShaman::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).build("goblin_shaman"));

    /** The boss: three blocks tall (the player model at 5/3). */
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinKing>> GOBLIN_KING = REGISTER.register("goblin_king",
            () -> EntityType.Builder.of(GoblinKing::new, MobCategory.MONSTER)
                    .sized(1.0F, 3.0F).eyeHeight(2.7F).clientTrackingRange(10).fireImmune().build("goblin_king"));

    /** A child: the type is twice its size, being a baby halves it (one block tall). */
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinChild>> GOBLIN_CHILD = REGISTER.register("goblin_child",
            () -> EntityType.Builder.of(GoblinChild::new, MobCategory.CREATURE)
                    .sized(0.8F, 2.0F).eyeHeight(1.7F).clientTrackingRange(8).build("goblin_child"));

    /** Settlers of the peaceful villages. */
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinVillager>> GOBLIN_VILLAGER = REGISTER.register("goblin_villager",
            () -> EntityType.Builder.of(GoblinVillager::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).build("goblin_villager"));

    /** The hermit of the lone tents. */
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinSage>> GOBLIN_SAGE = REGISTER.register("goblin_sage",
            () -> EntityType.Builder.of(GoblinSage::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.75F).eyeHeight(1.55F).clientTrackingRange(8).build("goblin_sage"));

    /** The headman of a peaceful village, who hands out errands. */
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinHeadman>> GOBLIN_HEADMAN = REGISTER.register("goblin_headman",
            () -> EntityType.Builder.of(GoblinHeadman::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).build("goblin_headman"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOBLIN_HEADMAN.get(), GoblinHeadman.createAttributes().build());
        event.put(GOBLIN_VILLAGER.get(), GoblinVillager.createAttributes().build());
        event.put(GOBLIN_SAGE.get(), GoblinSage.createAttributes().build());
        event.put(GOBLIN_CHILD.get(), GoblinChild.createAttributes().build());
        event.put(SMALL_GOBLIN.get(), SmallGoblin.createAttributes().build());
        event.put(RUTHLESS_GOBLIN.get(), RuthlessGoblin.createAttributes().build());
        event.put(NEUTRAL_GOBLIN.get(), NeutralGoblin.createAttributes().build());
        event.put(GIANT_GOBLIN.get(), GiantGoblin.createAttributes().build());
        event.put(GOBLIN_TRADER.get(), GoblinTrader.createAttributes().build());
        event.put(GOBLIN_ELDER.get(), GoblinElder.createAttributes().build());
        event.put(CRAZY_GOBLIN.get(), CrazyGoblin.createAttributes().build());
        event.put(GOBLIN_SHAMAN.get(), GoblinShaman.createAttributes().build());
        event.put(GOBLIN_KING.get(), GoblinKing.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (var type : new EntityType[] {SMALL_GOBLIN.get(), RUTHLESS_GOBLIN.get(), NEUTRAL_GOBLIN.get(), GIANT_GOBLIN.get(),
                GOBLIN_TRADER.get(), GOBLIN_ELDER.get(), CRAZY_GOBLIN.get(), GOBLIN_SHAMAN.get(), GOBLIN_KING.get()}) {
            @SuppressWarnings("unchecked")
            EntityType<AbstractGoblin> goblin = (EntityType<AbstractGoblin>) type;
            event.register(goblin, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    AbstractGoblin::checkGoblinSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private ModEntities() {
    }
}
