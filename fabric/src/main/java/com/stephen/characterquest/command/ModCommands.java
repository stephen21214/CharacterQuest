package com.stephen.characterquest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.stephen.characterquest.entity.QuestGiverEntity;
import com.stephen.characterquest.quest.ItemFetchQuest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ModCommands {
    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("characterquest")
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /characterquest <check|pick|reroll>"));
                            return 0;
                        })
                        .then(Commands.literal("check").executes(ctx -> check(ctx.getSource())))
                        .then(Commands.literal("pick")
                                .then(Commands.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> pick(
                                                ctx.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "index")
                                        ))))
                        .then(Commands.literal("reroll").executes(ctx -> reroll(ctx.getSource())))
        );
    }

    private static int check(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        ItemFetchQuest quest = QuestGiverEntity.getActiveQuest(player.getUUID());
        if (quest == null) {
            player.sendSystemMessage(Component.literal("You have no active quest."));
            return 1;
        }

        player.sendSystemMessage(Component.literal("Active quest:"));
        player.sendSystemMessage(Component.literal(quest.getDisplayText()));
        return 1;
    }

    private static int pick(CommandSourceStack source, int index1Based) {
        Entity entity = source.getEntity();
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        return QuestGiverEntity.selectQuestFor(player, index1Based - 1);
    }

    private static int reroll(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        return QuestGiverEntity.rerollQuestsFor(player);
    }
}
