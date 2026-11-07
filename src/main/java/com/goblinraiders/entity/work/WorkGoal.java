package com.goblinraiders.entity.work;

import com.goblinraiders.entity.AbstractGoblin;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A working goblin's day, one task at a time. Farmers harvest, replant, sow and carry the harvest to the chest; miners
 * chip at the rock face and bring each haul to the chest; lumberjacks fell trees log by log, plant a sapling in their
 * place and carry the logs away; cooks fetch crops and cook them into grub; builders fetch wood and stone and put up
 * the buildings on the building sites around.
 */
public class WorkGoal extends Goal {
    private enum Task { NONE, HARVEST, PLANT, DEPOSIT, MINE, CHOP, FETCH, BUILD, COOK }

    /** Builders fetch materials from chests this far from their workplace and build on sites this far away. */
    private static final int BUILDER_RANGE = 48;
    /** Builders put up one block every this many ticks. */
    private static final int BUILD_TICKS = 8;
    /** How long a goblin leaves alone a chest it could not get to. */
    private static final int UNREACHABLE_TICKS = 2400;

    private final GoblinWorker worker;
    private final AbstractGoblin goblin;
    private Task task = Task.NONE;
    @Nullable
    private BlockPos target;
    private int cooldown;
    private int walkTicks;
    private int workTicks;
    /** A lumberjack's tree, top log first. */
    private List<BlockPos> trunk = List.of();
    @Nullable
    private Block felled;
    /** What a builder is fetching, and what it is putting up. */
    private Construction.Material fetching = Construction.Material.NONE;
    @Nullable
    private Construction.Step step;
    @Nullable
    private BlockPos site;
    /** Wood a builder has paid for and not used up yet (a log is worth four blocks). */
    private int woodCredit;
    /** Chests and crates the goblin could not get to, and until when it leaves them be. */
    private final Map<BlockPos, Long> unreachable = new HashMap<>();

    public WorkGoal(GoblinWorker worker) {
        this.worker = worker;
        this.goblin = worker.asGoblin();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.worker.isWorking() && !this.goblin.isDozing() && this.goblin.getTarget() == null && this.goblin.getTalkingTo() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.task == Task.NONE) {
            if (--this.cooldown <= 0) {
                this.cooldown = 20;
                this.plan();
            }
            return;
        }
        BlockPos walkTo = switch (this.task) {
            case MINE, COOK -> this.worker.getWorkplace();
            default -> this.target;
        };
        if (walkTo == null || this.target == null) {
            this.task = Task.NONE;
            return;
        }
        double reach = switch (this.task) {
            case MINE -> 1.5;
            case COOK -> 2.0;
            case BUILD, CHOP -> 4.0;
            default -> 2.5;
        };
        double dx = this.goblin.getX() - (walkTo.getX() + 0.5);
        double dz = this.goblin.getZ() - (walkTo.getZ() + 0.5);
        double dy = walkTo.getY() - this.goblin.getY();
        // Builders reach up to the roof from the ground (they have their ladders), everybody else needs to be close.
        boolean near = this.task == Task.BUILD ? dx * dx + dz * dz <= reach * reach && dy > -3 && dy < 16
                : dx * dx + dz * dz + dy * dy <= reach * reach + 1.0;
        if (!near) {
            if (this.walkTicks++ % 20 == 0) {
                this.goblin.getNavigation().moveTo(walkTo.getX() + 0.5, walkTo.getY(), walkTo.getZ() + 0.5, 0.8);
            }
            if (this.walkTicks > 400) {
                if (this.task == Task.DEPOSIT || this.task == Task.FETCH) {
                    // The crates of one storehouse stand together: if one is out of reach, so is the rest of the hall.
                    long until = this.goblin.level().getGameTime() + UNREACHABLE_TICKS;
                    this.unreachable.put(this.target, until);
                    for (BlockPos crate : WorkSkills.storehouse(this.worker)) {
                        if (crate.closerThan(this.target, 12)) {
                            this.unreachable.put(crate, until);
                        }
                    }
                }
                this.task = Task.NONE;   // cannot get there
                this.cooldown = 100;
            }
            return;
        }
        this.goblin.getNavigation().stop();
        this.goblin.getLookControl().setLookAt(this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);
        switch (this.task) {
            case HARVEST -> this.done(WorkSkills.harvest(this.worker, this.target));
            case PLANT -> this.done(WorkSkills.plant(this.worker, this.target));
            case DEPOSIT -> this.done(WorkSkills.deposit(this.worker, this.target));
            case FETCH -> this.done(this.fetch());
            case MINE -> this.dig();
            case CHOP -> this.chop();
            case BUILD -> this.build();
            case COOK -> this.cook();
            default -> this.task = Task.NONE;
        }
    }

    @Override
    public String toString() {
        return "work " + this.task + " -> " + this.target + " walked " + this.walkTicks + " skipping " + this.unreachable.keySet();
    }

    private boolean unreachable(BlockPos pos) {
        Long until = this.unreachable.get(pos);
        if (until != null && until < this.goblin.level().getGameTime()) {
            this.unreachable.remove(pos);
            return false;
        }
        return until != null;
    }

    private void done(boolean ok) {
        this.task = Task.NONE;
        this.cooldown = ok ? 5 : 40;
    }

    private void begin(Task task, @Nullable BlockPos target) {
        if (target != null) {
            this.task = task;
            this.target = target;
            this.walkTicks = 0;
            this.workTicks = 0;
        }
    }

    // ---------------------------------------------------------------- planning

    private void plan() {
        switch (this.worker.getJob()) {
            case FARMER -> this.planFarm();
            case MINER -> this.planMine();
            case LUMBERJACK -> this.planChop();
            case COOK -> this.planCook();
            case BUILDER -> this.planBuild();
            default -> {
            }
        }
    }

    private void planFarm() {
        BlockPos chest = WorkSkills.findChest(this.worker, this::unreachable);
        BlockPos ripe = WorkSkills.findCrop(this.worker, false);
        boolean full = WorkSkills.pocketFull(this.worker);
        BlockPos field = WorkSkills.hasSeeds(this.worker) ? WorkSkills.findEmptyField(this.worker) : null;
        if (chest != null && WorkSkills.hasSomethingToDeposit(this.worker) && (full || ripe == null && field == null)) {
            this.begin(Task.DEPOSIT, chest);
        } else if (ripe != null && !full) {
            this.begin(Task.HARVEST, ripe);
        } else if (field != null) {
            this.begin(Task.PLANT, field);
        } else if (full) {
            this.worker.complain("no_chest");
        } else if (ripe == null && WorkSkills.findCrop(this.worker, true) == null && WorkSkills.findEmptyField(this.worker) == null) {
            this.worker.complain("no_field");
        }
    }

    private void planMine() {
        BlockPos workplace = this.worker.getWorkplace();
        BlockPos chest = WorkSkills.findChest(this.worker, this::unreachable);
        if (workplace == null || !WorkSkills.isMine(this.goblin.level(), workplace)) {
            this.worker.complain("not_mine");
        } else if (WorkSkills.itemsCarried(this.worker) > 0 && chest != null) {
            this.begin(Task.DEPOSIT, chest);
        } else if (WorkSkills.pocketFull(this.worker)) {
            this.worker.complain("no_chest");
        } else {
            this.begin(Task.MINE, WorkSkills.findRockFace(this.worker));
        }
    }

    private void planChop() {
        BlockPos chest = WorkSkills.findChest(this.worker, this::unreachable);
        boolean full = WorkSkills.pocketFull(this.worker);
        BlockPos tree = full ? null : WorkSkills.findTree(this.worker);
        if (chest != null && WorkSkills.itemsCarried(this.worker) > 0 && (full || tree == null || WorkSkills.itemsCarried(this.worker) >= 24)) {
            this.begin(Task.DEPOSIT, chest);
        } else if (tree != null) {
            this.trunk = WorkSkills.trunk(this.goblin.level(), tree);
            this.felled = this.goblin.level().getBlockState(tree).getBlock();
            this.begin(Task.CHOP, tree);
        } else if (full) {
            this.worker.complain("no_chest");
        } else {
            this.worker.complain("no_trees");
        }
    }

    private void planCook() {
        BlockPos fire = WorkSkills.findKitchenFire(this.worker);
        BlockPos chest = WorkSkills.findChest(this.worker, this::unreachable);
        if (fire == null) {
            this.worker.complain("no_kitchen");
        } else if (chest != null && WorkSkills.hasSomethingToDeposit(this.worker)) {
            this.begin(Task.DEPOSIT, chest);
        } else if (WorkSkills.count(this.worker, WorkSkills.FOOD) >= 2) {
            this.begin(Task.COOK, fire);
        } else {
            BlockPos pantry = WorkSkills.findContainer(this.worker, this.worker.workRadius() + 8, WorkSkills.holding(WorkSkills.FOOD),
                    this::unreachable);
            if (pantry != null) {
                this.fetching = Construction.Material.NONE;
                this.begin(Task.FETCH, pantry);
            } else {
                this.worker.complain("no_food");
            }
        }
    }

    private void planBuild() {
        BlockPos workplace = this.worker.getWorkplace();
        if (!(this.goblin.level() instanceof ServerLevel level) || workplace == null) {
            return;
        }
        this.site = Construction.findSite(level, workplace, BUILDER_RANGE);
        if (this.site == null) {
            return;   // nothing to build: a day off
        }
        this.step = Construction.nextStep(level, this.site);
        if (this.step == null) {
            Construction.finish(level, this.site);
            this.goblin.sayRandom("built");
            return;
        }
        Construction.Material material = this.step.material();
        if (this.canPay(material)) {
            this.begin(Task.BUILD, this.step.pos());
            return;
        }
        BlockPos store = WorkSkills.findContainer(this.worker, BUILDER_RANGE, WorkSkills.holding(s -> Construction.worth(s, material) > 0),
                this::unreachable);
        if (store != null) {
            this.fetching = material;
            this.begin(Task.FETCH, store);
        } else {
            this.worker.complain(material == Construction.Material.WOOD ? "no_wood" : "no_stone");
        }
    }

    // ---------------------------------------------------------------- doing

    private boolean fetch() {
        if (this.worker.getJob() == WorkJob.COOK) {
            return WorkSkills.fetch(this.worker, this.target, WorkSkills.FOOD, 8) > 0;
        }
        Construction.Material material = this.fetching;
        return WorkSkills.fetch(this.worker, this.target, s -> Construction.worth(s, material) > 0, material == Construction.Material.WOOD ? 8 : 24) > 0;
    }

    /** Whether the builder has what the block costs (logs are worth four wooden blocks). */
    private boolean canPay(Construction.Material material) {
        return switch (material) {
            case NONE -> true;
            case WOOD -> this.woodCredit > 0 || WorkSkills.count(this.worker, s -> Construction.worth(s, material) > 0) > 0;
            case STONE -> WorkSkills.count(this.worker, s -> Construction.worth(s, material) > 0) > 0;
        };
    }

    private boolean pay(Construction.Material material) {
        switch (material) {
            case WOOD -> {
                if (this.woodCredit == 0) {
                    if (WorkSkills.takeFromPocket(this.worker, s -> Construction.worth(s, Construction.Material.WOOD) == 4, 1)) {
                        this.woodCredit = 4;
                    } else if (WorkSkills.takeFromPocket(this.worker, s -> Construction.worth(s, Construction.Material.WOOD) == 1, 1)) {
                        this.woodCredit = 1;
                    } else {
                        return false;
                    }
                }
                this.woodCredit--;
                return true;
            }
            case STONE -> {
                return WorkSkills.takeFromPocket(this.worker, s -> Construction.worth(s, Construction.Material.STONE) > 0, 1);
            }
            default -> {
                return true;
            }
        }
    }

    private void build() {
        if (this.step != null && this.goblin.getBoundingBox().intersects(new AABB(this.step.pos()))) {
            // Standing right where the block goes: step aside first.
            Vec3 away = this.goblin.position().subtract(Vec3.atCenterOf(this.step.pos())).add(0.01, 0.0, 0.01).normalize().scale(2.5);
            this.goblin.getNavigation().moveTo(this.goblin.getX() + away.x, this.goblin.getY(), this.goblin.getZ() + away.z, 1.0);
            this.task = Task.NONE;
            this.cooldown = 20;
            return;
        }
        if (++this.workTicks < BUILD_TICKS) {
            return;
        }
        if (this.step != null && this.site != null && this.goblin.level() instanceof ServerLevel level && this.pay(this.step.material())) {
            Construction.place(level, this.site, this.step);
            this.goblin.swing(InteractionHand.MAIN_HAND);
            if (this.goblin.getRandom().nextInt(40) == 0) {
                this.goblin.sayRandom("work");
            }
        }
        this.task = Task.NONE;
        this.cooldown = 1;
    }

    /** Chips at the rock: swings, knocks and cracks, and every {@link WorkSkills#HAUL_TICKS} a haul is done. */
    private void dig() {
        BlockState rock = this.goblin.level().getBlockState(this.target);
        if (rock.isAir()) {
            this.task = Task.NONE;
            return;
        }
        this.workTicks++;
        if (this.workTicks % 8 == 0) {
            this.goblin.swing(InteractionHand.MAIN_HAND);
            this.goblin.level().playSound(null, this.target, rock.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.5F, 0.8F);
        }
        this.goblin.level().destroyBlockProgress(this.goblin.getId(), this.target, this.workTicks * 10 / WorkSkills.HAUL_TICKS);
        if (this.workTicks >= WorkSkills.HAUL_TICKS) {
            this.goblin.level().destroyBlockProgress(this.goblin.getId(), this.target, -1);
            BlockPos workplace = this.worker.getWorkplace();
            for (ItemStack stack : WorkSkills.haul(this.goblin.getRandom(), workplace == null ? 64 : workplace.getY())) {
                this.worker.onFound(stack);
                WorkSkills.pocketOrDrop(this.worker, stack);
            }
            this.task = Task.NONE;
            this.cooldown = 5;
        }
    }

    /** Fells the tree log by log (top first, each with a few strokes), then plants a sapling. */
    private void chop() {
        if (this.trunk.isEmpty() || WorkSkills.pocketFull(this.worker)) {
            this.finishTree();
            return;
        }
        BlockPos log = this.trunk.get(0);
        this.workTicks++;
        if (this.workTicks % 6 == 0) {
            this.goblin.swing(InteractionHand.MAIN_HAND);
            this.goblin.level().playSound(null, log, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.7F, 0.9F);
        }
        this.goblin.getLookControl().setLookAt(log.getX() + 0.5, log.getY() + 0.5, log.getZ() + 0.5);
        this.goblin.level().destroyBlockProgress(this.goblin.getId(), log, this.workTicks * 10 / WorkSkills.CHOP_TICKS);
        if (this.workTicks >= WorkSkills.CHOP_TICKS) {
            this.goblin.level().destroyBlockProgress(this.goblin.getId(), log, -1);
            WorkSkills.chop(this.worker, log);
            this.trunk = this.trunk.subList(1, this.trunk.size());
            this.workTicks = 0;
        }
    }

    private void finishTree() {
        if (this.trunk.isEmpty() && this.felled != null && this.target != null) {
            WorkSkills.replant(this.goblin.level(), this.target, this.felled);
            if (this.goblin.getRandom().nextInt(3) == 0) {
                this.goblin.sayRandom("work");
            }
        }
        this.trunk = List.of();
        this.felled = null;
        this.task = Task.NONE;
        this.cooldown = 10;
    }

    /** Stirs the pot by the fire for a while; two ingredients become a bowl of grub. */
    private void cook() {
        this.workTicks++;
        if (this.workTicks % 20 == 0 && this.goblin.level() instanceof ServerLevel level) {
            this.goblin.swing(InteractionHand.MAIN_HAND);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.target.getX() + 0.5, this.target.getY() + 1.1, this.target.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.01);
            if (this.workTicks % 80 == 0) {
                level.playSound(null, this.target, SoundEvents.SMOKER_SMOKE, SoundSource.NEUTRAL, 0.6F, 1.0F);
            }
        }
        if (this.workTicks >= WorkSkills.COOK_TICKS) {
            WorkSkills.cook(this.worker);
            if (this.goblin.getRandom().nextInt(3) == 0) {
                this.goblin.sayRandom("work");
            }
            this.task = Task.NONE;
            this.cooldown = 5;
        }
    }

    @Override
    public void stop() {
        if ((this.task == Task.MINE || this.task == Task.CHOP) && this.target != null) {
            BlockPos cracked = this.task == Task.CHOP && !this.trunk.isEmpty() ? this.trunk.get(0) : this.target;
            this.goblin.level().destroyBlockProgress(this.goblin.getId(), cracked, -1);
        }
        this.task = Task.NONE;
        this.trunk = List.of();
        this.workTicks = 0;
        this.goblin.getNavigation().stop();
    }
}
