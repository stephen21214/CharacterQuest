package com.stephen.characterquest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.stephen.characterquest.CharacterQuestMod;
import com.stephen.characterquest.entity.QuestGiverEntity;
import com.stephen.characterquest.quest.ItemFetchQuest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CharacterQuestMod.MOD_ID)
public final class ModCommands {
    private ModCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var check = (com.mojang.brigadier.Command<CommandSourceStack>) ctx -> {
            CommandSourceStack source = ctx.getSource();
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
        };

        dispatcher.register(
                Commands.literal("characterquest")
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /characterquest check"));
                            return 0;
                        })
                        .then(Commands.literal("check").executes(check))
        );
    }
}

