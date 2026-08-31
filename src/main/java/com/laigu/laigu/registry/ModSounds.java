package com.laigu.laigu.registry;

import com.laigu.laigu.Laigu;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 来古牌声音事件注册表。 */
public final class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Laigu.MODID);

    public static final RegistryObject<SoundEvent> FUSHENG_TOUXIAN =
            SOUNDS.register("fusheng_touxian",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Laigu.MODID, "fusheng_touxian")));

    private ModSounds()
    {
    }
}
