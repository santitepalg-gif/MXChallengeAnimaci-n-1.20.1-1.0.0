package com.mxchallende;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MXChallenge.MODID);

    /** Equivale a "minecraft:reviil" del resource pack original (categoría voice). */
    public static final RegistryObject<SoundEvent> REVIIL = SOUNDS.register("reviil",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MXChallenge.MODID, "reviil")));

    private ModSounds() {}
}
