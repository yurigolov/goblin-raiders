package com.goblinraiders.test;

import com.goblinraiders.entity.GoblinHeadman;
import com.goblinraiders.entity.GoblinVillager;
import com.goblinraiders.block.StorehouseCrateBlockEntity;
import com.goblinraiders.network.OpenQuestDialogPayload;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import com.goblinraiders.entity.work.WorkJob;
import com.goblinraiders.network.OpenWorkerDialogPayload;
import com.goblinraiders.entity.GoblinChild;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.client.ClientDialogs;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GoblinTrader;
import com.goblinraiders.entity.Speech;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.OpenDialogPayload;
import com.goblinraiders.network.OpenKingDialogPayload;
import com.goblinraiders.registry.ModEffects;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModSounds;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev-only scripted tour of the client side of the mod: {@code ./gradlew runClient -Pautotest=true}.
 * Creates (or opens) a flat world "goblin_tour", lines up the goblins with speech bubbles, opens the trader and elder
 * dialogs, the trading menu and the trader's tantrum, takes screenshots (run/screenshots/tour_*.png) and quits.
 * Does nothing unless the system property is set in a dev environment.
 * <p>
 * {@code -Pautotest=hamlet} instead creates a normal world "goblin_hamlet_tour", finds the nearest peaceful village
 * generated on the real terrain and photographs it, its headman, his errand window and the storehouse.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID, value = Dist.CLIENT)
public final class ClientTour {
    private static final String MODE = System.getProperty("goblinraiders.autotest", "false");
    private static final boolean ENABLED = !FMLEnvironment.production && !"false".equals(MODE);
    private static final boolean HAMLET_TOUR = "hamlet".equals(MODE);
    private static final String WORLD = HAMLET_TOUR ? "goblin_hamlet_tour" : "goblin_tour";

    private record Step(int waitTicks, Consumer<Minecraft> action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int step = -1;
    private static int wait;
    private static boolean worldRequested;
    private static final List<Integer> goblins = new ArrayList<>();
    private static int traderId;
    private static int elderId;
    private static int kingId;
    private static int neutralId;
    private static int childId;
    private static int ruthlessId;
    private static int settlerId;
    private static int sageId;
    private static BlockPos hamletCenter = BlockPos.ZERO;
    private static int headmanId;
    private static Vec3 lineupSpot = Vec3.ZERO;
    private static final List<Integer> faces = new ArrayList<>();

    /** Every sound of the mod the client plays during a tour goes to the log: the settlements have to be heard. */
    @SubscribeEvent
    public static void onPlaySound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if (ENABLED && event.getSound() != null && GoblinRaiders.MODID.equals(event.getSound().getLocation().getNamespace())) {
            var sound = event.getSound();
            GoblinRaiders.LOGGER.info("[tour] heard {} at {} {} {}", sound.getLocation().getPath(), (int) sound.getX(), (int) sound.getY(),
                    (int) sound.getZ());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested) {
            if (mc.screen instanceof TitleScreen) {
                worldRequested = true;
                openWorld(mc);
            }
            return;
        }
        if (step < 0) {
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
                return;
            }
            if (HAMLET_TOUR) {
                buildHamletSteps();
            } else {
                buildSteps();
            }
            step = 0;
            wait = 40;
            GoblinRaiders.LOGGER.info("[tour] in the world, {} steps", STEPS.size());
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (step >= STEPS.size()) {
            GoblinRaiders.LOGGER.info("[tour] done");
            step = Integer.MIN_VALUE / 2;
            mc.stop();
            return;
        }
        Step s = STEPS.get(step++);
        try {
            s.action().accept(mc);
            GoblinRaiders.LOGGER.info("[tour] step {} ok", step - 1);
        } catch (Exception e) {
            GoblinRaiders.LOGGER.error("[tour] step {} failed", step - 1, e);
        }
        wait = s.waitTicks();
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> GoblinRaiders.LOGGER.error("[tour] could not open the world"));
            return;
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261025L, true, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(HAMLET_TOUR ? WorldPresets.NORMAL : WorldPresets.FLAT)
                        .value().createWorldDimensions(),
                new TitleScreen());
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.executeBlocking(() -> action.accept(server.getPlayerList().getPlayers().get(0)));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "tour_" + name + ".png", mc.getMainRenderTarget(),
                msg -> GoblinRaiders.LOGGER.info("[tour] {}", msg.getString()));
    }

    private static AbstractGoblin goblin(ServerLevel level, int id) {
        return (AbstractGoblin) level.getEntity(id);
    }

    private static void buildSteps() {
        // 1. All seven goblins in a row facing the player; three of them talk.
        STEPS.add(new Step(50, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            BlockPos base = player.blockPosition();
            player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() - 6.5, 0.0F, 8.0F);
            EntityType<?>[] types = {ModEntities.SMALL_GOBLIN.get(), ModEntities.CRAZY_GOBLIN.get(), ModEntities.GOBLIN_ELDER.get(),
                    ModEntities.GOBLIN_TRADER.get(), ModEntities.NEUTRAL_GOBLIN.get(), ModEntities.RUTHLESS_GOBLIN.get(),
                    ModEntities.GIANT_GOBLIN.get(), ModEntities.SMALL_GOBLIN.get()};
            double[] xs = {-5.5, -3.8, -2.1, -0.3, 1.5, 3.3, 5.6, -7.2};
            for (int i = 0; i < types.length; i++) {
                AbstractGoblin goblin = (AbstractGoblin) types[i].create(level);
                goblin.moveTo(base.getX() + 0.5 + xs[i], base.getY(), base.getZ() + 0.5, 180.0F, 0.0F);
                goblin.finalizeSpawn(level, level.getCurrentDifficultyAt(base), MobSpawnType.SPAWN_EGG, null);
                goblin.setNoAi(true);
                if (i == 0) {
                    goblin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_BOW.get()));
                } else if (i == 6) {
                    goblin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_CLUB.get()));
                } else if (i == 7) {
                    goblin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_DAGGER.get()));
                }
                goblin.setYHeadRot(180.0F);
                goblin.setYBodyRot(180.0F);
                level.addFreshEntity(goblin);
                goblins.add(goblin.getId());
                if (goblin instanceof GoblinTrader) {
                    traderId = goblin.getId();
                } else if (types[i] == ModEntities.GOBLIN_ELDER.get()) {
                    elderId = goblin.getId();
                }
            }
            goblin(level, goblins.get(0)).shout("goblinraiders.speech.small.battle.3", List.of(player));
            goblin(level, goblins.get(1)).say("goblinraiders.speech.crazy.idle.2");
            goblin(level, goblins.get(6)).say("goblinraiders.speech.giant.battle.3");
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "01_goblins")));
        STEPS.add(new Step(20, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            goblin(level, goblins.get(2)).say("goblinraiders.speech.elder.idle.1");
            goblin(level, goblins.get(3)).say("goblinraiders.speech.trader.idle.1");
            goblin(level, goblins.get(5)).say(Speech.conversationLine(0, 0));
            goblin(level, goblins.get(4)).say(Speech.conversationLine(0, 1));
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "02_chatter")));
        // The crazy goblin up close (a slim-armed skin), then back to the line.
        STEPS.add(new Step(20, mc -> server(mc, player -> {
            Entity crazy = player.serverLevel().getEntity(goblins.get(1));
            lineupSpot = player.position();
            player.teleportTo(player.serverLevel(), crazy.getX(), crazy.getY(), crazy.getZ() - 1.6, 0.0F, 12.0F);
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "02b_crazy_close")));
        // Faces: a battle cry (angry), thanks (happy), a call for help (worried), a goblin being hit, one in a fight
        // without a word (snarling) and a child at play (its big head must carry the face too).
        STEPS.add(new Step(30, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            BlockPos base = BlockPos.containing(lineupSpot).offset(0, 0, -12);
            for (int i = 0; i < 6; i++) {
                AbstractGoblin goblin = (AbstractGoblin) (i < 5 ? ModEntities.NEUTRAL_GOBLIN.get() : ModEntities.GOBLIN_CHILD.get()).create(level);
                goblin.moveTo(base.getX() + 0.5 + (i - 2.5) * 1.1, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
                goblin.setNoAi(true);
                goblin.setYHeadRot(0.0F);
                goblin.setYBodyRot(0.0F);
                level.addFreshEntity(goblin);
                faces.add(goblin.getId());
            }
            player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 3.6, 180.0F, 12.0F);
        })));
        STEPS.add(new Step(4, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            String[] lines = {"goblinraiders.speech.small.battle.1", "goblinraiders.speech.trader.thanks.1", "goblinraiders.speech.elder.help.1",
                    null, null, "goblinraiders.speech.child.play.1"};
            for (int i = 0; i < faces.size(); i++) {
                AbstractGoblin goblin = goblin(level, faces.get(i));
                if (lines[i] != null) {
                    goblin.say(lines[i]);
                }
            }
            goblin(level, faces.get(3)).hurt(level.damageSources().generic(), 0.01F);
            goblin(level, faces.get(4)).setAggressive(true);
        })));
        STEPS.add(new Step(5, mc -> shot(mc, "02c_faces")));
        STEPS.add(new Step(10, mc -> shot(mc, "02d_faces")));
        STEPS.add(new Step(10, mc -> server(mc, player -> {
            faces.forEach(id -> goblin(player.serverLevel(), id).discard());
            player.teleportTo(player.serverLevel(), lineupSpot.x, lineupSpot.y, lineupSpot.z, 0.0F, 8.0F);
        })));

        // 2. The trader's dialog with the price list.
        STEPS.add(new Step(20, mc -> ClientDialogs.open(new OpenDialogPayload(traderId, GoblinTrader.createOffers()))));
        STEPS.add(new Step(10, mc -> shot(mc, "03_trader_dialog")));

        // 3. The elder: greeting with topics, then the first page of the first story.
        STEPS.add(new Step(20, mc -> {
            mc.setScreen(null);
            ClientDialogs.open(new OpenDialogPayload(elderId, new MerchantOffers()));
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "04_elder_topics")));
        STEPS.add(new Step(20, mc -> mc.screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button) c)
                .findFirst().ifPresent(Button::onPress)));
        STEPS.add(new Step(10, mc -> shot(mc, "05_elder_story")));

        // 4. Agreeing to trade opens the merchant menu.
        STEPS.add(new Step(30, mc -> {
            mc.setScreen(null);
            server(mc, player -> ((GoblinTrader) player.serverLevel().getEntity(traderId)).onDialogChoice(player, DialogChoicePayload.ACCEPT));
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "06_trading")));

        // 5. Refusing: the trader yells (chat and bubble).
        STEPS.add(new Step(30, mc -> {
            mc.player.closeContainer();
            server(mc, player -> ((GoblinTrader) player.serverLevel().getEntity(traderId)).onDialogChoice(player, DialogChoicePayload.REFUSE));
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "07_trader_angry")));

        // 6. The stun effect (icon in the corner, stars over the head) and the new weapons in the inventory.
        STEPS.add(new Step(20, mc -> server(mc, player -> {
            player.getPersistentData().remove("goblinraiders.stun_immune_until");
            ModEffects.stun(player);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(ModItems.GOBLIN_CLUB.get()));
            player.getInventory().setItem(1, new ItemStack(ModItems.GOBLIN_BOW.get()));
            player.getInventory().setItem(2, new ItemStack(ModItems.GOBLIN_DAGGER.get()));
            player.getInventory().setItem(3, new ItemStack(ModItems.GOBLIN_SWORD.get()));
            player.getInventory().setItem(4, new ItemStack(ModItems.GOBLIN_COIN.get(), 12));
            player.getInventory().setItem(5, new ItemStack(ModItems.TELEPORT_AMULET.get()));
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "08_stunned")));
        // The raid alarm, played the way a real raid plays it (the log shows if the sound file fails to load).
        STEPS.add(new Step(90, mc -> server(mc, player -> player.playNotifySound(ModSounds.RAID_ALARM.get(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F))));
        STEPS.add(new Step(20, mc -> mc.setScreen(new InventoryScreen(mc.player))));
        STEPS.add(new Step(10, mc -> shot(mc, "09_items")));

        // 7. The shaman casting, the king, and a caravan carrier with a sack.
        STEPS.add(new Step(40, mc -> {
            mc.setScreen(null);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                player.removeAllEffects();
                goblins.forEach(id -> {
                    if (level.getEntity(id) != null) {
                        level.getEntity(id).discard();
                    }
                });
                BlockPos base = player.blockPosition().offset(0, 0, 6);
                EntityType<?>[] types = {ModEntities.GOBLIN_SHAMAN.get(), ModEntities.GOBLIN_KING.get(), ModEntities.SMALL_GOBLIN.get()};
                double[] xs = {-3.0, 0.5, 3.5};
                for (int i = 0; i < types.length; i++) {
                    AbstractGoblin goblin = (AbstractGoblin) types[i].create(level);
                    goblin.moveTo(base.getX() + 0.5 + xs[i], base.getY(), base.getZ() + 0.5 + (i == 1 ? 1.5 : 0.0), 180.0F, 0.0F);
                    goblin.finalizeSpawn(level, level.getCurrentDifficultyAt(base), MobSpawnType.SPAWN_EGG, null);
                    goblin.setNoAi(true);
                    if (i == 2) {
                        goblin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LOOT_SACK.get()));
                    }
                    goblin.setYHeadRot(180.0F);
                    goblin.setYBodyRot(180.0F);
                    level.addFreshEntity(goblin);
                    if (i == 1) {
                        kingId = goblin.getId();
                    }
                }
                goblin(level, kingId).say("goblinraiders.speech.king.idle.1");
                level.getEntitiesOfClass(AbstractGoblin.class, player.getBoundingBox().inflate(12.0),
                        g -> g.getType() == ModEntities.GOBLIN_SHAMAN.get()).forEach(g -> g.say("goblinraiders.speech.shaman.spell.1"));
            });
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "10_shaman_king")));

        // 8. The king's dialog: submit or fight, then what a vassal sees.
        STEPS.add(new Step(20, mc -> ClientDialogs.openKing(new OpenKingDialogPayload(kingId, false, 0, 12))));
        STEPS.add(new Step(10, mc -> shot(mc, "11_king_dialog")));
        STEPS.add(new Step(20, mc -> ClientDialogs.openKing(new OpenKingDialogPayload(kingId, true, 50000, 12))));
        STEPS.add(new Step(10, mc -> shot(mc, "12_king_vassal")));

        // 9. The disguise (the player drawn as a goblin, seen from the front), then the crown.
        STEPS.add(new Step(30, mc -> {
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            server(mc, player -> player.addEffect(new MobEffectInstance(ModEffects.DISGUISE, 1200)));
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "13_disguise")));
        STEPS.add(new Step(30, mc -> server(mc, player -> {
            player.removeEffect(ModEffects.DISGUISE);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOBLIN_CROWN.get()));
            var stand = EntityType.ARMOR_STAND.create(player.serverLevel());
            stand.moveTo(player.getX() + 1.5, player.getY(), player.getZ(), 180.0F, 0.0F);
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOBLIN_CROWN.get()));
            player.serverLevel().addFreshEntity(stand);
            player.inventoryMenu.sendAllDataToRemote();
        })));
        STEPS.add(new Step(10, mc -> {
            GoblinRaiders.LOGGER.info("[tour] client head slot: {}", mc.player.getItemBySlot(EquipmentSlot.HEAD));
            shot(mc, "14_crown");
        }));

        // 10. The new items and the curse icon.
        STEPS.add(new Step(20, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.addEffect(new MobEffectInstance(ModEffects.CURSE, 400));
                player.addEffect(new MobEffectInstance(ModEffects.DISGUISE, 1200));
                player.getInventory().clearContent();
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOBLIN_CROWN.get()));
                player.getInventory().setItem(0, new ItemStack(ModItems.CONCEALMENT_AMULET.get()));
                player.getInventory().setItem(1, new ItemStack(ModItems.GOBLIN_GRUB.get(), 5));
                player.getInventory().setItem(2, new ItemStack(ModItems.KING_SCEPTER.get()));
                player.getInventory().setItem(3, new ItemStack(ModItems.VASSAL_CHARTER.get()));
                player.getInventory().setItem(4, new ItemStack(ModItems.LOOT_SACK.get()));
                player.getInventory().setItem(5, new ItemStack(ModItems.GOBLIN_SHAMAN_SPAWN_EGG.get()));
                player.getInventory().setItem(6, new ItemStack(ModItems.GOBLIN_KING_SPAWN_EGG.get()));
            });
        }));
        STEPS.add(new Step(20, mc -> mc.setScreen(new InventoryScreen(mc.player))));
        STEPS.add(new Step(10, mc -> shot(mc, "15_new_items")));
        // The goblin arm in first person: disguised with an empty hand (this used to recurse endlessly and crash).
        STEPS.add(new Step(30, mc -> {
            mc.setScreen(null);
            mc.player.getInventory().selected = 8;
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "16_goblin_arm")));
        STEPS.add(new Step(10, mc -> server(mc, player -> player.removeAllEffects())));

        // 11. Night in a village: two goblins asleep, a watchman by the campfire.
        STEPS.add(new Step(40, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(18000);
            level.getEntitiesOfClass(AbstractGoblin.class, player.getBoundingBox().inflate(30.0), g -> g.getId() != kingId).forEach(g -> g.discard());
            BlockPos base = player.blockPosition();
            level.setBlockAndUpdate(base.offset(0, 0, 5), Blocks.CAMPFIRE.defaultBlockState());
            EntityType<?>[] types = {ModEntities.SMALL_GOBLIN.get(), ModEntities.GIANT_GOBLIN.get(), ModEntities.RUTHLESS_GOBLIN.get()};
            double[][] spots = {{2.5, 3.5}, {-3.0, 5.0}, {1.2, 6.8}};
            float[] yaws = {90.0F, 200.0F, 160.0F};
            for (int i = 0; i < types.length; i++) {
                AbstractGoblin goblin = (AbstractGoblin) types[i].create(level);
                goblin.moveTo(base.getX() + 0.5 + spots[i][0], base.getY(), base.getZ() + 0.5 + spots[i][1], yaws[i], 0.0F);
                goblin.finalizeSpawn(level, level.getCurrentDifficultyAt(base), MobSpawnType.SPAWN_EGG, null);
                goblin.setNoAi(true);
                goblin.setYHeadRot(yaws[i]);
                goblin.setYBodyRot(yaws[i]);
                level.addFreshEntity(goblin);
                if (i < 2) {
                    goblin.fallAsleep();
                    goblin.say("goblinraiders.speech.common.sleep." + (i + 1));
                } else {
                    goblin.say("goblinraiders.speech.common.watch.5");
                }
            }
        })));
        STEPS.add(new Step(240, mc -> shot(mc, "17_night")));   // long enough for the sleepers to snore

        // 12. Small talk with a neutral goblin: greeting, then a piece of advice.
        STEPS.add(new Step(30, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            level.getEntitiesOfClass(AbstractGoblin.class, player.getBoundingBox().inflate(30.0), g -> g.getId() != kingId).forEach(g -> g.discard());
            AbstractGoblin goblin = ModEntities.NEUTRAL_GOBLIN.get().create(level);
            goblin.moveTo(player.getX() - 1.5, player.getY(), player.getZ() + 3.0, 180.0F, 0.0F);
            goblin.setNoAi(true);
            level.addFreshEntity(goblin);
            neutralId = goblin.getId();
            player.getInventory().add(new ItemStack(ModItems.GOBLIN_GRUB.get(), 3));
        })));
        STEPS.add(new Step(20, mc -> ClientDialogs.open(new OpenDialogPayload(neutralId, new MerchantOffers()))));
        STEPS.add(new Step(10, mc -> shot(mc, "18_neutral_dialog")));
        STEPS.add(new Step(20, mc -> pressButton(mc, 3)));
        STEPS.add(new Step(10, mc -> shot(mc, "19_neutral_advice")));

        // 13. The king's vassal dialog with the new "betray" button, then its confirmation.
        STEPS.add(new Step(20, mc -> ClientDialogs.openKing(new OpenKingDialogPayload(kingId, true, 50000, 12))));
        STEPS.add(new Step(10, mc -> shot(mc, "20_king_vassal")));
        STEPS.add(new Step(20, mc -> mc.screen.children().stream().filter(c -> c instanceof Button b
                && b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("goblinraiders.dialog.king.betray").getString()))
                .map(c -> (Button) c).findFirst().ifPresent(Button::onPress)));
        STEPS.add(new Step(10, mc -> shot(mc, "21_king_betray")));

        // 14. The advancement tab, with a few advancements done.
        STEPS.add(new Step(30, mc -> {
            mc.setScreen(null);
            server(mc, player -> {
                for (String name : List.of(GoblinAdvancements.ROBBED, GoblinAdvancements.ELDER, GoblinAdvancements.NEUTRAL_TALK,
                        GoblinAdvancements.TRADE, GoblinAdvancements.NIGHT_THIEF)) {
                    GoblinAdvancements.award(player, name);
                }
            });
        }));
        STEPS.add(new Step(140, mc -> {
            var advancements = mc.player.connection.getAdvancements();
            advancements.setSelectedTab(advancements.get(GoblinRaiders.id("root")), true);
            mc.setScreen(new AdvancementsScreen(advancements));
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "22_advancements")));
        STEPS.add(new Step(10, mc -> mc.setScreen(null)));

        // 15. Goblin children in both looks, next to a small goblin and a giant for scale; a worker child; a disguised chat.
        STEPS.add(new Step(40, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.getEntitiesOfClass(AbstractGoblin.class, player.getBoundingBox().inflate(30.0), g -> g.getId() != kingId).forEach(g -> g.discard());
            BlockPos base = player.blockPosition();
            EntityType<?>[] types = {ModEntities.SMALL_GOBLIN.get(), ModEntities.GOBLIN_CHILD.get(), ModEntities.GOBLIN_CHILD.get(),
                    ModEntities.GIANT_GOBLIN.get(), ModEntities.RUTHLESS_GOBLIN.get()};
            double[] xs = {2.5, 1.0, -0.5, -2.5, 4.5};
            for (int i = 0; i < types.length; i++) {
                AbstractGoblin goblin = (AbstractGoblin) types[i].create(level);
                goblin.moveTo(base.getX() + 0.5 + xs[i], base.getY(), base.getZ() + 4.0, 180.0F, 0.0F);
                goblin.setNoAi(true);
                goblin.setYHeadRot(180.0F);
                goblin.setYBodyRot(180.0F);
                if (goblin instanceof GoblinChild child) {
                    child.setVariant(i == 1 ? GoblinChild.VARIANT_SMALL : GoblinChild.VARIANT_GIANT);
                }
                level.addFreshEntity(goblin);
                if (i == 1) {
                    goblin.say("goblinraiders.speech.child.idle.4");
                    childId = goblin.getId();
                } else if (i == 4) {
                    ruthlessId = goblin.getId();
                }
            }
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "23_children")));
        STEPS.add(new Step(20, mc -> {
            server(mc, player -> ((GoblinChild) player.serverLevel().getEntity(childId)).employ(player, WorkJob.FARMER));
        }));
        STEPS.add(new Step(20, mc -> ClientDialogs.openWorker(new OpenWorkerDialogPayload(childId, WorkJob.FARMER.ordinal(), true,
                GoblinChild.Problem.NO_CHEST.ordinal(), 7))));
        STEPS.add(new Step(10, mc -> shot(mc, "24_worker_dialog")));
        STEPS.add(new Step(20, mc -> ClientDialogs.openKing(new OpenKingDialogPayload(kingId, true, 50000, 12))));
        STEPS.add(new Step(10, mc -> shot(mc, "25_king_shop")));
        STEPS.add(new Step(30, mc -> {
            mc.setScreen(null);
            server(mc, player -> player.addEffect(new MobEffectInstance(ModEffects.DISGUISE, 1200)));
        }));
        STEPS.add(new Step(20, mc -> ClientDialogs.open(new OpenDialogPayload(ruthlessId, new MerchantOffers()))));
        STEPS.add(new Step(10, mc -> shot(mc, "26_talk_ruthless")));
        STEPS.add(new Step(10, mc -> {
            mc.setScreen(null);
            server(mc, player -> player.removeAllEffects());
        }));

        // 16. A peaceful village from above, settlers (a man and a woman) and the sage up close, their windows.
        STEPS.add(new Step(80, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(5000);
            level.getEntitiesOfClass(AbstractGoblin.class, player.getBoundingBox().inflate(40.0)).forEach(g -> g.discard());
            BlockPos origin = player.blockPosition().offset(0, 0, 70);
            player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                    "place structure goblinraiders:goblin_hamlet " + origin.getX() + " " + origin.getY() + " " + origin.getZ());
            hamletCenter = level.getEntitiesOfClass(GoblinHeadman.class, new net.minecraft.world.phys.AABB(origin).inflate(90.0)).stream()
                    .findFirst().map(GoblinHeadman::blockPosition).orElse(origin);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, hamletCenter.getX() + 0.5, hamletCenter.getY() + 18, hamletCenter.getZ() - 34.5, 0.0F, 28.0F);
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "27_hamlet")));
        STEPS.add(new Step(40, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            BlockPos base = hamletCenter.offset(0, 0, -40);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 8.0F);
            double[] xs = {1.6, 0.0, -1.6};
            for (int i = 0; i < 3; i++) {
                AbstractGoblin goblin = (AbstractGoblin) (i == 2 ? ModEntities.GOBLIN_SAGE.get() : ModEntities.GOBLIN_VILLAGER.get()).create(level);
                goblin.moveTo(base.getX() + 0.5 + xs[i], base.getY(), base.getZ() + 3.5, 180.0F, 0.0F);
                goblin.setNoAi(true);
                goblin.setYHeadRot(180.0F);
                goblin.setYBodyRot(180.0F);
                if (goblin instanceof GoblinVillager settler) {
                    settler.setFemale(i == 1);
                    settler.setJob(i == 1 ? WorkJob.COOK : WorkJob.BUILDER);
                    settlerId = settler.getId();
                } else {
                    goblin.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.BOOK));
                    sageId = goblin.getId();
                }
                level.addFreshEntity(goblin);
            }
            ((AbstractGoblin) level.getEntity(settlerId)).say("goblinraiders.speech.cook.idle.2");
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "28_settlers")));
        STEPS.add(new Step(20, mc -> ClientDialogs.open(new OpenDialogPayload(settlerId, new MerchantOffers()))));
        STEPS.add(new Step(10, mc -> shot(mc, "29_settler_talk")));
        STEPS.add(new Step(20, mc -> {
            mc.setScreen(null);
            ClientDialogs.open(new OpenDialogPayload(sageId, new MerchantOffers()));
        }));
        STEPS.add(new Step(20, mc -> pressButton(mc, 0)));
        STEPS.add(new Step(10, mc -> shot(mc, "30_sage")));
        STEPS.add(new Step(10, mc -> mc.setScreen(null)));
    }

    /** The peaceful village tour on a normal world: {@code -Pautotest=hamlet}. */
    private static void buildHamletSteps() {
        // 1. Find the nearest hamlet and fly over it while its chunks generate.
        STEPS.add(new Step(400, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(3000);
            Holder<Structure> hamlet = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_hamlet")));
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(hamlet), player.blockPosition(), 100, false);
            if (found == null) {
                GoblinRaiders.LOGGER.error("[tour] no hamlet found");
                return;
            }
            hamletCenter = found.getFirst();
            GoblinRaiders.LOGGER.info("[tour] hamlet at {}", hamletCenter);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, hamletCenter.getX() + 0.5, 160, hamletCenter.getZ() - 40.5, 0.0F, 40.0F);
        })));
        // 2. Find the headman on his square; look at the whole village from the south-west, high up.
        STEPS.add(new Step(200, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            var headmen = level.getEntitiesOfClass(GoblinHeadman.class, new net.minecraft.world.phys.AABB(hamletCenter).inflate(120.0, 200.0, 120.0));
            GoblinRaiders.LOGGER.info("[tour] headmen around: {}, settlers: {}", headmen.size(),
                    level.getEntitiesOfClass(GoblinVillager.class, new net.minecraft.world.phys.AABB(hamletCenter).inflate(120.0, 200.0, 120.0)).size());
            if (!headmen.isEmpty()) {
                GoblinHeadman headman = headmen.get(0);
                headman.setNoAi(true);
                headmanId = headman.getId();
                hamletCenter = headman.blockPosition();
            }
            look(player, hamletCenter.offset(-38, 38, -38), hamletCenter);
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "h1_overview")));
        STEPS.add(new Step(160, mc -> server(mc, player -> look(player, hamletCenter.offset(48, 30, 20), hamletCenter.offset(0, -4, 0)))));
        STEPS.add(new Step(10, mc -> shot(mc, "h2_overview_east")));
        STEPS.add(new Step(160, mc -> server(mc, player -> look(player, hamletCenter.offset(-20, 22, 50), hamletCenter.offset(0, -4, 0)))));
        STEPS.add(new Step(10, mc -> shot(mc, "h3_overview_north")));
        // 3. The headman up close, then his window: an errand on offer and an errand under way.
        STEPS.add(new Step(60, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            Entity headman = level.getEntity(headmanId);
            if (headman instanceof GoblinHeadman goblin) {
                Vec3 front = headman.position().add(Vec3.directionFromRotation(0.0F, goblin.getYRot()).scale(3.5));
                BlockPos stand = BlockPos.containing(front);
                look(player, new BlockPos(stand.getX(), headman.getBlockY(), stand.getZ()), headman.blockPosition().above());
                goblin.sayRandom("give");
            }
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "h4_headman")));
        STEPS.add(new Step(20, mc -> ClientDialogs.openQuest(new OpenQuestDialogPayload(headmanId, "", 0, 3))));
        STEPS.add(new Step(10, mc -> shot(mc, "h5_quest_offer")));
        STEPS.add(new Step(20, mc -> ClientDialogs.openQuest(new OpenQuestDialogPayload(headmanId, "logs", 9, 3))));
        STEPS.add(new Step(10, mc -> shot(mc, "h6_quest_logs")));
        STEPS.add(new Step(20, mc -> ClientDialogs.openQuest(new OpenQuestDialogPayload(headmanId, "zombies", 4, 3))));
        STEPS.add(new Step(10, mc -> shot(mc, "h7_quest_zombies")));
        // 4. The storehouse: inside the hall, looking at the crates along the back wall.
        STEPS.add(new Step(60, mc -> {
            mc.setScreen(null);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                // The hall: crates two high along the back wall, one high along the sides. Stand in the middle of the
                // hall and look at the middle of the back wall.
                List<BlockPos> crates = crates(level, hamletCenter);
                if (!crates.isEmpty()) {
                    BlockPos first = crates.get(0);
                    List<BlockPos> hall = crates.stream().filter(c -> c.closerThan(first, 30)).toList();
                    int floor = hall.stream().mapToInt(BlockPos::getY).min().orElse(first.getY());
                    List<BlockPos> back = hall.stream().filter(c -> c.getY() > floor).toList();
                    if (back.isEmpty()) {
                        back = hall;
                    }
                    // The back wall runs along x when its upper crates share a z (and the other way round); the hall
                    // opens towards the side crates.
                    boolean alongX = back.stream().mapToInt(BlockPos::getZ).distinct().count() == 1;
                    BlockPos wall = average(back, floor);
                    double side = hall.stream().filter(c -> alongX ? c.getZ() != wall.getZ() : c.getX() != wall.getX())
                            .mapToInt(c -> alongX ? c.getZ() - wall.getZ() : c.getX() - wall.getX()).average().orElse(1);
                    int dir = side >= 0 ? 1 : -1;
                    BlockPos stand = alongX ? wall.offset(0, 0, dir * 5) : wall.offset(dir * 5, 0, 0);
                    look(player, new BlockPos(stand.getX(), floor, stand.getZ()), wall);
                }
            });
        }));
        STEPS.add(new Step(10, mc -> shot(mc, "h8_storehouse")));
        // 5. Settlers at work: stand back from one of them.
        STEPS.add(new Step(100, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.getEntitiesOfClass(GoblinVillager.class, new net.minecraft.world.phys.AABB(hamletCenter).inflate(80.0, 60.0, 80.0)).stream()
                    .filter(v -> v.getWorkplace() != null && !v.isDozing()).forEach(settler -> {
                        if (settlerId != 0) {
                            return;
                        }
                        // A spot a few steps away on open ground with a clear view of the settler's face.
                        BlockPos at = settler.blockPosition();
                        for (int i = 0; i < 16 && settlerId == 0; i++) {
                            double angle = Math.PI * 2 * i / 16;
                            int x = at.getX() + (int) Math.round(Math.cos(angle) * 4.5);
                            int z = at.getZ() + (int) Math.round(Math.sin(angle) * 4.5);
                            BlockPos stand = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                            Vec3 eye = Vec3.atBottomCenterOf(stand).add(0, player.getEyeHeight(), 0);
                            if (Math.abs(stand.getY() - at.getY()) <= 2 && level.clip(new net.minecraft.world.level.ClipContext(eye, settler.getEyePosition(),
                                    net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, player))
                                    .getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                                settler.setNoAi(true);
                                settlerId = settler.getId();
                                look(player, stand, at.above());
                                settler.sayRandom(Speech.IDLE);
                            }
                        }
                    });
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "h9_settler")));
        // 6. Listening: the hamlet by day and by night, then the nearest war camp by day (the log shows what was heard).
        STEPS.add(new Step(400, mc -> server(mc, player -> {
            GoblinRaiders.LOGGER.info("[tour] listening: hamlet by day");
            look(player, hamletCenter.offset(3, 1, 3), hamletCenter);
        })));
        STEPS.add(new Step(400, mc -> server(mc, player -> {
            GoblinRaiders.LOGGER.info("[tour] listening: hamlet at night");
            player.serverLevel().setDayTime(18000);
        })));
        STEPS.add(new Step(200, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(4000);
            Holder<Structure> camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_warcamp")));
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(camp), player.blockPosition(), 100, false);
            if (found == null) {
                GoblinRaiders.LOGGER.error("[tour] no war camp found");
                return;
            }
            BlockPos at = found.getFirst();
            GoblinRaiders.LOGGER.info("[tour] war camp at {}", at);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, at.getX() + 0.5, 140, at.getZ() + 0.5, 0.0F, 60.0F);
        })));
        STEPS.add(new Step(600, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            BlockPos at = player.blockPosition();
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
            GoblinRaiders.LOGGER.info("[tour] listening: war camp by day, standing at y {}", ground);
            player.teleportTo(level, at.getX() + 0.5, ground, at.getZ() + 0.5, 0.0F, 10.0F);
        })));
        STEPS.add(new Step(10, mc -> shot(mc, "h10_war_camp")));
    }

    /** Puts the player at {@code from} (flying when off the ground), looking at {@code at}. */
    private static void look(ServerPlayer player, BlockPos from, BlockPos at) {
        double dx = at.getX() - from.getX();
        double dy = at.getY() + 0.5 - (from.getY() + player.getEyeHeight());
        double dz = at.getZ() - from.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.getAbilities().flying = !player.serverLevel().getBlockState(from.below()).isSolid();
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), from.getX() + 0.5, from.getY(), from.getZ() + 0.5, yaw, pitch);
    }

    /** The storehouse crates around {@code center}, nearest first. */
    private static List<BlockPos> crates(ServerLevel level, BlockPos center) {
        List<BlockPos> crates = new ArrayList<>();
        ChunkPos chunk = new ChunkPos(center);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (BlockEntity entity : level.getChunk(chunk.x + dx, chunk.z + dz).getBlockEntities().values()) {
                    if (entity instanceof StorehouseCrateBlockEntity) {
                        crates.add(entity.getBlockPos());
                    }
                }
            }
        }
        crates.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return crates;
    }

    private static BlockPos average(List<BlockPos> positions, int y) {
        double x = positions.stream().mapToInt(BlockPos::getX).average().orElse(0);
        double z = positions.stream().mapToInt(BlockPos::getZ).average().orElse(0);
        return BlockPos.containing(x, y, z);
    }

    /** Presses the n-th button of the open screen. */
    private static void pressButton(Minecraft mc, int index) {
        mc.screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button) c).skip(index).findFirst().ifPresent(Button::onPress);
    }

    private ClientTour() {
    }
}
