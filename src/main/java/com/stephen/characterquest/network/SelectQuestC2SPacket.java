package com.stephen.characterquest.network;

import com.stephen.characterquest.entity.QuestGiverEntity;
import com.stephen.characterquest.quest.ItemFetchQuest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectQuestC2SPacket {

    public final int entityId;
    public final int index;

    public SelectQuestC2SPacket(int entityId, int index) {
        this.entityId = entityId;
        this.index = index;
    }

    public static void encode(SelectQuestC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.index);
    }

    public static SelectQuestC2SPacket decode(FriendlyByteBuf buf) {
        return new SelectQuestC2SPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(SelectQuestC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.entityId);
            if (entity instanceof QuestGiverEntity questGiver) {
                questGiver.selectQuestFor(player, msg.index);
            }
        });
        ctx.setPacketHandled(true);
    }
}

