#!/usr/bin/env python3
"""Fit an exact tempo and beat grid to a track, then print a bar-level energy map.

Usage: python tools/analyze_beats.py "assets/Audio/Music/Austine Phase 2.mp3" [min_bpm] [max_bpm] [start end]

Uses a comb filter over a 200 fps onset envelope, which resolves tempo far better than
autocorrelation at 50 fps. Requires requirements-viz.txt.
"""
import shutil
import subprocess
import sys

import numpy as np

SR = 22050
ENV_FPS = 200
HOP = SR // ENV_FPS
FFT = 1024


def find_ffmpeg():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    import imageio_ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()


def decode(path):
    out = subprocess.run(
        [find_ffmpeg(), "-v", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
        check=True,
        capture_output=True,
    )
    return np.frombuffer(out.stdout, dtype=np.float32)


def envelopes(samples):
    n = len(samples) // HOP
    pad = np.concatenate((np.zeros(FFT // 2, np.float32), samples, np.zeros(FFT, np.float32)))
    idx = np.arange(n)[:, None] * HOP + np.arange(FFT)[None, :]
    seg = pad[idx]
    win = np.hanning(FFT).astype(np.float32)
    spec = np.abs(np.fft.rfft(seg * win, axis=1))
    rms = np.sqrt((seg ** 2).mean(axis=1))
    freqs = np.fft.rfftfreq(FFT, 1 / SR)
    log = np.log1p(spec * 1e-2)
    flux = np.maximum(0, np.diff(log, axis=0, prepend=log[:1])).sum(axis=1)
    env = np.clip(flux - np.percentile(flux, 20), 0, None)
    env /= np.percentile(env, 99) + 1e-9
    return env, rms, spec, freqs


def comb(env, bpm, offset, sub=4, t0=0.0, t1=None):
    """Mean onset strength on the beat grid, from t0 to t1 (seconds)."""
    period = 60.0 / bpm
    end = len(env) / ENV_FPS if t1 is None else min(t1, len(env) / ENV_FPS)
    times = np.arange(offset, end, period / sub)
    times = times[times >= t0]
    if len(times) < 2:
        return -1e9
    i = np.clip((times * ENV_FPS).astype(int), 0, len(env) - 1)
    w = np.tile(np.array([1.6, 1.0, 1.0, 1.0]), len(times) // sub + 1)[: len(times)]
    return float((env[i] * w).mean())


def fit(env, lo, hi, t0=0.0, t1=None, step=0.25):
    best = (-1e9, 0.0, 0.0)
    for bpm in np.arange(lo, hi, step):
        period = 60.0 / bpm
        for offset in np.arange(0, period, 0.005):
            s = comb(env, bpm, offset, t0=t0, t1=t1)
            if s > best[0]:
                best = (s, bpm, offset)
    return best


def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    path = sys.argv[1]
    lo = float(sys.argv[2]) if len(sys.argv) > 2 else 70.0
    hi = float(sys.argv[3]) if len(sys.argv) > 3 else 200.0
    t0 = float(sys.argv[4]) if len(sys.argv) > 4 else 0.0
    t1 = float(sys.argv[5]) if len(sys.argv) > 5 else None

    samples = decode(path)
    dur = len(samples) / SR
    env, rms, spec, freqs = envelopes(samples)
    peak = np.percentile(rms, 99.5) + 1e-9
    print(f"duration {dur:.2f}s, envelope {len(env)} frames @ {ENV_FPS}fps")

    score, bpm, offset = fit(env, lo, hi, t0, t1)
    print(f"best fit         : {bpm:.2f} BPM, offset {offset:.3f}s, score {score:.4f}")
    for mult, name in ((2.0, "double"), (0.5, "half"), (1.5, "3/2"), (2.0 / 3.0, "2/3")):
        alt = bpm * mult
        if lo <= alt <= hi:
            s = max(comb(env, alt, o, t0=t0, t1=t1) for o in np.arange(0, 60.0 / alt, 0.005))
            print(f"  {name:6s}       : {alt:.2f} BPM score {s:.4f}")

    print("\ntempo in 16s windows (is it constant?):")
    for start in np.arange(0, dur - 16, 8):
        s, b, o = fit(env, lo, hi, start, start + 16, step=0.25)
        print(f"  {start:6.0f}-{start + 16:6.0f}s : {b:6.2f} BPM off {o:.3f} score {s:.3f}")

    beat = 60.0 / bpm
    nbeats = int((dur - offset) / beat)
    print(f"\nbeat = {beat * 1000:.1f}ms, bar = {beat * 4 * 1000:.1f}ms, {nbeats} beats")

    def lvl(t):
        s = int(t * ENV_FPS)
        if s < 0 or s >= len(rms):
            return -1
        v = rms[max(0, s - 8):s + 8].mean() / peak
        return min(9, int(v * 10))

    print("\nbar map (4 beats per bar, '|' = section-ish jump, level 0-9, '.' = quiet):")
    prev = None
    for bar in range(0, nbeats // 4 + 1):
        t = offset + bar * 4 * beat
        row = ""
        for b in range(4):
            v = lvl(t + b * beat)
            row += "." if v <= 1 else (" " if v < 0 else str(v))
        mark = " "
        if prev is not None and abs(sum(lvl(t + b * beat) for b in range(4)) - prev) >= 7:
            mark = "|"
        prev = sum(lvl(t + b * beat) for b in range(4))
        print(f"  bar {bar:3d} {mark} {t:7.2f}s |{row}|")

    print("\nstrongest beats (candidate accents / drops):")
    vals = []
    for i in range(nbeats):
        t = offset + i * beat
        s = int(t * ENV_FPS)
        if 0 <= s < len(rms):
            vals.append((rms[max(0, s - 8):s + 8].mean() / peak, t, i))
    vals.sort(reverse=True)
    for v, t, i in vals[:32]:
        print(f"  {t:7.2f}s  beat {i:4d}  bar {i // 4:3d}.{i % 4}  level {v:.4f}")


if __name__ == "__main__":
    main()
