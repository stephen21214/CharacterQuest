package com.stephen.aiquests.network;

import com.stephen.aiquests.entity.QuestGiverEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RerollQuestsC2SPacket {

    public final int entityId;

    public RerollQuestsC2SPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(RerollQuestsC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static RerollQuestsC2SPacket decode(FriendlyByteBuf buf) {
        return new RerollQuestsC2SPacket(buf.readInt());
    }

    public static void handle(RerollQuestsC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.entityId);
            if (entity instanceof QuestGiverEntity questGiver) {
                questGiver.rerollQuestsFor(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}

