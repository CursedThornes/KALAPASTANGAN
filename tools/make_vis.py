#!/usr/bin/env python3
"""Bake the 22-channel, 60 FPS battle visualizer.

Usage: python tools/make_vis.py input.mp3 output.vis
Requires the packages in requirements-viz.txt.
"""
import shutil
import subprocess
import sys

import numpy as np

SAMPLE_RATE = 44100
FPS = 60
HOP = SAMPLE_RATE // FPS
FFT_SIZE = 2048
BANDS = 20


def find_ffmpeg():
    executable = shutil.which("ffmpeg")
    if executable:
        return executable
    try:
        import imageio_ffmpeg
    except ImportError as error:
        raise RuntimeError(
            "FFmpeg was not found. Install tools/requirements-viz.txt or add FFmpeg to PATH."
        ) from error
    return imageio_ffmpeg.get_ffmpeg_exe()


def decode(path):
    result = subprocess.run(
        [
            find_ffmpeg(), "-v", "error", "-i", path, "-ac", "1",
            "-ar", str(SAMPLE_RATE), "-f", "f32le", "-"
        ],
        check=True,
        capture_output=True,
    )
    return np.frombuffer(result.stdout, dtype=np.float32)


def flux(spectrum, frequencies, low, high):
    mask = (frequencies >= low) & (frequencies < high)
    energy = np.log1p(spectrum[:, mask].sum(axis=1) * 1e-2)
    delta = np.maximum(0, np.diff(energy, prepend=energy[:1]))
    return np.clip(delta / (np.percentile(delta, 99) + 1e-9), 0, 1)


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    source, destination = sys.argv[1:]
    samples = decode(source)
    frames = (len(samples) + HOP - 1) // HOP
    if frames == 0:
        raise RuntimeError("The input audio contains no samples.")

    padded = np.pad(samples, (FFT_SIZE // 2, FFT_SIZE))
    indices = np.arange(frames)[:, None] * HOP + np.arange(FFT_SIZE)[None, :]
    segments = padded[indices]
    del indices, padded
    window = np.hanning(FFT_SIZE).astype(np.float32)
    spectrum = np.abs(np.fft.rfft(segments * window, axis=1)) ** 2
    del segments
    frequencies = np.fft.rfftfreq(FFT_SIZE, 1 / SAMPLE_RATE)

    edges = np.geomspace(45, 14000, BANDS + 1)
    band_power = np.stack(
        [
            spectrum[:, (frequencies >= edges[i]) & (frequencies < edges[i + 1])].sum(axis=1)
            for i in range(BANDS)
        ]
    )
    decibels = 10 * np.log10(band_power + 1e-9)
    low = np.percentile(decibels, 8, axis=1, keepdims=True)
    high = np.percentile(decibels, 99.5, axis=1, keepdims=True)
    levels = np.clip((decibels - low) / (high - low + 1e-6), 0, 1) ** 1.35
    data = np.vstack(
        [levels, flux(spectrum, frequencies, 30, 9000)[None, :],
         flux(spectrum, frequencies, 35, 130)[None, :]]
    ).T

    with open(destination, "wb") as output:
        output.write(bytes([BANDS + 2, FPS]))
        output.write((data * 255).astype(np.uint8).tobytes())

    print(f"{frames} frames, {frames / FPS:.2f}s -> {destination}")


if __name__ == "__main__":
    main()
