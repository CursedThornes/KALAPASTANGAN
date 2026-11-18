package com.java.KALAPASTANGAN.battle;

/**
 * Chart data and the pure maths behind it: where a section starts, where the menu between two
 * sections opens and closes, when a cue lands, and how far the arena can grow or travel before it
 * leaves the screen. Nothing here touches libGDX or the arena's state, so it can be reasoned about
 * (and tested) on its own.
 *
 * ALL times are seconds unless the name says otherwise; chart tables are in beats.
 */
public final class BattleChart {
    public static final int PHASE_ONE = 1, PHASE_TWO = 2, PHASE_THREE = 3;

    // ---- phase 1, 172 bpm ('LIYAB') ----------------------------------------------------------
    public static final float P1_BPM = 172f;
    public static final float P1_OFFSET = 0.051f;
    public static final float P1_BEAT = 60f / 172f;
    public static final float P1_SONG_LEN = 111.7257f;
    public static final float P1_DROP_BEAT = 34f;
    public static final float P1_DRAIN_TIME = 11.04f;
    public static final float P1_DROP_TIME = 0.051f + P1_DROP_BEAT * P1_BEAT;
    // 34-64, 80-112, 130-164, 180-228, 244-312
    public static final float[][] P1_SEQ = {
        {34f, 64f}, {80f, 112f}, {130f, 164f}, {180f, 228f}, {244f, 312f}
    };
    public static final int[] P1_DIFFICULTY = { 0, 1, 2, 3, 5 };
    public static final String[] P1_NAMES = {
        "IGNITION", "LATTICE", "STANDSTILL", "COLLAPSE", "INFERNO"
    };
    // {beat, lakas, kind}: 2 mega slam, 1 slam, 0 glitch
    public static final float[] P1_CUES = {
        34f, 1f, 2,
        39f, 0.50f, 0, 42f, 0.50f, 0, 50f, 0.60f, 0, 55f, 0.70f, 1, 58f, 0.60f, 0,
        82f, 0.55f, 0, 101f, 0.60f, 0, 103f, 0.65f, 0, 109f, 0.55f, 0, 119f, 0.70f, 1,
        125f, 0.60f, 0, 130f, 0.90f, 1, 131f, 0.50f, 0, 164f, 0.80f, 1,
        166f, 0.40f, 0, 168f, 0.45f, 0, 170f, 0.50f, 0, 171f, 0.55f, 0,
        172f, 0.60f, 0, 173f, 0.65f, 0, 174f, 0.50f, 0, 176f, 0.70f, 0, 178f, 0.80f, 1,
        180f, 1f, 2, 234f, 0.60f, 0, 244f, 0.85f, 1, 248f, 0.50f, 0,
        257.5f, 0.50f, 0, 264f, 0.70f, 1, 272.5f, 0.50f, 0, 274f, 0.75f, 1,
        282.5f, 0.50f, 0, 294.5f, 0.60f, 0, 296.5f, 0.60f, 0, 297.5f, 0.60f, 0,
        299.5f, 0.60f, 0, 302.5f, 0.65f, 0, 303.5f, 0.65f, 0, 312f, 0.55f, 1
    };
    public static final float[] P1_SECT = {
        0f, 0f, 11f, 0f, 11.85f, 0f, 11.91f, 1f, 15f, 0.60f, 22.4f, 0.65f, 33.5f, 0.72f,
        36.3f, 0.82f, 44.4f, 0.82f, 44.9f, 0.30f, 45.4f, 1f, 47.6f, 0.88f, 49.2f, 0.95f, 51.4f, 1f, 57.1f, 1f,
        57.4f, 0.40f, 62.7f, 0.55f, 62.84f, 1f, 66f, 0.95f, 108.7f, 0.95f, 109.4f, 0.10f, 111.8f, 0f
    };
    public static final float[] P1_TENS = { 11.21f, 11.91f, 44.70f, 45.40f, 57.30f, 62.84f };

    // ---- phase 2, 183.58 bpm ('DILIM') -------------------------------------------------------
    // tahimik hanggang bar 15, sabog sa bar 16 (20.94s)
    public static final float P2_BPM = 183.58f;
    public static final float P2_OFFSET = 0.020f;
    public static final float P2_BEAT = 60f / 183.58f;
    public static final float P2_SONG_LEN = 149.57f;
    public static final float P2_DRAIN_TIME = 19.63f;          // bar 15: the hole
    public static final float P2_DROP_TIME = 20.94f;           // bar 16: the body
    public static final float[][] P2_SEQ = {
        {64f, 144f}, {160f, 236f}, {252f, 300f}, {316f, 424f}
    };
    public static final int[] P2_DIFFICULTY = { 2, 3, 5, 7 };
    public static final String[] P2_NAMES = { "DESCENT", "THRALL", "VORTEX", "COLLAPSE" };
    public static final float[] P2_CUES = {
        64f, 1f, 2,
        96f, 0.55f, 0, 112f, 0.60f, 1, 128f, 0.55f, 0, 144f, 0.65f, 1,
        160f, 0.70f, 1, 176f, 0.50f, 0, 192f, 0.55f, 0, 208f, 0.50f, 0,
        224f, 0.60f, 1,
        252f, 1f, 2, 264f, 0.55f, 0, 280f, 0.60f, 1, 292f, 0.55f, 0,
        316f, 1f, 2, 324f, 0.60f, 0, 332f, 0.65f, 1, 340f, 0.60f, 0, 348f, 0.65f, 1,
        356f, 0.70f, 1, 364f, 0.70f, 1, 372f, 0.65f, 0, 380f, 0.70f, 1, 388f, 0.75f, 1,
        396f, 0.80f, 1, 404f, 0.80f, 1, 412f, 0.85f, 1, 416f, 1f, 2
    };
    // fx kada 2 bars, galing sa totoong track
    public static final float[] P2_SECT = {
        0.02f, 0.54f, 2.63f, 0.56f, 5.25f, 0.56f, 7.86f, 0.61f, 10.48f, 0.69f,
        13.09f, 0.73f, 15.71f, 0.72f, 18.32f, 0.73f, 20.94f, 0.83f, 23.55f, 0.78f,
        26.17f, 0.78f, 28.78f, 0.78f, 31.40f, 0.79f, 34.01f, 0.78f, 36.63f, 0.78f,
        39.24f, 0.78f, 41.85f, 0.70f, 44.47f, 0.70f, 47.08f, 0.71f, 49.70f, 0.72f,
        52.31f, 0.84f, 54.93f, 0.79f, 57.54f, 0.82f, 60.16f, 0.81f, 62.77f, 0.82f,
        65.39f, 0.63f, 68.00f, 0.69f, 70.62f, 0.51f, 73.23f, 0.79f, 75.85f, 0.65f,
        78.46f, 0.70f, 81.07f, 0.64f, 83.69f, 0.80f, 86.30f, 0.76f, 88.92f, 0.76f,
        91.53f, 0.75f, 94.15f, 0.79f, 96.76f, 0.78f, 99.38f, 0.77f, 101.99f, 0.79f,
        104.61f, 0.88f, 107.22f, 0.79f, 109.84f, 0.81f, 112.45f, 0.80f, 115.07f, 0.85f,
        117.68f, 0.82f, 120.29f, 0.81f, 122.91f, 0.80f, 125.52f, 0.82f, 128.14f, 0.81f,
        130.75f, 0.82f, 133.37f, 0.81f, 135.98f, 0.53f, 138.60f, 0.20f, 141.21f, 0.11f,
        143.83f, 0.17f, 146.44f, 0f
    };
    public static final float[] P2_TENS = {
        19.63f, 20.94f, 62.77f, 68.00f, 78.46f, 82.38f, 130.75f, 136.00f
    };

    // ---- phase 3, 99.75 bpm ('SIGAW') --------------------------------------------------------
    public static final float P3_BPM = 99.75f;
    public static final float P3_OFFSET = 0.375f;
    public static final float P3_BEAT = 60f / 99.75f;
    public static final float P3_SONG_LEN = 175.03f;
    public static final float P3_DRAIN_TIME = 9.60f;           // bar 4 counts in, then drops
    public static final float P3_DROP_TIME = 10.00f;
    // pader ng phase 3: {segundo, dulo, lalim}. humihinga kasabay ng track
    public static final float[] P3_WALL = {
        78f, 86f, 0.55f,   88f, 92f, 0.70f,
        144f, 152f, 0.60f, 156f, 164f, 0.72f, 166f, 171f, 0.50f
    };
    public static final float[][] P3_SEQ = {
        {16f, 64f},     // 10.00 to 38.87 MAYDAY: the broadcast, then the guitar enters
        {72f, 116f},    // 43.68 to 70.15 RUPTURE
        {124f, 172f},   // 74.96 to 103.83 STATIC: the 1:36 remix and the 1:40 shred
        {180f, 224f},   // 108.65 to 135.11 SIGNAL: builds through the 2:14 change
        {232f, 288f}    // 139.92 to 173.61 SIGAW: the last scream
    };
    public static final int[] P3_DIFFICULTY = { 2, 3, 5, 7, 8 };
    public static final String[] P3_NAMES = {
        "MAYDAY", "RUPTURE", "STATIC", "SIGNAL", "SIGAW"
    };
    // 9 mono glitch, hiwalay sa boss ang timing kasi buong screen ang tinitira
    public static final float[] P3_CUES = {
        0.008f, 0.42f, 1f, 8.005f, 0.35f, 0f, 16.002f, 0.9f, 2f, 23.998f, 0.28f, 0f,
        31.995f, 0.37f, 1f, 36.001f, 0.9f, 2f, 40.008f, 0.33f, 0f, 48.005f, 0.37f, 1f,
        56.001f, 0.33f, 0f, 63.998f, 0.9f, 2f, 71.995f, 0.5f, 0f, 80.008f, 0.61f, 1f,
        88.004f, 0.46f, 0f, 91.994f, 0.9f, 2f, 96.001f, 0.66f, 1f, 103.998f, 0.52f, 0f,
        111.994f, 0.66f, 1f, 116.001f, 0.9f, 2f, 120.008f, 0.57f, 0f, 128.004f, 0.63f, 1f,
        136.001f, 0.54f, 0f, 143.997f, 0.66f, 1f, 148.004f, 0.9f, 2f, 151.994f, 0.54f, 0f,
        160.007f, 0.63f, 1f, 168.004f, 0.5f, 0f, 171.994f, 0.9f, 2f, 176.001f, 0.55f, 1f,
        183.997f, 0.57f, 0f, 191.994f, 0.61f, 1f, 200.007f, 0.5f, 0f, 208.004f, 0.61f, 1f,
        216.0f, 0.57f, 0f, 220.007f, 0.9f, 2f, 223.997f, 0.55f, 1f, 231.994f, 0.48f, 0f,
        240.007f, 0.9f, 2f, 248.003f, 0.5f, 0f, 256.0f, 0.55f, 1f, 263.997f, 0.48f, 0f,
        271.993f, 0.9f, 2f, 280.007f, 0.5f, 0f,
    };
    public static final float[] P3_SECT = {
        0.375f, 0.5f, 5.187f, 0.5f, 9.999f, 0.5f, 14.811f, 0.406f,
        19.623f, 0.438f, 24.435f, 0.469f, 29.247f, 0.438f, 34.059f, 0.469f,
        38.871f, 0.625f, 43.683f, 0.719f, 48.495f, 0.719f, 53.307f, 0.656f,
        58.119f, 0.781f, 62.931f, 0.75f, 67.743f, 0.781f, 72.555f, 0.812f,
        77.367f, 0.75f, 82.18f, 0.781f, 86.992f, 0.781f, 91.804f, 0.781f,
        96.616f, 0.75f, 101.428f, 0.719f, 106.24f, 0.656f, 111.052f, 0.812f,
        118.27f, 0.688f, 123.082f, 0.719f, 127.894f, 0.688f, 132.706f, 0.812f,
        137.518f, 0.719f, 142.33f, 0.656f, 147.142f, 0.625f, 151.954f, 0.688f,
        156.766f, 0.625f, 161.578f, 0.719f, 166.39f, 0.688f, 171.202f, 0.656f,
        175.03f, 0f,
    };
    public static final float[] P3_TENS = {
        8.75f, 10.00f, 20.78f, 22.03f, 37.62f, 38.87f, 42.43f, 43.68f,
        54.46f, 55.71f, 68.90f, 70.15f, 88.15f, 89.40f, 92.96f, 94.21f,
        102.58f, 103.83f, 112.21f, 113.46f, 121.5f, 122.5f, 133.86f, 135.11f,
        143.45f, 144.74f, 153.11f, 154.36f, 162.73f, 163.98f, 169.95f, 171.20f
    };

    // chart ends this early so the box can close
    public static final float TAIL_BEATS = 1.5f;

    // teleport set-piece: 49.3-51.4s, apat na blast, kada 2 beats
    public static final float[] TP_BEATS = { 141f, 143f, 145f, 147f };
    // station = {x, y, tilt}
    public static final float[][] TP_ST = {
        {-1f, 0.25f, -12f}, {1f, -0.25f, 13f}, {-1f, 0.25f, -15f}, {1f, -0.25f, 17f}
    };

    // palettes: baga/kalawang/amber, dugo/apoy/dilaw, bakal/buto/isang puting ilaw
    public static final float[][] P1_PALETTE = {
        {0.48f, 0.23f, 0.08f},
        {0.82f, 0.25f, 0.045f},
        {0.98f, 0.52f, 0.09f},
        {1.00f, 0.78f, 0.24f},
        {0.56f, 0.16f, 0.035f}
    };
    public static final float[][] P2_PALETTE = {
        {0.56f, 0.045f, 0.018f},
        {0.92f, 0.13f, 0.025f},
        {1.00f, 0.36f, 0.045f},
        {1.00f, 0.78f, 0.22f},
        {0.62f, 0.075f, 0.018f}
    };
    public static final float[][] P3_PALETTE = {
        {0.17f, 0.18f, 0.21f},
        {0.44f, 0.46f, 0.52f},
        {0.74f, 0.77f, 0.84f},
        {1.00f, 1.00f, 1.00f},
        {0.29f, 0.30f, 0.36f}
    };

    private BattleChart() {
    }

    public static float[][] sequenceTable(int phase) {
        return phase == PHASE_THREE ? P3_SEQ : phase == PHASE_TWO ? P2_SEQ : P1_SEQ;
    }

    public static String[] sequenceNames(int phase) {
        return phase == PHASE_THREE ? P3_NAMES : phase == PHASE_TWO ? P2_NAMES : P1_NAMES;
    }

    public static int[] sequenceDifficulty(int phase) {
        return phase == PHASE_THREE ? P3_DIFFICULTY : phase == PHASE_TWO ? P2_DIFFICULTY : P1_DIFFICULTY;
    }

    public static float[][] palette(int phase) {
        return phase == PHASE_THREE ? P3_PALETTE : phase == PHASE_TWO ? P2_PALETTE : P1_PALETTE;
    }

    // simula ng sequence i, sa segundo
    public static float sequenceStartTime(float offset, float beat, float[][] seq, int i) {
        return offset + seq[clamp(i, 0, seq.length - 1)][0] * beat;
    }

    public static float sequenceLength(float beat, float[][] seq, int i, float tailBeats) {
        return (seq[clamp(i, 0, seq.length - 1)][1] - seq[clamp(i, 0, seq.length - 1)][0] - tailBeats) * beat;
    }

    // menu = gap ng SEQ. song-locked kaya zero talaga sa boundary
    public static float menuStartTime(float offset, float beat, float[][] seq, int afterSeq) {
        return offset + seq[clamp(afterSeq, 0, seq.length - 2)][1] * beat;
    }

    public static float menuEndTime(float offset, float beat, float[][] seq, int afterSeq) {
        return sequenceStartTime(offset, beat, seq, clamp(afterSeq, 0, seq.length - 2) + 1);
    }

    public static float menuLength(float offset, float beat, float[][] seq, int afterSeq) {
        return menuEndTime(offset, beat, seq, afterSeq) - menuStartTime(offset, beat, seq, afterSeq);
    }

    // countdown, ilagay sa timer bar
    public static float menuLeft(float offset, float beat, float[][] seq, int afterSeq, float songPos) {
        return Math.max(0f, menuEndTime(offset, beat, seq, afterSeq) - songPos);
    }

    public static float cueTime(float offset, float beat, float[] cues, int i) {
        return offset + cues[i * 3] * beat;
    }

    /** Piecewise-linear lookup in a flat {x0, y0, x1, y1, ...} table. */
    public static float table(float[] t, float x) {
        if (x <= t[0]) return t[1];
        for (int i = 2; i < t.length; i += 2) {
            if (x <= t[i]) {
                float u = (x - t[i - 2]) / Math.max(1e-4f, t[i] - t[i - 2]);
                return t[i - 1] + (t[i + 1] - t[i - 1]) * u;
            }
        }
        return t[t.length - 1];
    }

    /**
     * Largest scale at which a rotated arena centred on (cx, cy) still fits the screen, margin
     * included. Used to keep the box inside the frame while the camera tilts and pans.
     */
    public static float fitScale(float aw, float ah, float cx, float cy, float rotDeg,
                                 float scrX, float scrY, float scrW, float scrH, float margin) {
        float r = Math.abs(rotDeg) * (float) Math.PI / 180f;
        float c = Math.abs((float) Math.cos(r)), s = Math.abs((float) Math.sin(r));
        float hw = (aw / 2f) * c + (ah / 2f) * s;
        float hh = (aw / 2f) * s + (ah / 2f) * c;
        float sx = Math.min(cx - scrX - margin, scrX + scrW - margin - cx) / Math.max(1f, hw);
        float sy = Math.min(cy - scrY - margin, scrY + scrH - margin - cy) / Math.max(1f, hh);
        return Math.min(sx, sy);
    }

    /** f in -1..1 of the free room left on a screen half-axis. */
    public static float roomFit(float f, float half, float c0, float scrHalf, float margin) {
        float lim = Math.max(0f, scrHalf - margin - half);
        return f >= 0f ? f * Math.max(0f, lim - c0) : f * Math.max(0f, lim + c0);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
