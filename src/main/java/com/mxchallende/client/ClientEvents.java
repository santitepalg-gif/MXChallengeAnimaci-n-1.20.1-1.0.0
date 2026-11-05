package com.mxchallende.client;

import com.mxchallende.Config;
import com.mxchallende.MXChallenge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientEvents {

    @Mod.EventBusSubscriber(modid = MXChallenge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("skull_animation",
                    (gui, graphics, partialTick, width, height) -> SkullAnimation.render(graphics, partialTick, width, height));
        }
    }

    @Mod.EventBusSubscriber(modid = MXChallenge.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
                SkullAnimation.tick();
            }
        }

        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            if (!SkullAnimation.isActive() || !Config.CLIENT.cameraShake.get()) return;
            float s = SkullAnimation.shakeAt(SkullAnimation.time((float) event.getPartialTick()));
            float t = SkullAnimation.time((float) event.getPartialTick());
            event.setRoll(event.getRoll() + (float) Math.sin(t * 3.1F) * s * 2.5F);
            event.setPitch(event.getPitch() + (float) Math.sin(t * 5.3F) * s * 1.2F);
            event.setYaw(event.getYaw() + (float) Math.cos(t * 4.1F) * s * 1.2F);
        }
    }

    private ClientEvents() {}
}
