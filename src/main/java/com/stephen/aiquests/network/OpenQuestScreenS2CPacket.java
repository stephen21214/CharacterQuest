package com.stephen.aiquests.network;

import com.stephen.aiquests.client.screen.QuestScreen;
import com.stephen.aiquests.quest.ItemFetchQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenQuestScreenS2CPacket {

    public final int entityId;
    public final List<ItemFetchQuest> options;

    public OpenQuestScreenS2CPacket(int entityId, List<ItemFetchQuest> options) {
        this.entityId = entityId;
        this.options = options;
    }

    public static void encode(OpenQuestScreenS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.options.size());
        for (ItemFetchQuest q : msg.options) {
            buf.writeUtf(q.id);
            buf.writeInt(Item.getId(q.requiredItem));
            buf.writeInt(q.requiredCount);
            buf.writeInt(Item.getId(q.rewardItem));
            buf.writeInt(q.rewardCount);
        }
    }

    public static OpenQuestScreenS2CPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int size = buf.readInt();
        List<ItemFetchQuest> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            Item required = Item.byId(buf.readInt());
            int requiredCount = buf.readInt();
            Item reward = Item.byId(buf.readInt());
            int rewardCount = buf.readInt();
            list.add(new ItemFetchQuest(id, required, requiredCount, reward, rewardCount));
        }
        return new OpenQuestScreenS2CPacket(entityId, list);
    }

    public static void handle(OpenQuestScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OpenQuestScreenS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new QuestScreen(msg.entityId, msg.options));
    }
}

