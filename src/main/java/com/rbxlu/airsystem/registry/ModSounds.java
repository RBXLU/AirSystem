package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, AirSystem.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_ALERT = register("siren_alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_ALL_CLEAR = register("siren_all_clear");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_BUTTON_CLICK = register("alarm_button_click");

    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_PISTON = register("drone_piston");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_JET = register("drone_jet");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_ELECTRIC = register("drone_electric");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_DIVE = register("drone_dive");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_LAUNCH = register("drone_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_FAILURE = register("engine_failure");

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_NEAR = register("explosion_near");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_DISTANT = register("explosion_distant");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDOW_SHATTER = register("window_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEBRIS_FALL = register("debris_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_FIRE_35 = register("turret_fire_35");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_FIRE_30 = register("turret_fire_30");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_TRAVERSE = register("turret_traverse");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_RELOAD = register("turret_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_LOCK = register("radar_lock");

    public static final DeferredHolder<SoundEvent, SoundEvent> REMOTE_BEEP = register("remote_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> REMOTE_ERROR = register("remote_error");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAP_MARK = register("map_mark");

    // Mutes the vanilla explosion; the mod plays its own report, delayed by range.
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENCE = register("silence");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private ModSounds() {
    }
}
