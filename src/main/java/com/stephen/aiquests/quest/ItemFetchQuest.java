package com.stephen.aiquests.quest;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Random;

public class ItemFetchQuest {

    public final String id;
    public final Item requiredItem;
    public final int requiredCount;
    public final Item rewardItem;
    public final int rewardCount;

    public ItemFetchQuest(String id, Item requiredItem, int requiredCount, Item rewardItem, int rewardCount) {
        this.id = id;
        this.requiredItem = requiredItem;
        this.requiredCount = requiredCount;
        this.rewardItem = rewardItem;
        this.rewardCount = rewardCount;
    }

    private static final Random RANDOM = new Random();

    public static ItemFetchQuest getRandomQuest() {
        // Randomly choose what to collect – balanced so trades feel fair
        Item[] easyItems = new Item[] {
                Items.OAK_LOG, Items.BIRCH_LOG, Items.SPRUCE_LOG,
                Items.COBBLESTONE, Items.SAND
        };

        Item[] mediumItems = new Item[] {
                Items.COAL, Items.IRON_INGOT, Items.COPPER_INGOT
        };

        Item[] hardItems = new Item[] {
                Items.DIAMOND, Items.GOLD_INGOT, Items.LAPIS_LAZULI
        };

        int difficulty = RANDOM.nextInt(3); // 0 = easy, 1 = medium, 2 = hard

        Item required;
        int requiredCount;
        Item reward;
        int rewardCount;

        switch (difficulty) {
            case 0 -> { // easy: logs / cobble / sand -> small iron/coal
                required = easyItems[RANDOM.nextInt(easyItems.length)];
                requiredCount = 16 + RANDOM.nextInt(3) * 16; // 16, 32, 48
                reward = RANDOM.nextBoolean() ? Items.IRON_INGOT : Items.COAL;
                rewardCount = 4 + RANDOM.nextInt(5); // 4‑8
            }
            case 1 -> { // medium: coal / iron / copper -> a few diamonds
                required = mediumItems[RANDOM.nextInt(mediumItems.length)];
                requiredCount = 24 + RANDOM.nextInt(4) * 8; // 24‑48
                reward = Items.DIAMOND;
                rewardCount = 1 + RANDOM.nextInt(2); // 1‑2
            }
            default -> { // hard: diamonds / gold / lapis -> netherite scrap but in fair amounts
                required = hardItems[RANDOM.nextInt(hardItems.length)];
                requiredCount = 8 + RANDOM.nextInt(3) * 4; // 8‑16
                reward = Items.NETHERITE_SCRAP;
                rewardCount = 1; // always 1 scrap
            }
        }

        String id = "random_" + System.currentTimeMillis() + "_" + RANDOM.nextInt(1000);
        return new ItemFetchQuest(id, required, requiredCount, reward, rewardCount);
    }

    public String getDisplayText() {
        return "Bring me " + requiredCount + " " +
                requiredItem.getDescription().getString().toLowerCase() +
                " and I'll pay you " + rewardCount + " " +
                rewardItem.getDescription().getString().toLowerCase() + ".";
    }
}