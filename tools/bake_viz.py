#!/usr/bin/env python3
"""Bake spectrum and beat data for BeatViz.java.

Usage: python tools/bake_viz.py assets/Audio/Music/MenuOST.mp3 assets/Audio/Music/MenuOST.viz
Requires the packages listed in requirements-viz.txt.
"""
import shutil
import struct
import subprocess
import sys

import numpy as np

SAMPLE_RATE = 22050
FPS = 50
HOP = SAMPLE_RATE // FPS
FFT_SIZE = 2048
BANDS = 32
FREQ_LOW, FREQ_HIGH = 40.0, 10000.0


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


def smooth(values, width):
    kernel = np.ones(max(1, width), dtype=np.float32)
    return np.convolve(values, kernel / kernel.sum(), mode="same")


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    source, destination = sys.argv[1:]
    samples = decode(source)
    frame_count = len(samples) // HOP
    if frame_count == 0:
        raise RuntimeError(f"No audio frames decoded from {source}")

    padded = np.concatenate((np.zeros(FFT_SIZE // 2, np.float32), samples, np.zeros(FFT_SIZE, np.float32)))
    indices = np.arange(frame_count)[:, None] * HOP + np.arange(FFT_SIZE)[None, :]
    segments = padded[indices]
    rms = np.sqrt((segments ** 2).mean(axis=1))
    spectrum = np.abs(
        np.fft.rfft(segments * np.hanning(FFT_SIZE).astype(np.float32), axis=1)
    )
    del segments, indices, padded
    frequencies = np.fft.rfftfreq(FFT_SIZE, 1 / SAMPLE_RATE)

    edges = np.logspace(np.log10(FREQ_LOW), np.log10(FREQ_HIGH), BANDS + 1)
    bands = np.zeros((frame_count, BANDS), np.float32)
    for band in range(BANDS):
        mask = (frequencies >= edges[band]) & (frequencies < edges[band + 1])
        if not mask.any():
            mask[np.argmin(np.abs(frequencies - (edges[band] + edges[band + 1]) / 2))] = True
        bands[:, band] = spectrum[:, mask].mean(axis=1)

    decibels = 20 * np.log10(bands + 1e-6)
    low = np.percentile(decibels, 10, axis=0)
    high = np.percentile(decibels, 99.5, axis=0)
    levels = np.clip((decibels - low) / (high - low + 1e-6), 0, 1) ** 1.4
    bass = levels[:, :6].mean(axis=1)

    smooth_rms = smooth(rms, 25)
    energy_top = np.percentile(smooth_rms, 90)
    energy = np.clip((smooth_rms - 0.05) / (energy_top - 0.05 + 1e-6), 0, 1) ** 1.3

    bass_mask = (frequencies >= 30) & (frequencies <= 180)
    log_bass = np.log10(spectrum[:, bass_mask] + 1e-4)
    flux = np.maximum(0, np.diff(log_bass, axis=0, prepend=log_bass[:1])).sum(axis=1)
    threshold = smooth(flux, 50) * 1.6 + 0.3 * flux.std()
    peak_radius = 5
    padded_flux = np.pad(flux, peak_radius)
    windows = np.lib.stride_tricks.sliding_window_view(padded_flux, 2 * peak_radius + 1)
    peaks = (flux >= windows.max(axis=1)) & (flux > threshold)
    peak_scale = np.percentile(flux[peaks], 90) if peaks.any() else 1.0
    kicks = np.zeros(frame_count, np.float32)
    kicks[peaks] = np.clip(flux[peaks] / peak_scale, 0.4, 1.0)

    header = b"VIZ1" + struct.pack(">iii", FPS, frame_count, BANDS)
    frames = np.column_stack((energy, kicks, bass, levels))
    with open(destination, "wb") as output:
        output.write(header)
        output.write(np.clip(frames * 255, 0, 255).astype(np.uint8).tobytes())

    duration = frame_count / FPS
    print(f"Baked {frame_count} frames ({duration:.1f}s) to {destination}")
    print(f"Detected {int(peaks.sum())} bass hits")


if __name__ == "__main__":
    main()
