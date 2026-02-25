package com.stephen.aiquests.client;

import com.stephen.aiquests.CharacterQuestMod;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.stephen.aiquests.client.QuestGiverRenderer;

@Mod.EventBusSubscriber(modid = CharacterQuestMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CharacterQuestMod.QUEST_GIVER.get(), QuestGiverRenderer::new);
    }
}

