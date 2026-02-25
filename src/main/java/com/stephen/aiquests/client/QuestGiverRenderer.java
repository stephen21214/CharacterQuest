package com.stephen.aiquests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stephen.aiquests.entity.QuestGiverEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;

public class QuestGiverRenderer extends MobRenderer<QuestGiverEntity, VillagerModel<QuestGiverEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("characterquest", "textures/entity/quest_villager.png");

    public QuestGiverRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(QuestGiverEntity entity) {
        return TEXTURE;
    }
}