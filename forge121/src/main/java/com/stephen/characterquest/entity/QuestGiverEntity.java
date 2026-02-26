package com.stephen.characterquest.entity;

import com.stephen.characterquest.quest.ItemFetchQuest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestGiverEntity extends PathfinderMob {

    private static final Map<UUID, ItemFetchQuest> ACTIVE_QUESTS = new HashMap<>();
    private static final Map<UUID, ItemFetchQuest[]> QUEST_OFFERS = new HashMap<>();

    public QuestGiverEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        UUID playerId = player.getUUID();
        ItemFetchQuest active = ACTIVE_QUESTS.get(playerId);
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

        ItemFetchQuest[] offers = QUEST_OFFERS.computeIfAbsent(playerId, ignored -> new ItemFetchQuest[] {
                ItemFetchQuest.getRandomQuest(),
                ItemFetchQuest.getRandomQuest(),
                ItemFetchQuest.getRandomQuest()
        });

        player.sendSystemMessage(Component.literal("I have jobs for you. Use /characterquest pick <1-3> to choose:"));
        for (int i = 0; i < offers.length; i++) {
            player.sendSystemMessage(Component.literal((i + 1) + ") " + offers[i].getDisplayText()));
        }
        player.sendSystemMessage(Component.literal("Need new choices? Use /characterquest reroll (cost: 1 diamond)."));
        return InteractionResult.CONSUME;
    }

    public static int selectQuestFor(ServerPlayer player, int index) {
        UUID playerId = player.getUUID();

        if (ACTIVE_QUESTS.containsKey(playerId)) {
            player.sendSystemMessage(Component.literal("You already have an active quest."));
            return 0;
        }

        ItemFetchQuest[] offers = QUEST_OFFERS.get(playerId);
        if (offers == null || index < 0 || index >= offers.length) {
            player.sendSystemMessage(Component.literal("No available quest offers. Talk to a Quest Giver first."));
            return 0;
        }

        ItemFetchQuest chosen = offers[index];
        ACTIVE_QUESTS.put(playerId, chosen);
        QUEST_OFFERS.remove(playerId);
        player.sendSystemMessage(Component.literal("Quest accepted: " + chosen.getDisplayText()));
        return 1;
    }

    public static int rerollQuestsFor(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (ACTIVE_QUESTS.containsKey(playerId)) {
            player.sendSystemMessage(Component.literal("Finish your active quest before rerolling."));
            return 0;
        }

        if (!removeOneDiamond(player)) {
            player.sendSystemMessage(Component.literal("You need 1 diamond to reroll quests."));
            return 0;
        }

        ItemFetchQuest[] offers = new ItemFetchQuest[] {
                ItemFetchQuest.getRandomQuest(),
                ItemFetchQuest.getRandomQuest(),
                ItemFetchQuest.getRandomQuest()
        };
        QUEST_OFFERS.put(playerId, offers);

        player.sendSystemMessage(Component.literal("Quest offers rerolled. Use /characterquest pick <1-3>."));
        for (int i = 0; i < offers.length; i++) {
            player.sendSystemMessage(Component.literal((i + 1) + ") " + offers[i].getDisplayText()));
        }
        return 1;
    }

    public static ItemFetchQuest getActiveQuest(UUID playerId) {
        return ACTIVE_QUESTS.get(playerId);
    }

    private static boolean removeOneDiamond(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == Items.DIAMOND && stack.getCount() > 0) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private boolean hasRequiredItems(Player player, ItemFetchQuest quest) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == quest.requiredItem) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == quest.requiredItem) {
                count += stack.getCount();
            }
        }
        return count >= quest.requiredCount;
    }

    private void takeRequiredItems(Player player, ItemFetchQuest quest) {
        int remaining = quest.requiredCount;

        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (stack.getItem() == quest.requiredItem) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (remaining <= 0) {
                break;
            }
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
}
