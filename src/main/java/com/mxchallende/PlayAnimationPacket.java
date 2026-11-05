package com.mxchallende;

import java.util.function.Supplier;

import com.mxchallende.client.SkullAnimation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> clientes: "reproduce la animación de la calavera". */
public class PlayAnimationPacket {
    public PlayAnimationPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket();
    }

    public static void handle(PlayAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> SkullAnimation::start));
        ctx.get().setPacketHandled(true);
    }
}
