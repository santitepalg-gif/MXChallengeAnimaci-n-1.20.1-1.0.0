package com.mxchallende;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Toda la lógica que antes vivía en las .mcfunction (comandos, muerte, loop). */
public class ServerEvents {
    // --- comandos.mcfunction: gamerules ---------------------------------------------------
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (Config.COMMON.applyGamerules.get()) {
            server.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, server);
            server.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);
        }
    }

    // --- muerte.mcfunction: aviso en chat + sonido + animación ----------------------------
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        // tellraw @a [{selector:@s, yellow}, {" se murió", yellow}]
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("mxchallende.died", player.getDisplayName()).withStyle(ChatFormatting.YELLOW),
                false);

        if (Config.COMMON.animateOnDeath.get()) {
            trigger(server);
        }
    }

    // gamemode spectator (el datapack lo hacía justo después de reaparecer con doImmediateRespawn)
    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return; // volver del End no es una muerte
        if (Config.COMMON.spectatorOnDeath.get() && event.getEntity() instanceof ServerPlayer player) {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    // --- /mxchallende preview (para probar la animación sin morir ni cambiar la dificultad) ---
    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mxchallende")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("preview").executes(ctx -> {
                    trigger(ctx.getSource().getServer());
                    return 1;
                })));
    }

    // --- helpers -----------------------------------------------------------------------
    /** playsound reviil a todos + animación en todos los clientes. */
    private static void trigger(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(ModSounds.REVIIL.get(), SoundSource.VOICE, 1.0F, 1.0F);
        }
        Net.sendToAll(new PlayAnimationPacket());
    }
}
