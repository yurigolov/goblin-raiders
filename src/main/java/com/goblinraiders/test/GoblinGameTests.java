package com.goblinraiders.test;

import com.goblinraiders.world.GoblinSoundscapes;
import com.goblinraiders.registry.ModSounds;
import com.goblinraiders.block.StorehouseCrateBlockEntity;
import com.goblinraiders.quest.Quests;
import com.goblinraiders.entity.GoblinHeadman;
import com.goblinraiders.registry.ModTags;
import net.minecraft.world.entity.MobCategory;
import com.goblinraiders.entity.work.Construction;
import com.goblinraiders.block.BuildingSiteBlockEntity;
import com.goblinraiders.entity.GoblinSage;
import com.goblinraiders.entity.GoblinVillager;
import com.goblinraiders.entity.work.WorkSkills;
import com.goblinraiders.entity.work.WorkJob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.CropBlock;
import com.goblinraiders.entity.goal.ChildScuffleGoal;
import com.goblinraiders.entity.GoblinChild;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.entity.Pose;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.AfterBatch;
import com.goblinraiders.entity.GoblinTalk;
import com.goblinraiders.entity.GoblinNight;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.block.GoblinStashBlockEntity;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GiantGoblin;
import com.goblinraiders.entity.CrazyGoblin;
import com.goblinraiders.entity.ElderLore;
import com.goblinraiders.entity.GoblinElder;
import com.goblinraiders.entity.GoblinFriends;
import com.goblinraiders.entity.GoblinKing;
import com.goblinraiders.entity.GoblinShaman;
import com.goblinraiders.entity.GoblinTrader;
import com.goblinraiders.entity.Speech;
import com.goblinraiders.entity.goal.GoblinChatGoal;
import com.goblinraiders.registry.ModEffects;
import com.goblinraiders.network.DialogChoicePayload;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import com.goblinraiders.entity.NeutralGoblin;
import com.goblinraiders.entity.RuthlessGoblin;
import com.goblinraiders.entity.SmallGoblin;
import com.goblinraiders.item.GoblinNoteItem;
import com.goblinraiders.item.RaidNote;
import com.goblinraiders.raid.GoblinRaidData;
import com.goblinraiders.raid.GoblinRaids;
import com.goblinraiders.raid.Caravan;
import com.goblinraiders.raid.Caravans;
import com.goblinraiders.raid.Horde;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import com.goblinraiders.registry.ModBlocks;
import com.goblinraiders.registry.ModDataComponents;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Dev-only game tests: {@code ./gradlew runGameTestServer}. NeoForge only registers them when game tests are enabled.
 * The pen template comes from tools/structures/test_pen.py, the village from tools/structures/goblin_village.py.
 * Relative y = 1 is the floor of the pen (the test structure block sits one block below the template), so things go at y = 2.
 */
@GameTestHolder(GoblinRaiders.MODID)
@PrefixGameTestTemplate(false)
public final class GoblinGameTests {
    private static final String PEN = "test_pen";

    /** A made-up village far away, unique per test position so parallel tests do not share loot. */
    private static BlockPos fakeVillage(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(5_000_000 + origin.getX(), 0, origin.getZ());
    }

    private static ChestBlockEntity placeChest(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, Blocks.CHEST.defaultBlockState());
        return helper.getBlockEntity(rel);
    }

    private static GoblinStashBlockEntity placeStash(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, ModBlocks.GOBLIN_STASH.get().defaultBlockState());
        return helper.getBlockEntity(rel);
    }

    @GameTest(template = PEN)
    public static void raidMovesLootToTheVillageStash(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChestBlockEntity chest = placeChest(helper, new BlockPos(2, 2, 2));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        chest.setItem(5, new ItemStack(Items.IRON_INGOT, 20));
        chest.setItem(13, new ItemStack(Items.BREAD, 3));
        BlockPos village = fakeVillage(helper);

        GoblinRaids.Outcome outcome = GoblinRaids.robChests(level, List.of(chest.getBlockPos()), village, 3, 1.0);
        helper.assertTrue(outcome.result() == GoblinRaids.Result.SUCCESS, "raid failed: " + outcome);
        helper.assertTrue(outcome.stacks() == 3 && outcome.chests() == 1, "unexpected outcome " + outcome);

        List<ItemStack> left = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (!chest.getItem(i).isEmpty()) {
                left.add(chest.getItem(i));
            }
        }
        helper.assertTrue(left.size() == 1 && left.get(0).is(ModItems.GOBLIN_NOTE.get()), "chest should hold only the note: " + left);
        RaidNote note = left.get(0).get(ModDataComponents.RAID_NOTE.get());
        helper.assertTrue(note != null && note.x() == village.getX() && note.z() == village.getZ(), "wrong note " + note);
        helper.assertTrue(chest.getItem(13).is(ModItems.GOBLIN_NOTE.get()), "the note goes in the middle slot");

        GoblinStashBlockEntity stash = placeStash(helper, new BlockPos(5, 2, 2));
        boolean overflow = stash.collectStolenLoot(level, new ChunkPos(village));
        helper.assertTrue(!overflow, "everything should fit");
        int diamonds = 0, iron = 0, bread = 0;
        for (int i = 0; i < stash.getContainerSize(); i++) {
            ItemStack s = stash.getItem(i);
            if (s.is(Items.DIAMOND)) diamonds += s.getCount();
            if (s.is(Items.IRON_INGOT)) iron += s.getCount();
            if (s.is(Items.BREAD)) bread += s.getCount();
        }
        helper.assertTrue(diamonds == 5 && iron == 20 && bread == 3,
                "stash got diamonds=" + diamonds + " iron=" + iron + " bread=" + bread);
        helper.assertTrue(GoblinRaidData.get(level.getServer()).stolenStacks(new ChunkPos(village)) == 0, "loot left in the village data");
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void notesAreNeverStolen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChestBlockEntity chest = placeChest(helper, new BlockPos(2, 2, 2));
        chest.setItem(0, GoblinNoteItem.create(1, 2));
        chest.setItem(1, new ItemStack(Items.DIRT, 10));
        BlockPos village = fakeVillage(helper);

        GoblinRaids.Outcome outcome = GoblinRaids.robChests(level, List.of(chest.getBlockPos()), village, 3, 1.0);
        helper.assertTrue(outcome.stacks() == 1, "only the dirt should go, took " + outcome.stacks());
        RaidNote old = chest.getItem(0).get(ModDataComponents.RAID_NOTE.get());
        helper.assertTrue(old != null && old.x() == 1 && old.z() == 2, "the old note must stay in place");
        GoblinRaidData.get(level.getServer()).takeStolen(new ChunkPos(village));

        GoblinRaids.Outcome again = GoblinRaids.robChests(level, List.of(chest.getBlockPos()), village, 3, 1.0);
        helper.assertTrue(again.result() == GoblinRaids.Result.NOTHING_TO_STEAL, "a chest with only notes has nothing to steal");
        helper.succeed();
    }

    /** A double chest is one chest: one note in it, whichever halves the player's base knows about. */
    @GameTest(template = PEN)
    public static void doubleChestGetsOneNote(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos west = new BlockPos(2, 2, 2);
        BlockPos east = west.east();
        // Facing north, the left half is the west one.
        helper.setBlock(west, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(east, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ChestBlockEntity left = helper.getBlockEntity(west);
        ChestBlockEntity right = helper.getBlockEntity(east);
        BlockPos village = fakeVillage(helper);

        left.setItem(0, new ItemStack(Items.DIRT, 10));
        right.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        GoblinRaids.Outcome outcome = GoblinRaids.robChests(level, List.of(left.getBlockPos(), right.getBlockPos()), village, 3, 1.0);
        helper.assertTrue(outcome.stacks() == 2 && outcome.chests() == 1, "one double chest, two stacks: " + outcome);
        helper.assertTrue(notes(left) + notes(right) == 1, "one note per double chest, found " + (notes(left) + notes(right)));

        // The base may know only one half; the goblins still empty the whole chest, and the same note is not left twice.
        left.setItem(5, new ItemStack(Items.DIRT, 10));
        right.setItem(5, new ItemStack(Items.COBBLESTONE, 10));
        GoblinRaids.Outcome again = GoblinRaids.robChests(level, List.of(left.getBlockPos()), village, 3, 1.0);
        helper.assertTrue(again.stacks() == 2, "both halves are robbed, took " + again.stacks());
        helper.assertTrue(notes(left) + notes(right) == 1, "still one note, found " + (notes(left) + notes(right)));
        GoblinRaidData.get(level.getServer()).takeStolen(new ChunkPos(village));
        helper.succeed();
    }

    private static int notes(ChestBlockEntity chest) {
        int notes = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(ModItems.GOBLIN_NOTE.get())) {
                notes++;
            }
        }
        return notes;
    }

    @GameTest(template = PEN)
    public static void stashKeepsWhatDoesNotFit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos village = new ChunkPos(fakeVillage(helper));
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            loot.add(new ItemStack(Items.COBBLESTONE, 64));
        }
        data.addStolen(village, loot);

        GoblinStashBlockEntity stash = placeStash(helper, new BlockPos(5, 2, 2));
        helper.assertTrue(stash.collectStolenLoot(level, village), "should report leftovers");
        helper.assertTrue(data.stolenStacks(village) == 60 - GoblinStashBlockEntity.SIZE, "leftover stacks: " + data.stolenStacks(village));
        data.takeStolen(village);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void goblinsHaveTheirSizeAndGear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SmallGoblin small = helper.spawn(ModEntities.SMALL_GOBLIN.get(), new BlockPos(2, 2, 2));
        RuthlessGoblin ruthless = helper.spawn(ModEntities.RUTHLESS_GOBLIN.get(), new BlockPos(5, 2, 2));
        NeutralGoblin neutral = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(8, 2, 2));
        GiantGoblin giant = helper.spawn(ModEntities.GIANT_GOBLIN.get(), new BlockPos(10, 2, 4));
        ruthless.finalizeSpawn(level, level.getCurrentDifficultyAt(ruthless.blockPosition()), MobSpawnType.STRUCTURE, null);

        helper.assertTrue(Math.abs(small.getBbHeight() - 1.5F) < 0.01F, "small goblin height " + small.getBbHeight());
        helper.assertTrue(Math.abs(ruthless.getBbHeight() - 1.8F) < 0.01F, "ruthless goblin height " + ruthless.getBbHeight());
        helper.assertTrue(Math.abs(giant.getBbHeight() - 3.6F) < 0.01F, "giant goblin height " + giant.getBbHeight());

        double smallSpeed = small.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double normalSpeed = neutral.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double giantSpeed = giant.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(smallSpeed > normalSpeed && normalSpeed > giantSpeed, "speeds " + smallSpeed + " " + normalSpeed + " " + giantSpeed);
        helper.assertTrue(ruthless.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) > neutral.getAttributeBaseValue(Attributes.ATTACK_DAMAGE),
                "ruthless goblin must hit harder than a neutral one");

        helper.assertTrue(ruthless.getMainHandItem().is(ModItems.GOBLIN_SWORD.get()), "ruthless goblin holds " + ruthless.getMainHandItem());
        helper.assertTrue(ruthless.isPersistenceRequired() && ruthless.getHome() != null, "structure goblins live in their village");
        helper.assertTrue(small.isAlliedTo(giant), "goblins are allies");
        // Equipment modifiers are applied on the next entity tick.
        helper.runAfterDelay(2, () -> {
            double damage = ruthless.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(damage >= 9.9, "ruthless goblin with the sword should deal 10, deals " + damage);
            helper.succeed();
        });
    }

    @GameTest(template = PEN)
    public static void amuletTeleportsToTheBoundPlace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);   // creative players do not wear items down
        BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos target = helper.absolutePos(new BlockPos(10, 2, 5));
        player.teleportTo(level, start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0.0F, 0.0F);

        ItemStack amulet = new ItemStack(ModItems.TELEPORT_AMULET.get());
        amulet.set(ModDataComponents.AMULET_TARGET.get(), GlobalPos.of(level.dimension(), target));
        player.setItemInHand(InteractionHand.MAIN_HAND, amulet);
        amulet.getItem().finishUsingItem(amulet, level, player);

        helper.assertTrue(player.blockPosition().equals(target), "player ended at " + player.blockPosition() + ", expected " + target);
        helper.assertTrue(amulet.getDamageValue() == 1, "amulet should lose durability");
        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.TELEPORT_AMULET.get()), "amulet should be on cooldown");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static final String[] VILLAGE_TYPES = {"goblin_camp", "goblin_village", "goblin_market", "goblin_warcamp", "goblin_megavillage"};
    private static final String[] TEMPLATES = {"camp_1", "camp_2", "village", "village_2", "market_1", "market_2", "warcamp_1", "warcamp_2",
            "mega_1"};

    /** Runs the real worldgen path (jigsaw placement of each village type), not just the template. */
    @GameTest(template = PEN)
    public static void everyVillageTypeGenerates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).containsKey(GoblinRaiders.id("goblin_villages")),
                "goblin_villages structure set is not registered");
        ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        var chunks = level.getChunkSource();
        for (String type : VILLAGE_TYPES) {
            Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id(type));
            helper.assertTrue(structure != null && structure.biomes().size() > 0, type + " is not registered or has no biomes");
            StructureStart start = structure.generate(level.registryAccess(), chunks.getGenerator(), chunks.getGenerator().getBiomeSource(),
                    chunks.randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level, biome -> true);
            helper.assertTrue(start.isValid() && !start.getPieces().isEmpty(), type + " generated no pieces");
            BoundingBox box = start.getBoundingBox();
            helper.assertTrue(box.getXSpan() >= 30 && box.getZSpan() >= 30, type + " is too small: " + box);
            // The start jigsaw sits at the campfire, so the chunk corner (what raid notes point at) is the village center.
            int dx = (box.minX() + box.maxX()) / 2 - chunk.getMinBlockX();
            int dz = (box.minZ() + box.maxZ()) / 2 - chunk.getMinBlockZ();
            helper.assertTrue(Math.abs(dx) <= 3 && Math.abs(dz) <= 3, type + " center is off the chunk corner by " + dx + ", " + dz);
        }
        helper.succeed();
    }

    /**
     * The test world is flat, so this builds a regular overworld generator (real terrain and biomes) and checks that
     * every village type comes out of the structure set's placement spots in 8x8 regions around the origin.
     */
    @GameTest(template = PEN)
    public static void villagesAppearInANormalOverworld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryAccess access = level.registryAccess();
        Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        BiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(
                access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        ChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        long seed = 20261025L;
        RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        StructureSet set = access.registryOrThrow(Registries.STRUCTURE_SET).get(GoblinRaiders.id("goblin_villages"));
        helper.assertTrue(set != null && set.structures().size() == VILLAGE_TYPES.length, "structure set is missing village types");
        RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();

        StringBuilder report = new StringBuilder();
        for (String type : VILLAGE_TYPES) {
            Structure structure = access.registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id(type));
            int regions = 0, villages = 0;
            for (int rx = -4; rx < 4; rx++) {
                for (int rz = -4; rz < 4; rz++) {
                    ChunkPos chunk = placement.getPotentialStructureChunk(seed, rx * placement.spacing(), rz * placement.spacing());
                    StructureStart start = structure.generate(access, generator, biomes, random, level.getStructureManager(), seed, chunk, 0,
                            level, structure.biomes()::contains);
                    regions++;
                    if (start.isValid()) {
                        villages++;
                    }
                }
            }
            report.append(type).append(' ').append(villages).append('/').append(regions).append("  ");
            helper.assertTrue(villages > 0, type + " never fits in " + regions + " regions of a normal overworld");
        }
        GoblinRaiders.LOGGER.info("[test] possible village spots: {}", report);
        helper.succeed();
    }

    /** Every template has exactly one stash and one start jigsaw (raids and notes rely on both). */
    @GameTest(template = PEN)
    public static void everyTemplateHasStashAndCenter(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        for (String name : TEMPLATES) {
            var template = manager.get(GoblinRaiders.id("goblin_village/" + name));
            helper.assertTrue(template.isPresent(), "missing template " + name);
            StructurePlaceSettings settings = new StructurePlaceSettings();
            int stashes = template.get().filterBlocks(BlockPos.ZERO, settings, ModBlocks.GOBLIN_STASH.get()).size();
            long centers = template.get().filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW).stream()
                    .filter(info -> info.nbt() != null && "goblinraiders:village_center".equals(info.nbt().getString("name"))).count();
            helper.assertTrue(stashes == 1 && centers == 1, name + ": " + stashes + " stashes, " + centers + " centers");
        }
        helper.succeed();
    }

    private static void assertGoblins(GameTestHelper helper, EntityType<?> special, int minimum) {
        List<AbstractGoblin> goblins = helper.getLevel().getEntitiesOfClass(AbstractGoblin.class, helper.getBounds());
        helper.assertTrue(goblins.size() >= minimum, "expected at least " + minimum + " goblins, found " + goblins.size());
        long count = goblins.stream().filter(g -> g.getType() == special).count();
        helper.assertTrue(count == 1, "expected one " + special.getDescriptionId() + ", found " + count);
        assertGiantsOutdoors(helper);
    }

    /** The test runner shuts every test in a barrier box with a ceiling; tests about the open sky take the ceiling off. */
    private static void openTheSky(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds();
        int top = (int) Math.floor(box.maxY);
        BlockPos.betweenClosed((int) Math.floor(box.minX) - 1, top - 1, (int) Math.floor(box.minZ) - 1,
                (int) Math.floor(box.maxX) + 1, top + 2, (int) Math.floor(box.maxZ) + 1).forEach(pos -> {
            if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    /** A giant does not fit through a door, so a village never puts one inside a building. */
    private static void assertGiantsOutdoors(GameTestHelper helper) {
        openTheSky(helper);
        for (GiantGoblin giant : helper.getLevel().getEntitiesOfClass(GiantGoblin.class, helper.getBounds())) {
            helper.assertTrue(AbstractGoblin.canWalkOut(giant.getType(), helper.getLevel(), giant.blockPosition()),
                    "a giant under a roof at " + helper.relativePos(giant.blockPosition()));
        }
    }

    @GameTest(template = "goblin_village/village", timeoutTicks = 40)
    public static void classicVillageHasTheElder(GameTestHelper helper) {
        assertGoblins(helper, ModEntities.GOBLIN_ELDER.get(), 8);
        helper.succeed();
    }

    @GameTest(template = "goblin_village/market_1", timeoutTicks = 40)
    public static void marketHasTheTrader(GameTestHelper helper) {
        assertGoblins(helper, ModEntities.GOBLIN_TRADER.get(), 6);
        helper.succeed();
    }

    /** {@code /locate structure goblinraiders:<type>} must know every type (the test world has no structures, so it reports "not found"). */
    @GameTest(template = PEN)
    public static void locateKnowsEveryVillageType(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withPosition(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withSuppressedOutput();
        for (String type : VILLAGE_TYPES) {
            String key = null;
            try {
                server.getCommands().getDispatcher().execute("locate structure goblinraiders:" + type, source);
                key = "found";
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                if (e.getRawMessage() instanceof net.minecraft.network.chat.Component c
                        && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                    key = t.getKey();
                } else {
                    key = e.getMessage();
                }
            }
            helper.assertTrue("found".equals(key) || "commands.locate.structure.not_found".equals(key),
                    "/locate structure goblinraiders:" + type + " -> " + key);
        }
        helper.succeed();
    }

    @GameTest(template = "goblin_village/mega_1", timeoutTicks = 40)
    public static void megaVillageHasEveryone(GameTestHelper helper) {
        List<AbstractGoblin> goblins = helper.getLevel().getEntitiesOfClass(AbstractGoblin.class, helper.getBounds());
        helper.assertTrue(goblins.size() >= 35, "expected at least 35 goblins in the mega village, found " + goblins.size());
        helper.assertTrue(goblins.stream().filter(g -> g.getType() == ModEntities.GOBLIN_TRADER.get()).count() == 1, "the mega village needs its trader");
        helper.assertTrue(goblins.stream().filter(g -> g.getType() == ModEntities.GOBLIN_ELDER.get()).count() == 1, "the mega village needs its elder");
        helper.assertTrue(goblins.stream().filter(g -> g.getType() == ModEntities.GOBLIN_KING.get()).count() == 1, "the throne hall needs its king");
        helper.assertTrue(goblins.stream().anyMatch(g -> g.getType() == ModEntities.GOBLIN_SHAMAN.get()), "the mega village needs a shaman");
        helper.assertTrue(goblins.stream().filter(g -> g.getType() == ModEntities.GOBLIN_CHILD.get()).count() >= 4, "the mega village has children");
        assertGiantsOutdoors(helper);
        helper.succeed();
    }

    @GameTest(template = "goblin_village/warcamp_1", timeoutTicks = 40)
    public static void warcampHasTheShaman(GameTestHelper helper) {
        assertGoblins(helper, ModEntities.GOBLIN_SHAMAN.get(), 8);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void goblinsOnlyChatSideBySide(GameTestHelper helper) {
        NeutralGoblin a = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(2, 2, 2));
        NeutralGoblin near = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(4, 2, 2));
        NeutralGoblin far = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(7, 2, 2));
        NeutralGoblin walled = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(2, 2, 4));
        for (int y = 2; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE.defaultBlockState());
            }
        }
        for (NeutralGoblin g : List.of(a, near, far, walled)) {
            g.setNoAi(true);
        }
        helper.assertTrue(GoblinChatGoal.canTalk(a, near), "goblins two blocks apart should be able to chat");
        helper.assertTrue(!GoblinChatGoal.canTalk(a, far), "goblins five blocks apart must not chat");
        helper.assertTrue(!GoblinChatGoal.canTalk(a, walled), "a wall between goblins must stop the chat");
        helper.succeed();
    }

    // ---------------------------------------------------------------- weapons

    @GameTest(template = PEN)
    public static void weaponsHaveTheirStats(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        zombie.setNoAi(true);
        // Dagger: iron sword damage (6 for a player), 1.5x its attack speed.
        double daggerDamage = attribute(ModItems.GOBLIN_DAGGER.get(), Attributes.ATTACK_DAMAGE, 1.0);
        double ironDamage = attribute(Items.IRON_SWORD, Attributes.ATTACK_DAMAGE, 1.0);
        double daggerSpeed = attribute(ModItems.GOBLIN_DAGGER.get(), Attributes.ATTACK_SPEED, 4.0);
        double ironSpeed = attribute(Items.IRON_SWORD, Attributes.ATTACK_SPEED, 4.0);
        helper.assertTrue(Math.abs(daggerDamage - ironDamage) < 0.01, "dagger damage " + daggerDamage + " vs iron sword " + ironDamage);
        helper.assertTrue(Math.abs(daggerSpeed - ironSpeed * 1.5) < 0.01, "dagger speed " + daggerSpeed + " vs iron sword " + ironSpeed);

        // Club: stuns, and the target cannot be stunned again right away.
        helper.assertTrue(ModEffects.stun(zombie), "first club hit must stun");
        helper.assertTrue(zombie.hasEffect(ModEffects.STUNNED), "target should have the stunned effect");
        helper.assertTrue(!ModEffects.stun(zombie), "no stun lock: the next hit within a few seconds does not stun again");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.001, "a stunned mob cannot move");
            zombie.discard();
            helper.succeed();
        });
    }

    private static double attribute(net.minecraft.world.item.Item item, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                    double base) {
        double[] value = {base};
        new ItemStack(item).forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attr, modifier) -> {
            if (attr.equals(attribute)) {
                value[0] += modifier.amount();
            }
        });
        return value[0];
    }

    @GameTest(template = PEN)
    public static void goblinArchersShootHarder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SmallGoblin archer = helper.spawn(ModEntities.SMALL_GOBLIN.get(), new BlockPos(2, 2, 4));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(11, 2, 4));
        target.setNoAi(true);
        archer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_BOW.get()));
        archer.performRangedAttack(target, 1.0F);
        List<net.minecraft.world.entity.projectile.AbstractArrow> arrows = level.getEntitiesOfClass(
                net.minecraft.world.entity.projectile.AbstractArrow.class, helper.getBounds());
        helper.assertTrue(!arrows.isEmpty(), "the goblin archer did not shoot");
        arrows.forEach(net.minecraft.world.entity.Entity::discard);
        // Vanilla adds a random spread to mob arrows, so the multiplier is checked on a plain arrow.
        var arrow = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, level);
        double base = arrow.getBaseDamage();
        ItemStack bow = new ItemStack(ModItems.GOBLIN_BOW.get());
        double damage = ((net.minecraft.world.item.ProjectileWeaponItem) bow.getItem()).customArrow(arrow, new ItemStack(Items.ARROW), bow).getBaseDamage();
        helper.assertTrue(Math.abs(damage - base * 1.5) < 0.001, "goblin bow arrows should hit 1.5x harder: " + base + " -> " + damage);
        target.discard();
        helper.succeed();
    }

    // ---------------------------------------------------------------- the new goblins

    @GameTest(template = PEN)
    public static void crazyGoblinIsFastAndWeak(GameTestHelper helper) {
        CrazyGoblin crazy = helper.spawn(ModEntities.CRAZY_GOBLIN.get(), new BlockPos(2, 2, 2));
        SmallGoblin small = helper.spawn(ModEntities.SMALL_GOBLIN.get(), new BlockPos(4, 2, 2));
        GoblinElder elder = helper.spawn(ModEntities.GOBLIN_ELDER.get(), new BlockPos(6, 2, 2));
        GoblinTrader trader = helper.spawn(ModEntities.GOBLIN_TRADER.get(), new BlockPos(8, 2, 2));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 2, 4));
        helper.assertTrue(Math.abs(crazy.getBbHeight() - small.getBbHeight()) < 0.01F, "crazy goblin should be as small as a small goblin");
        helper.assertTrue(Math.abs(elder.getBbHeight() - small.getBbHeight()) < 0.01F, "elder should be as small as a small goblin");
        helper.assertTrue(Math.abs(trader.getBbHeight() - 1.8F) < 0.01F, "trader height " + trader.getBbHeight());
        helper.assertTrue(crazy.getAttributeValue(Attributes.MOVEMENT_SPEED) > small.getAttributeValue(Attributes.MOVEMENT_SPEED),
                "crazy goblin must be the fastest");
        double half = zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) / 2.0;
        helper.assertTrue(Math.abs(crazy.getAttributeValue(Attributes.ATTACK_DAMAGE) - half) < 0.01,
                "crazy goblin damage " + crazy.getAttributeValue(Attributes.ATTACK_DAMAGE) + ", zombie half " + half);
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void traderTradesOrGetsAngry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GoblinTrader trader = helper.spawn(ModEntities.GOBLIN_TRADER.get(), new BlockPos(6, 2, 4));
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
        player.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);

        helper.assertTrue(trader.getOffers().size() >= 10, "trader has only " + trader.getOffers().size() + " offers");
        helper.assertTrue(trader.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.GOBLIN_SWORD.get())), "trader must sell the sword");
        helper.assertTrue(trader.getOffers().stream().allMatch(o -> o.getCostA().is(ModItems.GOBLIN_COIN.get()) || o.getResult().is(ModItems.GOBLIN_COIN.get())),
                "every trade goes through goblin coins");

        trader.onDialogChoice(player, DialogChoicePayload.ACCEPT);
        helper.assertTrue(player.containerMenu instanceof MerchantMenu, "accepting must open the merchant menu, got " + player.containerMenu);
        helper.assertTrue(trader.getTradingPlayer() == player, "trader should be trading with the player");
        player.closeContainer();
        helper.assertTrue(trader.getTradingPlayer() == null, "closing the menu ends the trade");

        trader.onDialogChoice(player, DialogChoicePayload.REFUSE);
        helper.assertTrue(trader.getTarget() == player && trader.isAngry(), "refusing must make the trader attack");
        helper.assertTrue(trader.isSpeaking(), "an angry trader yells");
        helper.assertTrue(trader.isAngryAt(player), "the trader remembers who refused");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void elderNeverFightsButGoblinsDefendHim(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GoblinElder elder = helper.spawn(ModEntities.GOBLIN_ELDER.get(), new BlockPos(4, 2, 4));
        NeutralGoblin guard = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(8, 2, 4));
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 4));
        player.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);

        elder.setTarget(player);
        helper.assertTrue(elder.getTarget() == null, "the elder never targets anyone");
        elder.hurt(level.damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(elder.getTarget() == null, "the elder does not fight back");
        helper.assertTrue(guard.getTarget() == player, "goblins around must defend the elder");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void goblinsShowAndHideSpeech(GameTestHelper helper) {
        SmallGoblin small = helper.spawn(ModEntities.SMALL_GOBLIN.get(), new BlockPos(4, 2, 4));
        small.setNoAi(true);   // no new lines (a battle cry at another test's player) while the bubble runs out
        small.sayRandom(Speech.IDLE);
        helper.assertTrue(small.isSpeaking() && small.getSpeech().startsWith("goblinraiders.speech.small.idle."), "speech: " + small.getSpeech());
        helper.runAfterDelay(AbstractGoblin.SPEECH_TICKS + 2, () -> {
            helper.assertTrue(!small.isSpeaking(), "speech bubble should disappear");
            helper.succeed();
        });
    }

    /** Every line the code can show exists in both language files. */
    @GameTest(template = PEN)
    public static void everyLineIsTranslated(GameTestHelper helper) {
        List<String> keys = new ArrayList<>(Speech.allKeys());
        for (int i = 0; i < ElderLore.TOPICS.length; i++) {
            keys.add("goblinraiders.dialog.elder.topic." + ElderLore.TOPICS[i]);
            for (int page = 1; page <= ElderLore.PAGES[i]; page++) {
                keys.add("goblinraiders.dialog.elder." + ElderLore.TOPICS[i] + "." + page);
            }
        }
        for (int g = 1; g <= ElderLore.GREETINGS; g++) {
            keys.add("goblinraiders.dialog.elder.greeting." + g);
            keys.add("goblinraiders.dialog.trader.greeting." + g);
        }
        for (var item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(GoblinRaiders.MODID)) {
                keys.add(item.getDescriptionId());
            }
        }
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(GoblinRaiders.MODID)) {
                keys.add(type.getDescriptionId());
            }
        }
        for (var effect : BuiltInRegistries.MOB_EFFECT) {
            if (BuiltInRegistries.MOB_EFFECT.getKey(effect).getNamespace().equals(GoblinRaiders.MODID)) {
                keys.add(effect.getDescriptionId());
            }
        }
        for (String dir : new String[] {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"}) {
            keys.add("goblinraiders.direction." + dir);
        }
        for (String key : new String[] {"greeting", "terms", "submit", "fight", "vassal", "due", "overdue", "coins", "pay", "hire_small",
                "hire_ruthless", "hire_giant", "bye"}) {
            keys.add("goblinraiders.dialog.king." + key);
        }
        for (String name : GoblinAdvancements.ALL) {
            keys.add("advancements.goblinraiders." + name + ".title");
            keys.add("advancements.goblinraiders." + name + ".description");
        }
        keys.addAll(GoblinTalk.allKeys());
        for (String topic : GoblinTalk.TOPICS) {
            keys.add("goblinraiders.dialog.neutral.topic." + topic);
        }
        for (String key : new String[] {"treat", "bye", "stash", "no_stash"}) {
            keys.add("goblinraiders.dialog.neutral." + key);
        }
        for (String key : new String[] {"betray", "betray_confirm", "betray_yes", "betray_no"}) {
            keys.add("goblinraiders.dialog.king." + key);
        }
        keys.add("goblinraiders.horde.betrayed");
        for (String key : new String[] {"stay", "follow", "farm", "mine", "hand_over", "bye", "job.farmer", "job.miner", "following",
                "working.farmer", "working.miner", "carried"}) {
            keys.add("goblinraiders.dialog.worker." + key);
        }
        for (GoblinChild.Problem problem : GoblinChild.Problem.values()) {
            if (problem != GoblinChild.Problem.NONE) {
                keys.add("goblinraiders.dialog.worker.problem." + problem.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
        for (String key : new String[] {"buy_farmer", "buy_miner", "buy_farmer.tip", "buy_miner.tip"}) {
            keys.add("goblinraiders.dialog.king." + key);
        }
        keys.add("goblinraiders.horde.worker_farmer");
        for (WorkJob job : WorkJob.values()) {
            keys.add("entity.goblinraiders.goblin_villager." + job.key());
            keys.add("entity.goblinraiders.goblin_villager." + job.key() + ".female");
        }
        for (String blueprint : Construction.BLUEPRINTS) {
            keys.add("goblinraiders.blueprint." + blueprint);
        }
        for (String key : new String[] {"site", "house_built", "built"}) {
            keys.add("goblinraiders.hamlet." + key);
        }
        for (String place : GoblinSage.places()) {
            keys.add("goblinraiders.sage.vision." + place);
            keys.add("goblinraiders.sage.vision." + place + ".none");
        }
        for (String key : new String[] {"tired", "vision", "no_coins", "blessed"}) {
            keys.add("goblinraiders.sage." + key);
        }
        for (String key : new String[] {"wisdom", "prophecy", "blessing", "bye", "prophecy_wait", "blessing_wait"}) {
            keys.add("goblinraiders.dialog.sage." + key);
        }
        keys.add("goblinraiders.dialog.neutral.trade");
        for (Quests.Quest quest : Quests.ALL) {
            keys.add("goblinraiders.quest." + quest.id() + ".title");
            keys.add("goblinraiders.quest." + quest.id() + ".text");
        }
        for (String key : new String[] {"take", "turn_in", "drop", "bye", "offer", "completed", "carried", "progress", "ready", "reward"}) {
            keys.add("goblinraiders.dialog.headman." + key);
        }
        for (String key : new String[] {"goblinraiders.quest.done", "goblinraiders.quest.progress", "goblinraiders.sign.notice_board",
                "goblinraiders.sign.storehouse", "container.goblinraiders.storehouse_crate"}) {
            keys.add(key);
        }
        keys.add("goblinraiders.horde.worker_miner");
        for (String lang : new String[] {"en_us", "ru_ru"}) {
            try (var in = GoblinGameTests.class.getResourceAsStream("/assets/goblinraiders/lang/" + lang + ".json")) {
                helper.assertTrue(in != null, "missing " + lang);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                List<String> missing = keys.stream().filter(k -> !json.has(k)).toList();
                helper.assertTrue(missing.isEmpty(), lang + " lacks " + missing);
            } catch (java.io.IOException e) {
                helper.fail(e.toString());
            }
        }
        helper.succeed();
    }

    /**
     * Every sound event is in sounds.json with a subtitle in both languages; every file it plays exists and every event
     * it borrows from the mod is defined too.
     */
    @GameTest(template = PEN)
    public static void everySoundIsDefined(GameTestHelper helper) {
        JsonObject sounds = readJson(helper, "/assets/goblinraiders/sounds.json");
        JsonObject ru = readJson(helper, "/assets/goblinraiders/lang/ru_ru.json");
        JsonObject en = readJson(helper, "/assets/goblinraiders/lang/en_us.json");
        List<String> problems = new ArrayList<>();
        for (var entry : ModSounds.REGISTER.getEntries()) {
            String name = entry.getId().getPath();
            if (!sounds.has(name)) {
                problems.add(name + " not in sounds.json");
                continue;
            }
            JsonObject event = sounds.getAsJsonObject(name);
            String subtitle = event.get("subtitle").getAsString();
            if (!ru.has(subtitle) || !en.has(subtitle)) {
                problems.add(subtitle + " untranslated");
            }
            for (var element : event.getAsJsonArray("sounds")) {
                JsonObject sound = element.getAsJsonObject();
                String id = sound.get("name").getAsString();
                boolean isEvent = sound.has("type") && "event".equals(sound.get("type").getAsString());
                if (id.startsWith(GoblinRaiders.MODID + ":")) {
                    String path = id.substring(GoblinRaiders.MODID.length() + 1);
                    if (isEvent ? !sounds.has(path) : GoblinGameTests.class.getResource("/assets/goblinraiders/sounds/" + path + ".ogg") == null) {
                        problems.add(name + " plays missing " + id);
                    }
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "sound problems: " + problems);
        helper.succeed();
    }

    /** Two goblins asleep side by side both snore, taking turns: never one over the other. */
    @GameTest(template = PEN, timeoutTicks = 500)
    public static void sleepersSnoreInTurn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SmallGoblin left = helper.spawn(ModEntities.SMALL_GOBLIN.get(), new BlockPos(4, 2, 3));
        SmallGoblin right = helper.spawn(ModEntities.SMALL_GOBLIN.get(), new BlockPos(6, 2, 3));
        for (SmallGoblin sleeper : List.of(left, right)) {
            sleeper.setNoAi(true);
            sleeper.fallAsleep();
        }
        AABB pen = helper.getBounds();
        List<long[]> snores = new ArrayList<>();   // game time, x
        java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == level && event.getSound() != null && event.getSound().value() == ModSounds.GOBLIN_SNORE.get()
                    && pen.contains(event.getPosition())) {
                snores.add(new long[] {level.getGameTime(), Math.round(event.getPosition().x * 10)});
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        helper.runAfterDelay(420, () -> {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            long sides = snores.stream().mapToLong(snore -> snore[1]).distinct().count();
            helper.assertTrue(sides == 2, "both sleepers snore: " + snores.size() + " snores from " + sides);
            for (int i = 1; i < snores.size(); i++) {
                helper.assertTrue(snores.get(i)[0] - snores.get(i - 1)[0] >= 45, "a snore over another at tick " + snores.get(i)[0]);
            }
            helper.succeed();
        });
    }

    /** Every goblin settlement sounds like something by day and by night. */
    @GameTest(template = PEN)
    public static void everySettlementHasItsSounds(GameTestHelper helper) {
        for (GoblinSoundscapes.Place place : GoblinSoundscapes.Place.values()) {
            for (boolean night : new boolean[] {false, true}) {
                List<GoblinSoundscapes.Cue> cues = GoblinSoundscapes.cues(place, night);
                helper.assertTrue(cues != null && !cues.isEmpty() && cues.stream().allMatch(c -> c.weight() > 0 && c.volume() > 0.0F),
                        place + (night ? " at night" : " by day") + " is silent");
            }
        }
        helper.succeed();
    }

    private static JsonObject readJson(GameTestHelper helper, String path) {
        try (var in = GoblinGameTests.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- disguise, grub, caravans, shaman, king

    /**
     * A mock player that really plays in survival. The helper's own mock player always reports creative, and goblins
     * (like every mob) never pick creative players as targets.
     */
    private static ServerPlayer survivalPlayer(GameTestHelper helper, BlockPos rel) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-survivor"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(rel);
        player.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        GoblinRaidData.get(player.server).removeVassal(player.getUUID());
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    @GameTest(template = PEN)
    public static void disguisedPlayersAreLeftAlone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        RuthlessGoblin goblin = helper.spawn(ModEntities.RUTHLESS_GOBLIN.get(), new BlockPos(6, 2, 4));
        RuthlessGoblin other = helper.spawn(ModEntities.RUTHLESS_GOBLIN.get(), new BlockPos(9, 2, 4));
        goblin.setNoAi(true);
        other.setNoAi(true);
        helper.assertTrue(goblin.wantsToAttack(player), "a goblin attacks an undisguised player");

        // Using the amulet: the disguise for a minute, the goblins chasing the player lose track.
        goblin.setTarget(player);
        other.setTarget(player);
        ItemStack amulet = new ItemStack(ModItems.CONCEALMENT_AMULET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, amulet);
        amulet.use(level, player, InteractionHand.MAIN_HAND);
        var disguise = player.getEffect(ModEffects.DISGUISE);
        helper.assertTrue(disguise != null && disguise.getDuration() == 1200, "the amulet disguises for 60 s: " + disguise);
        helper.assertTrue(!goblin.wantsToAttack(player) && GoblinFriends.isFriend(player), "goblins leave a disguised player alone");
        helper.assertTrue(goblin.getTarget() == null && other.getTarget() == null, "goblins chasing the player lose track of them");
        helper.assertTrue(amulet.getDamageValue() == 1 && player.getCooldowns().isOnCooldown(ModItems.CONCEALMENT_AMULET.get()),
                "the amulet wears and cools down");

        // Striking a goblin breaks the disguise, and that goblin is not fooled by a new one.
        goblin.hurt(level.damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(!player.hasEffect(ModEffects.DISGUISE), "hitting a goblin must end the disguise");
        helper.assertTrue(goblin.holdsGrudge(player) && !other.holdsGrudge(player), "only the goblin that was hit holds a grudge");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.DISGUISE, 200));
        goblin.setTarget(player);
        GoblinFriends.calmGoblinsAround(player);
        helper.assertTrue(goblin.getTarget() == player, "a goblin that was just hit keeps fighting");

        // Refusing the trader breaks it too.
        GoblinTrader trader = helper.spawn(ModEntities.GOBLIN_TRADER.get(), new BlockPos(4, 2, 2));
        trader.onDialogChoice(player, DialogChoicePayload.REFUSE);
        helper.assertTrue(!player.hasEffect(ModEffects.DISGUISE), "refusing the trader must end the disguise");
        helper.assertTrue(trader.holdsGrudge(player), "the insulted trader is not fooled by a disguise");

        // The goblin king's crown works like a permanent disguise.
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOBLIN_CROWN.get()));
        helper.assertTrue(!other.wantsToAttack(player), "goblins do not attack the wearer of the crown");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void crownIsANetheriteHelmet(GameTestHelper helper) {
        double[] crown = armor(new ItemStack(ModItems.GOBLIN_CROWN.get()));
        double[] netherite = armor(new ItemStack(Items.NETHERITE_HELMET));
        helper.assertTrue(crown[0] == netherite[0] && crown[1] == netherite[1] && crown[2] == netherite[2],
                "crown armor/toughness/knockback " + java.util.Arrays.toString(crown) + " vs netherite " + java.util.Arrays.toString(netherite));
        helper.assertTrue(new ItemStack(ModItems.GOBLIN_CROWN.get()).getMaxDamage() == new ItemStack(Items.NETHERITE_HELMET).getMaxDamage(),
                "crown durability " + new ItemStack(ModItems.GOBLIN_CROWN.get()).getMaxDamage());
        helper.assertTrue(new ItemStack(ModItems.GOBLIN_CROWN.get()).has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT),
                "the crown survives lava like netherite");
        helper.succeed();
    }

    private static double[] armor(ItemStack stack) {
        double[] value = new double[3];
        stack.forEachModifier(EquipmentSlot.HEAD, (attr, modifier) -> {
            if (attr.equals(Attributes.ARMOR)) value[0] += modifier.amount();
            if (attr.equals(Attributes.ARMOR_TOUGHNESS)) value[1] += modifier.amount();
            if (attr.equals(Attributes.KNOCKBACK_RESISTANCE)) value[2] += modifier.amount();
        });
        return value;
    }

    @GameTest(template = PEN)
    public static void grubFillsTwiceAsMuchAsAnApple(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FoodProperties grub = new ItemStack(ModItems.GOBLIN_GRUB.get()).getFoodProperties(null);
        FoodProperties apple = new ItemStack(Items.APPLE).getFoodProperties(null);
        helper.assertTrue(grub != null && apple != null, "both must be food");
        helper.assertTrue(grub.nutrition() == 2 * apple.nutrition() && Math.abs(grub.saturation() - 2 * apple.saturation()) < 0.01F,
                "grub " + grub.nutrition() + "/" + grub.saturation() + " vs apple " + apple.nutrition() + "/" + apple.saturation());

        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 4));
        player.getFoodData().setFoodLevel(4);
        ItemStack stack = new ItemStack(ModItems.GOBLIN_GRUB.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ItemStack left = stack.finishUsingItem(level, player);
        var hunger = player.getEffect(MobEffects.HUNGER);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 12, "food level after the grub " + player.getFoodData().getFoodLevel());
        helper.assertTrue(hunger != null && hunger.getDuration() == 200, "grub gives hunger for 10 s: " + hunger);
        helper.assertTrue(left.is(Items.BOWL), "the bowl stays: " + left);
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN, timeoutTicks = 60)
    public static void caravanCarriesTheLootHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 2));
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            loot.add(new ItemStack(Items.GOLD_INGOT, 1 + i));
        }
        BlockPos village = fakeVillage(helper);
        Caravan caravan = Caravans.start(level, player.getUUID(), helper.absolutePos(new BlockPos(8, 2, 4)), village, loot);
        helper.assertTrue(caravan.shares.size() == 3, "20 stacks need 3 carriers, got " + caravan.shares.size());

        // The player is close: the caravan turns into goblins with sacks and guards.
        Caravans.materialize(level, caravan, level.getGameTime());
        List<AbstractGoblin> members = level.getEntitiesOfClass(AbstractGoblin.class, helper.getBounds().inflate(4.0),
                g -> caravan.id.equals(g.getCaravanId()));
        List<AbstractGoblin> carriers = members.stream().filter(AbstractGoblin::isCarryingLoot).toList();
        helper.assertTrue(carriers.size() == 3 && members.size() == 5, "carriers " + carriers.size() + ", members " + members.size());
        helper.assertTrue(carriers.stream().allMatch(g -> g.getMainHandItem().is(ModItems.LOOT_SACK.get()) && !g.wantsToAttack(player)),
                "carriers hold sacks and do not fight");
        helper.assertTrue(Caravans.destinationOf(carriers.get(0)).getX() == village.getX(), "carriers head for their village");

        // Killing a carrier drops its share.
        AbstractGoblin carrier = carriers.get(0);
        int share = carrier.getCaravanShare();
        int count = caravan.shares.get(share).size();
        carrier.hurt(level.damageSources().playerAttack(player), 1000.0F);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4.0), e -> e.getItem().is(Items.GOLD_INGOT));
        helper.assertTrue(drops.size() >= count, "the carrier dropped " + drops.size() + " of " + count + " stacks");
        helper.assertTrue(caravan.shares.get(share).isEmpty() && caravan.hasLoot(), "the share is gone, the rest still travels");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.INTERCEPT), "killing a carrier earns 'Stop, thief!'");
        drops.forEach(ItemEntity::discard);

        // Whatever reaches the village goes to its stash.
        int rest = caravan.allLoot().size();
        caravan.x = village.getX() + 10.5;
        caravan.z = village.getZ() + 0.5;
        caravan.materialized = false;
        members.forEach(AbstractGoblin::discard);
        Caravans.tick(level);
        helper.assertTrue(data.caravan(caravan.id) == null, "the caravan should have arrived");
        helper.assertTrue(data.stolenStacks(new ChunkPos(village)) == rest, "stash got " + data.stolenStacks(new ChunkPos(village)) + " of " + rest);
        data.takeStolen(new ChunkPos(village));
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void shamanBuffsHealsAndCurses(GameTestHelper helper) {
        GoblinShaman shaman = helper.spawn(ModEntities.GOBLIN_SHAMAN.get(), new BlockPos(2, 2, 4));
        GiantGoblin giant = helper.spawn(ModEntities.GIANT_GOBLIN.get(), new BlockPos(6, 2, 2));
        RuthlessGoblin warrior = helper.spawn(ModEntities.RUTHLESS_GOBLIN.get(), new BlockPos(6, 2, 5));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(11, 2, 5));
        for (var mob : List.of(shaman, giant, warrior, zombie)) {
            mob.setNoAi(true);
        }
        giant.setHealth(giant.getMaxHealth() * 0.5F);
        float before = giant.getHealth();
        helper.assertTrue(shaman.healGiant() && giant.getHealth() > before && giant.hasEffect(MobEffects.REGENERATION), "the shaman heals the giant");

        warrior.setTarget(zombie);
        helper.assertTrue(shaman.buffAllies() && warrior.hasEffect(MobEffects.DAMAGE_BOOST) && warrior.hasEffect(MobEffects.MOVEMENT_SPEED),
                "the shaman buffs goblins in a fight");
        helper.assertTrue(shaman.isSpeaking() && shaman.getSpeech().startsWith("goblinraiders.speech.shaman.spell."), "spells come with words: "
                + shaman.getSpeech());

        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 6));
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(shaman.curse(player) && player.hasEffect(ModEffects.CURSE), "the shaman curses the player");
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) < damage, "the curse weakens the player");
        leave(helper, player);
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void kingFightsOnlyWhenChallenged(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GoblinKing king = helper.spawn(ModEntities.GOBLIN_KING.get(), new BlockPos(8, 2, 4));
        king.setNoAi(true);
        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 4));
        helper.assertTrue(Math.abs(king.getBbHeight() - 3.0F) < 0.01F, "the king is 3 blocks tall, not " + king.getBbHeight());
        helper.assertTrue(king.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 15.0, "the king hits hard");

        float health = king.getHealth();
        helper.assertTrue(!king.hurt(level.damageSources().playerAttack(player), 20.0F) && king.getHealth() == health,
                "the king cannot be hurt before the talk");
        king.setTarget(player);
        helper.assertTrue(king.getTarget() == null, "the king does not fight before he is challenged");

        king.onDialogChoice(player, DialogChoicePayload.KING_FIGHT);
        helper.assertTrue(king.isChallenged() && king.getTarget() == player, "choosing to fight starts the fight");
        helper.assertTrue(king.hurt(level.damageSources().playerAttack(player), 20.0F) && king.getHealth() < health, "now the king takes damage");
        helper.assertTrue(king.holdsGrudge(player), "a disguise does not fool a challenged king");
        king.discard();
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN, timeoutTicks = 40)
    public static void vassalsPayTributeAndHireGoblins(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        GoblinKing king = helper.spawn(ModEntities.GOBLIN_KING.get(), new BlockPos(9, 2, 4));
        king.setNoAi(true);
        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 4));

        king.onDialogChoice(player, DialogChoicePayload.KING_SUBMIT);
        helper.assertTrue(Horde.isVassal(player) && !king.isChallenged(), "submitting makes the player a vassal");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.VASSAL_CHARTER.get())), "the vassal gets a charter");
        helper.assertTrue(player.hasEffect(ModEffects.DISGUISE) && GoblinFriends.isFriend(player), "vassals go about as goblins");

        // Vassals are not raided.
        ChestBlockEntity chest = placeChest(helper, new BlockPos(2, 2, 2));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        data.trackChest(player.getUUID(), chest.getBlockPos(), level.getGameTime());
        helper.assertTrue(GoblinRaids.tryRaid(player, true).result() == GoblinRaids.Result.VASSAL, "a vassal's base is never raided");

        // The tribute is collected from the inventory when due.
        GoblinRaidData.Vassal vassal = Horde.vassal(player);
        long now = level.getGameTime();
        player.getInventory().add(new ItemStack(ModItems.GOBLIN_COIN.get(), 10));
        vassal.nextTribute = now;
        Horde.tickVassal(player);
        helper.assertTrue(Horde.coinsOf(player) == 10 - Horde.TRIBUTE_COST && vassal.nextTribute == now + Horde.TRIBUTE_PERIOD,
                "tribute collected: coins " + Horde.coinsOf(player));

        // Hiring a goblin: it follows the vassal and never attacks them.
        player.getInventory().add(new ItemStack(ModItems.GOBLIN_COIN.get(), Horde.Hireling.RUTHLESS.cost - Horde.coinsOf(player)));
        helper.assertTrue(Horde.hire(player, Horde.Hireling.RUTHLESS), "hiring should work with enough coins");
        helper.assertTrue(Horde.coinsOf(player) == 0, "hiring costs " + Horde.Hireling.RUTHLESS.cost);
        List<RuthlessGoblin> hired = level.getEntitiesOfClass(RuthlessGoblin.class, helper.getBounds().inflate(4.0), AbstractGoblin::isHired);
        helper.assertTrue(hired.size() == 1 && hired.get(0).getHirer() == player, "one hired goblin for the player, found " + hired.size());
        helper.assertTrue(!Horde.hire(player, Horde.Hireling.SMALL), "no coins, no goblin");

        // Missing the tribute: a warning, then out of the horde after the grace period.
        vassal.nextTribute = now - 1;
        Horde.tickVassal(player);
        helper.assertTrue(Horde.isVassal(player) && vassal.warned, "first a warning");
        vassal.nextTribute = now - Horde.GRACE - 1;
        Horde.tickVassal(player);
        helper.assertTrue(!Horde.isVassal(player) && !player.hasEffect(ModEffects.DISGUISE), "an unpaid vassal is thrown out");

        // The hired goblin leaves when its time is up.
        RuthlessGoblin mercenary = hired.get(0);
        mercenary.hireBy(player.getUUID(), level.getGameTime() - 1);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(mercenary.isRemoved(), "the mercenary should leave after its time");
            data.untrackChest(chest.getBlockPos());
            king.discard();
            leave(helper, player);
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- advancements

    @GameTest(template = PEN)
    public static void advancementTabIsComplete(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        for (String name : GoblinAdvancements.ALL) {
            var advancement = advancements.get(GoblinRaiders.id(name));
            helper.assertTrue(advancement != null && advancement.value().display().isPresent(), "missing advancement " + name);
            helper.assertTrue(name.equals("root") == advancement.value().parent().isEmpty(), name + ": only the root has no parent");
        }
        helper.assertTrue(advancements.get(GoblinRaiders.id("root")).value().display().get().getBackground().isPresent(), "the tab needs a background");
        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 4));
        for (String name : GoblinAdvancements.CODE) {
            helper.assertTrue(!GoblinAdvancements.has(player, name), name + " granted too early");
            GoblinAdvancements.award(player, name);
            helper.assertTrue(GoblinAdvancements.has(player, name), name + " cannot be granted from code");
        }
        GoblinRaiders.LOGGER.info("[test] day time in the default batch: {}", helper.getLevel().getDayTime());
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void talkingAndClubbingEarnAdvancements(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        GoblinElder elder = helper.spawn(ModEntities.GOBLIN_ELDER.get(), new BlockPos(4, 2, 4));
        elder.setNoAi(true);
        elder.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.ELDER), "talking to the elder makes you 'Enlightened'");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 4));
        zombie.setNoAi(true);
        ItemStack club = new ItemStack(ModItems.GOBLIN_CLUB.get());
        club.getItem().hurtEnemy(club, zombie, player);
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.STUN), "stunning with the club earns 'Stunning!'");
        zombie.discard();
        leave(helper, player);
        helper.succeed();
    }

    // ---------------------------------------------------------------- the neutral goblin

    @GameTest(template = PEN)
    public static void neutralGoblinTalksAndTakesATreat(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        NeutralGoblin goblin = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(4, 2, 4));
        goblin.setNoAi(true);
        goblin.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(goblin.getTalkingTo() == player, "clicking a neutral goblin starts a conversation");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.NEUTRAL_TALK), "the conversation earns an advancement");

        helper.assertTrue(!goblin.takeTreat(player), "no grub, no treat");
        player.getInventory().add(new ItemStack(ModItems.GOBLIN_GRUB.get(), 2));
        helper.assertTrue(goblin.takeTreat(player), "the goblin takes a bowl of grub");
        helper.assertTrue(Horde.coinsOf(player) == 0 && countOf(player, ModItems.GOBLIN_GRUB.get()) == 1 && countOf(player, Items.BOWL) == 1,
                "one grub eaten, the bowl given back");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.TREAT), "the treat earns an advancement");
        helper.assertTrue(!goblin.takeTreat(player), "one treat a day");
        goblin.onDialogChoice(player, DialogChoicePayload.CLOSE);
        helper.assertTrue(goblin.getTalkingTo() == null, "closing the window ends the conversation");
        leave(helper, player);
        helper.succeed();
    }

    private static int countOf(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- mercenaries and betrayal

    @GameTest(template = PEN, timeoutTicks = 80)
    public static void mercenariesFightForTheirEmployer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        Horde.submit(player);
        AbstractGoblin mercenary = Horde.spawnMercenary(player, ModEntities.RUTHLESS_GOBLIN.get(), 2400);
        AbstractGoblin second = Horde.spawnMercenary(player, ModEntities.SMALL_GOBLIN.get(), 2400);
        NeutralGoblin villager = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(9, 2, 2));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, 2, 6));
        villager.setNoAi(true);
        zombie.setNoAi(true);
        helper.assertTrue(mercenary.isAlliedTo(player) && mercenary.isAlliedTo(second) && !mercenary.isAlliedTo(villager),
                "a mercenary sides with its employer and fellow mercenaries, not with the village");

        // Striking one's own mercenary: no betrayal, no fight back, the others do not turn on it.
        player.tickCount = 50;
        player.attack(mercenary);
        helper.assertTrue(player.hasEffect(ModEffects.DISGUISE), "hitting your own mercenary keeps the disguise");
        mercenary.setTarget(player);
        second.setTarget(mercenary);
        helper.assertTrue(mercenary.getTarget() != player && second.getTarget() != mercenary, "mercenaries never fight their side");

        // A zombie hits the player: the mercenaries go for it. (The mock player does not tick and is spawn-invulnerable,
        // so the hit is recorded the way LivingEntity#hurt records it.)
        player.tickCount = 100;
        player.setLastHurtByMob(zombie);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(mercenary.getTarget() == zombie, "the mercenary should defend against the zombie, targets " + mercenary.getTarget());
            // The player hits a goblin: the mercenaries join in, goblin or not.
            player.tickCount = 200;
            player.attack(villager);
            helper.runAfterDelay(6, () -> {
                helper.assertTrue(mercenary.getTarget() == villager || second.getTarget() == villager,
                        "the mercenaries go after the goblin the player hit: " + mercenary.getTarget() + ", " + second.getTarget());
                zombie.discard();
                mercenary.discard();
                second.discard();
                leave(helper, player);
                helper.succeed();
            });
        });
    }

    @GameTest(template = PEN)
    public static void traitorsCannotComeBack(GameTestHelper helper) {
        GoblinKing king = helper.spawn(ModEntities.GOBLIN_KING.get(), new BlockPos(9, 2, 4));
        king.setNoAi(true);
        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 4));
        king.onDialogChoice(player, DialogChoicePayload.KING_SUBMIT);
        helper.assertTrue(Horde.isVassal(player), "first a vassal");

        king.onDialogChoice(player, DialogChoicePayload.KING_BETRAY);
        helper.assertTrue(!Horde.isVassal(player) && Horde.isTraitor(player), "betraying leaves the horde for good");
        helper.assertTrue(king.isChallenged() && king.getTarget() == player, "the king attacks the traitor at once");
        helper.assertTrue(!player.hasEffect(ModEffects.DISGUISE) && countOf(player, ModItems.VASSAL_CHARTER.get()) == 0,
                "no disguise and no charter for a traitor");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.BETRAY), "betrayal earns 'A Knife in the Back'");
        Horde.submit(player);
        helper.assertTrue(!Horde.isVassal(player), "the horde does not take traitors back");
        king.discard();
        leave(helper, player);
        helper.succeed();
    }

    // ---------------------------------------------------------------- night

    private static final String NIGHT = "night";

    @BeforeBatch(batch = NIGHT)
    public static void nightFalls(ServerLevel level) {
        level.setDayTime(18000);
    }

    @AfterBatch(batch = NIGHT)
    public static void morningComes(ServerLevel level) {
        level.setDayTime(1000);
    }

    /** A village goblin with its home where it stands, made a sleeper or a watchman (that is decided by the UUID). */
    private static <T extends AbstractGoblin> T villager(GameTestHelper helper, EntityType<T> type, BlockPos rel, boolean sleeper) {
        ServerLevel level = helper.getLevel();
        T goblin = type.create(level);
        BlockPos at = helper.absolutePos(rel);
        goblin.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        goblin.setHome(at);
        do {
            goblin.setUUID(UUID.randomUUID());
        } while (goblin.isSleeper() != sleeper);
        level.addFreshEntity(goblin);
        return goblin;
    }

    @GameTest(template = PEN, batch = NIGHT, timeoutTicks = 100)
    public static void sleepersCanBeSneakedPast(GameTestHelper helper) {
        NeutralGoblin sleeper = villager(helper, ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(11, 2, 4), true);
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        helper.assertTrue(GoblinNight.isNight(helper.getLevel()), "the night batch runs at night");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(sleeper.isDozing(), "a sleeper dozes off at night at home");
            helper.assertTrue(!sleeper.notices(player) && !sleeper.canChat(), "a sleeping goblin notices nobody");
            // Sneaking right past it: it sleeps on.
            BlockPos close = helper.absolutePos(new BlockPos(10, 2, 4));
            player.teleportTo(helper.getLevel(), close.getX() + 0.5, close.getY(), close.getZ() + 0.5, 0.0F, 0.0F);
            player.setShiftKeyDown(true);
            player.setPose(Pose.CROUCHING);
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(sleeper.isDozing(), "a sneaking player does not wake it");
                // Walking normally: it wakes up and stays alert.
                player.setShiftKeyDown(false);
                player.setPose(Pose.STANDING);
                helper.runAfterDelay(25, () -> {
                    helper.assertTrue(!sleeper.isDozing() && sleeper.isAlert(), "footsteps wake the goblin and it stays alert");
                    helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.SPOTTED), "being noticed at night earns 'Who goes there?!'");
                    leave(helper, player);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = PEN, batch = NIGHT, timeoutTicks = 60)
    public static void watchmenSpotAndRaiseTheAlarm(GameTestHelper helper) {
        NeutralGoblin watchman = villager(helper, ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(2, 2, 4), false);
        SmallGoblin sleeper = villager(helper, ModEntities.SMALL_GOBLIN.get(), new BlockPos(5, 2, 2), true);
        watchman.setNoAi(true);
        sleeper.setNoAi(true);
        sleeper.fallAsleep();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(10, 2, 4));
        helper.assertTrue(watchman.isWatchman() && !sleeper.isWatchman(), "roles: " + watchman.isWatchman() + " " + sleeper.isWatchman());
        helper.assertTrue(GoblinNight.spots(watchman, player), "a watchman sees an intruder eight blocks away");
        player.setPose(Pose.CROUCHING);
        helper.assertTrue(!GoblinNight.spots(watchman, player), "sneaking, the intruder is not seen from eight blocks");
        helper.assertTrue(!watchman.wantsToAttack(player), "nor attacked from afar at night");
        player.setPose(Pose.STANDING);
        watchman.raiseAlarm(player);
        helper.assertTrue(!sleeper.isDozing() && sleeper.isAlert(), "the alarm wakes the sleepers around");
        helper.assertTrue(watchman.isSpeaking() && watchman.getSpeech().startsWith("goblinraiders.speech.common.alarm."), "the watchman shouts");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN, batch = NIGHT)
    public static void sneakingThiefOpensTheStash(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SmallGoblin sleeper = villager(helper, ModEntities.SMALL_GOBLIN.get(), new BlockPos(6, 2, 4), true);
        sleeper.setNoAi(true);
        sleeper.fallAsleep();
        placeStash(helper, new BlockPos(3, 2, 4));
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        BlockPos stash = helper.absolutePos(new BlockPos(3, 2, 4));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(stash), Direction.WEST, stash, false);

        player.setPose(Pose.CROUCHING);
        level.getBlockState(stash).useWithoutItem(level, player, hit);
        helper.assertTrue(sleeper.isDozing() && sleeper.getTarget() == null, "a sneaking thief does not wake the sleepers");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.NIGHT_THIEF), "earns 'Quiet as a mouse'");
        player.closeContainer();

        player.setPose(Pose.STANDING);
        level.getBlockState(stash).useWithoutItem(level, player, hit);
        helper.assertTrue(!sleeper.isDozing(), "opening the stash without sneaking wakes them");
        player.closeContainer();
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void goblinsTendTheCampfires(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(5, 2, 4), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        NeutralGoblin goblin = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(3, 2, 4));
        goblin.setNoAi(true);
        BlockPos fire = helper.absolutePos(new BlockPos(5, 2, 4));
        helper.assertTrue(GoblinNight.tendFire(goblin, true) && level.getBlockState(fire).getValue(CampfireBlock.LIT), "at night the fire is lit");
        helper.assertTrue(!GoblinNight.tendFire(goblin, true), "a burning fire needs nothing");
        helper.assertTrue(GoblinNight.tendFire(goblin, false) && !level.getBlockState(fire).getValue(CampfireBlock.LIT), "in the morning it is put out");
        helper.succeed();
    }

    // ---------------------------------------------------------------- talking in disguise, children, workers

    @GameTest(template = PEN)
    public static void goblinsTalkToThoseTheyTakeForTheirOwn(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        RuthlessGoblin ruthless = helper.spawn(ModEntities.RUTHLESS_GOBLIN.get(), new BlockPos(4, 2, 4));
        CrazyGoblin crazy = helper.spawn(ModEntities.CRAZY_GOBLIN.get(), new BlockPos(4, 2, 2));
        ruthless.setNoAi(true);
        crazy.setNoAi(true);
        ruthless.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ruthless.getTalkingTo() == null, "a ruthless goblin does not chat with a human");

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.DISGUISE, 1200));
        ruthless.interact(player, InteractionHand.MAIN_HAND);
        crazy.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ruthless.getTalkingTo() == player && crazy.getTalkingTo() == player, "disguised, the player can talk to any goblin");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.UNDERCOVER), "earns 'Undercover'");
        player.getInventory().add(new ItemStack(ModItems.GOBLIN_GRUB.get()));
        helper.assertTrue(ruthless.takeTreat(player), "any goblin takes a treat");
        ruthless.onDialogChoice(player, DialogChoicePayload.CLOSE);
        helper.assertTrue(ruthless.getTalkingTo() == null, "closing the window ends the talk");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void childrenNeverFightPlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        GoblinChild child = helper.spawn(ModEntities.GOBLIN_CHILD.get(), new BlockPos(5, 2, 4));
        GoblinChild other = helper.spawn(ModEntities.GOBLIN_CHILD.get(), new BlockPos(5, 2, 2));
        NeutralGoblin grownUp = helper.spawn(ModEntities.NEUTRAL_GOBLIN.get(), new BlockPos(9, 2, 4));
        for (var mob : List.of(child, other, grownUp)) {
            mob.setNoAi(true);
        }
        helper.assertTrue(child.isBaby() && Math.abs(child.getBbHeight() - 1.0F) < 0.01F, "a child is one block tall, not " + child.getBbHeight());
        other.setVariant(GoblinChild.VARIANT_GIANT);
        helper.assertTrue(other.getVariant() == GoblinChild.VARIANT_GIANT && child.getVariant() == GoblinChild.VARIANT_SMALL, "two looks");

        child.setTarget(player);
        helper.assertTrue(child.getTarget() == null, "a child never goes for a player");
        child.hurt(level.damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(child.getTarget() == null, "not even when hit");
        helper.assertTrue(grownUp.getTarget() == player, "the grown-ups stand up for the child");
        grownUp.callForHelp(player, 16.0);
        helper.assertTrue(child.getTarget() == null && other.getTarget() == null, "children do not join the grown-ups' fights");

        child.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(child.getTalkingTo() == player && GoblinAdvancements.has(player, GoblinAdvancements.CHILD_TALK),
                "children chat with anybody, earning 'Baby talk'");
        child.stopTalking();
        ChildScuffleGoal.start(child, other);
        helper.assertTrue(child.getTarget() == other && other.getTarget() == child, "but children scuffle with each other");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void kingSellsWorkerChildren(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(4, 2, 4));
        helper.assertTrue(!Horde.buyWorker(player, WorkJob.FARMER), "only vassals buy workers");
        Horde.submit(player);
        player.getInventory().add(new ItemStack(ModItems.GOBLIN_COIN.get(), Horde.WORKER_COST));
        helper.assertTrue(Horde.buyWorker(player, WorkJob.FARMER), "a vassal buys a farmer child");
        helper.assertTrue(Horde.coinsOf(player) == 0 && GoblinAdvancements.has(player, GoblinAdvancements.ARTEL), "it costs coins, earns 'Goblin workforce'");
        List<GoblinChild> workers = helper.getLevel().getEntitiesOfClass(GoblinChild.class, helper.getBounds().inflate(4.0), c -> c.isOwner(player));
        helper.assertTrue(workers.size() == 1, "one worker for the player, found " + workers.size());
        GoblinChild worker = workers.get(0);
        helper.assertTrue(worker.getJob() == WorkJob.FARMER && worker.getMainHandItem().is(Items.IRON_HOE), "a farmer with a hoe");
        helper.assertTrue(worker.getLeader() == player && !worker.isWorking(), "it follows the player at first");

        worker.onDialogChoice(player, DialogChoicePayload.WORKER_STAY);
        helper.assertTrue(worker.isWorking() && worker.getWorkplace().equals(worker.blockPosition()) && worker.getLeader() == null, "work here");
        worker.onDialogChoice(player, DialogChoicePayload.WORKER_MINE);
        helper.assertTrue(worker.getJob() == WorkJob.MINER && worker.getMainHandItem().is(Items.IRON_PICKAXE), "now a miner");
        worker.getPocket().addItem(new ItemStack(Items.COAL, 5));
        worker.onDialogChoice(player, DialogChoicePayload.WORKER_HAND_OVER);
        helper.assertTrue(countOf(player, Items.COAL) == 5 && worker.itemsCarried() == 0, "it hands over what it carries");
        worker.onDialogChoice(player, DialogChoicePayload.WORKER_FOLLOW);
        helper.assertTrue(!worker.isWorking() && worker.getLeader() == player, "and follows again");
        worker.discard();
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN, timeoutTicks = 400)
    public static void farmerChildHarvestsIntoTheChest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(11, 2, 6));
        for (int x = 4; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.FARMLAND.defaultBlockState());
        }
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        helper.setBlock(new BlockPos(4, 2, 2), wheat.getStateForAge(wheat.getMaxAge()));
        helper.setBlock(new BlockPos(5, 2, 2), wheat.getStateForAge(wheat.getMaxAge()));
        ChestBlockEntity chest = placeChest(helper, new BlockPos(2, 2, 5));
        GoblinChild farmer = helper.spawn(ModEntities.GOBLIN_CHILD.get(), new BlockPos(5, 2, 4));
        farmer.employ(player, WorkJob.FARMER);
        farmer.setWorkplace(farmer.blockPosition());
        helper.assertTrue(farmer.problem() == GoblinChild.Problem.NONE, "a field and a chest: nothing to complain about, got " + farmer.problem());
        helper.succeedWhen(() -> {
            int harvested = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i).is(Items.WHEAT)) {
                    harvested += chest.getItem(i).getCount();
                }
            }
            helper.assertTrue(harvested >= 2, "wheat in the chest: " + harvested);
            BlockState crop = level.getBlockState(helper.absolutePos(new BlockPos(4, 2, 2)));
            helper.assertTrue(crop.is(Blocks.WHEAT) && !wheat.isMaxAge(crop), "the field is sown again");
            helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.HARVEST), "the owner earns 'First harvest'");
            farmer.discard();
            leave(helper, player);
        });
    }

    @GameTest(template = PEN)
    public static void minerChildBringsUpOre(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(11, 2, 6));
        GoblinChild miner = helper.spawn(ModEntities.GOBLIN_CHILD.get(), new BlockPos(6, 2, 4));
        miner.setNoAi(true);
        miner.employ(player, WorkJob.MINER);
        miner.setWorkplace(miner.blockPosition());
        helper.assertTrue(!WorkSkills.isMine(helper.getLevel(), miner.getWorkplace()), "a stone floor alone is no mine");
        for (int x = 4; x <= 8; x++) {
            for (int y = 2; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, y, 6), Blocks.STONE.defaultBlockState());
            }
        }
        helper.assertTrue(WorkSkills.isMine(helper.getLevel(), miner.getWorkplace()), "walled in by stone, it is a mine");
        helper.assertTrue(miner.problem() == GoblinChild.Problem.NO_CHEST, "no chest yet: " + miner.problem());

        // Deep hauls give deepslate and can give diamonds, never coal; up high, never diamonds.
        RandomSource random = RandomSource.create(42);
        int diamondsDeep = 0, coalDeep = 0, diamondsHigh = 0;
        for (int i = 0; i < 3000; i++) {
            for (ItemStack stack : WorkSkills.haul(random, -30)) {
                helper.assertTrue(!stack.is(Items.COBBLESTONE), "deep down the rubble is deepslate");
                diamondsDeep += stack.is(Items.DIAMOND) ? 1 : 0;
                coalDeep += stack.is(Items.COAL) ? 1 : 0;
            }
            for (ItemStack stack : WorkSkills.haul(random, 80)) {
                diamondsHigh += stack.is(Items.DIAMOND) ? 1 : 0;
            }
        }
        helper.assertTrue(diamondsDeep > 10 && coalDeep == 0 && diamondsHigh == 0, "diamonds deep " + diamondsDeep + ", coal deep " + coalDeep
                + ", diamonds high " + diamondsHigh);

        ChestBlockEntity chest = placeChest(helper, new BlockPos(9, 2, 4));
        miner.finishHaul();
        helper.assertTrue(miner.itemsCarried() > 0 && miner.deposit(chest.getBlockPos()), "the haul goes to the chest");
        helper.assertTrue(!chest.isEmpty() && miner.itemsCarried() == 0, "the chest got it all");
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.HAUL), "the owner earns 'Mining shift'");
        miner.discard();
        leave(helper, player);
        helper.succeed();
    }

    /** The whole shift with the real AI: the miner digs at the rock face for a haul and carries it to the chest. */
    @GameTest(template = PEN, timeoutTicks = 900)
    public static void minerChildWorksAShift(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(11, 2, 6));
        for (int x = 4; x <= 8; x++) {
            for (int y = 2; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE.defaultBlockState());
            }
        }
        ChestBlockEntity chest = placeChest(helper, new BlockPos(9, 2, 4));
        GoblinChild miner = helper.spawn(ModEntities.GOBLIN_CHILD.get(), new BlockPos(6, 2, 3));
        miner.employ(player, WorkJob.MINER);
        miner.setWorkplace(miner.blockPosition());
        helper.succeedWhen(() -> {
            helper.assertTrue(!chest.isEmpty(), "the miner has not brought a haul yet");
            miner.discard();
            leave(helper, player);
        });
    }

    // ---------------------------------------------------------------- peaceful villages and the sage

    private static final String[] PEACEFUL_TYPES = {"goblin_hamlet", "goblin_sage_tent"};
    private static final String[] PEACEFUL_TEMPLATES = {"sage_tent_1", "sage_tent_2"};
    private static final String[] HAMLET_HOUSES = {"home_small", "home_small_2", "home_family", "home_tent", "farm", "farm_wheat",
            "lumber_yard", "grove", "mine_adit", "builder_yard", "kitchen", "site_hut", "site_big", "site_garden"};

    @GameTest(template = PEN)
    public static void peacefulStructuresGenerate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        var chunks = level.getChunkSource();
        for (String type : PEACEFUL_TYPES) {
            Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id(type));
            helper.assertTrue(structure != null && structure.biomes().size() > 0, type + " is not registered or has no biomes");
            StructureStart start = structure.generate(level.registryAccess(), chunks.getGenerator(), chunks.getGenerator().getBiomeSource(),
                    chunks.randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level, biome -> true);
            helper.assertTrue(start.isValid() && !start.getPieces().isEmpty(), type + " generated no pieces");
        }
        Structure hamlet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id("goblin_hamlet"));
        var monsters = hamlet.spawnOverrides().get(MobCategory.MONSTER);
        helper.assertTrue(monsters != null && monsters.spawns().isEmpty(), "no monsters may spawn in a peaceful village");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_hamlet"))).is(ModTags.GOBLIN_HAMLETS)
                && !level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_hamlet"))).is(ModTags.GOBLIN_VILLAGES),
                "a peaceful village is never where raiders take the loot");
        var manager = level.getStructureManager();
        for (String name : PEACEFUL_TEMPLATES) {
            var template = manager.get(GoblinRaiders.id("goblin_village/" + name));
            helper.assertTrue(template.isPresent(), "missing template " + name);
            StructurePlaceSettings settings = new StructurePlaceSettings();
            int stashes = template.get().filterBlocks(BlockPos.ZERO, settings, ModBlocks.GOBLIN_STASH.get()).size();
            long centers = template.get().filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW).stream()
                    .filter(info -> info.nbt() != null && "goblinraiders:village_center".equals(info.nbt().getString("name"))).count();
            helper.assertTrue(stashes == 0 && centers == 1, name + ": " + stashes + " stashes, " + centers + " centers");
        }
        helper.succeed();
    }

    private static long jigsaws(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template, String name) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW).stream()
                .filter(info -> info.nbt() != null && name.equals(info.nbt().getString("name"))).count();
    }

    /** The peaceful village grows from pieces: a square with three streets out, houses with a door jigsaw, job blocks. */
    @GameTest(template = PEN)
    public static void hamletPiecesFitTogether(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (String center : new String[] {"center_1", "center_2"}) {
            var template = manager.get(GoblinRaiders.id("hamlet/" + center)).orElseThrow();
            helper.assertTrue(jigsaws(template, "goblinraiders:hamlet_street_out") == 3, center + ": three streets leave the square");
            int crates = template.filterBlocks(BlockPos.ZERO, settings, ModBlocks.STOREHOUSE_CRATE.get()).size();
            helper.assertTrue(crates >= 10, center + " has " + crates + " storehouse crates");
        }
        for (String house : HAMLET_HOUSES) {
            var template = manager.get(GoblinRaiders.id("hamlet/" + house));
            helper.assertTrue(template.isPresent() && jigsaws(template.get(), "goblinraiders:hamlet_house") == 1,
                    house + " needs exactly one door jigsaw");
        }
        String[][] jobBlocks = {{"farm", "composter"}, {"lumber_yard", "fletching_table"}, {"mine_adit", "grindstone"},
                {"builder_yard", "stonecutter"}, {"kitchen", "smoker"}};
        for (String[] pair : jobBlocks) {
            var block = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(pair[1]));
            int found = manager.get(GoblinRaiders.id("hamlet/" + pair[0])).orElseThrow().filterBlocks(BlockPos.ZERO, settings, block).size();
            helper.assertTrue(found == 1, pair[0] + " needs its " + pair[1]);
        }
        // The real assembly: the square, streets and houses.
        ServerLevel level = helper.getLevel();
        Structure hamlet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id("goblin_hamlet"));
        var chunks = level.getChunkSource();
        StructureStart start = hamlet.generate(level.registryAccess(), chunks.getGenerator(), chunks.getGenerator().getBiomeSource(),
                chunks.randomState(), level.getStructureManager(), level.getSeed(), new ChunkPos(helper.absolutePos(BlockPos.ZERO)), 0, level,
                biome -> true);
        List<String> pieces = start.getPieces().stream().map(piece -> piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pe
                ? pe.getElement().toString() : piece.toString()).toList();
        helper.assertTrue(pieces.size() >= 8, "a village of " + pieces.size() + " pieces: " + pieces);
        helper.assertTrue(pieces.stream().anyMatch(n -> n.contains("hamlet/home")), "no homes in " + pieces);
        helper.assertTrue(pieces.stream().anyMatch(n -> n.contains("hamlet/street")), "no streets in " + pieces);
        helper.succeed();
    }

    /** A hamlet built over a lava lake fills the pool with stone and lets the lava up above drain off. */
    @GameTest(template = PEN)
    public static void hamletPutsOutTheLavaItStandsOn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure hamlet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id("goblin_hamlet"));
        helper.assertTrue(hamlet instanceof com.goblinraiders.world.HamletStructure, "the hamlet is a " + hamlet.type());
        BlockPos pool = new BlockPos(6, 1, 6);
        BlockPos pond = new BlockPos(2, 2, 6);
        BlockPos fall = new BlockPos(6, 5, 6);
        for (BlockPos pos : List.of(pool, pond, fall)) {
            helper.setBlock(pos, Blocks.LAVA.defaultBlockState());
        }
        var box = net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(helper.absolutePos(new BlockPos(5, 2, 5)),
                helper.absolutePos(new BlockPos(8, 4, 8)));
        var piece = new net.minecraft.world.level.levelgen.structure.StructurePiece(
                net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.JIGSAW, 0, box) {
            @Override
            protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,
                                                 net.minecraft.nbt.CompoundTag tag) {
            }

            @Override
            public void postProcess(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.StructureManager manager,
                                    net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random,
                                    net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            }
        };
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        hamlet.afterPlace(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(origin.offset(-16, -8, -16), origin.offset(32, 16, 32)),
                new ChunkPos(origin), new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(List.of(piece)));
        helper.assertBlockPresent(Blocks.STONE, pool);
        helper.assertBlockPresent(Blocks.STONE, pond);
        helper.assertBlockPresent(Blocks.AIR, fall);
        helper.succeed();
    }

    /** Both peaceful structure sets show up in a regular overworld (like villagesAppearInANormalOverworld). */
    @GameTest(template = PEN)
    public static void peacefulStructuresAppearInANormalOverworld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryAccess access = level.registryAccess();
        Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        BiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(
                access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        ChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        long seed = 20261026L;
        RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        for (String[] pair : new String[][] {{"goblin_hamlets", "goblin_hamlet"}, {"goblin_sage_tents", "goblin_sage_tent"}}) {
            StructureSet set = access.registryOrThrow(Registries.STRUCTURE_SET).get(GoblinRaiders.id(pair[0]));
            helper.assertTrue(set != null, "structure set " + pair[0] + " is missing");
            RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
            Structure structure = access.registryOrThrow(Registries.STRUCTURE).get(GoblinRaiders.id(pair[1]));
            int found = 0;
            for (int rx = -4; rx < 4; rx++) {
                for (int rz = -4; rz < 4; rz++) {
                    ChunkPos chunk = placement.getPotentialStructureChunk(seed, rx * placement.spacing(), rz * placement.spacing());
                    if (structure.generate(access, generator, biomes, random, level.getStructureManager(), seed, chunk, 0, level,
                            structure.biomes()::contains).isValid()) {
                        found++;
                    }
                }
            }
            helper.assertTrue(found > 0, pair[1] + " never fits in 64 regions of a normal overworld");
        }
        helper.succeed();
    }

    @GameTest(template = "hamlet/center_1", timeoutTicks = 40)
    public static void hamletSquareHasTheHeadman(GameTestHelper helper) {
        List<AbstractGoblin> goblins = helper.getLevel().getEntitiesOfClass(AbstractGoblin.class, helper.getBounds());
        helper.assertTrue(goblins.size() == 1 && goblins.get(0) instanceof GoblinHeadman, "the headman stands on the square: " + goblins);
        helper.succeed();
    }

    @GameTest(template = "hamlet/home_family", timeoutTicks = 40)
    public static void everySettlerHasAHome(GameTestHelper helper) {
        List<AbstractGoblin> goblins = helper.getLevel().getEntitiesOfClass(AbstractGoblin.class, helper.getBounds());
        helper.assertTrue(goblins.stream().filter(g -> g instanceof GoblinVillager).count() == 1
                && goblins.stream().filter(g -> g instanceof GoblinChild).count() == 1, "a family house: one settler and a child, " + goblins);
        helper.succeed();
    }

    /** Giants (and the king) do not spawn under a roof: they could never leave the house. Small goblins may. */
    @GameTest(template = PEN)
    public static void tallGoblinsSpawnOnlyUnderTheSky(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        openTheSky(helper);
        BlockPos open = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos roofed = helper.absolutePos(new BlockPos(8, 2, 4));
        helper.setBlock(new BlockPos(8, 5, 4), Blocks.OAK_PLANKS);
        helper.assertTrue(AbstractGoblin.canWalkOut(ModEntities.GIANT_GOBLIN.get(), level, open), "a giant may spawn in the open");
        helper.assertFalse(AbstractGoblin.canWalkOut(ModEntities.GIANT_GOBLIN.get(), level, roofed), "a giant must not spawn under a roof");
        helper.assertFalse(AbstractGoblin.canWalkOut(ModEntities.GOBLIN_KING.get(), level, roofed), "nor the king");
        helper.assertTrue(AbstractGoblin.canWalkOut(ModEntities.SMALL_GOBLIN.get(), level, roofed), "a small goblin fits through a door");
        helper.succeed();
    }

    /** The face's anger: a goblin with someone to fight is aggressive, even one whose attack goal does not say so. */
    @GameTest(template = PEN)
    public static void goblinsWithAFoeLookAngry(GameTestHelper helper) {
        GoblinShaman shaman = helper.spawn(ModEntities.GOBLIN_SHAMAN.get(), new BlockPos(2, 2, 2));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 2, 5));
        zombie.setNoAi(true);
        helper.assertFalse(shaman.isAggressive(), "a calm shaman looks calm");
        shaman.setTarget(zombie);
        helper.succeedWhen(() -> {
            helper.assertTrue(shaman.isAggressive(), "a shaman throwing potions at a foe looks angry");
            shaman.discard();
            zombie.discard();
        });
    }

    /** Every goblin skin has its eight face overlays (tools/textures/skins.py draws them; GoblinFaceTextures bakes them in). */
    @GameTest(template = PEN)
    public static void everySkinHasItsFaces(GameTestHelper helper) {
        String[] skins = {"small_goblin", "crazy_goblin", "goblin_elder", "ruthless_goblin", "neutral_goblin", "giant_goblin", "goblin_trader",
                "goblin_shaman", "goblin_king", "goblin_villager", "goblin_villager_female", "goblin_headman", "goblin_sage"};
        String[] states = {"closed", "angry", "worried", "happy", "hurt", "open", "snarl", "smile"};
        List<String> missing = new ArrayList<>();
        for (String skin : skins) {
            for (String state : states) {
                String path = "/assets/goblinraiders/textures/entity/goblin/face/" + skin + "_" + state + ".png";
                if (GoblinGameTests.class.getResource(path) == null) {
                    missing.add(skin + "_" + state);
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "missing faces: " + missing);
        helper.succeed();
    }

    @GameTest(template = "hamlet/home_tent")
    public static void settlerWalksOutOfItsTent(GameTestHelper helper) {
        helper.assertTrue(wayOutOfTheTent(helper, GoblinVillager.class), "the settler is shut in its tent");
        helper.succeed();
    }

    @GameTest(template = "goblin_village/sage_tent_1")
    public static void sageWalksOutOfTheTent(GameTestHelper helper) {
        helper.assertTrue(wayOutOfTheTent(helper, GoblinSage.class), "the sage is shut in the tent");
        helper.succeed();
    }

    /** Whether the goblin living in the tent can walk to some spot under the open sky. */
    private static boolean wayOutOfTheTent(GameTestHelper helper, Class<? extends Mob> kind) {
        ServerLevel level = helper.getLevel();
        openTheSky(helper);
        Mob dweller = level.getEntitiesOfClass(kind, helper.getBounds()).stream().findFirst().orElseThrow();
        dweller.setNoAi(true);
        dweller.setOnGround(true);
        BlockPos at = dweller.blockPosition();
        helper.assertTrue(at.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), "it starts inside the tent");
        Set<BlockPos> outside = new HashSet<>();
        BlockPos.betweenClosedStream(helper.getBounds().inflate(2.0))
                .filter(pos -> pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())
                        && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        && pos.distManhattan(at) >= 3)
                .forEach(pos -> outside.add(pos.immutable()));
        helper.assertFalse(outside.isEmpty(), "no open ground around the tent");
        Path path = dweller.getNavigation().createPath(outside, 0);
        return path != null && path.canReach();
    }

    /** A miner living elsewhere finds the grindstone at the end of the tunnel, and that is a mine. */
    @GameTest(template = "hamlet/mine_adit", timeoutTicks = 40)
    public static void settlerFindsTheJobBlockOfItsTrade(GameTestHelper helper) {
        GoblinVillager miner = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(3, 1, 7));
        miner.setNoAi(true);
        miner.setJob(WorkJob.MINER);
        miner.setHome(miner.blockPosition());
        miner.checkWorkplace();
        BlockPos workplace = miner.getWorkplace();
        helper.assertTrue(workplace != null && helper.getLevel().getBlockState(workplace).is(Blocks.GRINDSTONE), "the miner works by the grindstone");
        helper.assertTrue(WorkSkills.isMine(helper.getLevel(), workplace) && WorkSkills.findRockFace(miner) != null, "and that is a mine");
        // A trade the village has no job block for gets swapped for one it has.
        GoblinVillager cook = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(5, 1, 7));
        cook.setNoAi(true);
        cook.setJob(WorkJob.COOK);
        cook.setHome(cook.blockPosition());
        cook.checkWorkplace();
        helper.assertTrue(cook.getJob() == WorkJob.MINER && cook.getWorkplace() != null, "no smoker here: the cook became a " + cook.getJob().key());
        helper.succeed();
    }

    @GameTest(template = PEN, timeoutTicks = 1200)
    public static void settlersCarryTheirWorkToTheStorehouse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.FARMLAND.defaultBlockState());
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        helper.setBlock(new BlockPos(4, 2, 2), wheat.getStateForAge(wheat.getMaxAge()));
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.COMPOSTER.defaultBlockState());
        ChestBlockEntity chest = placeChest(helper, new BlockPos(2, 2, 4));
        helper.setBlock(new BlockPos(11, 2, 5), ModBlocks.STOREHOUSE_CRATE.get().defaultBlockState());
        StorehouseCrateBlockEntity crate = helper.getBlockEntity(new BlockPos(11, 2, 5));
        GoblinVillager farmer = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(5, 2, 5));
        farmer.setJob(WorkJob.FARMER);
        farmer.setHome(farmer.blockPosition());
        farmer.setWorkplace(helper.absolutePos(new BlockPos(3, 2, 4)));
        helper.succeedWhen(() -> {
            int inCrate = 0;
            for (int i = 0; i < crate.getContainerSize(); i++) {
                inCrate += crate.getItem(i).is(Items.WHEAT) ? crate.getItem(i).getCount() : 0;
            }
            helper.assertTrue(inCrate >= 1 && chest.isEmpty(), "the harvest goes to the storehouse, not the chest by the field: " + inCrate);
            farmer.discard();
        });
    }

    @GameTest(template = PEN)
    public static void headmanGivesAndPaysForErrands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        GoblinHeadman headman = helper.spawn(ModEntities.GOBLIN_HEADMAN.get(), new BlockPos(5, 2, 4));
        headman.setNoAi(true);
        helper.setBlock(new BlockPos(9, 2, 5), ModBlocks.STOREHOUSE_CRATE.get().defaultBlockState());
        StorehouseCrateBlockEntity crate = helper.getBlockEntity(new BlockPos(9, 2, 5));

        headman.onDialogChoice(player, DialogChoicePayload.QUEST_TAKE);
        helper.assertTrue(Quests.current(player) != null, "the headman hands out an errand");
        headman.onDialogChoice(player, DialogChoicePayload.QUEST_DROP);
        helper.assertTrue(Quests.current(player) == null, "an errand can be given up");

        // Goods: delivered into the storehouse, paid in coins.
        Quests.Quest logs = Quests.byId("logs").orElseThrow();
        Quests.assign(player, logs);
        player.getInventory().add(new ItemStack(Items.OAK_LOG, 30));
        helper.assertTrue(Quests.isDone(player), "30 logs are enough for " + logs.count());
        headman.onDialogChoice(player, DialogChoicePayload.QUEST_TURN_IN);
        helper.assertTrue(Quests.current(player) == null && countOf(player, Items.OAK_LOG) == 30 - logs.count()
                && Horde.coinsOf(player) == logs.coins(), "the logs go, the coins come");
        int stored = 0;
        for (int i = 0; i < crate.getContainerSize(); i++) {
            stored += crate.getItem(i).is(Items.OAK_LOG) ? crate.getItem(i).getCount() : 0;
        }
        helper.assertTrue(stored == logs.count(), "delivered logs end up in the storehouse: " + stored);
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.QUEST) && Quests.completed(player) == 1, "the first errand is done");

        // Hunting: kills count.
        Quests.Quest zombies = Quests.byId("zombies").orElseThrow();
        Quests.assign(player, zombies);
        for (int i = 0; i < zombies.count(); i++) {
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 2, 2));
            zombie.setNoAi(true);
            zombie.hurt(level.damageSources().playerAttack(player), 1000.0F);
        }
        helper.assertTrue(Quests.isDone(player), "the hunt counts the kills: " + Quests.progress(player));
        leave(helper, player);
        helper.succeed();
    }

        @GameTest(template = PEN)
    public static void settlersNeverFight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        GoblinVillager settler = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(5, 2, 4));
        GoblinVillager neighbour = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(8, 2, 4));
        settler.setNoAi(true);
        neighbour.setNoAi(true);
        settler.setTarget(player);
        helper.assertTrue(settler.getTarget() == null && !settler.wantsToAttack(player), "settlers never go for a player");
        settler.hurt(level.damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(settler.getTarget() == null && neighbour.getTarget() == null, "not even when hit");
        settler.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(settler.getTalkingTo() == player && GoblinAdvancements.has(player, GoblinAdvancements.SETTLER_TALK),
                "settlers chat with anybody");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = PEN)
    public static void settlersTradeTheirGoods(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        GoblinVillager cook = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(5, 2, 4));
        cook.setNoAi(true);
        cook.setJob(WorkJob.COOK);
        for (WorkJob job : new WorkJob[] {WorkJob.FARMER, WorkJob.LUMBERJACK, WorkJob.MINER, WorkJob.BUILDER, WorkJob.COOK}) {
            var offers = GoblinVillager.offersFor(job);
            helper.assertTrue(offers.size() >= 4, job.key() + " has " + offers.size() + " offers");
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.GOBLIN_COIN.get())), job.key() + " buys something for coins");
        }
        helper.assertTrue(cook.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.GOBLIN_GRUB.get())), "the cook sells grub");
        cook.onDialogChoice(player, DialogChoicePayload.ACCEPT);
        helper.assertTrue(player.containerMenu instanceof MerchantMenu && cook.getTradingPlayer() == player, "trade opens the merchant menu");
        player.closeContainer();
        helper.assertTrue(cook.getTradingPlayer() == null, "closing ends the trade");
        leave(helper, player);
        helper.succeed();
    }

    /** Long timeout: a neighbouring test's storehouse may be in range, and the lumberjack first has to give up on it. */
    @GameTest(template = PEN, timeoutTicks = 2400)
    public static void lumberjackFellsATreeAndPlantsAnother(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(8, 1, 4), Blocks.DIRT.defaultBlockState());
        for (int y = 2; y <= 5; y++) {
            helper.setBlock(new BlockPos(8, y, 4), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int x = 7; x <= 9; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 6, z), Blocks.OAK_LEAVES.defaultBlockState());
            }
        }
        ChestBlockEntity chest = placeChest(helper, new BlockPos(2, 2, 6));
        GoblinVillager lumberjack = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(3, 2, 4));
        lumberjack.setJob(WorkJob.LUMBERJACK);
        lumberjack.setHome(lumberjack.blockPosition());
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.FLETCHING_TABLE.defaultBlockState());
        lumberjack.setWorkplace(helper.absolutePos(new BlockPos(2, 2, 3)));
        helper.assertTrue(WorkSkills.isTreeBase(level, helper.absolutePos(new BlockPos(8, 2, 4))), "a log on dirt with leaves is a tree");
        helper.succeedWhen(() -> {
            int logs = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                logs += chest.getItem(i).is(Items.OAK_LOG) ? chest.getItem(i).getCount() : 0;
            }
            helper.assertTrue(logs == 4, "logs in the chest: " + logs + ", lumberjack at " + lumberjack.blockPosition() + " holds "
                    + lumberjack.getMainHandItem() + ", storehouse " + WorkSkills.storehouse(lumberjack) + ", "
                    + lumberjack.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof com.goblinraiders.entity.work.WorkGoal)
                    .map(g -> g.getGoal().toString()).toList());
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(8, 2, 4))).is(Blocks.OAK_SAPLING), "a sapling where the tree stood");
            lumberjack.discard();
        });
    }

    @GameTest(template = PEN, timeoutTicks = 2400)
    public static void cookMakesGrubFromTheHarvest(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.SMOKER.defaultBlockState());
        ChestBlockEntity pantry = placeChest(helper, new BlockPos(10, 2, 5));
        pantry.setItem(0, new ItemStack(Items.POTATO, 4));
        ChestBlockEntity kitchen = placeChest(helper, new BlockPos(2, 2, 5));
        GoblinVillager cook = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(3, 2, 4));
        cook.setJob(WorkJob.COOK);
        cook.setHome(cook.blockPosition());
        cook.setWorkplace(helper.absolutePos(new BlockPos(3, 2, 2)));
        helper.succeedWhen(() -> {
            int grub = 0;
            for (ChestBlockEntity chest : List.of(pantry, kitchen)) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    grub += chest.getItem(i).is(ModItems.GOBLIN_GRUB.get()) ? chest.getItem(i).getCount() : 0;
                }
            }
            helper.assertTrue(grub >= 2, "grub cooked: " + grub + ", cook " + cook.getJob() + " at " + cook.blockPosition() + " works at "
                    + cook.getWorkplace() + " carrying " + cook.getPocket() + ", fire " + WorkSkills.findKitchenFire(cook) + ", chest " + WorkSkills.findChest(cook)
                    + ", deposit? " + WorkSkills.hasSomethingToDeposit(cook) + ", kitchen at " + kitchen.getBlockPos() + ", pose " + cook.getPose()
                    + ", " + cook.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof com.goblinraiders.entity.work.WorkGoal).map(g -> g.getGoal().toString()).toList());
            cook.discard();
        });
    }

    @GameTest(template = PEN, timeoutTicks = 6000)
    public static void builderPutsUpAHouse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, new BlockPos(12, 2, 1));
        BlockPos site = new BlockPos(5, 2, 4);
        // The door faces east, where the pen has room to walk out (the pen is too narrow north to south).
        helper.setBlock(site, ModBlocks.BUILDING_SITE.get().defaultBlockState().setValue(com.goblinraiders.block.BuildingSiteBlock.FACING,
                net.minecraft.core.Direction.EAST));
        BuildingSiteBlockEntity entity = helper.getBlockEntity(site);
        entity.setBlueprint("hut_small");
        ChestBlockEntity store = placeChest(helper, new BlockPos(12, 2, 6));
        store.setItem(0, new ItemStack(Items.OAK_LOG, 64));
        store.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        store.setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        GoblinVillager builder = helper.spawn(ModEntities.GOBLIN_VILLAGER.get(), new BlockPos(11, 2, 3));
        builder.setJob(WorkJob.BUILDER);
        builder.setHome(builder.blockPosition());
        helper.setBlock(new BlockPos(12, 2, 3), Blocks.STONECUTTER.defaultBlockState());
        builder.setWorkplace(helper.absolutePos(new BlockPos(12, 2, 3)));
        helper.assertTrue(Construction.blueprint(level, "hut_small") != null && Construction.nextStep(level, helper.absolutePos(site)) != null,
                "the blueprint loads and there is work to do");
        helper.succeedWhen(() -> {
            helper.assertTrue(!level.getBlockState(helper.absolutePos(site)).is(ModBlocks.BUILDING_SITE.get()),
                    "still building: " + entity.getProgress() + " steps done, next " + Construction.nextStep(level, helper.absolutePos(site))
                            + ", site at " + helper.absolutePos(site) + ", builder at " + builder.blockPosition() + " carrying " + builder.getPocket());
            long settlers = level.getEntitiesOfClass(GoblinVillager.class, helper.getBounds().inflate(8.0)).size();
            helper.assertTrue(settlers >= 2, "a new family moves in, settlers: " + settlers);
            helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.NEW_HOUSE), "the player nearby earns 'Housewarming'");
            level.getEntitiesOfClass(GoblinVillager.class, helper.getBounds().inflate(8.0)).forEach(v -> v.discard());
            leave(helper, player);
        });
    }

    @GameTest(template = PEN)
    public static void sageBlessesAndProphesies(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, new BlockPos(2, 2, 4));
        GoblinSage sage = helper.spawn(ModEntities.GOBLIN_SAGE.get(), new BlockPos(5, 2, 4));
        sage.setNoAi(true);
        sage.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GoblinAdvancements.has(player, GoblinAdvancements.SAGE), "talking to the sage earns 'The hermit'");
        helper.assertTrue(!sage.bless(player), "no coins, no blessing");
        player.getInventory().add(new ItemStack(ModItems.GOBLIN_COIN.get(), GoblinSage.BLESSING_COST));
        helper.assertTrue(sage.bless(player) && Horde.coinsOf(player) == 0 && player.hasEffect(MobEffects.LUCK), "a blessing for coins");
        helper.assertTrue(sage.prophesy(player) && GoblinAdvancements.has(player, GoblinAdvancements.PROPHECY), "a prophecy");
        helper.assertTrue(!sage.prophesy(player), "only one prophecy a day");
        sage.setTarget(player);
        helper.assertTrue(sage.getTarget() == null, "the sage never fights");
        leave(helper, player);
        helper.succeed();
    }

    private GoblinGameTests() {
    }
}
