#!/usr/bin/env python3
"""Create an original, offline sound-effect library from the sound_fx brief."""

from __future__ import annotations

import json
import math
import random
import shutil
import subprocess
import wave
from pathlib import Path

SAMPLE_RATE = 48_000
OUTPUT_DIR = Path("/Users/theranosis_dx/assets/sounds")
TEMP_DIR = OUTPUT_DIR / ".intermediate_wav"
FFMPEG = shutil.which("ffmpeg") or "/usr/local/opt/ffmpeg-full/bin/ffmpeg"


def clamp(value: float) -> float:
    return max(-0.98, min(0.98, value))


def write_wav(name: str, seconds: float, sample_at) -> Path:
    frames = int(seconds * SAMPLE_RATE)
    path = TEMP_DIR / f"{name}.wav"
    with wave.open(str(path), "wb") as output:
        output.setnchannels(2)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        chunk = bytearray()
        for index in range(frames):
            left, right = sample_at(index / SAMPLE_RATE, index, frames)
            chunk.extend(int(clamp(left) * 32767).to_bytes(2, "little", signed=True))
            chunk.extend(int(clamp(right) * 32767).to_bytes(2, "little", signed=True))
            if len(chunk) >= 65_536:
                output.writeframesraw(chunk)
                chunk.clear()
        if chunk:
            output.writeframesraw(chunk)
    return path


def encode_aac(wav_path: Path, destination: Path) -> None:
    subprocess.run(
        [FFMPEG, "-y", "-hide_banner", "-loglevel", "error", "-i", str(wav_path), "-c:a", "aac", "-b:a", "128k", str(destination)],
        check=True,
    )


def make_brown(seed: int):
    rng = random.Random(seed)
    value = 0.0

    def sample() -> float:
        nonlocal value
        value = (value + 0.025 * rng.uniform(-1, 1)) / 1.025
        return value * 3.2

    return sample


def make_pink(seed: int):
    rng = random.Random(seed)
    filters = [0.0] * 6
    b6 = 0.0

    def sample() -> float:
        nonlocal b6
        white = rng.uniform(-1, 1)
        filters[0] = 0.99886 * filters[0] + white * 0.0555179
        filters[1] = 0.99332 * filters[1] + white * 0.0750759
        filters[2] = 0.96900 * filters[2] + white * 0.1538520
        filters[3] = 0.86650 * filters[3] + white * 0.3104856
        filters[4] = 0.55000 * filters[4] + white * 0.5329522
        filters[5] = -0.7616 * filters[5] - white * 0.0168980
        b6 = white * 0.115926
        return (sum(filters) + b6 + white * 0.5362) * 0.11

    return sample


def black_noise():
    brown, low = make_brown(11), 0.0

    def at(_, __, ___):
        nonlocal low
        low += 0.008 * (brown() - low)
        return low * 0.20, low * 0.20

    return at


def waterfall():
    brown, pink = make_brown(21), make_pink(22)
    roar_low = splash_low = splash_high = 0.0

    def at(_, __, ___):
        nonlocal roar_low, splash_low, splash_high
        roar_low += 0.03 * (brown() - roar_low)
        splash_low += 0.09 * (pink() - splash_low)
        splash_high += 0.015 * (splash_low - splash_high)
        value = roar_low * 0.18 + (splash_low - splash_high) * 0.35
        return value * 0.64, value * 0.64

    return at


def rainfall():
    pink = make_pink(31)
    low = high = 0.0

    def at(time, _, __):
        nonlocal low, high
        low += 0.075 * (pink() - low)
        high += 0.012 * (low - high)
        gust = 0.60 + 0.25 * (0.5 + 0.5 * math.sin(2 * math.pi * 0.05 * time))
        value = (low - high) * gust * 0.72
        return value * 0.7, value * 0.7

    return at


def ocean_waves():
    brown = make_brown(41)
    low = 0.0

    def at(time, _, __):
        nonlocal low
        low += 0.012 * (brown() - low)
        swell = 0.08 + 0.36 * (0.5 + 0.5 * math.sin(2 * math.pi * 0.08 * time)) ** 2
        value = low * swell
        return value * 0.60, value * 0.60

    return at


def binaural(carrier: float, beat: float):
    def at(time, _, __):
        return 0.16 * math.sin(2 * math.pi * (carrier - beat / 2) * time), 0.16 * math.sin(2 * math.pi * (carrier + beat / 2) * time)

    return at


def isochronic():
    def at(time, _, __):
        pulse = 0.10 + 0.90 * (0.5 + 0.5 * math.sin(2 * math.pi * 14 * time)) ** 4
        value = 0.17 * pulse * math.sin(2 * math.pi * 250 * time)
        return value, value

    return at


def somatic_purr():
    def at(time, _, __):
        saw = 2 * ((25 * time) % 1) - 1
        breath = 0.34 + 0.28 * (0.5 + 0.5 * math.sin(2 * math.pi * 0.6 * time))
        value = saw * breath * 0.22
        return value, value

    return at


def ear_brushing():
    pink = make_pink(61)
    low = 0.0

    def at(time, _, __):
        nonlocal low
        low += 0.10 * (pink() - low)
        brushing = low * (0.55 + 0.45 * math.sin(2 * math.pi * 0.5 * time)) * 0.52
        pan = 0.5 + 0.5 * math.sin(2 * math.pi * 0.2 * time)
        return brushing * math.sqrt(1 - pan), brushing * math.sqrt(pan)

    return at


def stochastic_crinkle():
    rng = random.Random(71)
    burst = 0.0

    def at(_, index, __):
        nonlocal burst
        if index % 2_400 == 0 and rng.random() > 0.42:
            burst = rng.uniform(0.18, 0.62)
        burst *= 0.9993
        value = rng.uniform(-1, 1) * burst * 0.30
        return value * (0.85 + rng.random() * 0.15), value * (0.85 + rng.random() * 0.15)

    return at


def meditation_bell():
    partials = [(1.0, 4.0, 0.20), (2.76, 2.5, 0.12), (5.40, 1.5, 0.08), (8.90, 0.8, 0.04)]

    def at(time, _, __):
        attack = min(1, time / 0.02)
        value = sum(amp * math.exp(-time / decay) * math.sin(2 * math.pi * 432 * ratio * time) for ratio, decay, amp in partials) * attack
        return value * 0.75, value * 0.75

    return at


def vinyl_crackle():
    rng = random.Random(81)
    pop = 0.0

    def at(time, index, __):
        nonlocal pop
        if index % 4_800 == 0 and rng.random() > 0.45:
            pop = rng.uniform(0.12, 0.36)
        pop *= 0.998
        hum = 0.015 * math.sin(2 * math.pi * 60 * time)
        dust = rng.uniform(-1, 1) * pop
        value = hum + dust
        return value * 0.8, value * 0.8

    return at


SOUNDS = [
    ("black_noise", "Black Noise", "loop", 30, black_noise, "Ultra-low brown-noise bed, low-passed for a near-silent backdrop."),
    ("waterfall", "Waterfall", "loop", 30, waterfall, "Low roar and soft filtered mist."),
    ("rainfall", "Dynamic Rainfall", "loop", 30, rainfall, "Pink-noise rain with a slow gust envelope."),
    ("ocean_waves", "Ocean Waves", "loop", 30, ocean_waves, "Brown-noise swell at a slow 12-second rhythm."),
    ("binaural_peak_focus", "Binaural: Peak Focus", "loop", 30, lambda: binaural(315, 40), "315 Hz carrier with 40 Hz left/right difference; headphones only."),
    ("binaural_analytical", "Binaural: Analytical", "loop", 30, lambda: binaural(250, 15), "250 Hz carrier with 15 Hz left/right difference; headphones only."),
    ("binaural_flow_state", "Binaural: Flow State", "loop", 30, lambda: binaural(200, 10), "200 Hz carrier with 10 Hz left/right difference; headphones only."),
    ("binaural_short_break", "Binaural: Short Break", "loop", 30, lambda: binaural(160, 6), "160 Hz carrier with 6 Hz left/right difference; headphones only."),
    ("binaural_long_break", "Binaural: Long Break", "loop", 30, lambda: binaural(100, 2), "100 Hz carrier with 2 Hz left/right difference; headphones only."),
    ("isochronic_analytical", "Isochronic Analytical", "loop", 30, isochronic, "250 Hz tone pulsed at 14 Hz for speaker playback."),
    ("somatic_purr", "Somatic Purr", "loop", 30, somatic_purr, "Soft 25 Hz sawtooth vibration with a breathing envelope."),
    ("ear_brushing", "Ear-to-Ear Brushing", "loop", 30, ear_brushing, "Filtered pink noise slowly moving between channels."),
    ("stochastic_crinkle", "Stochastic Crinkle", "loop", 30, stochastic_crinkle, "Sparse, low-volume procedural crinkle texture."),
    ("meditation_bell", "Meditation Bell", "one-shot", 6, meditation_bell, "Additive bell strike tuned from a 432 Hz fundamental."),
    ("vinyl_crackle", "Lo-Fi Vinyl Crackle", "loop", 30, vinyl_crackle, "Low hum with occasional soft dust pops."),
]


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    TEMP_DIR.mkdir(exist_ok=True)
    manifest = {
        "format": "AAC-LC / M4A, 48 kHz stereo, 128 kbps",
        "source": "Original procedural synthesis based on sound_fx.md",
        "safety_note": "Brainwave-named assets are optional ambient audio, not medical treatment or a clinical claim.",
        "sounds": [],
    }
    for slug, title, kind, seconds, generator, description in SOUNDS:
        wav_path = write_wav(slug, seconds, generator())
        destination = OUTPUT_DIR / f"{slug}.m4a"
        encode_aac(wav_path, destination)
        wav_path.unlink()
        manifest["sounds"].append({"file": destination.name, "title": title, "type": kind, "duration_seconds": seconds, "description": description})
        print(f"created {destination}")
    (OUTPUT_DIR / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    TEMP_DIR.rmdir()


if __name__ == "__main__":
    main()
