package com.stephen.characterquest.entity;

import com.stephen.characterquest.network.NetworkHandler;
import com.stephen.characterquest.network.OpenQuestScreenS2CPacket;
import com.stephen.characterquest.quest.ItemFetchQuest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestGiverEntity extends PathfinderMob {

    // Global per-player active quest: shared across ALL quest givers
    private static final Map<UUID, ItemFetchQuest> ACTIVE_QUESTS = new HashMap<>();
    // Per-NPC quest offers for each player
    private final Map<UUID, ItemFetchQuest[]> questOffers = new HashMap<>();

    public QuestGiverEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Float in water
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Walk around slowly
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.35));

        // Look at players nearby
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));

        // Randomly look around
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        UUID playerId = player.getUUID();
        ItemFetchQuest active = ACTIVE_QUESTS.get(playerId);

        // If the player already has an active quest, handle turn‑in instead of opening the menu again
        if (active != null) {
            if (hasRequiredItems(player, active)) {
                takeRequiredItems(player, active);
                giveReward(player, active);
                player.sendSystemMessage(Component.literal("Nice work! Here's your reward."));
                ACTIVE_QUESTS.remove(playerId);
            } else {
                player.sendSystemMessage(Component.literal("You're not done yet. I still need those items."));
            }
            return InteractionResult.CONSUME;
        }

        ItemFetchQuest[] offers = questOffers.get(playerId);
        if (offers == null) {
            offers = new ItemFetchQuest[] {
                    ItemFetchQuest.getRandomQuest(),
                    ItemFetchQuest.getRandomQuest(),
                    ItemFetchQuest.getRandomQuest()
            };
            questOffers.put(playerId, offers);
        }

        String playerName = player.getName().getString();
        player.sendSystemMessage(Component.literal("I have a job for you, " + playerName + "."));

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.CHANNEL.sendTo(
                    new OpenQuestScreenS2CPacket(this.getId(), List.of(offers)),
                    serverPlayer.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }

        return InteractionResult.CONSUME;
    }

    public void selectQuestFor(ServerPlayer player, int index) {
        UUID playerId = player.getUUID();

        // Do not allow picking another quest when one is already active
        if (ACTIVE_QUESTS.containsKey(playerId)) {
            return;
        }

        ItemFetchQuest[] offers = questOffers.get(playerId);
        if (offers == null || index < 0 || index >= offers.length) {
            return;
        }

        ItemFetchQuest chosen = offers[index];
        ACTIVE_QUESTS.put(playerId, chosen);
        questOffers.remove(playerId);

        player.sendSystemMessage(Component.literal("Quest accepted: " + chosen.getDisplayText()));
    }

    public void rerollQuestsFor(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // cost: 1 diamond
        if (!removeOneDiamond(player)) {
            player.sendSystemMessage(Component.literal("You need 1 diamond to reroll quests."));
            return;
        }

        ItemFetchQuest[] offers = new ItemFetchQuest[] {
                ItemFetchQuest.getRandomQuest(),
                ItemFetchQuest.getRandomQuest(),
                ItemFetchQuest.getRandomQuest()
        };
        questOffers.put(playerId, offers);

        NetworkHandler.CHANNEL.sendTo(
                new OpenQuestScreenS2CPacket(this.getId(), List.of(offers)),
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    public void onQuestScreenClosed(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!ACTIVE_QUESTS.containsKey(playerId) && questOffers.containsKey(playerId)) {
            player.sendSystemMessage(Component.literal("See you again soon!"));
            questOffers.remove(playerId);
        }
    }

    private boolean removeOneDiamond(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == net.minecraft.world.item.Items.DIAMOND && stack.getCount() > 0) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private boolean hasRequiredItems(Player player, ItemFetchQuest quest) {
        int count = 0;

        // main inventory + hotbar
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == quest.requiredItem) {
                count += stack.getCount();
            }
        }
        // offhand
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == quest.requiredItem) {
                count += stack.getCount();
            }
        }

        return count >= quest.requiredCount;
    }

    private void takeRequiredItems(Player player, ItemFetchQuest quest) {
        int remaining = quest.requiredCount;

        // main inventory + hotbar
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == quest.requiredItem) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        // offhand
        for (int i = 0; i < player.getInventory().offhand.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (stack.getItem() == quest.requiredItem) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    private void giveReward(Player player, ItemFetchQuest quest) {
        ItemStack rewardStack = new ItemStack(quest.rewardItem, quest.rewardCount);
        if (!player.getInventory().add(rewardStack)) {
            player.drop(rewardStack, false);
        }
    }

    public static ItemFetchQuest getActiveQuest(UUID playerId) {
        return ACTIVE_QUESTS.get(playerId);
    }
}