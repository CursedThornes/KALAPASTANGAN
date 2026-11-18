package com.java.KALAPASTANGAN.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.java.KALAPASTANGAN.GameSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

// ang hirap nito, wag ka nang magtanong bakit
public class BattleArena {
    public enum Result { RUNNING, DONE, DEAD }

    public static final int MAX_HP = 20;

    // tuning
    private static final float L = 300f;            // local arena size
    private static final float HIT_R = 3.6f;        // heart hitbox radius (tiny, bullet-hell style)
    private static final float SPEED = 260f;        // heart speed, local units / s
    private static final float SLOW = 112f;         // while holding SHIFT
    private static final float CURSOR_GAIN = 1f;    // parehas na bilis sa WASD, walang bentahe
    private static final float BASE_DAMAGE = 1f;
    private static final float IFRAMES = 0.9f;
    private static final int HEAL_AMOUNT = 4;
    private static final float BEAM_LEN = 480f;
    private static final int BANDS = 20;
    private static final float DROP_BEAT = BattleChart.P1_DROP_BEAT;
    private static final float DRAIN_TIME = BattleChart.P1_DRAIN_TIME;
    private static final float DROP_TIME = BattleChart.P1_DROP_TIME;

    // chart data lives in BattleChart; the arena keeps short aliases for its own reads
    private static final float[][] SEQ = BattleChart.P1_SEQ;
    private static final int[] SEQ_D = BattleChart.P1_DIFFICULTY;
    private static final String[] SEQ_NAMES = BattleChart.P1_NAMES;

    private static final float P2_BPM = BattleChart.P2_BPM;
    private static final float P2_OFFSET = BattleChart.P2_OFFSET;
    private static final float P2_BEAT = BattleChart.P2_BEAT;
    private static final float P2_SONG_LEN = BattleChart.P2_SONG_LEN;
    private static final float P2_DRAIN_TIME = BattleChart.P2_DRAIN_TIME;
    private static final float P2_DROP_TIME = BattleChart.P2_DROP_TIME;
    private static final float[][] P2_SEQ = BattleChart.P2_SEQ;
    private static final int[] P2_D = BattleChart.P2_DIFFICULTY;
    private static final String[] P2_NAMES = BattleChart.P2_NAMES;
    private static final float[] P2_CUES = BattleChart.P2_CUES;
    private static final float[] P2_SECT = BattleChart.P2_SECT;
    private static final float[] P2_TENS = BattleChart.P2_TENS;

    private static final float P3_BPM = BattleChart.P3_BPM;
    private static final float P3_OFFSET = BattleChart.P3_OFFSET;
    private static final float P3_BEAT = BattleChart.P3_BEAT;
    private static final float P3_SONG_LEN = BattleChart.P3_SONG_LEN;
    private static final float P3_DRAIN_TIME = BattleChart.P3_DRAIN_TIME;
    private static final float P3_DROP_TIME = BattleChart.P3_DROP_TIME;
    private static final float[] P3_WALL = BattleChart.P3_WALL;
    private static final float[][] P3_SEQ = BattleChart.P3_SEQ;
    private static final int[] P3_D = BattleChart.P3_DIFFICULTY;
    private static final String[] P3_NAMES = BattleChart.P3_NAMES;
    private static final float[] P3_CUES = BattleChart.P3_CUES;
    private static final float[] P3_SECT = BattleChart.P3_SECT;
    private static final float[] P3_TENS = BattleChart.P3_TENS;

    private static final float TAIL_BEATS = BattleChart.TAIL_BEATS;

    // screen-fx feel
    private static final float FX_SHAKE_X = 7f, FX_SHAKE_Y = 5f, FX_SHAKE_ROT = 0.8f;   // light shake
    private static final float TRAUMA_GAIN = 0.45f;       // scales every shake hit
    private static final float PAN_GAIN = 0.4f;           // scales every camera shove
    private static final float TILT_MAX = 28f;            // tilt target (degrees) aggressive
    private static final float TILT_LIMIT = 36f;          // hard spring limit
    private static final float IMPACT_TIME = 0.34f;       // speed-line impact frame
    private static final float CRACK_LIFE_SCALE = 0.28f;  // cracks vanish fast
    private static final float PROP_BEAM_LEN = L * 0.75f;
    private static final float PROP_BEAM_WIDTH = 18f;

    private static final float[] TP_BEATS = BattleChart.TP_BEATS;
    private static final float TP_LEAD = 3.0f;       // beats of pre-lean before the first blast
    private static final float TP_NUDGE = 0.02f;     // fire slightly early so the frame lands on the hit
    private static final float TP_HOLD = 1.0f;       // beats the last station is held before easing home
    private static final float FIT_MARGIN = 24f;     // keep the arena this far inside the screen edge
    private static final float BOX_FADE = 0.12f;     // dialogue box fade-out after a pick (seconds)
    private static final float[][] TP_ST = BattleChart.TP_ST;

    private static final float[][] P2_PAL = BattleChart.P2_PALETTE;
    private static final float[][] P3_PAL = BattleChart.P3_PALETTE;
    private static final float[][] PAL = BattleChart.P1_PALETTE;

    // states
    private static final int ST_IDLE = 0, ST_PRE = 1, ST_LIVE = 2, ST_OUT = 3, ST_DEAD = 4,
        ST_OPENING = 5;
    // event kinds
    private static final int K_ORB = 0, K_AIM = 1, K_HOLD = 2, K_BEAM = 3, K_BOMB = 4,
        K_FREEZE = 5, K_PICKUP = 6, K_BONE = 7;

    // data classes
    private static final class Bullet {
        float x, y, vx, vy, r, age, delay, spd, holdAt = -1f, holdDur, boost = 1f, killR;
        boolean aimed, fired, boosted, grazed, active;
        int hue;
    }

    private static final class Beam {
        float x, y, ang, thick, warn, on, spin, age;
        boolean fake, propellerBeam, slash, active;
        int hue;
    }

    private static final class Bomb {
        float x, y, R, warn, age, spd, rot;
        int count, hue;
        boolean blown, active;
    }

    // isang buto = isang slab, pader = dalawa
    private static final class Bone {
        float x, y0, y1, w, vx, age;
        boolean active;
        int hue;
    }

    private static final class Pickup {
        float x, y, age;
        boolean active;
        Pickup(float x, float y) { this.x = x; this.y = y; }
    }

    private static final class Ev {
        float t;
        int k;
        float[] p;
    }

    // fields
    private final Texture orbTex, glowTex, ringTex;
    private final Texture throwAxeTex;
    private final Texture throwAxeOutlineTex;
    private final Random rng = new Random();
    private Texture px;

    private float bpm = 172f, offset = 0.051f, beat = 60f / 172f;

    // default = phase 1, pinapalitan lahat pag nag-phase
    private boolean phaseTwo, phaseThree, monochromeCue;
    private float[][] seq = SEQ;
    private int[] seqDiff = SEQ_D;
    private String[] seqNames = SEQ_NAMES;
    private float[] cues = CUES;
    private float[] sect = SECT;
    private float[] tens = TENS;
    private float[] tpBeats = TP_BEATS;
    private float songLen = SONG_LEN;
    private float dropTime = DROP_TIME;
    private float drainTime = DRAIN_TIME;
    private float ax, ay, aw = L, ah = L, k = 1f;

    private int state = ST_IDLE;
    private int hp = MAX_HP;
    private int roundNo, graze;
    private float dmgMul = 1f;
    private boolean god, hitPending;
    private float hx = L / 2f, hy = L / 2f, hurt;
    // cursor mode: posisyon ng mouse sa loob ng playfield
    private boolean pointerValid;
    private float pointerLX, pointerLY;

    private float preT, roundT, roundLen, outT, deadT, songAnchor, lastSong = -1f;
    private float openingTime, dropImpact;
    private float freeze, burstT, animT, wash;
    private int next, lastGap = 7;
    private boolean openingStill, openingMuted, dropPending;
    private int seqIdx, seqStartFlag = -1, lastBar = -1, lastHalf = -1;
    private float seqStartT, barFlip = 1f, fxBlend, finaleBlend;
    private final List<Ev> evs = new ArrayList<Ev>();
    private final List<Bullet> bullets = new ArrayList<Bullet>();
    private final List<Beam> beams = new ArrayList<Beam>();
    private final List<Bomb> bombs = new ArrayList<Bomb>();
    private final List<Pickup> pickups = new ArrayList<Pickup>();
    private final List<Bone> bones = new ArrayList<Bone>();

    // pool para walang GC spike sa gitna ng bullet hell
    private final java.util.ArrayList<Bullet> bulletPool = new java.util.ArrayList<Bullet>();
    private final java.util.ArrayList<Beam> beamPool = new java.util.ArrayList<Beam>();
    private final java.util.ArrayList<Bomb> bombPool = new java.util.ArrayList<Bomb>();
    private final java.util.ArrayList<Pickup> pickupPool = new java.util.ArrayList<Pickup>();

    private static final Comparator<Ev> EV_COMPARATOR = new Comparator<Ev>() {
        @Override public int compare(Ev a, Ev b) { return Float.compare(a.t, b.t); }
    };

    // visualiser
    private byte[] vis;
    private int visCh, visFps, visFrames;
    private float visT, kickEnv, prevKick, kickCool, thumpV, pulseV, fluxEnv;
    private final float[] bars = new float[BANDS];
    private long lastTickFrame = -1L;

    private static final int FX_OFF = 0, FX_HALF = 1, FX_FULL = 2;
    private static final float SONG_LEN = BattleChart.P1_SONG_LEN;
    private static final float[] CUES = BattleChart.P1_CUES;
    private static final float[] SECT = BattleChart.P1_SECT;
    private static final float[] TENS = BattleChart.P1_TENS;

    private int fxMode = FX_FULL;
    private boolean fxLive;
    private float holdTilt;
    private float trauma, fxTilt, fxTiltV, fxZoom, fxZoomV, fxPanX, fxPanY, fxPanVX, fxPanVY;
    private float secLevel, tension, tensF, spinLean, spinLeanSm, lastCueT = -1f;
    private float flashV, glitchT, tiltSign = 1f;
    private float focusSm, impactT, impactP, focusBurst;
    private float squeeze;
    private int wallIdx = -1;
    private final Matrix4 overlayTransformBackup = new Matrix4();
    private float tpX, tpY, tpR, tpTX, tpTY, tpTR, tpW, tpStreakT, tpFlash, tpStreakAng, surgeF;
    private float spinAng, spinT, spinBoomT, spinDir = 1f;
    private float spinMoveT, spinMoveLen, spinFromX, spinFromY, spinToX, spinToY;
    private int tpFired = -1;
    private int menuIdx = -1, pickKind = -1;
    private float menuT0, menuT1, pickT = -1f;
    private boolean pickDone;
    private float camX, camY, camVX, camVY;                 // camera chase
    private float ghostT, ghX0, ghY0, ghX1, ghY1;           // teleport afterimages
    private float safeX, safeY, safeW, safeH;
    private boolean safeSet;
    private float outX, outY, outRot, outScale = 1f;
    private float scrX, scrY, scrW = 1280f, scrH = 720f;
    private int cueIdx;

    private static final int CRN = 5, CSEG = 80;
    private final float[] cMX = new float[CRN * CSEG], cMY = new float[CRN * CSEG], cL = new float[CRN * CSEG],
        cAng = new float[CRN * CSEG], cD = new float[CRN * CSEG], cW = new float[CRN * CSEG];
    private final int[] cCount = new int[CRN];
    private final float[] cAge = new float[CRN], cLife = new float[CRN], cPow = new float[CRN];
    private int crCursor;

    private static final int SN = 240;
    private final float[] shX = new float[SN], shY = new float[SN], shVX = new float[SN], shVY = new float[SN],
        shR = new float[SN], shRV = new float[SN], shW = new float[SN], shH = new float[SN],
        shLife = new float[SN];
    private final int[] shHue = new int[SN];
    private int shCursor;

    // particles
    private static final int PN = 600;
    private final float[] pX = new float[PN], pY = new float[PN], pVX = new float[PN], pVY = new float[PN],
        pLife = new float[PN], pMax = new float[PN], pSize = new float[PN], pR = new float[PN],
        pG = new float[PN], pB = new float[PN];
    private int pCursor;

    // rings
    private static final int RN = 32;
    private final float[] rX = new float[RN], rY = new float[RN], rAge = new float[RN], rLife = new float[RN],
        rR0 = new float[RN], rR1 = new float[RN], rA = new float[RN];
    private final int[] rHue = new int[RN];
    private final boolean[] rBack = new boolean[RN];
    private int rCursor;

    // death shards
    private final float[] sVX = new float[8], sVY = new float[8];

    private static final int[] HEART = {
        0b011000110, 0b111101111, 0b111111111, 0b111111111,
        0b011111110, 0b001111100, 0b000111000, 0b000010000
    };

    // setup

    public BattleArena() {
        orbTex = makeTex(64, 0);
        glowTex = makeTex(64, 1);
        ringTex = makeTex(128, 2);
        throwAxeTex = new Texture(Gdx.files.internal("BattleUI/ThrowAxe.png"));
        throwAxeOutlineTex = makeWhiteMask("BattleUI/ThrowAxe.png");
        throwAxeTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        for (int i = 0; i < RN; i++) rLife[i] = 0f;
    }

    private static Texture makeTex(int size, int kind) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        float c = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nd = (float) Math.sqrt((x - c) * (x - c) + (y - c) * (y - c)) / (size / 2f);
                float a;
                if (kind == 0) {
                    a = MathUtils.clamp((1f - nd) / 0.08f, 0f, 1f);
                } else if (kind == 1) {
                    a = nd >= 1f ? 0f : (float) Math.pow(1f - nd, 2.2);
                } else {
                    float q = (nd - 0.9f) / 0.05f;
                    a = nd > 1f ? 0f : (float) Math.exp(-q * q);
                }
                pm.setColor(1f, 1f, 1f, a);
                pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private static Texture makeWhiteMask(String path) {
        Pixmap source = new Pixmap(Gdx.files.internal(path));
        Pixmap mask = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
        mask.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = source.getPixel(x, y) & 0xff;
                mask.drawPixel(x, y, 0xffffff00 | alpha);
            }
        }
        Texture texture = new Texture(mask);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        source.dispose();
        mask.dispose();
        return texture;
    }

    public void configure(float bpm, float offset) {
        this.bpm = bpm;
        this.offset = offset;
        this.beat = 60f / bpm;
    }

    // balik sa LIYAB
    public void usePhaseOne() {
        phaseTwo = false;
        phaseThree = false;
        bpm = BattleChart.P1_BPM;
        offset = BattleChart.P1_OFFSET;
        beat = BattleChart.P1_BEAT;
        seq = SEQ;
        seqDiff = SEQ_D;
        seqNames = SEQ_NAMES;
        cues = CUES;
        sect = SECT;
        tens = TENS;
        tpBeats = TP_BEATS;
        songLen = SONG_LEN;
        dropTime = DROP_TIME;
        drainTime = DRAIN_TIME;
    }

    // phase 2. bago ang resetHp(), dun binabasa ang tables
    public void usePhaseTwo() {
        phaseTwo = true;
        phaseThree = false;
        bpm = P2_BPM;
        offset = P2_OFFSET;
        beat = P2_BEAT;
        seq = P2_SEQ;
        seqDiff = P2_D;
        seqNames = P2_NAMES;
        cues = P2_CUES;
        sect = P2_SECT;
        tens = P2_TENS;
        tpBeats = new float[0];             // no teleport set-piece in phase 2
        songLen = P2_SONG_LEN;
        dropTime = P2_DROP_TIME;
        drainTime = P2_DRAIN_TIME;
    }

    public void usePhaseThree() {
        phaseTwo = false;
        phaseThree = true;
        bpm = P3_BPM;
        offset = P3_OFFSET;
        beat = P3_BEAT;
        seq = P3_SEQ;
        seqDiff = P3_D;
        seqNames = P3_NAMES;
        cues = P3_CUES;
        sect = P3_SECT;
        tens = P3_TENS;
        tpBeats = new float[0];
        songLen = P3_SONG_LEN;
        dropTime = P3_DROP_TIME;
        drainTime = P3_DRAIN_TIME;
    }

    public boolean isPhaseTwo() { return phaseTwo; }

    public boolean consumeMonochromeCue() {
        boolean pending = monochromeCue;
        monochromeCue = false;
        return pending;
    }

    // signal hit: zoom sa puso, crack, glitch. mono shader sa boss
    public void signalBurst() {
        focusBurst = 1f;
        glitchT = Math.max(glitchT, 0.42f);
        flashV = Math.max(flashV, 0.65f);
        addTrauma(1.1f);
        fxZoom += 0.2f;
        shatterBox(16, 1f);
        addRing(L / 2f, L / 2f, 18f, 470f, 0.5f, 1, 0.9f, false);
        addRing(L / 2f, L / 2f, 44f, 300f, 0.7f, 4, 0.7f, true);
        sparks(hx, hy, 44, 300f, 0.55f, 3.4f, 0.95f, 0.92f, 1f);
    }

    public void resetHp() {
        hp = MAX_HP;
        roundNo = 0;
        graze = 0;
        state = ST_IDLE;
        hurt = 0f;
        seqIdx = 0;
        seqStartFlag = -1;
        lastBar = -1;
        fxBlend = 0f;
        finaleBlend = 0f;
        clearWorld();
        clearFx();
    }

    public void heal(int amount) {
        hp = Math.min(MAX_HP, hp + amount);
    }

    public void setFxLive(boolean live) {
        fxLive = live;
    }

    public void setArena(float x, float y, float w, float h) {
        ax = x;
        ay = y;
        aw = w;
        ah = h;
        k = Math.min(w, h) / L;
    }

    // spectrum galing make_vis.py. kung wala, beat ang basehan
    public void loadVis(FileHandle f) {
        vis = null;
        if (f == null || !f.exists()) return;
        byte[] b = f.readBytes();
        if (b.length < 4) return;
        visCh = b[0] & 0xff;
        visFps = b[1] & 0xff;
        visFrames = (b.length - 2) / Math.max(1, visCh);
        if (visCh >= BANDS + 2 && visFrames > 2) vis = b;
    }

    public void dispose() {
        orbTex.dispose();
        glowTex.dispose();
        ringTex.dispose();
        throwAxeTex.dispose();
        throwAxeOutlineTex.dispose();
    }

    public int hp() { return hp; }

    public float fxX() { return outX; }

    public float fxY() { return outY; }

    public float fxRot() { return outRot; }

    // dagdag na ikot sa box (1:43 spin)
    public float spinAngle() { return spinAng; }

    public float fxScale() { return outScale; }

    // 0..1 lalim ng dive ng camera
    public float fxFocus() { return fxMode == FX_OFF ? 0f : focusSm; }

    // posisyon ng puso, pagkatapos ng setArena
    public float heartX() { return wx(hx); }

    public float heartY() { return wy(hy); }

    // san na ang arena. i-offset mo ang frame dito
    public float arenaShiftX() { return tpX; }

    public float arenaShiftY() { return tpY; }

    // ligtas na kublihan ng arena. iwanan ng space para sa HUD
    public void setSafeRect(float x, float y, float w, float h) {
        safeX = x;
        safeY = y;
        safeW = w;
        safeH = h;
        safeSet = true;
    }

    public float pulse() { return pulseV; }

    // 0..1 lapit ng pader ng phase 3
    public float squeeze() { return squeeze; }

    // 0..1 spike kada malakas na kick
    public float thump() { return thumpV; }

    public int sequenceCount() { return seq.length; }

    public String sequenceName(int i) { return seqNames[MathUtils.clamp(i, 0, seq.length - 1)]; }

    // simula ng sequence i, sa segundo
    public float sequenceStartTime(int i) {
        return BattleChart.sequenceStartTime(offset, beat, seq, i);
    }

    private float seqLen(int i) {
        return BattleChart.sequenceLength(beat, seq, i, TAIL_BEATS);
    }

    // menu = gap ng SEQ. song-locked kaya zero talaga sa boundary

    public float menuStartTime(int afterSeq) {
        return BattleChart.menuStartTime(offset, beat, seq, afterSeq);
    }

    public float menuEndTime(int afterSeq) {
        return BattleChart.menuEndTime(offset, beat, seq, afterSeq);
    }

    public float menuLength(int afterSeq) {
        return BattleChart.menuLength(offset, beat, seq, afterSeq);
    }

    // countdown, ilagay sa timer bar
    public float menuLeft(int afterSeq, float songPos) {
        return BattleChart.menuLeft(offset, beat, seq, afterSeq, songPos);
    }

    // tawagin paglabas ng menu
    public void beginMenu(int afterSeq) {
        menuIdx = MathUtils.clamp(afterSeq, 0, seq.length - 2);
        menuT0 = menuStartTime(menuIdx);
        menuT1 = menuEndTime(menuIdx);
        pickKind = -1;
        pickT = -1f;
        pickDone = false;
    }

    // pag pumili na (0 atk, 1 def, 2 reason). sasapat sa half-beat
    public void pickMenu(int kind) {
        if (menuIdx < 0 || pickT >= 0f) return;
        float half = beat / 2f;
        float x = (visT - offset) / half;
        float into = x - (float) Math.floor(x);
        pickKind = kind;
        pickT = Math.min(visT + (1f - into) * half, Math.max(visT, menuT1 - 0.05f));
    }

    // alpha ng dialogue box
    public float menuBoxAlpha() {
        if (pickT < 0f) return 1f;
        return 1f - MathUtils.clamp((visT - pickT) / BOX_FADE, 0f, 1f);
    }

    // 0..1 ramp ng menu, squared
    public float menuPressure() {
        if (menuIdx < 0 || menuT1 <= menuT0) return 0f;
        float u = MathUtils.clamp((visT - menuT0) / (menuT1 - menuT0), 0f, 1f);
        return u * u;
    }

    // index ng bagong sequence, o -1
    public int consumeSeqStart() {
        int s = seqStartFlag;
        seqStartFlag = -1;
        return s;
    }

    public boolean consumeHit() {
        boolean h = hitPending;
        hitPending = false;
        return h;
    }

    public boolean consumeDrop() {
        boolean d = dropPending;
        dropPending = false;
        return d;
    }

    // visual tick

    private float sample(float t, int ch) {
        if (vis == null) return 0f;
        float dur = visFrames / (float) visFps;
        float tt = t % dur;
        if (tt < 0f) tt += dur;
        float fp = tt * visFps;
        int i0 = (int) fp;
        float fr = fp - i0;
        int i1 = (i0 + 1) % visFrames;
        i0 = i0 % visFrames;
        float v0 = (vis[2 + i0 * visCh + ch] & 0xff) / 255f;
        float v1 = (vis[2 + i1 * visCh + ch] & 0xff) / 255f;
        return v0 + (v1 - v0) * fr;
    }

    private float beatPhase(float t) {
        float x = (t - offset) / beat;
        return x - (float) Math.floor(x);
    }

    // bawat frame, pati menu
    public void tickVisual(float d, float songPos) {
        lastTickFrame = Gdx.graphics.getFrameId();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            fxMode = fxMode == FX_OFF ? FX_FULL : fxMode - 1;
        }
        animT += d;
        if (songPos >= 0f) visT = songPos;
        else visT += d;

        float phase = beatPhase(visT);
        float beatEnv = (float) Math.pow(1f - phase, 3.0);
        float kick;
        if (vis != null) {
            for (int i = 0; i < BANDS; i++) {
                float tgt = sample(visT, i);
                bars[i] = tgt > bars[i] ? tgt : Math.max(tgt, bars[i] - d * 2.4f);
            }
            fluxEnv = sample(visT, BANDS);
            kick = sample(visT, BANDS + 1);
        } else {
            for (int i = 0; i < BANDS; i++) {
                float tgt = beatEnv * (0.4f + 0.6f * (float) Math.abs(Math.sin(i * 1.7f + animT * 3f)));
                bars[i] = tgt > bars[i] ? tgt : Math.max(tgt, bars[i] - d * 2.4f);
            }
            kick = phase < 0.08f ? 1f : 0f;
            fluxEnv = beatEnv;
        }
        kickEnv = Math.max(kick, kickEnv - d * 5f);
        kickCool -= d;
        if (kick > 0.55f && prevKick <= 0.55f && kickCool <= 0f) {
            kickCool = 0.12f;
            thumpV = 1f;
            addRing(L / 2f, L / 2f, 40f, 360f, 0.55f, kickEnv > 0.9f ? 2 : 0, 0.34f, true);
            // puso sabay sa kick
            if (state == ST_LIVE || (state == ST_OPENING && !openingStill)) {
                addRing(hx, hy, 3f, 30f + 12f * secLevel, 0.26f, 2, 0.5f, false);
            }
            kickFx();
        }
        prevKick = kick;
        thumpV = Math.max(0f, thumpV - d * 6f);
        wash = Math.max(0f, wash - d * 2.5f);
        dropImpact = Math.max(0f, dropImpact - d * 1.5f);
        pulseV = Math.min(1f, Math.max(kickEnv * 0.9f, beatEnv * 0.8f));
        float finTarget = (seqIdx == seq.length - 1
            && (state == ST_LIVE || state == ST_PRE || state == ST_OUT)) ? 1f : surgeF;
        finaleBlend += (finTarget - finaleBlend) * Math.min(1f, d * 2.5f);
        if (state == ST_OPENING && openingStill) {
            for (int i = 0; i < BANDS; i++) bars[i] = 0f;
            kickEnv = 0f;
            thumpV = 0f;
            pulseV = 0f;
            wash = 0f;
        }
        advanceFx(d);
        tickFx(d);
    }

    private void advanceFx(float d) {
        for (int i = 0; i < RN; i++) if (rLife[i] > 0f) {
            rAge[i] += d;
            if (rAge[i] >= rLife[i]) rLife[i] = 0f;
        }
        for (int i = 0; i < PN; i++) if (pLife[i] > 0f) {
            pLife[i] -= d;
            pX[i] += pVX[i] * d;
            pY[i] += pVY[i] * d;
            float drag = Math.max(0f, 1f - 2.4f * d);
            pVX[i] *= drag;
            pVY[i] *= drag;
        }
    }

    private void sparks(float x, float y, int n, float spd, float life, float size, float r, float g, float b) {
        for (int i = 0; i < n; i++) {
            int j = pCursor++ % PN;
            float a = rng.nextFloat() * MathUtils.PI2;
            float s = spd * (0.35f + rng.nextFloat() * 0.65f);
            pX[j] = x;
            pY[j] = y;
            pVX[j] = MathUtils.cos(a) * s;
            pVY[j] = MathUtils.sin(a) * s;
            pLife[j] = life * (0.6f + rng.nextFloat() * 0.4f);
            pMax[j] = pLife[j];
            pSize[j] = size * (0.6f + rng.nextFloat() * 0.8f);
            pR[j] = r;
            pG[j] = g;
            pB[j] = b;
        }
    }

    private void slashLanes(float b, boolean vertical, float warnBeats) {
        float off = rng.nextFloat() * 20f;
        float spin = (rng.nextBoolean() ? 1f : -1f) * 180f;
        for (int i = 0; i < 6; i++) {
            float pos = 14f + off + i * 50f;
            ev(b, K_BEAM, vertical ? pos : L / 2f, vertical ? L / 2f : pos,
                vertical ? 90f : 0f, 28f, B(warnBeats), B(0.3f), spin, 0f, 1f, 2f);
        }
    }

    private void beamPropeller(float b, float warnBeats, float durBeats, float degPerSec) {
        ev(b, K_BEAM, L / 2f, L / 2f, 0f, PROP_BEAM_WIDTH, B(warnBeats), B(durBeats),
            degPerSec, 0f, 1f, 1f);
    }

    private void addRing(float x, float y, float r0, float r1, float life, int hue, float alpha, boolean back) {
        int j = rCursor++ % RN;
        rX[j] = x;
        rY[j] = y;
        rR0[j] = r0;
        rR1[j] = r1;
        rAge[j] = 0f;
        rLife[j] = life;
        rHue[j] = hue;
        rA[j] = alpha;
        rBack[j] = back;
    }

    // round control

    private float B(float beats) { return beats * beat; }

    private void clearWorld() {
        bullets.clear();
        beams.clear();
        bombs.clear();
        pickups.clear();
        bones.clear();
        evs.clear();
        next = 0;
        freeze = 0f;
        burstT = 0f;
    }

    public void startOpening(float damageMul, float songPos,
                             float x, float y, float w, float h) {
        setArena(x, y, w, h);
        dmgMul = damageMul;
        clearWorld();
        hx = L / 2f;
        hy = L / 2f;
        hurt = 0f;
        openingTime = Math.max(0f, songPos);
        lastSong = songPos;
        openingStill = false;
        openingMuted = true;
        dropPending = false;
        dropImpact = 0f;
        roundNo = 0;
        seqIdx = 0;
        buildOpening();
        state = ST_OPENING;
    }

    private void openingEvent(float at, int kind, float... p) {
        Ev e = obtainEv();
        e.t = at;
        e.k = kind;
        e.p = p;
        evs.add(e);
    }

    private void openingRain(float at, int rows, float step, float speed) {
        int gap = 5 + rng.nextInt(4);
        for (int row = 0; row < rows; row++) {
            gap = MathUtils.clamp(gap + rng.nextInt(3) - 1, 2, 10);
            for (int col = 0; col < 14; col++) {
                if (col == gap || col == gap + 1) continue;
                openingEvent(at + row * step, K_ORB, 10.7f + col * 21.4f, L + 120f,
                    -90f, speed, 7f, 0f);
            }
        }
    }

    private void buildOpening() {
        if (phaseThree) { buildOpeningThree(); return; }
        if (phaseTwo) { buildOpeningTwo(); return; }
        openingRain(1.25f, 3, 0.95f, 108f);
        openingRain(4.65f, 3, 1.05f, 116f);
        openingEvent(3.2f, K_AIM, -70f, 95f, 132f, 7.5f, 0.8f, 0f);
        openingEvent(7.0f, K_AIM, L + 70f, 205f, 138f, 7.5f, 0.9f, 0f);
        Collections.sort(evs, EV_COMPARATOR);
    }

    // phase 2 reveal: 15 bars na tahimik, taps blangko isang bar. dun unang volley
    private void buildOpeningTwo() {
        openingRing(4.0f, L / 2f, L / 2f, 12, 70f, 0f);
        openingRing(7.0f, 70f, 230f, 14, 72f, 10f);
        openingRing(9.5f, 230f, 70f, 14, 74f, 20f);
        openingRain(12.6f, 2, 1.3f, 96f);
        openingEvent(14.2f, K_AIM, -70f, 120f, 120f, 7.5f, 1.2f, 0f);
        openingRing(15.0f, L / 2f, L / 2f, 18, 90f, 5f);
        openingEvent(16.0f, K_AIM, L + 70f, 190f, 124f, 7.5f, 1.2f, 0f);
        openingRain(17.1f, 2, 1.25f, 104f);
        openingEvent(18.4f, K_AIM, 150f, -60f, 118f, 7.5f, 1.0f, 0f);
        Collections.sort(evs, EV_COMPARATOR);
    }

    // phase 3 layer 1: isang bomba kada beat, sabay sa drop sa 10.00s
    private void buildOpeningThree() {
        for (int i = 0; i < 13; i++) {
            openingEvent(0.55f + i * beat, K_BOMB,
                60f + (i * 71f) % 180f, 60f + (i * 47f) % 180f,
                34f, 0.45f, 10f, 105f, i * 13f, (i & 1) == 0 ? 3f : 1f);
        }
        openingRing(4.4f, L / 2f, L / 2f, 10, 62f, 0f);
        openingEvent(6.6f, K_AIM, -70f, 190f, 126f, 7.5f, 0.7f, 3f);
        openingRing(7.8f, 80f, 220f, 12, 72f, 5f);
        openingRain(8.9f, 2, 0.5f, 118f);
        Collections.sort(evs, EV_COMPARATOR);
    }

    private void openingRing(float at, float x, float y, int count, float spd, float phase) {
        for (int i = 0; i < count; i++) {
            float ang = phase + i * (360f / count);
            float rad = ang * MathUtils.degreesToRadians;
            openingEvent(at, K_ORB, x + MathUtils.cos(rad) * 14f, y + MathUtils.sin(rad) * 14f,
                ang, spd, 6f, i % 2);
        }
    }

    // 11.9s drop: sequence I
    private void triggerDrop(float songPos) {
        clearWorld();
        openingStill = false;
        openingMuted = false;
        roundNo++;
        seqIdx = 0;
        seqStartT = dropTime;
        seqStartFlag = 0;
        buildSeq(0);
        roundLen = seqLen(0);
        roundT = Math.max(0f, songPos >= 0f ? songPos - dropTime : openingTime - dropTime);
        songAnchor = (songPos >= 0f ? songPos : openingTime) - roundT;
        lastSong = songPos;
        state = ST_LIVE;
        dropImpact = 1f;
        wash = 1f;
        thumpV = 1f;
        addRing(L / 2f, L / 2f, 12f, 390f, 0.65f, 1, 1f, false);
        addRing(L / 2f, L / 2f, 38f, 340f, 0.8f, 1, 0.8f, true);
        dropPending = true;
        fxMega(1f);
        fxImpact(1f);
    }

    // simulan ang sequence idx. hinihintay ang boundary, dive ang camera
    public void startSequence(int idx, float damageMul, float songPos,
                              float x, float y, float w, float h) {
        setArena(x, y, w, h);
        dmgMul = damageMul;
        clearWorld();
        openingStill = false;
        openingMuted = false;
        dropPending = false;
        hx = L / 2f;
        hy = L / 2f;
        hurt = 0f;
        seqIdx = MathUtils.clamp(idx, 0, seq.length - 1);
        seqStartT = sequenceStartTime(seqIdx);
        roundNo++;
        buildSeq(seqIdx);
        roundT = 0f;
        roundLen = seqLen(seqIdx);
        preT = songPos >= 0f ? Math.max(0f, seqStartT - songPos) : beat * 2f;
        lastSong = songPos;
        state = ST_PRE;
    }

    public Result update(float d, float songPos) {
        if (Gdx.graphics.getFrameId() != lastTickFrame) tickVisual(d, songPos);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F) || Gdx.input.isKeyJustPressed(Input.Keys.F2)) god = !god;
        if (state == ST_DEAD) return Result.DEAD;
        if (state == ST_IDLE) return Result.RUNNING;

        if (state == ST_OPENING) {
            if (songPos >= 0f) openingTime = songPos;
            else openingTime += d;
            if (!openingStill) {
                moveHeart(d);
                hurt = Math.max(0f, hurt - d);
                while (next < evs.size() && evs.get(next).t <= openingTime) fire(evs.get(next++));
                Result openingResult = stepWorld(d);
                if (state == ST_DEAD || openingResult == Result.DEAD) return Result.DEAD;
                if (openingTime >= drainTime) {
                    clearWorld();
                    for (int i = 0; i < PN; i++) pLife[i] = 0f;
                    for (int i = 0; i < RN; i++) rLife[i] = 0f;
                    wash = 0f;
                    openingStill = true;
                    fxFakeOut();
                }
            }
            if (openingTime >= dropTime) triggerDrop(songPos);
            return Result.RUNNING;
        }

        moveHeart(d);
        hurt = Math.max(0f, hurt - d);

        if (state == ST_PRE) {
            boolean go;
            if (songPos >= 0f) {
                go = songPos >= seqStartT;
            } else {
                preT -= d;
                go = preT <= 0f;
            }
            if (go) {
                state = ST_LIVE;
                if (songPos >= 0f) {
                    roundT = Math.max(0f, songPos - seqStartT);
                    songAnchor = songPos - roundT;
                } else {
                    roundT = Math.max(0f, -preT);
                    songAnchor = 0f;
                }
                lastSong = songPos;
                while (next < evs.size() && evs.get(next).t < roundT - 0.25f) next++;
                seqStartFlag = seqIdx;
                menuIdx = -1;
                pickKind = -1;
                pickT = -1f;
                pickDone = false;
                addRing(L / 2f, L / 2f, 20f, 320f, 0.5f, 3, 0.5f, false);
                wash = 1f;
                fxSeqStart(seqIdx);
                fxImpact(0.85f);
            }
            return Result.RUNNING;
        }

        if (state == ST_LIVE) {
            roundT += d;
            if (songPos >= 0f && lastSong >= 0f) {
                if (songPos + 0.5f < lastSong) {
                    songAnchor = songPos - roundT;                 // the track looped
                } else {
                    float drift = (songPos - songAnchor) - roundT;
                    if (Math.abs(drift) > 0.3f) roundT += drift;
                    else roundT += drift * Math.min(1f, d * 2f);
                }
            }
            lastSong = songPos;

            while (next < evs.size() && evs.get(next).t <= roundT) {
                Ev e = evs.get(next++);
                fire(e);
            }
            Result r = stepWorld(d);
            if (state == ST_DEAD || r == Result.DEAD) return Result.DEAD;

            if (roundT >= roundLen) {
                state = ST_OUT;
                outT = 0f;
                for (Bullet b : bullets) sparks(b.x, b.y, 2, 60f, 0.5f, 3f, 0.85f, 0.82f, 0.74f);
                bullets.clear();
                beams.clear();
                bombs.clear();
                wash = 1f;
                addRing(L / 2f, L / 2f, 20f, 380f, 0.6f, 2, 0.6f, false);
            }
            return Result.RUNNING;
        }

        // ST_OUT
        outT += d;
        return outT > 0.5f ? Result.DONE : Result.RUNNING;
    }

    // heart

    // world -> local na playfield (kaparehong space ng wx()/wy())
    public void setPointer(float worldX, float worldY) {
        if (k <= 0f) {
            pointerValid = false;
            return;
        }
        pointerLX = (worldX - ax - tpX) / k;
        pointerLY = (worldY - ay - tpY) / k;
        pointerValid = true;
    }

    public void clearPointer() {
        pointerValid = false;
    }

    private void moveHeart(float d) {
        boolean slow = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        float sp = slow ? SLOW : SPEED;
        if (GameSettings.battleMouse && pointerValid) {
            // cursor: sumusunod sa mouse, pero hindi lalampas sa bilis ng puso
            float dx = pointerLX - hx, dy = pointerLY - hy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.001f) {
                float step = Math.min(len, sp * CURSOR_GAIN * d);
                if (step >= len) {
                    // abot na, dumikit agad sa cursor para walang huling lag
                    hx = pointerLX;
                    hy = pointerLY;
                } else {
                    hx += dx / len * step;
                    hy += dy / len * step;
                }
            }
        } else {
            float mx = 0f, my = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) mx -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) mx += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) my += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) my -= 1f;
            float len = (float) Math.sqrt(mx * mx + my * my);
            if (len > 1f) {
                mx /= len;
                my /= len;
            }
            hx += mx * sp * d;
            hy += my * sp * d;
        }
        hx = MathUtils.clamp(hx, 5f, L - 5f);
        hy = MathUtils.clamp(hy, 5f, L - 5f);
        if (spinAng != 0f) clampToSpinBox();
    }

    // puso dapat nasa loob ng playfield AT ng box
    private void clampToSpinBox() {
        float half = L / 2f - 5f;
        float c = MathUtils.cosDeg(spinAng), s = MathUtils.sinDeg(spinAng);
        for (int it = 0; it < 3; it++) {
            float dx = hx - L / 2f, dy = hy - L / 2f;
            float u = MathUtils.clamp(dx * c + dy * s, -half, half);
            float v = MathUtils.clamp(-dx * s + dy * c, -half, half);
            hx = MathUtils.clamp(L / 2f + u * c - v * s, 5f, L - 5f);
            hy = MathUtils.clamp(L / 2f + u * s + v * c, 5f, L - 5f);
        }
    }

    private void hitPlayer() {
        if (hurt > 0f || (state != ST_LIVE && state != ST_OPENING)) return;
        hurt = IFRAMES;
        hitPending = true;
        wash = 1f;
        sparks(hx, hy, 34, 190f, 0.7f, 3.2f, 0.78f, 0.08f, 0.12f);
        addRing(hx, hy, 6f, 90f, 0.45f, 1, 0.8f, false);
        fxHit();
        if (god) return;
        hp -= (int) BASE_DAMAGE;                       // isang HP lang bawat tama, kahit anong phase
        if (hp <= 0) {
            hp = 0;
            state = ST_DEAD;
            deadT = 0f;
            for (int i = 0; i < 8; i++) {
                float a = rng.nextFloat() * MathUtils.PI2;
                float s = 60f + rng.nextFloat() * 120f;
                sVX[i] = MathUtils.cos(a) * s;
                sVY[i] = MathUtils.sin(a) * s + 40f;
            }
            fxDeath();
        }
    }

    // world step

    private Result stepWorld(float d) {
        if (freeze > 0f) {
            freeze -= d;
            if (freeze <= 0f) {
                burstT = 0.35f;
                fxSlam(0.85f);
                fxImpact(0.75f);
            }
        }
        float bdt = freeze > 0f ? 0f : d * (1f + 0.7f * (burstT / 0.35f));
        burstT = Math.max(0f, burstT - d);

        // bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.age += bdt;
            boolean waiting = b.aimed && !b.fired;
            if (waiting) {
                if (b.age >= b.delay) {
                    float dx = hx - b.x, dy = hy - b.y;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len < 1f) len = 1f;
                    b.vx = dx / len * b.spd;
                    b.vy = dy / len * b.spd;
                    b.fired = true;
                    waiting = false;
                    sparks(b.x, b.y, 8, 90f, 0.3f, 2.6f, 0.9f, 0.86f, 0.78f);
                }
            } else if (b.holdAt >= 0f && b.age >= b.holdAt && b.age < b.holdAt + b.holdDur) {
                // nakahinto: fake pause
            } else {
                if (b.holdAt >= 0f && !b.boosted && b.age >= b.holdAt + b.holdDur) {
                    b.vx *= b.boost;
                    b.vy *= b.boost;
                    b.boosted = true;
                    sparks(b.x, b.y, 4, 80f, 0.3f, 2.4f, 0.9f, 0.88f, 0.82f);
                }
                b.x += b.vx * bdt;
                b.y += b.vy * bdt;
            }
            if (b.killR > 0f) {                      // implosion bullets vanish at the centre
                float cx = b.x - L / 2f, cy = b.y - L / 2f;
                if (cx * cx + cy * cy < b.killR * b.killR) {
                    sparks(b.x, b.y, 2, 50f, 0.3f, 2.4f, 0.85f, 0.82f, 0.74f);
                    freeBullet(b);
                    bullets.remove(i);
                    continue;
                }
            }
            if (b.x < -220f || b.x > L + 220f || b.y < -220f || b.y > L + 560f) {
                freeBullet(b);
                bullets.remove(i);
                continue;
            }
            if (waiting) continue;
            float dx = b.x - hx, dy = b.y - hy;
            float dist2 = dx * dx + dy * dy;
            float hr = b.r * 0.78f + HIT_R;
            if (dist2 < hr * hr) {
                hitPlayer();
            } else if (!b.grazed && dist2 < (hr + 9f) * (hr + 9f) && hurt <= 0f) {
                b.grazed = true;
                graze++;
                sparks(hx, hy, 3, 70f, 0.25f, 2.2f, 0.88f, 0.85f, 0.76f);
                if (fxOn()) {
                    fxZoom += 0.003f;
                    addRing(hx, hy, 4f, 24f, 0.22f, 2, 0.5f, false);
                }
            }
        }

        // beams
        boolean beamFired = false, beamVert = false;
        spinLean = 0f;
        for (int i = beams.size() - 1; i >= 0; i--) {
            Beam bm = beams.get(i);
            float prevAge = bm.age;
            bm.age += d;
            if (!bm.fake && prevAge < bm.warn && bm.age >= bm.warn) {
                beamFired = true;
                if (Math.abs(MathUtils.sinDeg(bm.ang)) > 0.7f) beamVert = true;
            }
            if (!bm.fake && bm.spin != 0f && bm.age >= bm.warn && bm.age < bm.warn + bm.on) {
                spinLean = MathUtils.clamp(bm.spin * 0.1f, -9f, 9f);
            }
            boolean active = !bm.fake && bm.age >= bm.warn && bm.age < bm.warn + bm.on;
            if ((bm.propellerBeam && bm.age < bm.warn + bm.on)
                || (bm.age >= bm.warn && bm.age < bm.warn + bm.on)) {
                bm.ang += bm.spin * d;
            }
            if (active && bm.propellerBeam) {
                float dx = hx - bm.x, dy = hy - bm.y;
                for (int blade = 0; blade < 4; blade++) {
                    float rad = (bm.ang + blade * 90f) * MathUtils.degreesToRadians;
                    float along = dx * MathUtils.cos(rad) + dy * MathUtils.sin(rad);
                    float across = Math.abs(-dx * MathUtils.sin(rad) + dy * MathUtils.cos(rad));
                    if (along >= 0f && along <= PROP_BEAM_LEN
                        && across < PROP_BEAM_WIDTH / 2f + HIT_R) {
                        hitPlayer();
                    }
                }
            } else if (active) {
                float rad = bm.ang * MathUtils.degreesToRadians;
                float dx = hx - bm.x, dy = hy - bm.y;
                float perp = Math.abs(dx * MathUtils.sin(rad) - dy * MathUtils.cos(rad));
                if (perp < bm.thick / 2f + HIT_R - 1f) hitPlayer();
            }
            if (bm.age > bm.warn + bm.on + 0.2f) { freeBeam(bm); beams.remove(i); continue; }
        }
        if (beamFired) fxBeam(beamVert);

        // bombs
        for (int i = bombs.size() - 1; i >= 0; i--) {
            Bomb bo = bombs.get(i);
            bo.age += d;
            if (!bo.blown && bo.age >= bo.warn) {
                bo.blown = true;
                for (int n = 0; n < bo.count; n++) {
                    float ang = bo.rot + n * 360f / bo.count;
                    addBullet(bo.x, bo.y, ang, bo.spd, 6f, n % 2 == 0 ? bo.hue : 2);
                }
                float[] sparkColor = activePalette()[3];
                sparks(bo.x, bo.y, 40, 220f, 0.6f, 3.4f,
                    sparkColor[0], sparkColor[1], sparkColor[2]);
                addRing(bo.x, bo.y, 8f, bo.R * 1.8f, 0.4f, 3, 0.8f, false);
                wash = 1f;
                fxBomb(bo.x, bo.y);
            }
            if (bo.blown && bo.age < bo.warn + 0.14f) {
                float dx = bo.x - hx, dy = bo.y - hy;
                float rr = bo.R + HIT_R * 0.5f;
                if (dx * dx + dy * dy < rr * rr) hitPlayer();
            }
            if (bo.age > bo.warn + 0.4f) { freeBomb(bo); bombs.remove(i); continue; }
        }

        // buto: yung 1:36 lane, hindi nag-freeze kasabay ng kanta
        boolean boneHit = false;
        for (int i = bones.size() - 1; i >= 0; i--) {
            Bone b = bones.get(i);
            b.age += d;
            b.x += b.vx * d;
            if (b.x < -40f) { bones.remove(i); continue; }
            float cx = MathUtils.clamp(hx, b.x - b.w / 2f, b.x + b.w / 2f);
            float cy = MathUtils.clamp(hy, b.y0, b.y1);
            float bdx = hx - cx, bdy = hy - cy;
            if (bdx * bdx + bdy * bdy < HIT_R * HIT_R) boneHit = true;
        }
        if (boneHit) hitPlayer();

        // pickups
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.age += d;
            float dx = p.x - hx, dy = p.y - hy;
            if (dx * dx + dy * dy < 12f * 12f) {
                hp = Math.min(MAX_HP, hp + HEAL_AMOUNT);
                sparks(p.x, p.y, 26, 150f, 0.7f, 3f, 0.62f, 0.9f, 0.78f);
                addRing(p.x, p.y, 6f, 70f, 0.5f, 2, 0.7f, false);
                freePickup(p);
                pickups.remove(i);
                continue;
            } else if (p.age > 3.6f) {
                freePickup(p);
                pickups.remove(i);
            }
        }
        return Result.RUNNING;
    }

    // spawning

    private Bullet addBullet(float x, float y, float angDeg, float spd, float r, int hue) {
        Bullet b = obtainBullet();
        b.x = x;
        b.y = y;
        b.active = true;
        b.aimed = false;
        b.fired = false;
        b.boosted = false;
        b.grazed = false;
        b.holdAt = -1f;
        b.holdDur = 0f;
        b.boost = 1f;
        b.killR = 0f;
        float rad = angDeg * MathUtils.degreesToRadians;
        b.vx = MathUtils.cos(rad) * spd;
        b.vy = MathUtils.sin(rad) * spd;
        b.r = r;
        b.hue = hue;
        b.age = 0f;
        b.delay = 0f;
        if (bullets.size() < 1200) bullets.add(b);
        return b;
    }

    private Bullet obtainBullet() {
        if (!bulletPool.isEmpty()) return bulletPool.remove(bulletPool.size() - 1);
        return new Bullet();
    }

    private void freeBullet(Bullet b) {
        if (!b.active) return;
        b.active = false;
        bulletPool.add(b);
    }

    private Beam obtainBeam() {
        if (!beamPool.isEmpty()) return beamPool.remove(beamPool.size() - 1);
        return new Beam();
    }

    private void freeBeam(Beam bm) {
        if (!bm.active) return;
        bm.active = false;
        beamPool.add(bm);
    }

    private Bomb obtainBomb() {
        if (!bombPool.isEmpty()) return bombPool.remove(bombPool.size() - 1);
        return new Bomb();
    }

    private void freeBomb(Bomb bo) {
        if (!bo.active) return;
        bo.active = false;
        bombPool.add(bo);
    }

    private Pickup obtainPickup(float x, float y) {
        if (!pickupPool.isEmpty()) {
            Pickup p = pickupPool.remove(pickupPool.size() - 1);
            p.x = x;
            p.y = y;
            p.age = 0f;
            p.active = true;
            return p;
        }
        Pickup p = new Pickup(x, y);
        p.active = true;
        return p;
    }

    private void freePickup(Pickup p) {
        if (!p.active) return;
        p.active = false;
        pickupPool.add(p);
    }

    private void fire(Ev e) {
        float[] p = e.p;
        switch (e.k) {
            case K_ORB: {
                Bullet b = addBullet(p[0], p[1], p[2], p[3], p[4], (int) p[5]);
                if (p.length > 6) b.killR = p[6];
                break;
            }
            case K_AIM: {
                Bullet b = addBullet(p[0], p[1], 0f, 0f, p[3], (int) p[5]);
                b.aimed = true;
                b.spd = p[2];
                b.delay = p[4];
                break;
            }
            case K_HOLD: {
                Bullet b = addBullet(p[0], p[1], p[2], p[3], p[4], (int) p[8]);
                b.holdAt = p[5];
                b.holdDur = p[6];
                b.boost = p[7];
                break;
            }
            case K_BEAM: {
                Beam bm = obtainBeam();
                bm.x = p[0];
                bm.y = p[1];
                bm.ang = p[2];
                bm.thick = p[3];
                bm.warn = p[4];
                bm.on = p[5];
                bm.spin = p[6];
                bm.fake = p[7] > 0.5f;
                bm.hue = (int) p[8];
                bm.propellerBeam = p.length > 9 && p[9] < 1.5f && p[9] > 0.5f;
                bm.slash = p.length > 9 && p[9] > 1.5f;
                bm.age = 0f;
                bm.active = true;
                beams.add(bm);
                break;
            }
            case K_BOMB: {
                Bomb bo = obtainBomb();
                bo.x = p[0];
                bo.y = p[1];
                bo.R = p[2];
                bo.warn = p[3];
                bo.count = (int) p[4];
                bo.spd = p[5];
                bo.rot = p[6];
                bo.hue = (int) p[7];
                bo.age = 0f;
                bo.blown = false;
                bo.active = true;
                bombs.add(bo);
                break;
            }
            case K_FREEZE:
                freeze = p[0];
                wash = 1f;
                fxFreezeStart();
                break;
            case K_PICKUP:
                if (hp < MAX_HP) pickups.add(obtainPickup(p[0], p[1]));
                break;
            case K_BONE: {
                // p: gitna ng butas, taas, bilis, kapal, hue
                float gapC = p[0], gapH = p[1], spd = p[2], thick = p[3];
                int hue = (int) p[4];
                addBone(L + 70f, 0f, gapC - gapH / 2f, spd, thick, hue);
                addBone(L + 70f, gapC + gapH / 2f, L, spd, thick, hue);
                break;
            }
            default:
                break;
        }
    }

    // the charts

    private void ev(float atBeat, int kind, float... p) {
        Ev e = obtainEv();
        e.t = atBeat * beat;
        e.k = kind;
        e.p = p;
        evs.add(e);
    }

    private Ev obtainEv() {
        return new Ev();
    }

    // ilang column (21.4 units) ang kayang hop, base sa travel time
    private int maxShiftCols(float step, float spd) {
        float sep = spd * step * beat;
        float free = sep - 18.1f;
        if (free <= 0f) return 0;
        float reach = SPEED * 0.7f * (free / spd);
        return (int) (reach / 21.4f);
    }

    private void rain(float b0, int n, float step, float spd, boolean wide) {
        int maxShift = maxShiftCols(step, spd) + (wide ? 1 : 0);
        for (int i = 0; i < n; i++) {
            int shift = maxShift > 0 ? rng.nextInt(maxShift * 2 + 1) - maxShift : 0;
            int g = MathUtils.clamp(lastGap + shift, 1, 12);
            lastGap = g;
            for (int c = 0; c < 14; c++) {
                if (c == g || (wide && c == g + 1)) continue;
                float x = 10.7f + c * 21.4f;
                ev(b0 + i * step, K_ORB, x, L + 120f, -90f, spd, 7f, ((c + i) % 3 == 0) ? 2 : 0);
            }
        }
    }

    private void aimBurst(float b, int n, float gapBeats, float hoverBeats, int side, float spd) {
        for (int i = 0; i < n; i++) {
            float u = 0.12f + 0.76f * (n == 1 ? 0.5f : i / (float) (n - 1));
            float x, y;
            switch (side) {
                case 0: x = -70f; y = u * L; break;
                case 1: x = L + 70f; y = u * L; break;
                case 2: x = u * L; y = -70f; break;
                default: x = u * L; y = L + 100f; break;
            }
            ev(b, K_AIM, x, y, spd, 7.5f, B(hoverBeats + i * gapBeats), (i % 2 == 0) ? 1 : 2);
        }
    }

    private void holdWave(float b, int rows, float spd, float holdAtBeats, float holdBeats) {
        int gap = 1 + rng.nextInt(rows - 2);
        float rowSp = L / rows;
        for (int r = 0; r < rows; r++) {
            if (r == gap) continue;
            ev(b, K_HOLD, -60f, rowSp * (r + 0.5f), 0f, spd, 7.5f, B(holdAtBeats), B(holdBeats), 2.2f, 2f);
        }
    }

    // anim na beam, 28 lapad, 50 pitch = 22 units na ligtas
    private void lanes(float b, boolean vertical, float warnBeats, boolean fake) {
        float off = rng.nextFloat() * 20f;
        for (int i = 0; i < 6; i++) {
            float pos = 14f + off + i * 50f;
            ev(b, K_BEAM, vertical ? pos : L / 2f, vertical ? L / 2f : pos, vertical ? 90f : 0f,
                28f, B(warnBeats), 0.2f, 0f, fake ? 1f : 0f, 1f);
        }
    }

    private void spinCross(float b, float warnBeats, float durBeats, float degPerSec) {
        float a0 = rng.nextInt(4) * 22.5f;
        ev(b, K_BEAM, L / 2f, L / 2f, a0, 12f, B(warnBeats), B(durBeats), degPerSec, 0f, 1f);
        ev(b, K_BEAM, L / 2f, L / 2f, a0 + 90f, 12f, B(warnBeats), B(durBeats), degPerSec, 0f, 1f);
    }

    private void bombSet(float b, int n, float warnBeats, float spd, int count) {
        float[] bx = new float[n], by = new float[n];
        for (int i = 0; i < n; i++) {
            int tries = 0;
            do {
                bx[i] = 70f + rng.nextFloat() * 160f;
                by[i] = 70f + rng.nextFloat() * 160f;
                tries++;
            } while (tries < 20 && tooClose(bx, by, i));
            ev(b + i * 0.5f, K_BOMB, bx[i], by[i], 46f, B(warnBeats), count, spd, rng.nextFloat() * 30f, 1f);
        }
    }

    // isang bomba, sasabog warnBeats pagkatapos ng spawn
    private void bombAt(float spawnBeat, float warnBeats, float x, float y, float spd, int count) {
        ev(spawnBeat, K_BOMB, x, y, 46f, B(warnBeats), count, spd, rng.nextFloat() * 30f, 1f);
    }

    private void addBone(float x, float y0, float y1, float spd, float w, int hue) {
        float lo = Math.max(0f, y0), hi = Math.min(L, y1);
        if (hi - lo < 3f) return;
        Bone b = new Bone();
        b.x = x;
        b.y0 = lo;
        b.y1 = hi;
        b.w = w;
        b.vx = -spd;
        b.hue = hue;
        b.active = true;
        bones.add(b);
    }

    // 1:36 bone lane. isang butas lang, sundan mo. ito na ang attack
    private void boneLane(float fromBeat, float toBeat) {
        // makapal: pader kada 0.42 beat, mabilis
        float step = 0.42f;
        float spd = 245f;
        int i = 0;
        float prevC = L / 2f;
        for (float b = fromBeat; b < toBeat; b += step) {
            // wave ang butas, kaya ahas ang corridor
            float c = L / 2f + MathUtils.sin(i * 0.26f + 0.4f) * (L / 2f - 40f);
            // siguraduhing kayang abutin ang sunod kahit ganito kakapal
            c = MathUtils.clamp(c, prevC - 26f, prevC + 26f);
            prevC = c;
            float gapH = MathUtils.clamp(72f - i * 0.5f, 54f, 72f);
            ev(b, K_BONE, c, gapH, spd + 7f * (i % 3), 20f, (i & 1) == 0 ? 3 : 2);
            i++;
        }
    }

    private boolean tooClose(float[] bx, float[] by, int i) {
        for (int j = 0; j < i; j++) {
            float dx = bx[i] - bx[j], dy = by[i] - by[j];
            if (dx * dx + dy * dy < 90f * 90f) return true;
        }
        return false;
    }

    // ring na pumapalibot, isang butas. nawawala sa gitna kaya hindi na babalik
    private void implode(float b, float gapDeg, float spd, int count, int gapCount) {
        float step = 360f / count;
        float R = 262f;
        for (int i = 0; i < count; i++) {
            float ang = i * step;
            float da = Math.abs(((ang - gapDeg + 540f) % 360f) - 180f);
            if (da < gapCount * step / 2f) continue;
            float rad = ang * MathUtils.degreesToRadians;
            ev(b, K_ORB, L / 2f + MathUtils.cos(rad) * R, L / 2f + MathUtils.sin(rad) * R,
                ang + 180f, spd, 7.5f, (i % 3 == 0) ? 2 : 0, 3f);
        }
    }

    private float nextGap(float prev) {
        float shift = (rng.nextBoolean() ? 1f : -1f) * (60f + 40f * rng.nextFloat());
        return (prev + shift + 360f) % 360f;
    }

    // stream na tatluhan, umiindayog
    private void weave(float b, int n, float stepBeats, float spd, float phase) {
        for (int i = 0; i < n; i++) {
            float x = L / 2f + MathUtils.sin(phase + i * 0.5f) * 100f;
            for (int s = -1; s <= 1; s++) {
                ev(b + i * stepBeats, K_ORB, x + s * 26f, L + 90f, -90f + s * 7f, spd, 6.5f, s == 0 ? 2 : 0);
            }
        }
    }

    private void sortEvents() {
        Collections.sort(evs, EV_COMPARATOR);
    }

    // isang chart kada sequence, relative sa simula. rain ~3.4 beats bago umabot
    private void buildSeq(int idx) {
        if (phaseThree) {
            buildSeqThree(idx);
            sortEvents();
            return;
        }
        if (phaseTwo) {
            buildSeqTwo(idx);
            sortEvents();
            return;
        }
        int d = seqDiff[idx];
        float spd = 215f + 12f * d;
        int aimN = Math.min(7, 3 + d);
        float dir = (roundNo & 1) == 0 ? 1f : -1f;
        lastGap = 7;
        float spin = dir * (58f + 8f * d);

        switch (idx) {
            case 0: {   // IGNITION: rain, beams, axes, slash lanes
                rain(0f, 3, 1f, spd, false);
                rain(3f, 3, 1f, spd, true);
                aimBurst(2.5f, aimN, 0.25f, 1.25f, 0, 245f);
                holdWave(9f, 6, 150f, 2f, 1.5f);
                bombSet(9.5f, 2, 1.5f, 150f, 14);
                slashLanes(14f, true, 2f);
                slashLanes(16.5f, false, 1.5f);
                beamPropeller(19.5f, 1.5f, 6f, Math.max(130f, Math.abs(spin) * 2.4f));
                aimBurst(21f, aimN, 0.25f, 1.25f, 1, 250f);
                ev(21f, K_PICKUP, 38f, 38f);
                aimBurst(25f, aimN, 0.25f, 1.25f, 3, 250f);
                break;
            }
            case 1: {   // LATTICE: beams, sweep, spinning cross
                lanes(0f, true, 2f, false);
                lanes(2.5f, false, 1.5f, false);
                aimBurst(1f, aimN, 0.25f, 1.25f, 2, 250f);
                rain(5f, 4, 1f, spd, false);
                lanes(6.5f, false, 1.5f, false);
                lanes(9f, false, 1.5f, false);
                holdWave(11.5f, 6, 150f, 2f, 2f);
                bombSet(12f, 3, 1.5f, 150f, 14);
                lanes(17f, true, 2f, false);
                spinCross(20f, 1.5f, 6f, spin);
                aimBurst(22f, aimN, 0.25f, 1.25f, 1, 250f);
                ev(21.5f, K_PICKUP, 262f, 38f);
                aimBurst(25.5f, aimN, 0.25f, 1.25f, 3, 250f);
                bombSet(26.5f, 2, 2f, 150f, 14);
                break;
            }
            case 2: {   // STANDSTILL: bombs, tapos tahimik. freeze, decoy beams, launch sa hit
                rain(0f, 3, 1f, spd, false);
                aimBurst(2f, aimN, 0.25f, 1.25f, 0, 250f);
                rain(3.5f, 3, 1f, spd, true);
                aimBurst(5.5f, aimN, 0.25f, 1.25f, 1, 250f);
                ev(8f, K_PICKUP, 150f, 40f);
                bombSet(7.5f, 3, 2f, 150f, 16);
                // apat na blast: beats 141/143/145/147
                bombAt(9f, 2f, 80f, 220f, 150f, 16);
                bombAt(11f, 2f, 220f, 80f, 150f, 16);
                bombAt(13f, 2f, 80f, 80f, 150f, 16);
                bombAt(15f, 2f, 220f, 220f, 150f, 16);
                aimBurst(10.75f, 2, 0.5f, 1.25f, 0, 250f);   // fires on the off-beats between blasts
                aimBurst(12.75f, 2, 0.5f, 1.25f, 1, 250f);
                aimBurst(14.75f, 2, 0.5f, 1.25f, 3, 250f);
                holdWave(17f, 6, 170f, 1f, 1.5f);             // the release: rows stall, then rocket out
                aimBurst(19.5f, aimN, 0.25f, 1.25f, 3, 250f);
                lanes(21f, true, 2f, false);
                lanes(23.5f, false, 1.5f, false);
                rain(24f, 4, 1f, spd, false);
                ev(28.5f, K_FREEZE, B(1.5f));                 // camera dives on the heart, then the burst
                break;
            }
            case 3: {   // COLLAPSE: rings, streams, decoys, spin
                float g = rng.nextFloat() * 360f;
                implode(1.5f, g, 112f, 36, 4);
                g = nextGap(g);
                implode(5.5f, g, 112f, 36, 4);
                g = nextGap(g);
                implode(9.5f, g, 112f, 36, 4);
                weave(12f, 14, 0.5f, 240f, rng.nextFloat() * MathUtils.PI2);
                aimBurst(14f, aimN, 0.25f, 1.25f, 1, 255f);
                lanes(19f, true, 2f, false);
                lanes(21.5f, false, 1.5f, false);
                ev(22f, K_PICKUP, 150f, 40f);
                lanes(24f, true, 2.5f, true);
                rain(24.5f, 4, 1f, spd, false);
                lanes(25.5f, false, 1.5f, false);
                spinCross(29f, 1.5f, 6f, spin);
                aimBurst(31f, aimN, 0.25f, 1.25f, 3, 255f);
                aimBurst(34f, aimN, 0.25f, 1.25f, 0, 255f);
                g = nextGap(g);
                implode(36.5f, g, 112f, 36, 4);
                bombSet(38f, 2, 2f, 150f, 16);
                g = nextGap(g);
                implode(40f, g, 112f, 36, 4);
                break;
            }
            default: {  // INFERNO: everything, faster, no breathing room, two heals
                // act I: walls + volleys + fake pause
                rain(1.5f, 4, 1f, spd, true);
                aimBurst(2f, aimN, 0.25f, 1.25f, 0, 260f);
                aimBurst(6.5f, aimN, 0.25f, 1.25f, 1, 260f);
                holdWave(8f, 6, 160f, 2f, 1.5f);
                bombSet(9f, 3, 1.5f, 150f, 16);
                ev(14f, K_PICKUP, 150f, 40f);
                // act II: rings, then the lattice
                float g = rng.nextFloat() * 360f;
                implode(16.5f, g, 115f, 40, 4);
                g = nextGap(g);
                implode(20.5f, g, 115f, 40, 4);
                lanes(26.5f, true, 2f, false);
                lanes(29f, false, 1.5f, false);
                lanes(31f, true, 1.5f, false);
                lanes(33f, false, 1.5f, false);
                // act III: double spin
                spinCross(35f, 1.5f, 6f, spin);
                aimBurst(37f, aimN, 0.25f, 1.25f, 3, 260f);
                aimBurst(40f, aimN, 0.25f, 1.25f, 2, 260f);
                bombSet(42f, 4, 2f, 150f, 18);
                ev(47f, K_PICKUP, 262f, 38f);
                // act IV: stream, decoys, rain curtain, freeze, closing bombs
                weave(48f, 14, 0.5f, 250f, rng.nextFloat() * MathUtils.PI2);
                aimBurst(50f, aimN, 0.25f, 1.25f, 1, 260f);
                lanes(54f, true, 2.5f, true);
                rain(54.5f, 5, 1f, spd, false);
                lanes(55.5f, false, 1.5f, false);
                lanes(58f, false, 1.5f, false);
                ev(60f, K_FREEZE, B(1.5f));
                bombSet(61f, 3, 2f, 150f, 20);
                aimBurst(62f, aimN, 0.25f, 1.25f, 0, 260f);
                lanes(63.5f, false, 1.5f, false);
                break;
            }
        }
        sortEvents();
    }

    // bawal maging phase 1 ulit ang phase 2

    // spiral palabas
    private void spiral(float b0, int steps, float stepBeats, float spd, int arms, float turn) {
        float cx = L / 2f, cy = L / 2f;
        for (int s = 0; s < steps; s++) {
            float base = s * turn;
            for (int a = 0; a < arms; a++) {
                float ang = base + a * (360f / arms);
                float rad = ang * MathUtils.degreesToRadians;
                ev(b0 + s * stepBeats, K_ORB, cx + MathUtils.cos(rad) * 16f,
                    cy + MathUtils.sin(rad) * 16f, ang, spd, 6.5f, (s + a) % 3 == 0 ? 2 : 1);
            }
        }
    }

    // bloom mula sa isang punto
    private void bloom(float b, float x, float y, int count, float spd, float phase) {
        for (int i = 0; i < count; i++) {
            float ang = phase + i * (360f / count);
            float rad = ang * MathUtils.degreesToRadians;
            ev(b, K_ORB, x + MathUtils.cos(rad) * 14f, y + MathUtils.sin(rad) * 14f,
                ang, spd, 6f, i % 2 == 0 ? 3 : 1);
        }
    }

    // pincer: dalawang pader, magkaibang butas
    private void pincer(float b, float spd, boolean horizontal) {
        int gapA = 2 + rng.nextInt(10), gapB = 2 + rng.nextInt(10);
        for (int c = 0; c < 14; c++) {
            float p = 10.7f + c * 21.4f;
            if (c != gapA) {
                if (horizontal) ev(b, K_ORB, -70f, p, 0f, spd, 7f, 0);
                else ev(b, K_ORB, p, -70f, 90f, spd, 7f, 0);
            }
            if (c != gapB) {
                if (horizontal) ev(b, K_ORB, L + 70f, p, 180f, spd, 7f, 2);
                else ev(b, K_ORB, p, L + 70f, -90f, spd, 7f, 2);
            }
        }
    }

    // zipper: talon kada row
    private void zipper(float b0, int n, float stepBeats, float spd, int gap) {
        int g = MathUtils.clamp(gap, 1, 12);
        for (int i = 0; i < n; i++) {
            g = MathUtils.clamp(g + ((i & 1) == 0 ? 3 : -2), 1, 12);
            for (int c = 0; c < 14; c++) {
                if (c == g || c == g + 1) continue;
                ev(b0 + i * stepBeats, K_ORB, 10.7f + c * 21.4f, L + 120f, -90f, spd, 7f,
                    (c + i) % 2 == 0 ? 1 : 0);
            }
        }
    }

    // gulong ng ilaw
    private void kaleido(float b, float warnBeats, float durBeats, float spin, int arms) {
        for (int i = 0; i < arms; i++) {
            ev(b, K_BEAM, L / 2f, L / 2f, i * (360f / arms), 11f, B(warnBeats), B(durBeats),
                spin, 0f, 4f);
        }
    }

    // crossfire, isa-isa ang gilid
    private void crossfire(float b0, int n, float stepBeats, float spd) {
        for (int i = 0; i < n; i++) {
            float u = 0.15f + rng.nextFloat() * 0.7f;
            float x, y;
            switch (i % 4) {
                case 0: x = -60f; y = u * L; break;
                case 1: x = L + 60f; y = u * L; break;
                case 2: x = u * L; y = -60f; break;
                default: x = u * L; y = L + 60f; break;
            }
            ev(b0 + i * stepBeats, K_AIM, x, y, spd, 6.5f, B(1.25f), (i % 2 == 0) ? 1 : 4);
        }
    }

    // apat na sequence: 20.94 / 52.31 / 82.38 / 103.30. spin sa 1:43
    private void buildSeqTwo(int idx) {
        int d = seqDiff[idx];
        float spd = 200f + 10f * d;
        float dir = (roundNo & 1) == 0 ? 1f : -1f;
        lastGap = 7;
        float spin = dir * (70f + 10f * d);

        switch (idx) {
            case 0: {   // DESCENT: the drop, bagong idea kada phrase
                spiral(0f, 18, 0.5f, 150f, 3, 21f);
                pincer(2f, 205f, true);
                crossfire(5f, 10, 1f, 245f);
                kaleido(10f, 1.5f, 6f, spin, 6);
                zipper(12f, 8, 0.5f, 220f, 4);
                bloom(15f, L / 2f, L / 2f, 24, 130f, 0f);
                ev(18f, K_PICKUP, 150f, 40f);
                pincer(19f, 210f, false);
                spiral(21f, 14, 0.5f, 158f, 4, -17f);
                crossfire(25f, 10, 1f, 250f);
                zipper(28f, 8, 0.5f, 228f, 9);
                kaleido(31f, 1.5f, 6f, -spin, 6);
                bloom(34f, 150f, 150f, 22, 135f, 9f);
                pincer(37f, 215f, true);
                spiral(39f, 16, 0.5f, 162f, 3, 19f);
                crossfire(43f, 10, 0.75f, 255f);
                zipper(45f, 8, 0.5f, 232f, 6);
                ev(48f, K_PICKUP, 262f, 38f);
                kaleido(50f, 1.25f, 6f, spin, 6);
                bloom(53f, L / 2f, L / 2f, 28, 150f, 0f);
                pincer(56f, 220f, false);
                spiral(58f, 18, 0.5f, 166f, 4, -15f);
                ev(62f, K_FREEZE, B(1.5f));
                crossfire(63f, 8, 0.5f, 260f);
                // huling act: walang pahinga
                pincer(67f, 225f, true);
                spiral(69f, 12, 0.5f, 172f, 4, 22f);
                zipper(71.5f, 8, 0.5f, 242f, 5);
                kaleido(73.5f, 1.25f, 6f, -spin * 1.2f, 8);
                crossfire(74f, 8, 0.25f, 265f);
                bloom(76.5f, L / 2f, L / 2f, 26, 190f, 0f);
                break;
            }
            case 1: {   // THRALL: malakas, tapos breakdown, tapos rebuild
                kaleido(0f, 1.5f, 5f, spin, 4);
                pincer(2f, 205f, true);
                spiral(4f, 15, 0.5f, 155f, 3, 20f);
                crossfire(8f, 10, 0.75f, 252f);
                zipper(10f, 7, 0.5f, 225f, 3);
                bloom(13f, 120f, 180f, 22, 135f, 0f);
                ev(16f, K_PICKUP, 38f, 38f);
                kaleido(17f, 1.5f, 5f, -spin, 4);
                pincer(19f, 210f, false);
                spiral(21f, 12, 0.5f, 158f, 4, -18f);
                crossfire(24f, 10, 0.75f, 215f);
                zipper(26f, 7, 0.5f, 228f, 10);
                bloom(29f, L / 2f, L / 2f, 26, 140f, 15f);
                // breakdown: mabagal, malapad, mabigat
                ev(32f, K_FREEZE, B(1f));
                bloom(34f, L / 2f, L / 2f, 16, 95f, 0f);
                pincer(37f, 145f, true);
                bloom(40f, 90f, 210f, 14, 100f, 22f);
                ev(42f, K_PICKUP, 262f, 38f);
                pincer(44f, 150f, false);
                bloom(47f, L / 2f, L / 2f, 18, 105f, 0f);
                crossfire(50f, 6, 1.5f, 200f);
                ev(53f, K_FREEZE, B(1.5f));
                // rebuild papuntang second drop
                spiral(55f, 14, 0.5f, 172f, 3, 22f);
                kaleido(59f, 1.25f, 5f, spin, 6);
                pincer(61f, 222f, true);
                zipper(63f, 8, 0.5f, 238f, 5);
                bloom(66f, L / 2f, L / 2f, 28, 160f, 0f);
                spiral(68f, 14, 0.5f, 178f, 3, -24f);
                zipper(70.5f, 8, 0.5f, 248f, 4);
                crossfire(71f, 6, 0.25f, 268f);
                ev(73f, K_FREEZE, B(1.5f));
                break;
            }
            case 2: {   // VORTEX: lahat umiikot
                spiral(0f, 20, 0.5f, 165f, 4, 26f);
                kaleido(2f, 1.25f, 6f, spin * 1.4f, 6);
                zipper(4f, 8, 0.5f, 235f, 7);
                bloom(7f, L / 2f, L / 2f, 30, 150f, 0f);
                pincer(9f, 225f, true);
                spiral(11f, 18, 0.5f, 170f, 3, -24f);
                crossfire(14f, 10, 0.75f, 230f);
                ev(16f, K_PICKUP, 150f, 40f);
                kaleido(17f, 1.25f, 6f, -spin * 1.4f, 6);
                zipper(19f, 8, 0.5f, 240f, 3);
                bloom(22f, 90f, 210f, 24, 155f, 18f);
                spiral(24f, 16, 0.5f, 175f, 4, 27f);
                pincer(27f, 230f, false);
                crossfire(29f, 10, 0.75f, 270f);
                ev(32f, K_PICKUP, 262f, 38f);
                kaleido(33f, 1.25f, 5f, spin * 1.6f, 8);
                zipper(35f, 8, 0.5f, 245f, 9);
                bloom(38f, L / 2f, L / 2f, 32, 165f, 0f);
                spiral(40f, 16, 0.5f, 180f, 3, -28f);
                ev(44f, K_FREEZE, B(1.5f));
                crossfire(44.5f, 6, 0.25f, 275f);
                bloom(46f, L / 2f, L / 2f, 22, 195f, 0f);
                break;
            }
            default: {  // COLLAPSE: the box leaves its frame; everything escalates toward 1:58
                float g = rng.nextFloat() * 360f;
                // act I
                spiral(0f, 16, 0.5f, 175f, 4, 25f);
                kaleido(2f, 1.25f, 6f, spin * 1.5f, 6);
                zipper(4f, 8, 0.5f, 250f, 6);
                bloom(7f, L / 2f, L / 2f, 30, 160f, 0f);
                pincer(9f, 235f, true);
                crossfire(11f, 10, 0.75f, 245f);
                implode(14f, g, 118f, 40, 4);
                ev(16f, K_PICKUP, 38f, 38f);
                kaleido(17f, 1.25f, 6f, -spin * 1.7f, 8);
                zipper(19f, 8, 0.5f, 255f, 4);
                bloom(22f, 90f, 210f, 26, 165f, 15f);
                spiral(24f, 14, 0.5f, 185f, 4, 28f);
                g = nextGap(g);
                implode(28f, g, 120f, 40, 4);
                pincer(30f, 240f, false);
                // act II
                kaleido(33f, 1.25f, 6f, spin * 1.9f, 6);
                ev(34f, K_PICKUP, 262f, 38f);
                zipper(35f, 8, 0.5f, 260f, 10);
                bloom(38f, L / 2f, L / 2f, 34, 170f, 0f);
                spiral(40f, 8, 0.5f, 190f, 3, -30f);
                ev(44f, K_FREEZE, B(1.5f));
                crossfire(45.5f, 10, 0.5f, 285f);
                kaleido(48f, 1.25f, 6f, -spin * 2f, 8);
                zipper(50f, 8, 0.5f, 265f, 7);
                bloom(53f, 120f, 180f, 30, 175f, 9f);
                g = nextGap(g);
                implode(55f, g, 122f, 40, 4);
                spiral(57f, 8, 0.5f, 195f, 4, 30f);
                // act III: walang hininga
                ev(60f, K_PICKUP, 150f, 40f);
                kaleido(61f, 1f, 6f, spin * 2.2f, 8);
                pincer(63f, 250f, true);
                crossfire(65f, 12, 0.5f, 255f);
                bloom(68f, L / 2f, L / 2f, 36, 180f, 0f);
                spiral(70f, 12, 0.5f, 200f, 3, -32f);
                zipper(74f, 8, 0.5f, 270f, 5);
                kaleido(77f, 1f, 5f, -spin * 2.4f, 8);
                bloom(80f, 90f, 210f, 30, 185f, 20f);
                spiral(82f, 8, 0.5f, 205f, 4, 34f);
                ev(86f, K_PICKUP, 38f, 262f);
                pincer(88f, 255f, false);
                crossfire(90f, 12, 0.5f, 295f);
                // act IV: pigil hininga, tapos boom sa 136.0s
                kaleido(92f, 1f, 4f, spin * 2.6f, 10);
                ev(97f, K_FREEZE, B(2f));
                g = nextGap(g);
                implode(99f, g, 125f, 40, 4);
                spiral(99.5f, 14, 0.25f, 215f, 5, 18f);
                crossfire(100.5f, 10, 0.5f, 305f);
                bloom(104f, L / 2f, L / 2f, 36, 190f, 0f);
                zipper(105f, 6, 0.2f, 285f, 4);
                kaleido(105.5f, 0.5f, 3f, spin * 3f, 10);
                break;
            }
        }
    }

    private void buildSeqThree(int idx) {
        int d = seqDiff[idx];
        float spd = 190f + 11f * d;
        float spin = ((roundNo & 1) == 0 ? 1f : -1f) * (65f + 9f * d);
        lastGap = 7;

        switch (idx) {
            case 0: {   // MAYDAY: isang mabigat kada beat, tapos gitara
                for (int i = 0; i < 12; i++) {
                    bombAt(i * 1.5f, 1.1f, 55f + (i * 71f) % 190f, 55f + (i * 47f) % 190f,
                        spd + 5f, 10);
                }
                aimBurst(1f, 4, 1f, 1.2f, 0, spd);
                aimBurst(6f, 4, 1f, 1.2f, 2, spd);
                ev(10f, K_PICKUP, 150f, 40f);
                zipper(13f, 8, 0.5f, spd + 12f, 4);
                kaleido(15f, 1.25f, 5f, spin, 6);
                crossfire(17f, 8, 0.75f, spd + 22f);
                pincer(21f, spd + 18f, true);
                spiral(23f, 12, 0.5f, spd + 8f, 3, 20f);
                bloom(25f, L / 2f, L / 2f, 22, 150f, 0f);
                ev(27f, K_PICKUP, 262f, 38f);
                kaleido(28f, 1.25f, 5f, -spin, 8);
                zipper(30f, 8, 0.45f, spd + 26f, 8);
                crossfire(32f, 10, 0.5f, spd + 30f);
                implode(35f, rng.nextFloat() * 360f, 118f, 36, 4);
                pincer(37f, spd + 32f, false);
                bloom(39f, 90f, 210f, 24, 165f, 14f);
                spiral(41f, 10, 0.4f, spd + 16f, 4, -24f);
                crossfire(43f, 8, 0.3f, spd + 36f);
                ev(44f, K_FREEZE, B(1f));
                break;
            }
            case 1: {   // RUPTURE: pader na palapit sa dalawang gilid
                kaleido(0f, 1.25f, 5f, spin, 6);
                pincer(2f, spd + 10f, true);
                spiral(5f, 14, 0.5f, spd + 5f, 4, -20f);
                crossfire(9f, 10, 0.5f, spd + 24f);
                zipper(11f, 8, 0.5f, spd + 20f, 3);
                bloom(14f, L / 2f, L / 2f, 26, 155f, 0f);
                ev(16f, K_PICKUP, 38f, 38f);
                kaleido(17f, 1.25f, 5f, -spin, 8);
                pincer(19f, spd + 15f, false);
                spiral(22f, 12, 0.5f, spd + 12f, 3, 22f);
                crossfire(25f, 10, 0.5f, spd + 28f);
                zipper(27f, 8, 0.45f, spd + 24f, 9);
                implode(30f, rng.nextFloat() * 360f, 120f, 40, 4);
                bloom(33f, 90f, 210f, 24, 165f, 18f);
                pincer(35f, spd + 24f, true);
                crossfire(37f, 12, 0.35f, spd + 32f);
                ev(39f, K_PICKUP, 262f, 38f);
                spiral(39f, 12, 0.5f, spd + 18f, 4, -26f);
                ev(41f, K_FREEZE, B(1f));
                break;
            }
            case 2: {   // STATIC: 1:36, bone lane
                spiral(0f, 16, 0.5f, spd + 6f, 4, 24f);
                kaleido(3f, 1f, 5f, spin * 1.2f, 8);
                zipper(5f, 8, 0.5f, spd + 26f, 6);
                crossfire(7f, 10, 0.5f, spd + 30f);
                bloom(9f, L / 2f, L / 2f, 28, 160f, 0f);
                pincer(11f, spd + 26f, true);
                ev(13f, K_PICKUP, 150f, 40f);
                kaleido(14f, 1f, 5f, -spin * 1.4f, 8);
                spiral(16f, 14, 0.4f, spd + 14f, 3, -28f);
                crossfire(19f, 12, 0.4f, spd + 34f);
                implode(22f, rng.nextFloat() * 360f, 122f, 40, 4);
                zipper(25f, 8, 0.4f, spd + 32f, 4);
                bloom(27f, 90f, 210f, 26, 170f, 20f);
                ev(29f, K_FREEZE, B(1.2f));
                // 1:36 remix: buto lang, walang beam, walang orb
                boneLane(35.5f, 46.4f);
                ev(46.1f, K_PICKUP, 150f, 40f);
                break;
            }
            case 3: {   // SIGNAL: lanes, fakes, rings hanggang 2:14
                lanes(0f, true, 1.5f, false);
                kaleido(2f, 1.25f, 5f, spin, 7);
                spiral(4f, 14, 0.5f, spd + 8f, 4, 22f);
                zipper(7f, 8, 0.5f, spd + 22f, 5);
                ev(9f, K_PICKUP, 262f, 38f);
                crossfire(10f, 10, 0.5f, spd + 26f);
                bloom(12f, L / 2f, L / 2f, 26, 150f, 0f);
                pincer(14f, spd + 20f, false);
                lanes(16f, false, 1.5f, true);
                implode(18f, rng.nextFloat() * 360f, 120f, 40, 4);
                kaleido(20f, 1f, 5f, -spin, 8);
                spiral(22f, 12, 0.4f, spd + 16f, 3, -26f);
                ev(24f, K_FREEZE, B(1f));
                crossfire(25f, 10, 0.35f, spd + 30f);
                zipper(27f, 8, 0.4f, spd + 28f, 7);
                bloom(29f, 90f, 210f, 24, 165f, 16f);
                pincer(31f, spd + 26f, true);
                ev(33f, K_PICKUP, 38f, 38f);
                lanes(34f, true, 1.2f, false);
                crossfire(35f, 12, 0.3f, spd + 36f);
                spiral(37f, 12, 0.3f, spd + 22f, 4, 30f);
                kaleido(39f, 0.75f, 4f, spin * 1.5f, 8);
                bloom(41f, L / 2f, L / 2f, 26, 175f, 0f);
                break;
            }
            default: {  // SIGAW: the last scream, with one held breath before the end
                spiral(0f, 18, 0.5f, spd + 10f, 4, 26f);
                kaleido(3f, 1f, 5f, spin * 1.3f, 8);
                pincer(5f, spd + 28f, true);
                crossfire(7f, 12, 0.5f, spd + 34f);
                zipper(9f, 8, 0.4f, spd + 30f, 4);
                bloom(11f, L / 2f, L / 2f, 30, 170f, 0f);
                ev(13f, K_PICKUP, 150f, 40f);
                implode(15f, rng.nextFloat() * 360f, 125f, 42, 4);
                kaleido(18f, 0.75f, 4f, -spin * 1.6f, 10);
                spiral(20f, 12, 0.4f, spd + 20f, 3, -30f);
                ev(23f, K_FREEZE, B(1.2f));
                crossfire(24f, 10, 0.35f, spd + 38f);
                bloom(26f, 90f, 210f, 26, 175f, 20f);
                zipper(28f, 8, 0.35f, spd + 36f, 9);
                pincer(30f, spd + 34f, false);
                ev(32f, K_PICKUP, 262f, 38f);
                crossfire(33f, 12, 0.3f, spd + 42f);
                kaleido(35f, 0.75f, 4f, spin * 1.8f, 10);
                spiral(36f, 14, 0.35f, spd + 26f, 4, 34f);
                bloom(38f, L / 2f, L / 2f, 32, 185f, 0f);
                implode(40f, rng.nextFloat() * 360f, 128f, 44, 4);
                pincer(42f, spd + 38f, true);
                zipper(44f, 8, 0.3f, spd + 40f, 6);
                crossfire(46f, 14, 0.25f, spd + 46f);
                ev(48f, K_PICKUP, 38f, 262f);
                kaleido(49f, 0.5f, 4f, -spin * 2f, 10);
                spiral(50f, 12, 0.25f, spd + 30f, 5, -36f);
                bloom(52f, L / 2f, L / 2f, 34, 190f, 0f);
                crossfire(53f, 6, 0.25f, spd + 50f);
                break;
            }
        }
    }

    // screen effects

    private void clearFx() {
        trauma = fxTilt = fxTiltV = fxZoom = fxZoomV = fxPanX = fxPanY = fxPanVX = fxPanVY = 0f;
        flashV = glitchT = tension = tensF = spinLean = spinLeanSm = holdTilt = 0f;
        focusSm = impactT = impactP = focusBurst = 0f;
        squeeze = 0f;
        wallIdx = -1;
        tpX = tpY = tpR = tpTX = tpTY = tpTR = tpW = tpStreakT = tpFlash = surgeF = 0f;
        camX = camY = camVX = camVY = ghostT = 0f;
        tpFired = -1;
        lastHalf = -1;
        menuIdx = -1;
        pickKind = -1;
        pickT = -1f;
        pickDone = false;
        outX = outY = outRot = 0f;
        outScale = 1f;
        spinAng = spinT = spinBoomT = 0f;
        spinDir = 1f;
        spinMoveT = spinMoveLen = 0f;
        spinFromX = spinFromY = spinToX = spinToY = 0f;
        for (int i = 0; i < CRN; i++) cLife[i] = 0f;
        for (int i = 0; i < SN; i++) shLife[i] = 0f;
        cueIdx = 0;
        lastCueT = -1f;
        monochromeCue = false;
    }

    private float songTime() {
        float s = visT % songLen;
        return s < 0f ? s + songLen : s;
    }

    private static float table(float[] t, float x) {
        return BattleChart.table(t, x);
    }

    private float cueTime(int i) { return BattleChart.cueTime(offset, beat, cues, i); }

    private int firstCueAfter(float st) {
        int n = cues.length / 3;
        for (int i = 0; i < n; i++) if (cueTime(i) > st) return i;
        return n;
    }

    private boolean cueNear(float st) {
        int n = cues.length / 3;
        for (int i = 0; i < n; i++) {
            if (Math.abs(cueTime(i) - st) < 0.3f && cues[i * 3 + 1] >= 0.7f) return true;
        }
        return false;
    }

    private float noise(float seed, float t) {
        return (MathUtils.sin(t * 43.7f + seed * 12.9f)
            + 0.7f * MathUtils.sin(t * 71.3f + seed * 78.2f + 1.7f)
            + 0.5f * MathUtils.sin(t * 113.1f + seed * 37.7f + 4.2f)) / 2.2f;
    }

    private float hash(int n) {
        n = (n << 13) ^ n;
        n = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
        return n / 2147483647f;
    }

    private void addTrauma(float amount) { trauma = Math.min(1.4f, trauma + amount * TRAUMA_GAIN); }

    private void panKick(float amount) {
        float a = rng.nextFloat() * MathUtils.PI2;
        fxPanVX += MathUtils.cos(a) * amount * PAN_GAIN;
        fxPanVY += MathUtils.sin(a) * amount * 0.7f * PAN_GAIN;
    }

    private void newTilt(float degrees) {
        tiltSign = -tiltSign;
        holdTilt = tiltSign * degrees;
        fxTiltV += tiltSign * degrees * 16f;
    }

    private boolean fxOn() { return fxLive && fxMode != FX_OFF; }

    // teleport set-piece + menu pick

    private float tpTime(int i) { return offset + tpBeats[i] * beat - TP_NUDGE; }

    // pinakamalaking scale na kasya pa rin kahit naka-tilt
    private float fitScale(float rotDeg, float panX, float panY) {
        return BattleChart.fitScale(aw, ah, ax + tpX + aw / 2f + panX, ay + tpY + ah / 2f + panY,
            rotDeg, scrX, scrY, scrW, scrH, FIT_MARGIN);
    }

    // f sa -1..1 ng libreng space
    private float roomFit(float f, float half, float c0, float scrHalf) {
        return BattleChart.roomFit(f, half, c0, scrHalf, FIT_MARGIN);
    }

    private static final float TP_FOLLOW = 0.8f;   // camera ends up this far toward the arena

    // san tatama ang arena sa blast i
    private void station(int i) {
        float[] st = TP_ST[i];
        float sinr = MathUtils.sinDeg(Math.abs(st[2])), cosr = MathUtils.cosDeg(st[2]);
        float hw = ((aw / 2f) * cosr + (ah / 2f) * sinr) * 1.08f;
        float hh = ((aw / 2f) * sinr + (ah / 2f) * cosr) * 1.08f;
        float sx = safeSet ? safeX : scrX + scrW * 0.12f;
        float sy = safeSet ? safeY : scrY + scrH * 0.14f;
        float sw = safeSet ? safeW : scrW * 0.76f;
        float sh2 = safeSet ? safeH : scrH * 0.72f;
        float lox = sx + hw, hix = sx + sw - hw, loy = sy + hh, hiy = sy + sh2 - hh;
        float mx = (lox + hix) / 2f, my = (loy + hiy) / 2f;
        float rx = Math.max(0f, (hix - lox) / 2f), ry = Math.max(0f, (hiy - loy) / 2f);
        tpTX = mx + st[0] * rx - (ax + aw / 2f);
        tpTY = my + st[1] * ry - (ay + ah / 2f);
        tpTR = st[2];
    }

    // sequence III (141/143/145/147): teleport ang arena, hinahabol ng camera
    // 1:43: bitaw na ang arena
    private boolean spinFinaleActive() {
        return phaseTwo && seqIdx == seq.length - 1 && state == ST_LIVE && fxLive;
    }

    // true habang tumatakbo ang finale
    public boolean spinFinale() {
        return spinFinaleActive();
    }

    // 0..1 sa spin finale
    public float finaleProgress() {
        return spinFinaleActive() ? MathUtils.clamp(roundT / Math.max(0.01f, roundLen), 0f, 1f) : 0f;
    }

    // isang leg ng spin, sa beats
    private static final float SPIN_LEG_BEATS = 10f;
    // px na iniwan sa gilid
    private static final float SPIN_MARGIN = 30f;

    // 1:43: kawala ang box. umiikot at lumilipad, camera nakapirme
    private void tickSpinFinale(float d) {
        float prog = finaleProgress();
        spinT += d;
        spinAng += d * spinDir * (17f + 9f * secLevel) * (1f + 1.15f * prog);

        // gaano kalayo pwede ang umiikot na box
        float halfDiag = (aw / 2f) * (Math.abs(MathUtils.cosDeg(spinAng))
            + Math.abs(MathUtils.sinDeg(spinAng)));
        float roomX = Math.max(0f, scrW / 2f - SPIN_MARGIN - halfDiag);
        float roomY = Math.max(0f, scrH / 2f - SPIN_MARGIN - halfDiag);

        spinMoveT += d;
        if (spinMoveT >= spinMoveLen) {
            spinFromX = tpTX;
            spinFromY = tpTY;
            spinToX = spinStop(spinFromX, roomX);
            spinToY = spinStop(spinFromY, roomY);
            spinMoveT = 0f;
            spinMoveLen = SPIN_LEG_BEATS * beat * (0.85f + 0.35f * prog);
            spinDir = -spinDir;
            addTrauma(0.2f + 0.25f * prog);
            addRing(L / 2f, L / 2f, 20f, 260f, 0.45f, 3, 0.55f, false);
            // speed lines
            tpStreakAng = MathUtils.atan2(spinToY - spinFromY, spinToX - spinFromX)
                * MathUtils.radiansToDegrees;
            tpStreakT = 0.22f;
        }

        // ease para may launch at settle
        float u = MathUtils.clamp(spinMoveT / Math.max(0.05f, spinMoveLen), 0f, 1f);
        float e = u * u * (3f - 2f * u);
        tpX = tpTX = spinFromX + (spinToX - spinFromX) * e;
        tpY = tpTY = spinFromY + (spinToY - spinFromY) * e;

        // box ang gumagalaw, hindi camera
        camVX += (-62f * camX - 8.5f * camVX) * d;
        camVY += (-62f * camY - 8.5f * camVY) * d;
        camX += camVX * d;
        camY += camVY * d;

        spinBoomT = Math.max(0f, spinBoomT - d);
        if (thumpV > 0.7f && spinBoomT <= 0f) {
            spinBoomT = 0.26f;
            addTrauma(0.6f + 0.5f * prog);
            fxZoom += 0.08f + 0.06f * prog;
            flashV = Math.max(flashV, 0.42f + 0.22f * prog);
            fxImpact(0.8f);
            addRing(L / 2f, L / 2f, 24f, 520f, 0.5f, 1, 0.85f, false);
            if (prog > 0.5f) addRing(L / 2f, L / 2f, 24f, 640f, 0.7f, 4, 0.7f, false);
            if (prog > 0.3f) shatterBox(3, 0.5f + 0.5f * prog);
        }
    }

    // bagong stop, dapat malayo sa dati
    private float spinStop(float from, float room) {
        float t = (rng.nextFloat() * 2f - 1f) * room;
        if (room > 1f && Math.abs(t - from) < room) {
            t = (from >= 0f ? -room : room) * (0.55f + 0.45f * rng.nextFloat());
        }
        return t;
    }

    private void tickTeleport(float d, float st) {
        if (tpBeats.length == 0) {
            if (spinFinaleActive()) {
                tickSpinFinale(d);
            } else {
                // pahingahin ang arena
                tpTX = tpTY = tpTR = 0f;
                float e = 1f - (float) Math.exp(-5.5f * d);
                tpX += (0f - tpX) * e;
                tpY += (0f - tpY) * e;
                tpR += (0f - tpR) * e;
                float snap = Math.round(spinAng / 90f) * 90f;
                spinAng += (snap - spinAng) * (1f - (float) Math.exp(-14f * d));
                if (Math.abs(snap - spinAng) < 0.3f) spinAng = 0f;
                camVX += (-62f * camX - 8.5f * camVX) * d;
                camVY += (-62f * camY - 8.5f * camVY) * d;
                camX += camVX * d;
                camY += camVY * d;
            }
            tpW += (0f - tpW) * Math.min(1f, d * 2.5f);
            tpStreakT = Math.max(0f, tpStreakT - d);
            tpFlash = Math.max(0f, tpFlash - d * 9f);
            ghostT = Math.max(0f, ghostT - d);
            return;
        }
        int last = tpBeats.length - 1;
        float tStart = tpTime(0) - TP_LEAD * beat;
        float tEnd = tpTime(last) + TP_HOLD * beat;
        boolean inSeq = state == ST_LIVE && seqIdx == 2;
        boolean win = fxOn() && inSeq && st >= tStart && st < tEnd;
        surgeF = (inSeq && st >= tpTime(last) && st < offset + seq[2][1] * beat) ? 1f : 0f;
        if (st < tStart) tpFired = -1;

        if (fxOn() && inSeq && tpFired == last && st >= tEnd && st < tEnd + 0.3f) {
            tpFired = tpBeats.length;                  // released: glide home and keep the energy UP
            fxSlam(1f);
            fxImpact(1f);
            tpFlash = 1f;
        }

        if (win) {
            if (st < tpTime(0)) {                       // pre-lean: the camera starts to tip
                float u = MathUtils.clamp(1f - (tpTime(0) - st) / (TP_LEAD * beat), 0f, 1f);
                tpTX = tpTY = 0f;
                tpTR = TP_ST[0][2] * 0.4f * u;
            }
            while (tpFired + 1 < tpBeats.length && st >= tpTime(tpFired + 1)) {
                int i = ++tpFired;
                if (st - tpTime(i) < 0.25f) fireTeleport(i);
            }
        } else {
            tpTX = tpTY = tpTR = 0f;
        }

        boolean held = win && tpFired >= 0;
        if (held) {
            tpX = tpTX;                                 // teleport: no travel time
            tpY = tpTY;
        } else {
            float ae = 1f - (float) Math.exp(-5.5f * d);
            tpX += (tpTX - tpX) * ae;
            tpY += (tpTY - tpY) * ae;
        }
        tpR += (tpTR - tpR) * (1f - (float) Math.exp(-(held ? 14f : 5.5f) * d));

        // chase: konting overshoot para ramdam
        camVX += (-62f * (camX - tpX * TP_FOLLOW) - 8.5f * camVX) * d;
        camVY += (-62f * (camY - tpY * TP_FOLLOW) - 8.5f * camVY) * d;
        camX += camVX * d;
        camY += camVY * d;

        tpW += ((win ? 1f : 0f) - tpW) * Math.min(1f, d * (win ? 6f : 2.5f));
        tpStreakT = Math.max(0f, tpStreakT - d);
        tpFlash = Math.max(0f, tpFlash - d * 9f);
        ghostT = Math.max(0f, ghostT - d);
    }

    private void fireTeleport(int i) {
        float ox = tpX, oy = tpY;
        station(i);
        ghX0 = ox;
        ghY0 = oy;
        ghX1 = tpTX;
        ghY1 = tpTY;
        ghostT = 0.4f;
        float dx = tpTX - ox, dy = tpTY - oy;
        tpX = tpTX;                                     // the arena is already there
        tpY = tpTY;
        camVX += dx * TP_FOLLOW * 3f;                   // camera lurches after it
        camVY += dy * TP_FOLLOW * 3f;
        tpStreakAng = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        tpStreakT = 0.28f;
        tpFlash = 1f;
        fxTiltV += (dx > 0f ? -1f : 1f) * 150f;
        float p = (i == tpBeats.length - 1) ? 1f : 0.7f + 0.1f * i;
        addTrauma(0.35f * p);
        fxZoom += 0.07f * p;
        flashV = Math.max(flashV, 0.4f * p);
        glitchT = Math.max(glitchT, 0.12f);
        impactT = IMPACT_TIME * 0.7f;
        impactP = 0.8f * p;
        addRing(L / 2f, L / 2f, 10f, 460f, 0.55f, (i & 1) == 0 ? 1 : 3, 0.9f, false);
        sparks(hx, hy, 30, 240f, 0.5f, 3f, 0.95f, 0.9f, 0.82f);
        shatterBox(14 + 6 * i, p);
    }

    private void tickMenuPick() {
        if (pickT >= 0f && !pickDone && visT >= pickT) {
            pickDone = true;
            fxPick(pickKind);
        }
    }

    // punch pag may napili
    private void fxPick(int kind) {
        sparks(hx, hy, 22, 170f, 0.5f, 3f, 0.95f, 0.9f, 0.82f);
        if (kind == 0) {                                // ATTACK: red slap out of the heart
            addRing(hx, hy, 4f, 220f, 0.45f, 1, 0.9f, false);
        } else if (kind == 1) {                         // DEFEND: the box draws in
            addRing(L / 2f, L / 2f, 300f, 20f, 0.5f, 3, 0.8f, false);
        } else {                                        // REASON: violet ripples
            addRing(hx, hy, 4f, 180f, 0.6f, 4, 0.8f, false);
            addRing(hx, hy, 4f, 120f, 0.45f, 4, 0.6f, false);
        }
        if (!fxOn()) return;
        if (kind == 0) {
            addTrauma(0.3f);
            fxZoom += 0.06f;
            flashV = Math.max(flashV, 0.35f);
            fxTiltV += 60f;
        } else if (kind == 1) {
            newTilt(3f);
            fxZoom += 0.03f;
            flashV = Math.max(flashV, 0.15f);
        } else {
            glitchT = Math.max(glitchT, 0.3f);
            flashV = Math.max(flashV, 0.2f);
            fxTiltV -= 50f;
        }
    }

    private void tickFx(float d) {
        float st = songTime();
        float target = table(sect, st);
        secLevel += (target - secLevel) * Math.min(1f, d * (target > secLevel ? 14f : 3f));

        // phase 3: humihinga ang pader
        float sq = 0f;
        int wallNow = -1;
        if (phaseThree) {
            for (int i = 0; i < P3_WALL.length; i += 3) {
                float a = P3_WALL[i], b = P3_WALL[i + 1], dep = P3_WALL[i + 2];
                if (st >= a && st < b) {
                    float edge = Math.min((st - a) / 0.9f, (b - st) / 0.9f);
                    sq = Math.max(sq, dep * MathUtils.clamp(edge, 0f, 1f));
                    wallNow = i / 3;
                }
            }
        }
        if (wallNow >= 0 && wallNow != wallIdx) {
            wallIdx = wallNow;
            addTrauma(0.55f);
            fxZoom += 0.07f;
            flashV = Math.max(flashV, 0.3f);
            glitchT = Math.max(glitchT, 0.22f);
            shatterBox(26, 0.9f);
            addRing(L / 2f, L / 2f, 214f, 22f, 0.5f, 3, 0.7f, false);
            for (int s = 0; s < 4; s++) {
                float ang = s * 90f;
                sparks(L / 2f + MathUtils.cosDeg(ang) * 140f, L / 2f + MathUtils.sinDeg(ang) * 140f,
                    10, 190f, 0.45f, 2.6f, 0.95f, 0.93f, 1f);
            }
        } else if (wallNow < 0) {
            wallIdx = -1;
        }
        squeeze += (sq - squeeze) * Math.min(1f, d * (sq > squeeze ? 4.5f : 6f));

        float tn = 0f;
        for (int i = 0; i < tens.length; i += 2) {
            if (st >= tens[i] && st < tens[i + 1]) {
                float u = (st - tens[i]) / (tens[i + 1] - tens[i]);
                tn = u * u;
            }
        }
        tensF += ((freeze > 0f ? 1f : 0f) - tensF) * Math.min(1f, d * 8f);
        tn = Math.max(tn, tensF * 0.8f);
        tn = Math.max(tn, menuPressure() * 0.85f);
        tension = tn > tension ? tension + (tn - tension) * Math.min(1f, d * 10f) : tn;

        // heart focus: dive sa puso
        float focusTarget = 0f;
        if (fxLive) {
            if (state == ST_OPENING && openingStill) {
                float u = MathUtils.clamp((visT - drainTime) / (dropTime - drainTime), 0f, 1f);
                float dive = 1f - (1f - u) * (1f - u) * (1f - u);
                focusTarget = Math.max(tension, dive);
            } else if (state == ST_PRE && seqIdx >= 1) {
                float left = seqStartT - visT;
                float u = MathUtils.clamp(1f - left / (5f * beat), 0f, 1f);
                focusTarget = u * u;
            }
            focusTarget = Math.max(focusTarget, tensF);
            if (pickT >= 0f) focusTarget = Math.max(focusTarget, menuPressure());
            focusBurst = Math.max(0f, focusBurst - d * 2.4f);
            focusTarget = Math.max(focusTarget, focusBurst);
        }
        focusSm += (focusTarget - focusSm) * Math.min(1f, d * (focusTarget > focusSm ? 6f : 22f));

        impactT = Math.max(0f, impactT - d);
        fxBlend += ((fxLive ? 1f : 0f) - fxBlend) * Math.min(1f, d * (fxLive ? 8f : 3.5f));

        int nCues = cues.length / 3;
        if (st < lastCueT - 0.4f) {
            cueIdx = firstCueAfter(st);
        } else {
            while (cueIdx < nCues && cueTime(cueIdx) <= st) {
                if (st - cueTime(cueIdx) < 0.25f) fireCue(cueIdx);
                cueIdx++;
            }
        }
        lastCueT = st;
        tickTeleport(d, st);
        tickMenuPick();

        // konting rock kada downbeat
        int barIdx = (int) Math.floor((visT - offset) / (beat * 4f));
        if (barIdx != lastBar) {
            if (lastBar >= 0 && state == ST_LIVE && fxOn()) barFx();
            lastBar = barIdx;
        }

        int half = (int) Math.floor((visT - offset) / (beat * 0.5f));
        if (half != lastHalf) {
            if (lastHalf >= 0 && surgeF > 0.5f && state == ST_LIVE && fxOn()) {
                fxZoom += 0.018f;
                addTrauma(0.05f);
                barFlip = -barFlip;
                fxTiltV += barFlip * 55f;
            }
            lastHalf = half;
        }

        if (!fxLive) {
            holdTilt = spinLean = spinLeanSm = 0f;
        }

        if (state != ST_LIVE && state != ST_OPENING) spinLean = 0f;
        spinLeanSm += (spinLean - spinLeanSm) * Math.min(1f, d * 4f);
        holdTilt *= Math.max(0f, 1f - 0.5f * d);
        float hold = fxLive && secLevel >= 0.15f ? holdTilt : 0f;
        float side = (roundNo & 1) == 0 ? 1f : -1f;
        float lean = fxLive ? (tension * 8f + focusSm * 9f) * side : 0f;
        float sway = fxLive ? MathUtils.sin((visT - offset) / beat * MathUtils.PI / 4f)
            * (5f + 4f * surgeF) * secLevel : 0f;
        float steer = (fxLive && (state == ST_LIVE || state == ST_OPENING))
            ? -((hx - L / 2f) / (L / 2f)) * 5f * secLevel : 0f;
        float calm = 1f - 0.85f * tpW;
        float tiltTarget = MathUtils.clamp((hold + spinLeanSm + lean + sway + steer) * calm, -TILT_MAX, TILT_MAX);

        fxTiltV += (-90f * (fxTilt - tiltTarget) - 10f * fxTiltV) * d;
        fxTilt = MathUtils.clamp(fxTilt + fxTiltV * d, -TILT_LIMIT, TILT_LIMIT);
        fxZoomV += (-70f * fxZoom - 11f * fxZoomV) * d;
        fxZoom += fxZoomV * d;
        fxPanVX += (-120f * fxPanX - 12f * fxPanVX) * d;
        fxPanVY += (-120f * fxPanY - 12f * fxPanVY) * d;
        fxPanX += fxPanVX * d;
        fxPanY += fxPanVY * d;
        trauma = Math.max(0f, trauma - d * (1.1f + 0.5f * (1f - secLevel)));
        flashV = Math.max(0f, flashV - d * 3.2f);
        glitchT = Math.max(0f, glitchT - d);

        float fs = fxMode == FX_FULL ? 1f : fxMode == FX_HALF ? 0.5f : 0f;
        float tr = Math.min(1f, trauma);
        float sh = Math.min(1f, tr * tr + (fxLive ? tension * tension * 0.12f : 0f));
        outX = fs * (sh * FX_SHAKE_X * noise(1f, animT) + fxPanX - camX);
        outY = fs * (sh * FX_SHAKE_Y * noise(2f, animT) + fxPanY - camY);
        outRot = fs * (fxTilt + tpR + sh * FX_SHAKE_ROT * noise(3f, animT));
        // lulusot ang arena pag tilt, para ramdam ang lunge
        float want = 1f + fs * Math.max(0f, fxZoom
            + (fxLive ? tension * 0.05f + 0.03f * kickEnv * secLevel : 0f)
            + (Math.abs(fxTilt) + Math.abs(tpR)) * 0.004f + 0.03f * tpW);
        outScale = fs == 0f ? 1f : want;

        for (int i = 0; i < CRN; i++) if (cLife[i] > 0f) {
            cAge[i] += d;
            if (cAge[i] >= cLife[i]) cLife[i] = 0f;
        }
        for (int i = 0; i < SN; i++) if (shLife[i] > 0f) {
            shLife[i] -= d;
            shVY[i] -= 760f * d;
            shX[i] += shVX[i] * d;
            shY[i] += shVY[i] * d;
            shR[i] += shRV[i] * d;
        }
    }

    private void kickFx() {
        if (!fxOn() || (state == ST_OPENING && openingStill)) return;
        float lv = secLevel;
        addTrauma(0.03f + 0.10f * lv * lv);
        fxZoom += 0.005f + 0.026f * lv;
        fxPanVY += (30f + 160f * lv) * 0.5f;
    }

    private void barFx() {
        float lv = secLevel;
        fxZoom += 0.005f + 0.02f * lv;
        barFlip = -barFlip;
        fxTiltV += barFlip * (20f + 70f * lv);
        if (seqIdx >= 3) addTrauma(0.06f * lv);
    }

    private void fireCue(int i) {
        if (!fxOn()) return;
        float p = cues[i * 3 + 1];
        int kind = (int) cues[i * 3 + 2];
        if (kind == 3) {
            monochromeCue = true;
            addTrauma(0.12f * p);
            glitchT = Math.max(glitchT, 0.10f * p);
        } else if (kind == 2) {
            if (state != ST_OPENING) fxMega(p);
        } else if (kind == 1) {
            fxSlam(p);
        } else {
            addTrauma(0.40f * p);
            fxZoom += 0.04f * p;
            panKick(300f * p);
            fxTiltV += (rng.nextBoolean() ? 1f : -1f) * 70f * p;
            glitchT = Math.max(glitchT, 0.06f);
            shatterBox(4, p);
            if (p >= 0.7f) miniImpact(0.6f * p);
        }
    }

    // impact frame pag bumitaw ang pigil hininga
    private void fxImpact(float p) {
        if (!fxOn()) return;
        focusSm *= 0.4f;
        impactT = IMPACT_TIME;
        impactP = p;
        flashV = Math.max(flashV, 1f * p);
        glitchT = Math.max(glitchT, 0.22f * p);
        fxZoom += 0.10f * p;
        addRing(hx, hy, 4f, 300f, 0.5f, 3, 0.9f * p, false);
        sparks(hx, hy, 36, 260f, 0.6f, 3.2f, 0.95f, 0.9f, 0.82f);
    }

    private void miniImpact(float p) {
        if (impactT > IMPACT_TIME * 0.5f) return;
        impactT = IMPACT_TIME * 0.55f;
        impactP = p;
    }

    private void fxSeqStart(int idx) {
        if (!fxOn() || idx == 0 || cueNear(songTime())) return;
        fxSlam(0.35f + 0.08f * idx);
        fxImpact(0.5f + 0.1f * idx);
    }

    private void fxMega(float p) {
        if (!fxOn()) return;
        addTrauma(1.3f * p);
        newTilt(18f * p);
        fxZoom += 0.20f * p;
        panKick(800f * p);
        flashV = Math.max(flashV, 0.9f * p);
        glitchT = Math.max(glitchT, 0.4f * p);
        float cx = wx(L / 2f), cy = wy(L / 2f);
        addCrack(cx, cy, p, 5.5f);
        addCrack(cx + (rng.nextFloat() - 0.5f) * scrW * 0.5f, cy + (rng.nextFloat() - 0.5f) * scrH * 0.4f,
            0.8f * p, 4.5f);
        shatterBox(46, p);
        shatterAt(cx, cy, 26, 520f * p, 0);
        debris(34, p);
        addRing(L / 2f, L / 2f, 10f, 520f, 0.9f, 3, 1f, false);
    }

    private void fxSlam(float p) {
        if (!fxOn()) return;
        addTrauma(0.70f * p);
        newTilt(14f * p);
        fxZoom += 0.10f * p;
        panKick(560f * p);
        flashV = Math.max(flashV, 0.35f * p);
        glitchT = Math.max(glitchT, 0.2f * p);
        addCrack(wx(L / 2f) + (rng.nextFloat() - 0.5f) * 220f,
            wy(L / 2f) + (rng.nextFloat() - 0.5f) * 160f, 0.55f * p, 3f);
        shatterBox(18, p);
        debris(12, p);
        addRing(L / 2f, L / 2f, 10f, 420f, 0.6f, 1, 0.8f, false);
        miniImpact(0.6f * p);
    }

    private void fxFakeOut() {
        if (!fxOn()) return;
        addCrack(wx(hx), wy(hy), 1f, 4f);
        addCrack(wx(L / 2f), wy(L / 2f), 0.7f, 3f);
        addTrauma(0.9f);
        fxZoom += 0.12f;
        flashV = Math.max(flashV, 0.7f);
        glitchT = Math.max(glitchT, 0.25f);
        impactT = IMPACT_TIME;
        impactP = 0.9f;
        newTilt(10f);
        shatterBox(26, 0.8f);
    }

    private void fxFreezeStart() {
        if (!fxOn()) return;
        addCrack(wx(hx), wy(hy), 0.9f, 4f);
        addTrauma(0.5f);
        fxZoom += 0.08f;
        flashV = Math.max(flashV, 0.5f);
        glitchT = Math.max(glitchT, 0.15f);
        impactT = IMPACT_TIME * 0.7f;
        impactP = 0.5f;
    }

    private void fxHit() {
        if (!fxOn()) return;
        addTrauma(0.40f);
        fxZoom += 0.06f;
        fxTiltV += (rng.nextBoolean() ? 1f : -1f) * 90f;
        panKick(420f);
        flashV = Math.max(flashV, 0.22f);
        glitchT = Math.max(glitchT, 0.15f);
        shatterAt(wx(hx), wy(hy), 12, 240f, 1);
    }

    private void fxDeath() {
        if (!fxOn()) return;
        addTrauma(1.6f);
        newTilt(15f);
        fxZoom += 0.22f;
        panKick(1200f);
        flashV = 0.85f;
        glitchT = 0.5f;
        addCrack(wx(hx), wy(hy), 1f, 40f);
        addCrack(wx(L / 2f), wy(L / 2f), 0.8f, 40f);
        shatterAt(wx(hx), wy(hy), 40, 420f, 1);
        shatterBox(40, 1f);
        debris(40, 1f);
    }

    private void fxBomb(float lx, float ly) {
        if (!fxOn()) return;
        addTrauma(0.30f);
        fxZoom += 0.03f;
        shatterAt(wx(lx), wy(ly), 12, 260f, 1);
        miniImpact(0.4f);
    }

    private void fxBeam(boolean vertical) {
        if (!fxOn()) return;
        addTrauma(0.35f);
        if (vertical) fxPanVX += (rng.nextBoolean() ? 1f : -1f) * 450f * PAN_GAIN;
        else fxPanVY += (rng.nextBoolean() ? 1f : -1f) * 450f * PAN_GAIN;
        fxTiltV += (rng.nextBoolean() ? 1f : -1f) * 50f;
        glitchT = Math.max(glitchT, 0.08f);
        miniImpact(0.35f);
    }

    private int crackWalk(int base, int n, float x, float y, float angDeg, float len, float w,
                          float dist0, boolean allowFork) {
        float dist = dist0;
        float px0 = x, py0 = y;
        float end = dist0 + len;
        while (dist < end && n < CSEG - 1) {
            float step = 10f + rng.nextFloat() * 22f;
            angDeg += (rng.nextFloat() - 0.5f) * 50f;
            float rad = angDeg * MathUtils.degreesToRadians;
            float nx = px0 + MathUtils.cos(rad) * step, ny = py0 + MathUtils.sin(rad) * step;
            dist += step;
            int i = base + n++;
            cMX[i] = (px0 + nx) / 2f;
            cMY[i] = (py0 + ny) / 2f;
            cL[i] = step;
            cAng[i] = angDeg;
            cD[i] = dist;
            cW[i] = w * (1f - 0.55f * (dist - dist0) / Math.max(1f, len));
            if (allowFork && n < CSEG - 14 && rng.nextFloat() < 0.13f) {
                float side = rng.nextBoolean() ? 1f : -1f;
                n = crackWalk(base, n, nx, ny, angDeg + side * (28f + rng.nextFloat() * 30f),
                    len * 0.32f, w * 0.6f, dist, false);
            }
            px0 = nx;
            py0 = ny;
        }
        return n;
    }

    // mabilis mawala ang crack
    private void addCrack(float x, float y, float power, float life) {
        int c = crCursor++ % CRN;
        int base = c * CSEG;
        int n = 0;
        int branches = 4 + (int) (power * 6f);
        float reach = 0.30f * Math.max(scrW, scrH) * (0.5f + power);
        float w = 1.6f + 2.2f * power;
        for (int b = 0; b < branches && n < CSEG - 6; b++) {
            float ang = b * 360f / branches + (rng.nextFloat() - 0.5f) * 40f;
            n = crackWalk(base, n, x, y, ang, reach * (0.45f + 0.55f * rng.nextFloat()), w, 0f, true);
        }
        cCount[c] = n;
        cAge[c] = 0f;
        cLife[c] = Math.min(life, 4f) * CRACK_LIFE_SCALE;
        cPow[c] = power;
    }

    private void spawnShard(float x, float y, float vx, float vy, float w, float h, float life, int hue) {
        int j = shCursor++ % SN;
        shX[j] = x;
        shY[j] = y;
        shVX[j] = vx;
        shVY[j] = vy;
        shW[j] = w;
        shH[j] = h;
        shR[j] = rng.nextFloat() * 360f;
        shRV[j] = (rng.nextFloat() - 0.5f) * 900f;
        shLife[j] = life;
        shHue[j] = hue;
    }

    private void shatterAt(float x, float y, int n, float spd, int hue) {
        for (int i = 0; i < n; i++) {
            float a = rng.nextFloat() * MathUtils.PI2;
            float s = spd * (0.3f + rng.nextFloat() * 0.7f);
            spawnShard(x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s + 60f,
                5f + rng.nextFloat() * 9f, 2f + rng.nextFloat() * 4f,
                0.6f + rng.nextFloat() * 0.6f, rng.nextInt(3) == 0 ? 1 : hue);
        }
    }

    private void shatterBox(int n, float p) {
        float bx = ax + tpX, by = ay + tpY;
        float cx = bx + aw / 2f, cy = by + ah / 2f;
        for (int i = 0; i < n; i++) {
            float x, y;
            int side = rng.nextInt(4);
            float u = rng.nextFloat();
            if (side == 0) { x = bx + u * aw; y = by; }
            else if (side == 1) { x = bx + u * aw; y = by + ah; }
            else if (side == 2) { x = bx; y = by + u * ah; }
            else { x = bx + aw; y = by + u * ah; }
            float dx = x - cx, dy = y - cy;
            float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
            float s = (120f + rng.nextFloat() * 380f) * (0.5f + p);
            spawnShard(x, y, dx / len * s + (rng.nextFloat() - 0.5f) * 120f, dy / len * s + 80f,
                6f + rng.nextFloat() * 16f, 2f + rng.nextFloat() * 5f,
                0.8f + rng.nextFloat() * 0.7f, rng.nextInt(5) == 0 ? 1 : 0);
        }
    }

    private void debris(int n, float p) {
        for (int i = 0; i < n; i++) {
            float x = scrX + rng.nextFloat() * scrW;
            float y = scrY + scrH + 10f + rng.nextFloat() * 40f;
            spawnShard(x, y, (rng.nextFloat() - 0.5f) * 90f, -60f - rng.nextFloat() * 240f,
                5f + rng.nextFloat() * 15f * p, 2f + rng.nextFloat() * 5f,
                1f + rng.nextFloat() * 0.7f, rng.nextInt(4) == 0 ? 1 : (rng.nextBoolean() ? 0 : 2));
        }
    }

    // overlay fx, sya na ang mag-fade
    public void drawOverlay(SpriteBatch batch, Texture pixel, float vx, float vy, float vw, float vh) {
        drawOverlay(batch, pixel, vx, vy, vw, vh, null);
    }

    public void drawOverlay(SpriteBatch batch, Texture pixel, float vx, float vy, float vw, float vh,
                            Matrix4 worldTransform) {
        px = pixel;
        scrX = vx;
        scrY = vy;
        scrW = vw;
        scrH = vh;
        float fb = fxBlend;
        if (fxMode == FX_OFF || fb < 0.01f) return;
        if (worldTransform != null) overlayTransformBackup.set(batch.getTransformMatrix());

        if (tension > 0.02f || focusSm > 0.02f) {
            normal(batch);
            float bh = Math.max(tension * 0.11f, focusSm * 0.09f) * vh;
            batch.setColor(0f, 0f, 0f, 0.92f * fb);
            batch.draw(pixel, vx, vy, vw, bh);
            batch.draw(pixel, vx, vy + vh - bh, vw, bh);
        }

        additive(batch);
        if (worldTransform != null) batch.setTransformMatrix(worldTransform);
        for (int c = 0; c < CRN; c++) {
            if (cLife[c] <= 0f) continue;
            float age = cAge[c];
            float fade = 1f - MathUtils.clamp((age - cLife[c] * 0.15f) / (cLife[c] * 0.85f), 0f, 1f);
            fade *= fade * fb;
            float reveal = age * 4200f;
            int base = c * CSEG;
            for (int s = 0; s < cCount[c]; s++) {
                int i = base + s;
                if (cD[i] > reveal) continue;
                batch.setColor(0.55f, 0.03f, 0.07f, (0.20f + 0.35f * kickEnv) * fade);
                bar(batch, cMX[i], cMY[i], cL[i] + 2f, cW[i] * 3.4f, cAng[i]);
                batch.setColor(0.97f, 0.94f, 0.88f, 0.85f * fade);
                bar(batch, cMX[i], cMY[i], cL[i] + 1f, cW[i], cAng[i]);
            }
        }
        if (worldTransform != null) batch.setTransformMatrix(overlayTransformBackup);

        // camera whip
        if (tpStreakT > 0f) {
            float u = tpStreakT / 0.28f;
            float rad = tpStreakAng * MathUtils.degreesToRadians;
            float nx = -MathUtils.sin(rad), ny = MathUtils.cos(rad);
            float cx0 = vx + vw / 2f, cy0 = vy + vh / 2f;
            float diag = (float) Math.sqrt(vw * vw + vh * vh);
            int seed = (int) Math.abs(tpStreakAng * 10f) + 3;
            for (int i = 0; i < 22; i++) {
                float o = (i / 21f - 0.5f) * diag * 0.7f;
                float len = diag * (0.25f + 0.5f * hash(seed + i * 7)) * u;
                float th = 1.5f + 5f * hash(seed + i * 13) * u;
                if (i % 4 == 0) batch.setColor(0.8f, 0.05f, 0.1f, 0.55f * u * fb);
                else batch.setColor(0.97f, 0.94f, 0.88f, 0.5f * u * fb);
                bar(batch, cx0 + nx * o, cy0 + ny * o, len, th, tpStreakAng);
            }
        }

        // impact frame
        if (impactT > 0f) {
            float u = impactT / IMPACT_TIME;
            float hxs = wx(hx), hys = wy(hy);
            float pop = Math.max(0f, (u - 0.65f) / 0.35f) * impactP;
            if (pop > 0.01f) {
                batch.setColor(1f, 0.97f, 0.92f, 0.7f * pop * fb);
                batch.draw(pixel, vx, vy, vw, vh);
            }
            if (worldTransform != null) batch.setTransformMatrix(worldTransform);
            int lines = 30;
            int seed = (int) (impactP * 100f) + 7;
            for (int i = 0; i < lines; i++) {
                float ang = i * (360f / lines) + hash(seed + i * 5) * 9f;
                float rad = ang * MathUtils.degreesToRadians;
                float inner = (50f + (1f - u) * 360f) * (0.7f + 0.6f * hash(seed + i * 3));
                float len = (80f + 420f * hash(seed + i * 11)) * u;
                float th = 1.5f + 5f * hash(seed + i * 17) * u;
                if ((i & 3) == 0) batch.setColor(0.8f, 0.05f, 0.1f, 0.75f * u * impactP * fb);
                else batch.setColor(0.97f, 0.94f, 0.88f, 0.8f * u * impactP * fb);
                bar(batch, hxs + MathUtils.cos(rad) * (inner + len / 2f),
                    hys + MathUtils.sin(rad) * (inner + len / 2f), len, th, ang);
            }
            if (worldTransform != null) batch.setTransformMatrix(overlayTransformBackup);
        }

        if (glitchT > 0f) {
            int seed = (int) (animT * 26f);
            float g = Math.min(1f, glitchT / 0.25f);
            int n = 5 + (int) (g * 7f);
            for (int i = 0; i < n; i++) {
                float r1 = hash(seed * 31 + i * 7), r2 = hash(seed * 17 + i * 13 + 5);
                float y = vy + r1 * vh, h = 3f + r2 * r2 * vh * 0.07f * (0.4f + g);
                if ((i & 1) == 0) batch.setColor(0.8f, 0.05f, 0.1f, 0.16f * g * fb);
                else batch.setColor(0.9f, 0.88f, 0.8f, 0.12f * g * fb);
                batch.draw(pixel, vx, y, vw, h);
            }
        }

        if (flashV > 0.01f) {
            batch.setColor(1f, 0.97f, 0.92f, flashV * 0.55f * fb);
            batch.draw(pixel, vx, vy, vw, vh);
        }

        if (tpFlash > 0.01f) {
            batch.setColor(1f, 0.97f, 0.92f, 0.85f * tpFlash * tpFlash * fb);
            batch.draw(pixel, vx, vy, vw, vh);
            if (worldTransform != null) batch.setTransformMatrix(worldTransform);
            batch.setColor(1f, 0.9f, 0.8f, tpFlash * fb);
            disc(batch, glowTex, wx(L / 2f), wy(L / 2f), 330f * k * (1.4f - 0.4f * tpFlash));
            if (worldTransform != null) batch.setTransformMatrix(overlayTransformBackup);
        }

        normal(batch);
        if (worldTransform != null) batch.setTransformMatrix(worldTransform);
        for (int i = 0; i < SN; i++) {
            if (shLife[i] <= 0f) continue;
            float a = Math.min(1f, shLife[i] / 0.3f) * fb;
            float w = shW[i], h = shH[i];
            switch (shHue[i]) {
                case 1: batch.setColor(0.72f, 0.07f, 0.12f, a); break;
                case 2: batch.setColor(0.34f, 0.30f, 0.38f, a); break;
                default: batch.setColor(0.84f, 0.82f, 0.76f, a); break;
            }
            batch.draw(pixel, shX[i] - w / 2f, shY[i] - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, shR[i],
                0, 0, 1, 1, false, false);
            if (w > 6f && h > 3.4f) {
                batch.setColor(0.02f, 0.02f, 0.03f, a * 0.8f);
                batch.draw(pixel, shX[i] - (w - 2.4f) / 2f, shY[i] - (h - 2.4f) / 2f,
                    (w - 2.4f) / 2f, (h - 2.4f) / 2f, w - 2.4f, h - 2.4f, 1f, 1f,
                    shR[i], 0, 0, 1, 1, false, false);
            }
        }
        if (worldTransform != null) batch.setTransformMatrix(overlayTransformBackup);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // drawing helpers

    private float wx(float lx) { return ax + tpX + lx * k; }

    private float wy(float ly) { return ay + tpY + ly * k; }

    private void additive(SpriteBatch b) { b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE); }

    private void normal(SpriteBatch b) { b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); }

    private void tint(SpriteBatch b, int hue, float a) {
        float[] c = activePalette()[Math.abs(hue) % PAL.length];
        if (openingMuted) {
            if (phaseThree) b.setColor(0.28f, 0.12f, 0.36f, MathUtils.clamp(a, 0f, 1f));
            else if (phaseTwo) b.setColor(0.68f, 0.22f, 0.055f, MathUtils.clamp(a, 0f, 1f));
            else b.setColor(0.58f, 0.32f, 0.12f, MathUtils.clamp(a, 0f, 1f));
            return;
        }
        if (dropImpact > 0f) {
            float t = MathUtils.clamp(dropImpact, 0f, 1f);
            float r = phaseThree ? 1f : phaseTwo ? 1f : 0.98f;
            float g = phaseThree ? 0.82f : phaseTwo ? 0.2f : 0.4f;
            float bl = phaseThree ? 1f : phaseTwo ? 0.025f : 0.045f;
            b.setColor(MathUtils.lerp(c[0], r, t), MathUtils.lerp(c[1], g, t),
                MathUtils.lerp(c[2], bl, t), MathUtils.clamp(a, 0f, 1f));
            return;
        }
        b.setColor(c[0], c[1], c[2], MathUtils.clamp(a, 0f, 1f));
    }

    private float[][] activePalette() {
        return phaseThree ? P3_PAL : phaseTwo ? P2_PAL : PAL;
    }

    private void bar(SpriteBatch b, float cx, float cy, float len, float th, float ang) {
        b.draw(px, cx - len / 2f, cy - th / 2f, len / 2f, th / 2f, len, th, 1f, 1f, ang, 0, 0, 1, 1, false, false);
    }

    private void drawCenteredSprite(SpriteBatch b, Texture texture, float cx, float cy,
                                    float maxSize, float rotation, boolean flipY) {
        float w, h;
        if (texture.getWidth() >= texture.getHeight()) {
            w = maxSize;
            h = maxSize * texture.getHeight() / texture.getWidth();
        } else {
            h = maxSize;
            w = maxSize * texture.getWidth() / texture.getHeight();
        }
        b.draw(texture, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, rotation,
            0, 0, texture.getWidth(), texture.getHeight(), false, flipY);
    }

    private void drawOutlinedSprite(SpriteBatch b, Texture texture, Texture outline, float cx, float cy,
                                    float maxSize, float rotation, float alpha, int hue, boolean flipY) {
        float[] c = activePalette()[Math.abs(hue) % PAL.length];
        // phase 1: orange rim para dumikit sa dilim
        float width = (phaseTwo || phaseThree) ? Math.max(1.2f, maxSize * 0.006f) : Math.max(2.6f, maxSize * 0.026f);
        int ring = (phaseTwo || phaseThree) ? 8 : 12;
        if (phaseTwo || phaseThree) b.setColor(c[0] * 0.5f, c[1] * 0.5f, c[2] * 0.5f, alpha * 0.8f);
        else b.setColor(1f, 0.46f, 0.06f, alpha * 0.95f);
        for (int i = 0; i < ring; i++) {
            float angle = i * MathUtils.PI2 / ring;
            float ox = MathUtils.cos(angle) * width, oy = MathUtils.sin(angle) * width;
            drawCenteredSprite(b, outline, cx + ox, cy + oy, maxSize, rotation, flipY);
        }
        // fill: full phase color
        b.setColor(c[0], c[1], c[2], alpha);
        drawCenteredSprite(b, texture, cx, cy, maxSize, rotation, flipY);
    }

    private void disc(SpriteBatch b, Texture t, float cx, float cy, float r) {
        b.draw(t, cx - r, cy - r, r * 2f, r * 2f);
    }

    private void dia(SpriteBatch b, float cx, float cy, float side, float rot) {
        b.draw(px, cx - side / 2f, cy - side / 2f, side / 2f, side / 2f, side, side, 1f, 1f, 45f + rot,
            0, 0, 1, 1, false, false);
    }

    public static void drawHeart(SpriteBatch batch, Texture pixel, float x, float y, float alpha) {
        drawHeartScaled(batch, pixel, x, y, 2f, 1f, 0.05f, 0.1f, alpha);
    }

    private static void drawHeartScaled(SpriteBatch batch, Texture pixel, float x, float y, float s,
                                        float r, float g, float bl, float alpha) {
        batch.setColor(r, g, bl, alpha);
        for (int row = 0; row < 8; row++) {
            int bits = HEART[row];
            for (int col = 0; col < 9; col++) {
                if (((bits >> (8 - col)) & 1) != 0) batch.draw(pixel, x + col * s, y + (7 - row) * s, s, s);
            }
        }
    }

    private void flame(SpriteBatch b, float level, float a) {
        if (phaseThree) {
            float t = openingMuted ? 0.35f + 0.35f * level : 0.25f + 0.75f * level;
            b.setColor(MathUtils.lerp(P3_PAL[0][0], P3_PAL[3][0], t),
                MathUtils.lerp(P3_PAL[0][1], P3_PAL[3][1], t),
                MathUtils.lerp(P3_PAL[0][2], P3_PAL[3][2], t), MathUtils.clamp(a, 0f, 1f));
        } else if (openingMuted) {
            if (phaseTwo) {
                b.setColor(0.34f + 0.2f * level, 0.055f + 0.14f * level, 0.018f + 0.025f * level,
                    MathUtils.clamp(a, 0f, 1f));
            } else {
                b.setColor(0.28f + 0.2f * level, 0.12f + 0.12f * level, 0.045f + 0.035f * level,
                    MathUtils.clamp(a, 0f, 1f));
            }
        } else if (phaseTwo) {
            b.setColor(0.42f + 0.58f * level, 0.035f + 0.64f * level, 0.008f + 0.08f * level,
                MathUtils.clamp(a, 0f, 1f));
        } else {
            b.setColor(0.22f + 0.73f * level, 0.07f + 0.58f * level, 0.012f + 0.07f * level,
                MathUtils.clamp(a, 0f, 1f));
        }
    }

    // backdrop (visualiser)

    // visualiser, bago ang scene frame
    public void drawBackdrop(SpriteBatch batch, Texture pixel, float vx, float vy, float vw, float vh, float alpha) {
        if (alpha <= 0.01f) return;
        px = pixel;
        normal(batch);
        batch.setColor(0f, 0f, 0f, (0.35f - 0.3f * kickEnv) * secLevel * alpha);
        batch.draw(pixel, vx - vw, vy - vh, vw * 3f, vh * 3f);
        additive(batch);

        // color wash kada beat, lumalakas pag riser
        float heat = seqIdx / (float) (seq.length - 1);
        float wa = (0.035f + 0.11f * pulseV + 0.22f * wash + 0.12f * thumpV + 0.2f * tension) * alpha
            * (1f + 0.35f * heat);
        if (openingMuted && phaseThree) batch.setColor(0.08f, 0.035f, 0.12f, wa * 0.72f);
        else if (openingMuted && phaseTwo) batch.setColor(0.24f, 0.045f, 0.012f, wa * 0.72f);
        else if (openingMuted) batch.setColor(0.22f, 0.09f, 0.025f, wa * 0.72f);
        else if (phaseThree) batch.setColor(0.20f, 0.045f, 0.25f, wa * 0.9f);
        else if (phaseTwo) batch.setColor(0.36f, 0.025f, 0.008f, wa * 0.9f);
        else batch.setColor(0.24f, 0.075f, 0.018f, wa * 0.85f);
        batch.draw(pixel, vx - vw, vy - vh, vw * 3f, vh * 3f);

        // spectrum ring sa paligid
        float cx = wx(L / 2f), cy = wy(L / 2f);
        int segs = BANDS * 2;
        float r0 = (250f + 22f * thumpV) * k;
        for (int i = 0; i < segs; i++) {
            int band = i < BANDS ? i : segs - 1 - i;
            float lv = bars[band];
            float ang = i * (360f / segs) + animT * 9f;
            float rad = ang * MathUtils.degreesToRadians;
            float len = (10f + lv * 95f) * k;
            float mid = r0 + len / 2f;
            flame(batch, lv, (0.22f + 0.5f * lv) * alpha);
            bar(batch, cx + MathUtils.cos(rad) * mid, cy + MathUtils.sin(rad) * mid, len, 1.4f + 1.1f * lv, ang);
            float tip = r0 + len + 4f;
            dia(batch, cx + MathUtils.cos(rad) * tip, cy + MathUtils.sin(rad) * tip, 3f + 3.5f * lv, 0f);
        }

        // finale: umiikot na ilaw
        if (finaleBlend > 0.01f) {
            int rays = 14;
            float len = 1100f * k;
            for (int i = 0; i < rays; i++) {
                float ang = i * (360f / rays) + animT * 12f;
                float rad = ang * MathUtils.degreesToRadians;
                flame(batch, kickEnv, (0.035f + 0.10f * kickEnv + 0.05f * fluxEnv) * finaleBlend * alpha);
                bar(batch, cx + MathUtils.cos(rad) * len / 2f, cy + MathUtils.sin(rad) * len / 2f,
                    len, (6f + 22f * kickEnv) * k, ang);
            }
        }

        // equalizer sa gilid
        int n = BANDS;
        float step = vh / n;
        for (int i = 0; i < n; i++) {
            float lv = bars[i];
            float len = (6f + lv * 110f);
            flame(batch, lv, (0.18f + 0.45f * lv) * alpha);
            float ly = vy + i * step + step / 2f - 1f;
            batch.draw(pixel, vx, ly, len, 2f);
            batch.draw(pixel, vx + vw - len, ly, len, 2f);
            dia(batch, vx + len + 4f, ly + 1f, 3f + 3f * lv, 0f);
            dia(batch, vx + vw - len - 4f, ly + 1f, 3f + 3f * lv, 0f);
        }

        // skyline sa taas
        int tops = 48;
        float tw = vw / tops;
        for (int i = 0; i < tops; i++) {
            float t = i / (float) (tops - 1);
            int band = (int) ((t < 0.5f ? t * 2f : (1f - t) * 2f) * (BANDS - 1));
            float lv = bars[band];
            float len = 6f + lv * 90f;
            flame(batch, lv, (0.14f + 0.35f * lv) * alpha);
            batch.draw(pixel, vx + i * tw + tw / 2f - 1f, vy + vh - len, 2f, len);
        }

        // shockwave ng kick
        for (int i = 0; i < RN; i++) {
            if (rLife[i] <= 0f || !rBack[i]) continue;
            float u = rAge[i] / rLife[i];
            float r = (rR0[i] + (rR1[i] - rR0[i]) * (1f - (1f - u) * (1f - u))) * k;
            tint(batch, rHue[i], rA[i] * (1f - u) * alpha);
            disc(batch, ringTex, wx(rX[i]), wy(rY[i]), r);
        }

        normal(batch);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // arena drawing

    public void draw(SpriteBatch batch, Texture pixel, float alpha) {
        px = pixel;
        float a = alpha;
        float pr = 1f + 0.16f * kickEnv;          // bullets swell on the kick

        if (state == ST_DEAD) {
            deadT += Math.min(0.05f, Gdx.graphics.getDeltaTime());
        }

        // additive pass
        additive(batch);

        // afterimage ng teleport
        if (ghostT > 0f) {
            float gu = ghostT / 0.4f;
            for (int g = 0; g < 3; g++) {
                float f = (g + 1) / 4f;
                float gx = ax + ghX0 + (ghX1 - ghX0) * f, gy = ay + ghY0 + (ghY1 - ghY0) * f;
                float gcx = gx + aw / 2f, gcy = gy + ah / 2f;
                batch.setColor(0.85f, 0.1f, 0.15f, (0.5f - 0.12f * g) * gu * a);
                bar(batch, gcx, gy, aw, 3f, 0f);
                bar(batch, gcx, gy + ah, aw, 3f, 0f);
                bar(batch, gx, gcy, ah, 3f, 90f);
                bar(batch, gx + aw, gcy, ah, 3f, 90f);
            }
        }

        // equalizer sa sahig ng box
        for (int i = 0; i < BANDS; i++) {
            float lv = bars[i];
            float w = L / BANDS;
            flame(batch, lv, 0.12f * a);
            batch.draw(pixel, wx(i * w + w / 2f - 1f), wy(0f), 2f * k, (4f + lv * 46f) * k);
        }

        // buto: casing, shaft, core
        for (int i = 0; i < bones.size(); i++) {
            Bone b = bones.get(i);
            if (!b.active) continue;
            float bcx = wx(b.x), bcy = wy((b.y0 + b.y1) / 2f);
            float blen = (b.y1 - b.y0) * k, bw = b.w * k;
            float[] bpc = activePalette()[Math.abs(b.hue) % PAL.length];
            batch.setColor(0.04f, 0.05f, 0.07f, 0.9f * a);
            bar(batch, bcx, bcy, blen + bw * 1.4f, bw * 1.7f, 90f);
            batch.setColor(0.86f, 0.89f, 0.95f, 0.97f * a);
            bar(batch, bcx, bcy, blen, bw * 0.8f, 90f);
            batch.setColor(bpc[0], bpc[1], bpc[2], 0.85f * a);
            bar(batch, bcx, bcy, blen, bw * 0.26f, 90f);
            batch.setColor(0.86f, 0.89f, 0.95f, 0.97f * a);
            disc(batch, orbTex, bcx, wy(b.y0) + bw * 0.1f, bw * 0.9f);
            disc(batch, orbTex, bcx, wy(b.y1) - bw * 0.1f, bw * 0.9f);
        }

        for (int i = 0; i < pickups.size(); i++) {
            Pickup p = pickups.get(i);
            if (!p.active) continue;
            float cx = wx(p.x), cy = wy(p.y);
            float life = 1f - p.age / 3.6f;
            float bob = 0.75f + 0.25f * (float) Math.sin(animT * 9f);
            batch.setColor(0.62f, 0.9f, 0.78f, 0.6f * bob * Math.min(1f, life * 4f) * a);
            disc(batch, glowTex, cx, cy, 24f * k);
            disc(batch, ringTex, cx, cy, (13f + 3f * bob) * k);
        }

        // beams
        for (int i = 0; i < beams.size(); i++) {
            Beam bm = beams.get(i);
            if (!bm.active) continue;
            float cx = wx(bm.x), cy = wy(bm.y);
            float th = bm.thick * k, len = BEAM_LEN * k;
            float rad = bm.ang * MathUtils.degreesToRadians;
            if (bm.propellerBeam) {
                boolean warning = bm.age < bm.warn;
                float fade = warning
                    ? 0.28f + 0.28f * (0.5f + 0.5f * (float) Math.sin(animT * 18f))
                    : 1f - MathUtils.clamp((bm.age - bm.warn - bm.on) / 0.2f, 0f, 1f);
                float bladeLen = PROP_BEAM_LEN * k;
                for (int blade = 0; blade < 4; blade++) {
                    float angle = bm.ang + blade * 90f;
                    float bladeRad = angle * MathUtils.degreesToRadians;
                    float bx = cx + MathUtils.cos(bladeRad) * bladeLen * 0.5f;
                    float by = cy + MathUtils.sin(bladeRad) * bladeLen * 0.5f;
                    if (warning) {
                        float blink = 0.5f + 0.5f * (float) Math.sin(animT * 18f);
                        float[] pc = activePalette()[Math.abs(bm.hue) % PAL.length];
                        batch.setColor(pc[0], pc[1], pc[2], (0.3f + 0.28f * blink) * fade * a);
                        bar(batch, bx, by, bladeLen, Math.max(2f * k, th * 0.55f), angle);
                        batch.setColor(pc[0], pc[1], pc[2], (0.5f + 0.3f * blink) * fade * a);
                        bar(batch, bx, by, bladeLen, 1.8f * k, angle);
                    } else {
                        float[] pc = activePalette()[Math.abs(bm.hue) % PAL.length];
                        batch.setColor(pc[0], pc[1], pc[2], 0.9f * fade * a);
                        bar(batch, bx, by, bladeLen, th * 2.2f, angle);
                        batch.setColor(pc[0], pc[1], pc[2], 0.95f * fade * a);
                        bar(batch, bx, by, bladeLen, th, angle);
                        batch.setColor(pc[0], pc[1], pc[2], 0.9f * fade * a);
                        bar(batch, bx, by, bladeLen, th * 0.38f, angle);
                    }
                }
            } else if (bm.slash && bm.age < bm.warn) {
                float t = bm.age / bm.warn;
                float blink = 0.5f + 0.5f * (float) Math.sin(animT * (10f + 14f * t));
                float[] pc = activePalette()[Math.abs(bm.hue) % PAL.length];
                batch.setColor(pc[0], pc[1], pc[2], (0.18f + 0.28f * blink) * a);
                bar(batch, cx, cy, len, 2.2f * k, bm.ang);
                batch.setColor(pc[0] * 1.15f, pc[1] * 1.15f, pc[2] * 1.15f, (0.22f + 0.2f * blink) * a);
                bar(batch, cx, cy, len, 1f * k, bm.ang);
            } else if (bm.slash && bm.age < bm.warn + bm.on + 0.2f) {
                float u = MathUtils.clamp((bm.age - bm.warn) / Math.max(0.01f, bm.on), 0f, 1.2f);
                float fade = 1f - MathUtils.clamp((bm.age - bm.warn - bm.on) / 0.2f, 0f, 1f);
                float punch = (bm.age - bm.warn < 0.07f ? 1.5f : 1f) * (1f + 0.25f * kickEnv);
                float[] pc = activePalette()[Math.abs(bm.hue) % PAL.length];
                batch.setColor(pc[0], pc[1], pc[2], 0.82f * fade * a);
                bar(batch, cx, cy, len * (0.9f + 0.1f * u), th * 1.7f * punch, bm.ang);
                batch.setColor(pc[0] * 1.15f, pc[1] * 1.15f, pc[2] * 1.15f, 0.95f * fade * a);
                bar(batch, cx, cy, len * (0.9f + 0.1f * u), th * 0.55f * punch, bm.ang);
            } else if (bm.age < bm.warn) {
                float t = bm.age / bm.warn;
                float blink = 0.5f + 0.5f * (float) Math.sin(animT * (10f + 14f * t));
                float aa = 1f;
                float[] base = activePalette()[Math.abs(bm.hue) % PAL.length];
                float cr = base[0], cg = base[1], cb = base[2];
                if (bm.fake) {
                    float tell = MathUtils.clamp((t - 0.55f) / 0.2f, 0f, 1f);
                    cr = MathUtils.lerp(cr, 0.55f, tell);
                    cg = MathUtils.lerp(cg, 0.62f, tell);
                    cb = MathUtils.lerp(cb, 0.75f, tell);
                    aa = 1f - MathUtils.clamp((t - 0.8f) / 0.2f, 0f, 1f);
                }
                batch.setColor(cr, cg, cb, (0.06f + 0.10f * t) * aa * a);
                bar(batch, cx, cy, len, th, bm.ang);
                float nx = -MathUtils.sin(rad), ny = MathUtils.cos(rad);
                batch.setColor(cr, cg, cb, (0.4f + 0.35f * blink) * aa * a);
                bar(batch, cx + nx * th / 2f, cy + ny * th / 2f, len, 2f, bm.ang);
                bar(batch, cx - nx * th / 2f, cy - ny * th / 2f, len, 2f, bm.ang);
                if (!bm.fake) {                       // real beams: a core that fills in as the shot nears
                    float[] pc = activePalette()[Math.abs(bm.hue) % PAL.length];
                    batch.setColor(pc[0], pc[1], pc[2], 0.18f * t * a);
                    bar(batch, cx, cy, len, th * t, bm.ang);
                }
            } else if (!bm.fake && bm.age < bm.warn + bm.on + 0.2f) {
                float u = MathUtils.clamp((bm.age - bm.warn) / Math.max(0.01f, bm.on), 0f, 1.2f);
                float fade = 1f - MathUtils.clamp((u - 0.7f) / 0.5f, 0f, 1f);
                float punch = (bm.age - bm.warn < 0.07f ? 1.5f : 1f) * (1f + 0.25f * kickEnv);
                float[] pc = activePalette()[Math.abs(bm.hue) % PAL.length];
                batch.setColor(pc[0], pc[1], pc[2], 0.6f * fade * a);
                bar(batch, cx, cy, len, th * 1.7f * punch, bm.ang);
                batch.setColor(pc[0] * 1.15f, pc[1] * 1.15f, pc[2] * 1.15f, 0.95f * fade * a);
                bar(batch, cx, cy, len, th * 0.6f * punch, bm.ang);
            }
        }

        // bombs
        for (int i = 0; i < bombs.size(); i++) {
            Bomb bo = bombs.get(i);
            if (!bo.active) continue;
            float cx = wx(bo.x), cy = wy(bo.y);
            float[][] palette = activePalette();
            float[] bombColor = palette[Math.abs(bo.hue) % PAL.length];
            float[] sparkColor = palette[3];
            if (!bo.blown) {
                float t = bo.age / bo.warn;
                float blink = 0.5f + 0.5f * (float) Math.sin(animT * (8f + 16f * t));
                batch.setColor(bombColor[0] * 0.5f, bombColor[1] * 0.5f,
                    bombColor[2] * 0.5f, (0.08f + 0.2f * t) * a);
                disc(batch, glowTex, cx, cy, bo.R * k * 1.15f);
                batch.setColor(sparkColor[0], sparkColor[1], sparkColor[2],
                    (0.45f + 0.35f * blink) * a);
                disc(batch, ringTex, cx, cy, bo.R * k);
                batch.setColor(bombColor[0], bombColor[1], bombColor[2], 0.5f * a);
                disc(batch, ringTex, cx, cy, bo.R * k * (2.2f - 1.2f * t));
                batch.setColor(sparkColor[0], sparkColor[1], sparkColor[2], 0.9f * a);
                for (int q = 0; q < 4; q++) {
                    float qa = q * MathUtils.PI / 2f + animT * 0.8f;
                    dia(batch, cx + MathUtils.cos(qa) * bo.R * k, cy + MathUtils.sin(qa) * bo.R * k, 6f, 0f);
                }
            } else {
                float u = MathUtils.clamp((bo.age - bo.warn) / 0.3f, 0f, 1f);
                batch.setColor(sparkColor[0], sparkColor[1], sparkColor[2], (1f - u) * a);
                disc(batch, glowTex, cx, cy, bo.R * k * (1.1f + u * 0.6f));
            }
        }

        // aim lines, glow, trail
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            if (!b.active) continue;
            float cx = wx(b.x), cy = wy(b.y), r = b.r * k * pr;
            boolean waiting = b.aimed && !b.fired;
            boolean held = b.holdAt >= 0f && b.age >= b.holdAt && b.age < b.holdAt + b.holdDur;
            if (waiting) {
                float t = MathUtils.clamp(b.age / b.delay, 0f, 1f);
                float tx = wx(hx), ty = wy(hy);
                float ang = (float) Math.atan2(ty - cy, tx - cx) * MathUtils.radiansToDegrees;
                float len = (float) Math.sqrt((tx - cx) * (tx - cx) + (ty - cy) * (ty - cy));
                tint(batch, b.hue, (0.10f + 0.28f * t) * (0.6f + 0.4f * (float) Math.sin(animT * 22f)) * a);
                bar(batch, (cx + tx) / 2f, (cy + ty) / 2f, len, 1.5f, ang);
                tint(batch, b.hue, 0.55f * a);
                disc(batch, ringTex, cx, cy, r * (3.2f - 2f * t));
            }
            tint(batch, 1, (waiting ? 0.10f : 0.22f + 0.12f * kickEnv) * a);
            disc(batch, glowTex, cx, cy, r * 2.6f);
            float sp = (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy);
            if (!waiting && !held && freeze <= 0f && sp > 40f) {
                float ang = (float) Math.atan2(b.vy, b.vx) * MathUtils.radiansToDegrees;
                float tl = Math.min(46f, sp * 0.12f) * k;
                float cs = MathUtils.cosDeg(ang), sn = MathUtils.sinDeg(ang);
                tint(batch, b.hue, 0.30f * a);
                bar(batch, cx - cs * tl / 2f, cy - sn * tl / 2f, tl, 1.6f, ang);
            }
        }

        // particles + rings
        for (int i = 0; i < PN; i++) {
            if (pLife[i] <= 0f) continue;
            float u = pLife[i] / pMax[i];
            batch.setColor(pR[i], pG[i], pB[i], u * a);
            float s = pSize[i] * (0.4f + 0.6f * u) * Math.max(0.6f, k);
            batch.draw(pixel, wx(pX[i]) - s / 2f, wy(pY[i]) - s / 2f, s, s);
        }
        for (int i = 0; i < RN; i++) {
            if (rLife[i] <= 0f || rBack[i]) continue;
            float u = rAge[i] / rLife[i];
            float r = (rR0[i] + (rR1[i] - rR0[i]) * (1f - (1f - u) * (1f - u))) * k;
            tint(batch, rHue[i], rA[i] * (1f - u) * a);
            disc(batch, ringTex, wx(rX[i]), wy(rY[i]), r);
        }

        // PRE: kumakaliit na ring, malapit na
        if (state == ST_PRE) {
            float ph = beatPhase(visT);
            batch.setColor(0.88f, 0.86f, 0.8f, (0.5f + 0.4f * focusSm) * (1f - ph) * a);
            disc(batch, ringTex, wx(hx), wy(hy), (46f - 36f * ph) * k);
        }

        // normal pass
        normal(batch);

        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            if (!b.active) continue;
            float cx = wx(b.x), cy = wy(b.y), r = b.r * k * pr;
            boolean waiting = b.aimed && !b.fired;
            boolean held = b.holdAt >= 0f && b.age >= b.holdAt && b.age < b.holdAt + b.holdDur;
            if (held || freeze > 0f) {
                cx += (float) Math.sin(animT * 90f + i) * 1.2f * k;
                batch.setColor(0.70f, 0.68f, 0.76f, a);
            } else {
                tint(batch, b.hue, (waiting ? 0.55f : 1f) * a);
            }
            float ds = r * (waiting ? 1f : 1.55f);
            if (b.aimed) {
                drawOutlinedSprite(batch, throwAxeTex, throwAxeOutlineTex, cx, cy,
                    ds * 2f, b.age * 720f, a, b.hue, false);
            } else {
                // orb body: phase color
                float[] oc = activePalette()[Math.abs(b.hue) % PAL.length];
                float oa = (waiting ? 0.55f : 1f) * a;
                batch.setColor(oc[0], oc[1], oc[2], oa);
                dia(batch, cx, cy, ds, 0f);
                // inner dark ring
                batch.setColor(oc[0] * 0.35f, oc[1] * 0.35f, oc[2] * 0.35f, oa * 0.9f);
                dia(batch, cx, cy, ds - 2.6f, 0f);
                // center highlight: bright phase accent
                float[] hi = activePalette()[Math.min(4, Math.abs(b.hue) + 2) % PAL.length];
                batch.setColor(hi[0], hi[1], hi[2], oa * 0.85f);
                dia(batch, cx, cy, ds * 0.26f, 0f);
            }
        }

        for (int i = 0; i < pickups.size(); i++) {
            Pickup p = pickups.get(i);
            if (!p.active) continue;
            float cx = wx(p.x), cy = wy(p.y);
            batch.setColor(0.8f, 0.95f, 0.86f, a);
            batch.draw(pixel, cx - 2.5f * k, cy - 7f * k, 5f * k, 14f * k);
            batch.draw(pixel, cx - 7f * k, cy - 2.5f * k, 14f * k, 5f * k);
        }

        // heart
        float hcx = wx(hx), hcy = wy(hy);
        float s = (1.6f + 0.25f * kickEnv + 0.35f * focusSm) * k;
        if (state == ST_DEAD) {
            if (deadT < 0.45f) {
                drawHeartScaled(batch, pixel, hcx - 4.5f * s + (float) Math.sin(deadT * 90f) * 1.5f, hcy - 4f * s, s,
                    1f, 0.1f, 0.15f, a);
            } else {
                float t = deadT - 0.45f;
                for (int i = 0; i < 8; i++) {
                    float sx = hcx + sVX[i] * t * k;
                    float sy = hcy + (sVY[i] * t - 160f * t * t) * k;
                    batch.setColor(1f, 0.1f, 0.15f, a * Math.max(0f, 1f - t * 0.8f));
                    batch.draw(pixel, sx - 2.5f * k, sy - 2.5f * k, 5f * k, 5f * k);
                }
            }
        } else if (state != ST_IDLE) {
            boolean visible = hurt <= 0f || (((int) (hurt * 22f)) & 1) == 0;
            if (visible) {
                if (god) {
                    drawHeartScaled(batch, pixel, hcx - 4.5f * s - 1f, hcy - 4f * s - 1f, s + 0.25f,
                        0.16f, 0.62f, 1f, 0.8f * a);
                    drawHeartScaled(batch, pixel, hcx - 4.5f * s, hcy - 4f * s, s, 0.2f, 0.48f, 1f, a);
                } else {
                    drawHeartScaled(batch, pixel, hcx - 4.5f * s, hcy - 4f * s, s, 1f, 0.06f, 0.12f, a);
                }
            }
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                batch.setColor(1f, 1f, 1f, a);
                disc(batch, orbTex, hcx, hcy, HIT_R * k);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }
}