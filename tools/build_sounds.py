"""Sound synthesis: air raid siren, drone engines, detonations, gun fire.

Every sound is built from physically meaningful layers — transient, body, tail,
reflections. Nothing is sampled; it is all computed here."""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import soundfile as sf

import dsp
from dsp import (SR, attack_decay, bandpass, decay, fade_edges, harmonic_stack, highpass,
                 impulse_response, limiter, loopable, lowpass, noise, normalize, phase,
                 pulse_train, resonator, reverb, t)

ROOT = Path(__file__).resolve().parents[1]
SOUNDS = ROOT / "src/main/resources/assets/airsystem/sounds"
RNG = np.random.default_rng(20240501)

IR_CITY = impulse_response(2.2, RNG, cutoff=3200.0, rate=2.6)
IR_FIELD = impulse_response(3.4, RNG, cutoff=1400.0, rate=1.5)
IR_SMALL = impulse_response(0.5, RNG, cutoff=6000.0, rate=9.0)


def save(name: str, signal: np.ndarray) -> None:
    SOUNDS.mkdir(parents=True, exist_ok=True)
    audio = signal.astype(np.float32) if np.max(np.abs(signal)) < 1e-6 \
        else normalize(limiter(signal), peak=0.85).astype(np.float32)
    sf.write(SOUNDS / f"{name}.ogg", audio, SR, format="OGG", subtype="VORBIS")
    print(f"  {name}.ogg  {len(audio) / SR:.1f} s")


def _chopper(frequency: np.ndarray, duty: float, sharpness: float = 0.06) -> np.ndarray:
    """Siren rotor chopping the air.

    A real siren is not a tone generator: the impeller closes stator ports and
    the result is a pulsed airflow, which is where its hard brassy timbre comes
    from. A sum of sines cannot reproduce it.
    """
    cycle = (np.cumsum(frequency) / SR) % 1.0
    rise = np.clip(cycle / sharpness, 0.0, 1.0)
    fall = np.clip((duty - cycle) / sharpness, 0.0, 1.0)
    return np.clip(rise, 0.0, 1.0) * np.clip(fall, 0.0, 1.0) * 2.0 - 1.0


def _horn(signal: np.ndarray) -> np.ndarray:
    """Horn: a passband with a pronounced resonance."""
    shaped = bandpass(signal, 320.0, 4200.0, order=3)
    return shaped + resonator(signal, 1150.0, q=3.5) * 0.7 + resonator(signal, 2300.0, q=5.0) * 0.3


def siren_alert() -> np.ndarray:
    """Electromechanical siren: spin-up, wail and coast-down."""
    duration = 9.0
    time = t(duration)

    rotor = np.interp(time,
                      [0.0, 0.6, 2.9, 4.4, 6.2, 8.3, 9.0],
                      [0.30, 0.42, 1.00, 1.00, 0.72, 0.34, 0.30])
    rotor = lowpass(rotor, 6.0, order=2)

    base = 250.0 + 330.0 * rotor
    voice_a = _chopper(base, duty=0.44)
    voice_b = _chopper(base * 1.2, duty=0.38)

    sweep = 0.82 + 0.18 * np.sin(2 * np.pi * 0.55 * time)

    air = bandpass(noise(duration, RNG), 500.0, 7000.0) * 0.16 * rotor
    motor = np.sin(phase(base / 8.0)) * 0.05 * rotor

    body = _horn(voice_a * 0.62 + voice_b * 0.42) * sweep + air + motor

    envelope = np.clip(time / 0.9, 0.0, 1.0) * np.clip((duration - time) / 0.9, 0.0, 1.0)
    return reverb(body * envelope * rotor, IR_CITY, mix=0.38)


def siren_all_clear() -> np.ndarray:
    """All-clear: a steady sustained note at constant rpm."""
    duration = 7.0
    time = t(duration)

    rotor = np.clip(time / 1.4, 0.0, 1.0) * np.clip((duration - time) / 1.2, 0.0, 1.0)
    base = np.full_like(time, 505.0) + 2.5 * np.sin(2 * np.pi * 4.0 * time)

    voice_a = _chopper(base, duty=0.44)
    voice_b = _chopper(base * 1.2, duty=0.38)
    air = bandpass(noise(duration, RNG), 500.0, 6000.0) * 0.12

    body = _horn(voice_a * 0.6 + voice_b * 0.4) * (0.86 + 0.14 * np.sin(2 * np.pi * 0.5 * time))
    return reverb((body + air) * rotor, IR_CITY, mix=0.34)


LOOP_DURATION = 2.12
LOOP_PERIOD = 2.00


def _wandering_phase(duration: float, base_hz: float, wander: float = 0.015,
                     wobble_hz: float = 1.5) -> tuple[np.ndarray, np.ndarray]:
    """Shaft phase and instantaneous frequency, with the rpm wandering slowly.

    An engine under load never holds its speed exactly, and a perfectly constant
    frequency is what makes a synthesised engine sound synthetic. The wander
    period divides the loop length, otherwise the seam clicks.
    """
    time = t(duration)
    cycles = max(1.0, round(wobble_hz * LOOP_PERIOD))
    drift = 1.0 + wander * np.sin(2 * np.pi * cycles / LOOP_PERIOD * time)
    freq = base_hz * drift
    return 2 * np.pi * np.cumsum(freq) / SR, freq


def drone_piston() -> np.ndarray:
    """Two-stroke: the moped rasp of a Shahed.

    Built like a real exhaust rather than a sum of sines. Each firing is a short
    asymmetric scavenging pulse, which is what produces the dense harmonic comb;
    the pulse then drives the resonances of the expansion chamber and stub. The
    two-blade propeller shares the shaft, adding a blade slap once per turn, and
    the firings vary slightly in strength the way a lightly loaded two-stroke
    misfires.
    """
    duration = LOOP_DURATION
    firing = 92.0

    crank, _freq = _wandering_phase(duration, firing)
    cycle = (crank / (2 * np.pi)) % 1.0

    blowdown = np.exp(-cycle * 26.0) - np.exp(-cycle * 150.0)

    index = np.floor(crank / (2 * np.pi)).astype(int)
    jitter = 0.72 + 0.28 * RNG.random(index.max() + 2)[index]
    blowdown = blowdown * jitter

    chamber = resonator(blowdown, 385.0, q=4.5) * 1.7
    header = resonator(blowdown, 1150.0, q=7.0) * 0.55
    crackle = resonator(blowdown, 2450.0, q=9.0) * 0.18

    breathing = 0.55 + 0.45 * np.cos(crank)
    intake = bandpass(noise(duration, RNG), 300.0, 2600.0) * breathing * 0.12

    blade = 0.6 + 0.4 * np.cos(2.0 * crank)
    wash = bandpass(noise(duration, RNG), 200.0, 1800.0) * blade * 0.22
    thump = np.sin(2.0 * crank) * 0.12

    body = chamber + header + crackle + intake + wash + thump
    return loopable(fade_edges(lowpass(body, 7000.0), 0.005))


def drone_jet() -> np.ndarray:
    """Small turbojet: compressor buzz-saw over combustor roar.

    What identifies a turbine is not the noise but the tones at shaft speed and
    blade-passing frequency; the broadband roar sits behind them.
    """
    duration = LOOP_DURATION
    shaft, _freq = _wandering_phase(duration, 165.0, wander=0.008, wobble_hz=1.0)

    buzz = np.zeros_like(shaft)
    for order in range(1, 9):
        buzz += np.sin(order * shaft) * (0.30 / order ** 0.7)

    blades = np.sin(11.0 * shaft) * 0.22 + np.sin(22.0 * shaft) * 0.09

    core = bandpass(noise(duration, RNG), 140.0, 1600.0) * 0.75
    jet_noise = highpass(noise(duration, RNG), 3200.0) * 0.20
    body = buzz + blades + core + jet_noise
    return loopable(fade_edges(body, 0.005))


def drone_electric() -> np.ndarray:
    """Electric motor and propeller: a thin whine over blade wash.

    Almost no bottom end — blade passing, winding whistle at the switching
    frequency and the wash, and nothing else.
    """
    duration = LOOP_DURATION
    shaft, _freq = _wandering_phase(duration, 118.0, wander=0.010, wobble_hz=2.0)

    blade = (np.sin(2.0 * shaft) * 0.30 + np.sin(4.0 * shaft) * 0.12
             + np.sin(6.0 * shaft) * 0.05)
    windings = np.sin(14.0 * shaft) * 0.10 + np.sin(28.0 * shaft) * 0.04
    wash = bandpass(noise(duration, RNG), 900.0, 6500.0) * (0.6 + 0.4 * np.cos(2.0 * shaft)) * 0.16

    return loopable(fade_edges(blade + windings + wash, 0.005))


def drone_dive() -> np.ndarray:
    """Dive: the rising wail of the airflow."""
    duration = 2.6
    time = t(duration)
    frequency = 220.0 + 1000.0 * (time / duration) ** 1.8
    tone = harmonic_stack(frequency, count=10, tilt=1.1) * 0.6
    rush = bandpass(noise(duration, RNG), 600.0, 6000.0) * (0.2 + 0.5 * time / duration)
    return fade_edges(tone + rush, 0.08)


def drone_launch() -> np.ndarray:
    """Launch: the shove of the booster off the rail."""
    duration = 1.8
    bang = highpass(noise(duration, RNG), 900.0) * decay(duration, 12.0)
    thrust = bandpass(noise(duration, RNG), 150.0, 2200.0) * attack_decay(duration, 0.05, 2.4)
    thump = np.sin(2 * np.pi * 62.0 * t(duration)) * decay(duration, 7.0) * 0.7
    return reverb(bang * 0.6 + thrust + thump, IR_FIELD, mix=0.2)


def engine_failure() -> np.ndarray:
    """Engine failure: misfires, bangs and a stop."""
    duration = 3.0
    time = t(duration)
    rpm = 104.0 * np.exp(-time * 0.55)
    pulses = (np.cos(phase(rpm)) > 0.94).astype(float)
    misfire = (RNG.random(len(time)) > 0.4).astype(float)
    body = resonator(pulses * misfire, 240.0, q=5.0) * 2.0
    knock = resonator(pulses, 1300.0, q=14.0) * 0.8
    return fade_edges(body + knock, 0.05)


def explosion_near() -> np.ndarray:
    """Close detonation: shock front, body, ground slap and rumble."""
    duration = 3.4
    time = t(duration)
    raw = noise(duration, RNG)

    crack = highpass(raw, 2600.0) * decay(duration, 45.0) * 1.1
    body = bandpass(raw, 90.0, 900.0) * decay(duration, 5.5) * 1.4
    sub_freq = 90.0 * np.exp(-time * 5.0) + 26.0
    sub = np.sin(phase(sub_freq)) * decay(duration, 2.6) * 1.2
    rumble = lowpass(raw, 180.0) * decay(duration, 1.2) * 0.9
    debris = highpass(raw, 1500.0) * decay(duration, 1.6) * 0.12

    mixed = crack + body + sub + rumble + debris
    return reverb(fade_edges(mixed, 0.002), IR_FIELD, mix=0.32)


def explosion_distant() -> np.ndarray:
    """Distant detonation: the air has eaten the top, leaving a heavy boom."""
    duration = 4.5
    time = t(duration)
    raw = noise(duration, RNG)

    body = lowpass(raw, 190.0) * attack_decay(duration, 0.05, 2.0) * 1.3
    sub_freq = 52.0 * np.exp(-time * 3.0) + 22.0
    sub = np.sin(phase(sub_freq)) * decay(duration, 1.4)
    tail = lowpass(raw, 110.0) * decay(duration, 0.6) * 0.8
    return reverb(fade_edges(body + sub + tail, 0.05), IR_FIELD, mix=0.45)


def window_shatter() -> np.ndarray:
    """Breaking glass: a scatter of short bright partials."""
    duration = 1.6
    out = np.zeros(int(SR * duration))
    for _ in range(34):
        start = int(RNG.integers(0, int(len(out) * 0.55)))
        length = int(SR * RNG.uniform(0.05, 0.22))
        end = min(len(out), start + length)
        local = np.arange(end - start) / SR
        freq = RNG.uniform(2400.0, 7200.0)
        partial = (np.sin(2 * np.pi * freq * local)
                   + 0.4 * np.sin(2 * np.pi * freq * 2.7 * local))
        out[start:end] += partial * np.exp(-local * RNG.uniform(16.0, 34.0)) * 0.45
    crackle = highpass(noise(duration, RNG), 3800.0) * decay(duration, 7.0) * 0.35
    return reverb(fade_edges(out + crackle, 0.002), IR_SMALL, mix=0.25)


def debris_fall() -> np.ndarray:
    """Falling debris."""
    duration = 2.2
    out = np.zeros(int(SR * duration))
    for _ in range(22):
        start = int(RNG.integers(0, len(out) - 3000))
        length = int(SR * RNG.uniform(0.03, 0.12))
        end = min(len(out), start + length)
        local = np.arange(end - start) / SR
        hit = lowpass(RNG.normal(0, 1, end - start), RNG.uniform(700.0, 2600.0))
        out[start:end] += hit * np.exp(-local * 26.0)
    return reverb(fade_edges(out, 0.01), IR_SMALL, mix=0.3)


def gun_shot(caliber: float, duration: float = 0.45) -> np.ndarray:
    """Autocannon shot: muzzle blast, body, short tail."""
    raw = noise(duration, RNG)
    muzzle = highpass(raw, 1800.0 * caliber) * decay(duration, 90.0) * 1.2
    body = bandpass(raw, 120.0 * caliber, 1400.0 * caliber) * decay(duration, 26.0) * 1.3
    thump = np.sin(phase(np.full(int(SR * duration), 78.0 * caliber))) \
        * decay(duration, 20.0) * 0.8
    mech = resonator(raw, 2400.0, q=18.0) * decay(duration, 60.0) * 0.25
    return reverb(fade_edges(muzzle + body + thump + mech, 0.0005), IR_FIELD, mix=0.22)


def turret_traverse() -> np.ndarray:
    """Traverse drive: a geared electric motor."""
    duration = 0.8
    time = t(duration)
    motor = harmonic_stack(np.full_like(time, 96.0), count=10, tilt=1.1) * 0.45
    gear = np.sin(2 * np.pi * 480.0 * time) * 0.12
    envelope = np.clip(time / 0.1, 0, 1) * np.clip((duration - time) / 0.2, 0, 1)
    return lowpass((motor + gear) * envelope, 3000.0)


def turret_reload() -> np.ndarray:
    """Reload: the clatter of the feed mechanism."""
    duration = 1.4
    out = np.zeros(int(SR * duration))
    for offset, gain in ((0.0, 1.0), (0.24, 0.8), (0.52, 0.9), (0.95, 1.0)):
        start = int(offset * SR)
        length = int(SR * 0.18)
        end = min(len(out), start + length)
        local = np.arange(end - start) / SR
        hit = RNG.normal(0, 1, end - start)
        clank = (resonator(hit, 780.0, q=20.0) + resonator(hit, 1850.0, q=26.0) * 0.6)
        out[start:end] += clank * np.exp(-local * 26.0) * gain
    return reverb(fade_edges(out, 0.002), IR_SMALL, mix=0.28)


def radar_lock() -> np.ndarray:
    """Target lock: a short series of beeps."""
    duration = 0.7
    out = np.zeros(int(SR * duration))
    for offset in (0.0, 0.16, 0.32):
        start = int(offset * SR)
        length = int(SR * 0.10)
        end = min(len(out), start + length)
        local = np.arange(end - start) / SR
        out[start:end] += (np.sin(2 * np.pi * 1560.0 * local)
                           + 0.3 * np.sin(2 * np.pi * 3120.0 * local)) \
            * np.exp(-local * 22.0)
    return fade_edges(out, 0.002)


def beep(frequency: float, duration: float) -> np.ndarray:
    time = t(duration)
    tone = np.sin(2 * np.pi * frequency * time) + 0.25 * np.sin(2 * np.pi * frequency * 2 * time)
    return fade_edges(tone * np.exp(-time * 8.0), 0.004)


def error_buzz() -> np.ndarray:
    """Fault: a low intermittent buzzer."""
    duration = 0.5
    time = t(duration)
    tone = harmonic_stack(np.full_like(time, 150.0), count=8, tilt=0.9)
    gate = (np.sin(2 * np.pi * 16.0 * time) > -0.2).astype(float)
    return fade_edges(tone * gate * np.exp(-time * 2.0), 0.004)


def silence() -> np.ndarray:
    """Placeholder: the vanilla explosion must play something, but the mod issues
    its own report, delayed by range."""
    return np.zeros(int(SR * 0.05))


def click() -> np.ndarray:
    """Toggle click."""
    duration = 0.14
    raw = noise(duration, RNG)
    body = resonator(raw, 2200.0, q=16.0) * decay(duration, 90.0)
    tick = highpass(raw, 4000.0) * decay(duration, 160.0) * 0.7
    return fade_edges(body + tick, 0.001)


GENERATORS = {
    "siren_alert": siren_alert,
    "siren_all_clear": siren_all_clear,
    "alarm_button_click": click,
    "drone_piston": drone_piston,
    "drone_jet": drone_jet,
    "drone_electric": drone_electric,
    "drone_dive": drone_dive,
    "drone_launch": drone_launch,
    "engine_failure": engine_failure,
    "explosion_near": explosion_near,
    "explosion_distant": explosion_distant,
    "window_shatter": window_shatter,
    "debris_fall": debris_fall,
    "turret_fire_35": lambda: gun_shot(0.8, 0.5),
    "turret_fire_30": lambda: gun_shot(1.15, 0.4),
    "turret_traverse": turret_traverse,
    "turret_reload": turret_reload,
    "radar_lock": radar_lock,
    "remote_beep": lambda: beep(1180.0, 0.16),
    "remote_error": error_buzz,
    "map_mark": lambda: beep(1860.0, 0.10),
    "silence": silence,
}

CATEGORIES = {
    "siren_alert": "block",
    "siren_all_clear": "block",
    "alarm_button_click": "block",
    "window_shatter": "block",
    "explosion_near": "block",
    "explosion_distant": "block",
    "debris_fall": "block",
    "remote_beep": "player",
    "remote_error": "player",
    "map_mark": "player",
    "silence": "block",
}


def build() -> None:
    print("Synthesising sounds:")
    entries = {}
    for name, generator in GENERATORS.items():
        save(name, generator())
        entries[name] = {
            "category": CATEGORIES.get(name, "neutral"),
            "sounds": [{"name": f"airsystem:{name}", "stream": False}],
        }

    path = ROOT / "src/main/resources/assets/airsystem/sounds.json"
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(entries, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


if __name__ == "__main__":
    build()
