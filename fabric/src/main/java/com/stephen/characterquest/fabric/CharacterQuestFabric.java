package com.stephen.characterquest.fabric;

import com.stephen.characterquest.client.QuestGiverRenderer;
import com.stephen.characterquest.command.ModCommands;
import com.stephen.characterquest.entity.QuestGiverEntity;
import com.stephen.characterquest.metrics.BStatsMetrics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CharacterQuestFabric implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "characterquest";

    public static final EntityType<QuestGiverEntity> QUEST_GIVER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "quest_giver"),
            EntityType.Builder.of(QuestGiverEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f).build("quest_giver")
    );

    public static final Item QUEST_GIVER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "villager_quest_spawn_egg"),
            new SpawnEggItem(QUEST_GIVER, 0x5E4E47, 0xC1A878, new Item.Properties())
    );

    private static final double QUEST_GIVER_SEARCH_RADIUS = 96.0D;
    private static final int PLAYER_VILLAGE_CHECK_INTERVAL_TICKS = 200;
    private static final Map<UUID, Long> NEXT_PLAYER_CHECK = new HashMap<>();
    private static volatile MinecraftServer CURRENT_SERVER;

    @Override
    public void onInitialize() {
        String modVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        BStatsMetrics.init(
                modVersion,
                29787,
                FabricLoader.getInstance().getConfigDir(),
                () -> CURRENT_SERVER == null ? 0 : CURRENT_SERVER.getPlayerCount(),
                () -> CURRENT_SERVER != null && CURRENT_SERVER.usesAuthentication() ? 1 : 0,
                () -> "Fabric"
        );

        FabricDefaultAttributeRegistry.register(QUEST_GIVER, Villager.createAttributes());

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> ModCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(CharacterQuestFabric::onServerTick);

        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(QUEST_GIVER_SPAWN_EGG));
    }

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(QUEST_GIVER, QuestGiverRenderer::new);
    }

    private static void onServerTick(MinecraftServer server) {
        CURRENT_SERVER = server;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onPlayerTick(player);
        }
    }

    private static void onPlayerTick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        UUID playerId = player.getUUID();
        long nextAllowedCheck = NEXT_PLAYER_CHECK.getOrDefault(playerId, 0L);

        if (gameTime < nextAllowedCheck) {
            return;
        }

        NEXT_PLAYER_CHECK.put(playerId, gameTime + PLAYER_VILLAGE_CHECK_INTERVAL_TICKS);

        if (!level.isVillage(player.blockPosition()) || hasQuestGiverNearby(level, player.getX(), player.getY(), player.getZ())) {
            return;
        }

        var searchBox = player.getBoundingBox().inflate(QUEST_GIVER_SEARCH_RADIUS);
        Villager villager = level.getEntitiesOfClass(Villager.class, searchBox).stream().findFirst().orElse(null);

        if (villager != null) {
            if (spawnQuestGiverAt(level, villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot())) {
                villager.discard();
            }
            return;
        }

        spawnQuestGiverAt(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    private static boolean hasQuestGiverNearby(ServerLevel level, double x, double y, double z) {
        var searchBox = new net.minecraft.world.phys.AABB(x, y, z, x, y, z).inflate(QUEST_GIVER_SEARCH_RADIUS);
        return !level.getEntitiesOfClass(QuestGiverEntity.class, searchBox).isEmpty();
    }

    private static boolean spawnQuestGiverAt(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        QuestGiverEntity questGiver = QUEST_GIVER.create(level);
        if (questGiver == null) {
            return false;
        }
        questGiver.moveTo(x, y, z, yRot, xRot);
        questGiver.setPersistenceRequired();
        return level.addFreshEntity(questGiver);
    }
}
