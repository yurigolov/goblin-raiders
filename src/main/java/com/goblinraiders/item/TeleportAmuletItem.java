package com.goblinraiders.item;

import com.goblinraiders.registry.ModDataComponents;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Goblins use these to blink back to their village after a raid.
 * Sneak + right click a block binds it there; holding right click teleports to the bound place,
 * or, when unbound, to the player's bed / respawn point.
 */
public class TeleportAmuletItem extends Item {
    private static final int USE_TICKS = 32;
    private static final int COOLDOWN_TICKS = 20 * 30;

    public TeleportAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        context.getItemInHand().set(ModDataComponents.AMULET_TARGET.get(), GlobalPos.of(level.dimension(), pos));
        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("item.goblinraiders.teleport_amulet.bound",
                    pos.getX(), pos.getY(), pos.getZ()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!stack.has(ModDataComponents.AMULET_TARGET.get())) {
                return InteractionResultHolder.pass(stack);
            }
            stack.remove(ModDataComponents.AMULET_TARGET.get());
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.goblinraiders.teleport_amulet.unbound"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) {
            var random = entity.getRandom();
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.PORTAL, entity.getRandomX(0.8), entity.getRandomY() - 0.25, entity.getRandomZ(0.8),
                        (random.nextDouble() - 0.5) * 2.0, -random.nextDouble(), (random.nextDouble() - 0.5) * 2.0);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return stack;
        }
        Optional<Destination> destination = findDestination(player, stack);
        if (destination.isEmpty()) {
            player.displayClientMessage(Component.translatable("item.goblinraiders.teleport_amulet.nowhere"), true);
            return stack;
        }
        Destination to = destination.get();
        ServerLevel from = player.serverLevel();
        effects(from, player.position());
        player.teleportTo(to.level(), to.pos().x, to.pos().y, to.pos().z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        effects(to.level(), to.pos());

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        return stack;
    }

    private static void effects(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 48, 0.4, 0.8, 0.4, 0.4);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private record Destination(ServerLevel level, Vec3 pos) {
    }

    private static Optional<Destination> findDestination(ServerPlayer player, ItemStack stack) {
        var server = player.server;
        GlobalPos bound = stack.get(ModDataComponents.AMULET_TARGET.get());
        if (bound != null) {
            ServerLevel level = server.getLevel(bound.dimension());
            return level == null ? Optional.empty() : freeSpotAbove(level, bound.pos()).map(pos -> new Destination(level, pos));
        }

        BlockPos respawn = player.getRespawnPosition();
        ServerLevel respawnLevel = server.getLevel(player.getRespawnDimension());
        if (respawn != null && respawnLevel != null) {
            respawnLevel.getChunk(respawn);
            BlockState state = respawnLevel.getBlockState(respawn);
            Optional<Vec3> standUp = Optional.empty();
            if (state.getBlock() instanceof BedBlock) {
                standUp = BedBlock.findStandUpPosition(EntityType.PLAYER, respawnLevel, respawn, state.getValue(BedBlock.FACING),
                        player.getRespawnAngle());
            } else if (state.getBlock() instanceof RespawnAnchorBlock) {
                standUp = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, respawnLevel, respawn);
            } else if (player.isRespawnForced()) {
                standUp = freeSpotAbove(respawnLevel, respawn);
            }
            if (standUp.isPresent()) {
                return Optional.of(new Destination(respawnLevel, standUp.get()));
            }
        }

        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        overworld.getChunk(spawn);
        return freeSpotAbove(overworld, overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn))
                .map(pos -> new Destination(overworld, pos));
    }

    /** First spot at or a few blocks above {@code pos} where a player fits. */
    private static Optional<Vec3> freeSpotAbove(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);
        for (int dy = 0; dy <= 8; dy++) {
            Vec3 spot = Vec3.atBottomCenterOf(pos.above(dy));
            if (level.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(spot))) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.AMULET_TARGET.get()) || super.isFoil(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.ENDER_PEARL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.goblinraiders.teleport_amulet.lore").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        GlobalPos bound = stack.get(ModDataComponents.AMULET_TARGET.get());
        if (bound != null) {
            BlockPos p = bound.pos();
            tooltip.add(Component.translatable("item.goblinraiders.teleport_amulet.target", p.getX(), p.getY(), p.getZ(),
                    bound.dimension().location().toString()).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.goblinraiders.teleport_amulet.target_home").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.goblinraiders.teleport_amulet.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
