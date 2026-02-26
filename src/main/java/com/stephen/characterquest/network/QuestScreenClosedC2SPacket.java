package com.stephen.characterquest.network;

import com.stephen.characterquest.entity.QuestGiverEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestScreenClosedC2SPacket {

    public final int entityId;

    public QuestScreenClosedC2SPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(QuestScreenClosedC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static QuestScreenClosedC2SPacket decode(FriendlyByteBuf buf) {
        return new QuestScreenClosedC2SPacket(buf.readInt());
    }

    public static void handle(QuestScreenClosedC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.entityId);
            if (entity instanceof QuestGiverEntity questGiver) {
                questGiver.onQuestScreenClosed(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}

