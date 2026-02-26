package com.stephen.characterquest;

import com.stephen.characterquest.entity.QuestGiverEntity;
import com.stephen.characterquest.metrics.BStatsMetrics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CharacterQuestMod.MOD_ID)
public class CharacterQuestMod {

    public static final String MOD_ID = "characterquest";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<EntityType<QuestGiverEntity>> QUEST_GIVER = ENTITY_TYPES.register(
            "quest_giver",
            () -> EntityType.Builder.of(QuestGiverEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "quest_giver").toString())
    );

    public static final RegistryObject<Item> QUEST_GIVER_SPAWN_EGG = ITEMS.register(
            "villager_quest_spawn_egg",
            () -> new SpawnEggItem(QUEST_GIVER.get(), 0x5E4E47, 0xC1A878, new Item.Properties())
    );

    public CharacterQuestMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        String modVersion = ModList.get().getModContainerById(MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        BStatsMetrics.init(modVersion, 29786);
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
    }
}
