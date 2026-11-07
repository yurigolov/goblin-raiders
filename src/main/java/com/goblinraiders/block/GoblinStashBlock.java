package com.goblinraiders.block;

import com.goblinraiders.entity.GoblinNight;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GoblinFriends;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The village treasury. Opening it pulls in everything the village's goblins have stolen
 * (see {@link GoblinStashBlockEntity#collectStolenLoot}) and makes the goblins nearby angry.
 */
public class GoblinStashBlock extends BaseEntityBlock {
    public static final MapCodec<GoblinStashBlock> CODEC = simpleCodec(GoblinStashBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final double GUARD_RANGE = 16.0;
    private static final double WAKE_RANGE = 8.0;

    public GoblinStashBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoblinStashBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof GoblinStashBlockEntity stash) {
            stash.unpackLootTable(player);
            int waiting = stash.stolenWaiting(serverLevel);
            boolean leftOver = stash.collectStolenLoot(serverLevel);
            if (waiting > stash.stolenWaiting(serverLevel)) {
                GoblinAdvancements.award(player, GoblinAdvancements.GOT_IT_BACK);
            }
            player.openMenu(stash);
            if (leftOver) {
                player.displayClientMessage(Component.translatable("block.goblinraiders.goblin_stash.more"), true);
            }
            if (!player.getAbilities().instabuild && !GoblinFriends.isFriend(player)) {
                boolean night = GoblinNight.isNight(level);
                if (night && !player.isCrouching()) {
                    wakeSleepers(serverLevel, pos, player);
                }
                angerGuards(serverLevel, pos, player);
                if (night && player.isCrouching() && sneakedPast(serverLevel, pos, player)) {
                    GoblinAdvancements.award(player, GoblinAdvancements.NIGHT_THIEF);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The lid creaks: sleepers close by wake up and the first one raises the alarm. */
    private static void wakeSleepers(ServerLevel level, BlockPos pos, Player thief) {
        for (AbstractGoblin goblin : level.getEntitiesOfClass(AbstractGoblin.class, new AABB(pos).inflate(WAKE_RANGE), AbstractGoblin::isDozing)) {
            goblin.wakeUp(true);
            goblin.raiseAlarm(thief);
        }
    }

    /** Sleepers close by, and nobody coming after the thief. */
    private static boolean sneakedPast(ServerLevel level, BlockPos pos, Player thief) {
        boolean sleepers = !level.getEntitiesOfClass(AbstractGoblin.class, new AABB(pos).inflate(12.0), AbstractGoblin::isDozing).isEmpty();
        return sleepers && level.getEntitiesOfClass(AbstractGoblin.class, new AABB(pos).inflate(24.0), g -> g.getTarget() == thief).isEmpty();
    }

    private static void angerGuards(ServerLevel level, BlockPos pos, Player thief) {
        for (AbstractGoblin goblin : level.getEntitiesOfClass(AbstractGoblin.class, new AABB(pos).inflate(GUARD_RANGE))) {
            if (goblin.getTarget() == null && !goblin.isDozing() && goblin.notices(thief) && goblin.hasLineOfSight(thief)) {
                goblin.setTarget(thief);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof GoblinStashBlockEntity stash) {
            stash.dropStolenLoot(serverLevel);
        }
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
