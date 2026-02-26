package com.stephen.characterquest;

import com.stephen.characterquest.entity.QuestGiverEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CharacterQuestNeoForge.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class CommonNeoForgeEvents {
    private static final double QUEST_GIVER_SEARCH_RADIUS = 96.0D;
    private static final int PLAYER_VILLAGE_CHECK_INTERVAL_TICKS = 200;
    private static final Map<UUID, Long> NEXT_PLAYER_CHECK = new HashMap<>();

    private CommonNeoForgeEvents() {}

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (event.loadedFromDisk() || !(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (!serverLevel.isVillage(villager.blockPosition())) {
            return;
        }

        if (hasQuestGiverNearby(serverLevel, villager.getX(), villager.getY(), villager.getZ())) {
            return;
        }

        if (spawnQuestGiverAt(serverLevel, villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
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

        AABB searchBox = player.getBoundingBox().inflate(QUEST_GIVER_SEARCH_RADIUS);
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
        AABB searchBox = new AABB(x, y, z, x, y, z).inflate(QUEST_GIVER_SEARCH_RADIUS);
        return !level.getEntitiesOfClass(QuestGiverEntity.class, searchBox).isEmpty();
    }

    private static boolean spawnQuestGiverAt(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        QuestGiverEntity questGiver = CharacterQuestNeoForge.QUEST_GIVER.get().create(level);
        if (questGiver == null) {
            return false;
        }

        questGiver.moveTo(x, y, z, yRot, xRot);
        questGiver.setPersistenceRequired();
        return level.addFreshEntity(questGiver);
    }
}
