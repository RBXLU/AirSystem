"""Sound synthesis helpers: scipy filters, synthetic reverb and loop utilities."""
from __future__ import annotations

import numpy as np
from scipy import signal as sps

SR = 44100


def t(duration: float) -> np.ndarray:
    return np.linspace(0.0, duration, int(SR * duration), endpoint=False)


def noise(duration: float, rng: np.random.Generator) -> np.ndarray:
    return rng.normal(0.0, 1.0, int(SR * duration))


def lowpass(x: np.ndarray, cutoff: float, order: int = 4) -> np.ndarray:
    sos = sps.butter(order, min(cutoff, SR * 0.45), btype="low", fs=SR, output="sos")
    return sps.sosfilt(sos, x)


def highpass(x: np.ndarray, cutoff: float, order: int = 4) -> np.ndarray:
    sos = sps.butter(order, max(cutoff, 10.0), btype="high", fs=SR, output="sos")
    return sps.sosfilt(sos, x)


def bandpass(x: np.ndarray, low: float, high: float, order: int = 4) -> np.ndarray:
    sos = sps.butter(order, [max(low, 10.0), min(high, SR * 0.45)], btype="band",
                     fs=SR, output="sos")
    return sps.sosfilt(sos, x)


def resonator(x: np.ndarray, frequency: float, q: float = 12.0) -> np.ndarray:
    """Resonator, for body and impact colour."""
    width = frequency / q
    return bandpass(x, max(20.0, frequency - width), frequency + width, order=2)


def decay(duration: float, rate: float, power: float = 1.0) -> np.ndarray:
    return np.exp(-t(duration) * rate) ** power


def attack_decay(duration: float, attack: float, rate: float) -> np.ndarray:
    time = t(duration)
    rise = np.clip(time / max(attack, 1e-4), 0.0, 1.0)
    return rise * np.exp(-time * rate)


def fade_edges(x: np.ndarray, fade: float = 0.01) -> np.ndarray:
    n = max(1, int(fade * SR))
    out = x.copy()
    out[:n] *= np.linspace(0.0, 1.0, n)
    out[-n:] *= np.linspace(1.0, 0.0, n)
    return out


def loopable(x: np.ndarray, crossfade: float = 0.12) -> np.ndarray:
    """Crossfades the tail into the head so the sample loops without a click."""
    n = int(crossfade * SR)
    if n <= 0 or n * 2 >= len(x):
        return x
    head, tail = x[:n], x[-n:]
    ramp = np.linspace(0.0, 1.0, n)
    blended = tail * (1.0 - ramp) + head * ramp
    out = x[n:-n]
    return np.concatenate([blended, out])


def impulse_response(duration: float, rng: np.random.Generator, cutoff: float = 4000.0,
                     rate: float = 3.0, predelay: float = 0.02) -> np.ndarray:
    """Synthetic impulse response: decaying noise plus early reflections."""
    tail = lowpass(noise(duration, rng), cutoff) * decay(duration, rate)
    n_pre = int(predelay * SR)
    tail[:n_pre] *= np.linspace(0.0, 1.0, n_pre)

    for delay, gain in ((0.013, 0.5), (0.029, 0.36), (0.047, 0.26), (0.071, 0.18)):
        index = int(delay * SR)
        if index < len(tail):
            tail[index] += gain
    return tail / (np.max(np.abs(tail)) + 1e-9)


def reverb(x: np.ndarray, ir: np.ndarray, mix: float = 0.3) -> np.ndarray:
    wet = sps.fftconvolve(x, ir)[: len(x)]
    wet /= np.max(np.abs(wet)) + 1e-9
    dry_peak = np.max(np.abs(x)) + 1e-9
    return x * (1.0 - mix) + wet * dry_peak * mix


def phase(frequency: np.ndarray) -> np.ndarray:
    return 2.0 * np.pi * np.cumsum(frequency) / SR


def harmonic_stack(freq: np.ndarray, count: int, tilt: float = 1.0,
                   odd_boost: float = 1.0) -> np.ndarray:
    """Harmonic comb: how machinery sounds, as opposed to a pure tone."""
    ph = phase(freq)
    out = np.zeros_like(ph)
    for n in range(1, count + 1):
        amplitude = (1.0 / n) ** tilt
        if n % 2 == 1:
            amplitude *= odd_boost
        out += amplitude * np.sin(ph * n)
    return out / (np.max(np.abs(out)) + 1e-9)


def pulse_train(duration: float, freq: float, width: float = 0.06) -> np.ndarray:
    """Firing pulses of a piston engine."""
    time = t(duration)
    cycle = (time * freq) % 1.0
    return (cycle < width).astype(float)


def limiter(x: np.ndarray, ceiling: float = 0.92) -> np.ndarray:
    peak = np.max(np.abs(x)) + 1e-9
    if peak <= ceiling:
        return x
    return np.tanh(x / peak * 1.6) * ceiling / np.tanh(1.6)


def normalize(x: np.ndarray, peak: float = 0.9) -> np.ndarray:
    maximum = np.max(np.abs(x))
    if maximum < 1e-9:
        return x
    return x / maximum * peak
