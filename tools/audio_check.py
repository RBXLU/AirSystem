"""Objective checks on the synthesised sounds: level, crest factor, spectral
centroid, attack time and the discontinuity at the loop seam."""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOUNDS = ROOT / "src/main/resources/assets/airsystem/sounds"


def spectrogram(signal: np.ndarray, sr: int, width: int = 640, height: int = 260) -> Image.Image:
    window = 2048
    hop = max(1, (len(signal) - window) // width)
    columns = []
    hann = np.hanning(window)
    for i in range(width):
        start = i * hop
        chunk = signal[start:start + window]
        if len(chunk) < window:
            chunk = np.pad(chunk, (0, window - len(chunk)))
        spectrum = np.abs(np.fft.rfft(chunk * hann))
        columns.append(spectrum)

    data = np.array(columns).T
    freqs = np.fft.rfftfreq(window, 1.0 / sr)

    edges = np.logspace(np.log10(30.0), np.log10(sr / 2), height + 1)
    binned = np.zeros((height, width))
    for row in range(height):
        mask = (freqs >= edges[row]) & (freqs < edges[row + 1])
        if mask.any():
            binned[row] = data[mask].mean(axis=0)

    db = 20.0 * np.log10(binned + 1e-8)
    db = np.clip((db - db.max() + 70.0) / 70.0, 0.0, 1.0)

    image = Image.new("RGB", (width, height))
    pixels = image.load()
    for x in range(width):
        for y in range(height):
            value = db[height - 1 - y, x]
            pixels[x, y] = (int(255 * value ** 1.4),
                            int(200 * value ** 1.8),
                            int(90 + 120 * value ** 2.4))
    return image


def metrics(signal: np.ndarray, sr: int) -> dict:
    peak = float(np.max(np.abs(signal)))
    rms = float(np.sqrt(np.mean(signal ** 2)))
    spectrum = np.abs(np.fft.rfft(signal))
    freqs = np.fft.rfftfreq(len(signal), 1.0 / sr)
    centroid = float((spectrum * freqs).sum() / (spectrum.sum() + 1e-9))
    seam = float(abs(signal[0] - signal[-1]))
    envelope = np.abs(signal)
    attack_index = int(np.argmax(envelope > peak * 0.9))
    return {
        "peak": peak,
        "rms": rms,
        "crest": peak / (rms + 1e-9),
        "centroid": centroid,
        "seam": seam,
        "attack_ms": attack_index / sr * 1000.0,
    }


def main(names) -> None:
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/spectro")
    out.mkdir(parents=True, exist_ok=True)

    print(f"{'sound':<20}{'peak':>6}{'RMS':>7}{'crest':>7}{'centroid':>10}{'seam':>8}{'attack ms':>10}")
    tiles = []
    for name in names:
        signal, sr = sf.read(SOUNDS / f"{name}.ogg")
        if signal.ndim > 1:
            signal = signal.mean(axis=1)
        m = metrics(signal, sr)
        print(f"{name:<20}{m['peak']:>6.2f}{m['rms']:>7.3f}{m['crest']:>7.1f}"
              f"{m['centroid']:>10.0f}{m['seam']:>8.3f}{m['attack_ms']:>10.1f}")
        tiles.append((name, spectrogram(signal, sr)))

    if tiles:
        width = max(image.width for _, image in tiles)
        height = max(image.height for _, image in tiles)
        columns = 2
        rows = (len(tiles) + columns - 1) // columns
        canvas = Image.new("RGB", (width * columns, height * rows), (12, 12, 14))
        for index, (name, image) in enumerate(tiles):
            canvas.paste(image, ((index % columns) * width, (index // columns) * height))
        canvas.save(out / "spectrograms.png")
        print(f"\nspectrograms: {out / 'spectrograms.png'}")


if __name__ == "__main__":
    main(["siren_alert", "explosion_near", "drone_piston", "drone_jet",
          "turret_fire_35", "window_shatter"])
