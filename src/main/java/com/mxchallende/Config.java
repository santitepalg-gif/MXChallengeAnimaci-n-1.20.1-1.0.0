package com.mxchallende;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Common, ForgeConfigSpec> c = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = c.getLeft();
        COMMON_SPEC = c.getRight();
        Pair<Client, ForgeConfigSpec> k = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = k.getLeft();
        CLIENT_SPEC = k.getRight();
    }

    /** Opciones del servidor / mundo (config/mxchallende-common.toml). */
    public static final class Common {
        public final ForgeConfigSpec.BooleanValue applyGamerules;
        public final ForgeConfigSpec.BooleanValue spectatorOnDeath;
        public final ForgeConfigSpec.BooleanValue animateOnDeath;

        Common(ForgeConfigSpec.Builder b) {
            applyGamerules = b.comment("Al iniciar el mundo aplica doImmediateRespawn=true y showDeathMessages=false (lo que hacía comandos.mcfunction).")
                    .define("applyGamerules", true);
            spectatorOnDeath = b.comment("Al morir, el jugador pasa a modo espectador (igual que el datapack).")
                    .define("spectatorOnDeath", true);
            animateOnDeath = b.comment("Mensaje en el chat + sonido 'reviil' + calavera cuando muere un jugador (igual que el datapack).")
                    .define("animateOnDeath", true);
        }
    }

    /** Opciones de cada jugador (config/mxchallende-client.toml). */
    public static final class Client {
        public final ForgeConfigSpec.BooleanValue epicSounds;
        public final ForgeConfigSpec.BooleanValue cameraShake;

        Client(ForgeConfigSpec.Builder b) {
            epicSounds = b.comment("Truenos y latidos durante la animación.").define("epicSounds", true);
            cameraShake = b.comment("Sacudida de cámara en el impacto de la calavera.").define("cameraShake", true);
        }
    }

    private Config() {}
}
