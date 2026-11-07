package com.goblinraiders.raid;

import com.goblinraiders.Config;
import com.goblinraiders.GoblinRaiders;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /goblinraid trigger [players] - raid right now, ignoring chance, cooldown and distance.
 * /goblinraid info [player]     - what the mod knows about a player's base.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class GoblinCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("goblinraid")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("trigger")
                        .executes(ctx -> trigger(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> trigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))));
    }

    private static int trigger(CommandSourceStack source, Collection<ServerPlayer> players) {
        int raided = 0;
        for (ServerPlayer player : players) {
            GoblinRaids.Outcome outcome = GoblinRaids.tryRaid(player, true);
            String key = "commands.goblinraid.trigger." + outcome.result().name().toLowerCase(java.util.Locale.ROOT);
            if (outcome.result() == GoblinRaids.Result.SUCCESS) {
                BlockPos v = outcome.village();
                source.sendSuccess(() -> Component.translatable(key, player.getDisplayName(), outcome.stacks(), outcome.chests(),
                        v.getX(), v.getZ()), true);
                raided++;
            } else {
                source.sendFailure(Component.translatable(key, player.getDisplayName()));
            }
        }
        return raided;
    }

    private static int info(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        GoblinRaidData.PlayerBase base = GoblinRaidData.get(player.server).base(player.getUUID());
        if (base == null || base.chests().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.goblinraid.info.none", player.getDisplayName()), false);
            return 0;
        }
        List<BlockPos> main = GoblinRaids.mainBase(base.chests(), Config.BASE_RADIUS.get());
        BlockPos center = GoblinRaids.centerOf(main);
        long now = player.server.overworld().getGameTime();
        long wait = Math.max(0, base.lastRaid() + Config.COOLDOWN_TICKS.get() - now);
        source.sendSuccess(() -> Component.translatable("commands.goblinraid.info", player.getDisplayName(), base.chests().size(),
                main.size(), center.getX(), center.getY(), center.getZ(), base.raids(), wait / 20), false);
        return base.chests().size();
    }

    private GoblinCommands() {
    }
}
