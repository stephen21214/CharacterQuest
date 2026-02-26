package com.stephen.characterquest.network;

import com.stephen.characterquest.CharacterQuestMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CharacterQuestMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    private NetworkHandler() {}

    public static void register() {
        registerMessage(OpenQuestScreenS2CPacket.class, OpenQuestScreenS2CPacket::encode,
                OpenQuestScreenS2CPacket::decode,
                OpenQuestScreenS2CPacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        registerMessage(SelectQuestC2SPacket.class, SelectQuestC2SPacket::encode,
                SelectQuestC2SPacket::decode,
                SelectQuestC2SPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(RerollQuestsC2SPacket.class, RerollQuestsC2SPacket::encode,
                RerollQuestsC2SPacket::decode,
                RerollQuestsC2SPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(QuestScreenClosedC2SPacket.class, QuestScreenClosedC2SPacket::encode,
                QuestScreenClosedC2SPacket::decode,
                QuestScreenClosedC2SPacket::handle, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void registerMessage(Class<T> type,
                                            BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
                                            Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
                                            BiConsumer<T, Supplier<NetworkEvent.Context>> handler,
                                            NetworkDirection direction) {
        CHANNEL.registerMessage(
                nextId++,
                type,
                encoder,
                decoder,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    if (direction == NetworkDirection.PLAY_TO_CLIENT) {
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(msg, ctxSupplier));
                    } else {
                        handler.accept(msg, ctxSupplier);
                    }
                }
        );
    }
}

