#!/usr/bin/env python3
"""Analyze a music track so a battle phase can be charted against it.

Usage: python tools/analyze_song.py "assets/Audio/Music/Austine Phase 2.mp3"

Prints duration, tempo estimate(s) over time, beat phase, an energy table, the loudest
transients (kicks/drops) and likely section boundaries. Requires requirements-viz.txt.
"""
import shutil
import subprocess
import sys

import numpy as np

SR = 22050
FPS = 50
HOP = SR // FPS
FFT = 2048


def find_ffmpeg():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg
    except ImportError as error:
        raise RuntimeError("Install tools/requirements-viz.txt") from error
    return imageio_ffmpeg.get_ffmpeg_exe()


def decode(path):
    out = subprocess.run(
        [find_ffmpeg(), "-v", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
        check=True,
        capture_output=True,
    )
    return np.frombuffer(out.stdout, dtype=np.float32)


def frames(samples):
    n = len(samples) // HOP
    pad = np.concatenate((np.zeros(FFT // 2, np.float32), samples, np.zeros(FFT, np.float32)))
    idx = np.arange(n)[:, None] * HOP + np.arange(FFT)[None, :]
    seg = pad[idx]
    win = np.hanning(FFT).astype(np.float32)
    spec = np.abs(np.fft.rfft(seg * win, axis=1))
    rms = np.sqrt((seg ** 2).mean(axis=1))
    freqs = np.fft.rfftfreq(FFT, 1 / SR)
    del seg, idx, pad
    return spec, rms, freqs


def onset_env(spec):
    log = np.log1p(spec * 1e-2)
    flux = np.maximum(0, np.diff(log, axis=0, prepend=log[:1])).sum(axis=1)
    env = flux - flux.min()
    return env / (np.percentile(env, 99) + 1e-9)


def tempo(env, lo=60.0, hi=200.0):
    """Autocorrelation tempo estimate. Returns (bpm, confidence)."""
    e = env - env.mean()
    ac = np.correlate(e, e, mode="full")[len(e) - 1:]
    ac /= ac[0] + 1e-9
    min_lag = int(round(60.0 / hi * FPS))
    max_lag = int(round(60.0 / lo * FPS))
    window = ac[min_lag:max_lag]
    best = int(np.argmax(window)) + min_lag
    return 60.0 * FPS / best, float(window.max())


def sweep_tempo(env, secs=8.0):
    """Tempo estimate in overlapping windows, to spot tempo changes."""
    w = int(secs * FPS)
    out = []
    for start in range(0, len(env) - w, w // 2):
        bpm, conf = tempo(env[start:start + w])
        out.append((start / FPS, bpm, conf))
    return out


def beat_phase(env, bpm):
    """Best-fitting beat offset in seconds for a known tempo."""
    period = 60.0 / bpm
    best_t, best_score = 0.0, -1e9
    for t in np.arange(0, period, 0.004):
        idx = ((np.arange(len(env)) / FPS - t) / period)
        idx = np.round(idx).astype(int)
        ok = (idx >= 0) & (idx < len(env))
        if not ok.any():
            continue
        score = env[idx[ok]].mean()
        if score > best_score:
            best_t, best_score = t, score
    return float(best_t), float(best_score)


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    path = sys.argv[1]
    samples = decode(path)
    dur = len(samples) / SR
    spec, rms, freqs = frames(samples)
    env = onset_env(spec)
    print(f"file           : {path}")
    print(f"duration       : {dur:.2f}s ({dur / 60:.2f} min)")

    bpm, conf = tempo(env)
    print(f"tempo          : {bpm:.2f} BPM (confidence {conf:.3f})")
    for cand in (bpm * 2, bpm / 2):
        if 60 <= cand <= 200:
            b2, c2 = tempo(env, cand * 0.97, cand * 1.03)
            print(f"  nearby       : {b2:.2f} BPM conf {c2:.3f}")
    off, score = beat_phase(env, bpm)
    print(f"beat offset    : {off:.3f}s (onset alignment {score:.3f})")

    print("\ntempo over time (8s windows):")
    for t, b, c in sweep_tempo(env):
        bar = "#" * int(min(40, max(0, (b - 60) / 4)))
        print(f"  {t:7.1f}s  {b:6.2f} BPM conf {c:.2f} {bar}")

    # energy: per-second mean RMS, normalised
    per_sec = rms[: len(rms) // FPS * FPS].reshape(-1, FPS).mean(axis=1)
    top = np.percentile(per_sec, 99) + 1e-9
    print("\nenergy per second (0-9):")
    for i in range(0, len(per_sec), 10):
        row = per_sec[i:i + 10]
        bars = "".join(str(min(9, int(v / top * 10))) for v in row)
        print(f"  {i:5.0f}s  {bars}")

    # section boundaries: places where a 1s rolling level moves a lot
    smooth = np.convolve(per_sec, np.ones(3) / 3, mode="same")
    jump = np.abs(np.diff(smooth))
    order = np.argsort(jump)[::-1][:24]
    print("\nbiggest level changes (candidate section boundaries):")
    for s in sorted(order):
        print(f"  {s + 1:6.1f}s  delta {jump[s]:+.4f}  (level {smooth[s]:.4f} -> {smooth[s + 1]:.4f})")

    # strong low-band hits (kicks / drop impacts)
    low = (freqs >= 30) & (freqs <= 150)
    lowmag = spec[:, low].mean(axis=1)
    lowmag = np.convolve(lowmag, np.ones(3) / 3, mode="same")
    d = np.maximum(0, np.diff(lowmag, prepend=lowmag[:1]))
    peaks = np.where((d > np.percentile(d, 99.2)) & (d > 0))[0]
    peaks = [p for p in peaks if p < len(d)]
    print(f"\n{len(peaks)} strong low hits; first 60 times (s):")
    row = []
    for p in peaks[:60]:
        row.append(f"{p / FPS:.2f}")
    for i in range(0, len(row), 10):
        print("  " + "  ".join(row[i:i + 10]))

    # quiet / silent stretches (intros, fake-outs, breaks)
    quiet = per_sec < top * 0.12
    runs, start = [], None
    for i, q in enumerate(quiet):
        if q and start is None:
            start = i
        elif not q and start is not None:
            if i - start >= 2:
                runs.append((start, i))
            start = None
    if start is not None and len(quiet) - start >= 2:
        runs.append((start, len(quiet)))
    print("\nquiet stretches (>=2s, candidate intro/break/fake-out):")
    for a, b in runs:
        print(f"  {a:6.1f}s - {b:6.1f}s  ({b - a}s)")


if __name__ == "__main__":
    main()
