package com.mxchallende;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MXChallenge.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, PlayAnimationPacket.class,
                PlayAnimationPacket::encode, PlayAnimationPacket::decode, PlayAnimationPacket::handle);
    }

    public static void sendToAll(PlayAnimationPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    private Net() {}
}
