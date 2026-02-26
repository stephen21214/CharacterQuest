package com.stephen.characterquest;

import com.stephen.characterquest.entity.QuestGiverEntity;
import com.stephen.characterquest.metrics.BStatsMetrics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(CharacterQuestNeoForge.MOD_ID)
public class CharacterQuestNeoForge {

    public static final String MOD_ID = "characterquest";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<QuestGiverEntity>> QUEST_GIVER = ENTITY_TYPES.register(
            "quest_giver",
            () -> EntityType.Builder.of(QuestGiverEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "quest_giver").toString())
    );

    public static final DeferredHolder<Item, Item> QUEST_GIVER_SPAWN_EGG = ITEMS.register(
            "villager_quest_spawn_egg",
            () -> new SpawnEggItem(QUEST_GIVER.get(), 0x5E4E47, 0xC1A878, new Item.Properties())
    );

    public CharacterQuestNeoForge(IEventBus modBus) {
        String modVersion = ModList.get().getModContainerById(MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        BStatsMetrics.init(
                modVersion,
                29788,
                FMLPaths.CONFIGDIR.get(),
                () -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    return server == null ? 0 : server.getPlayerCount();
                },
                () -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    return server != null && server.usesAuthentication() ? 1 : 0;
                },
                () -> "NeoForge"
        );
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
    }
}
