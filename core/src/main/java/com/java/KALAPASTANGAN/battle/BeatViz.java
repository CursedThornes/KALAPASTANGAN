package com.java.KALAPASTANGAN.battle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

// values galing sa spectrum file
public final class BeatViz {
    public static final int BANDS = 32;
    private static final int HEADER_SIZE = 16;
    private static final int FRAME_SIZE = 3 + BANDS;
    public static final float SYNC_OFFSET = 0.03f;

    public float energy;
    public float punch;
    public float bass;

    private final int fps;
    private final int frames;
    private final byte[] data;
    private final float[] bands = new float[BANDS];
    private final float[] targetBands = new float[BANDS];

    private float clock;
    private int lastFrame = -1;

    private BeatViz(int fps, int frames, byte[] data) {
        this.fps = fps;
        this.frames = frames;
        this.data = data;
    }

    public static BeatViz load(String path) {
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) return new BeatViz(50, 0, null);

        byte[] bytes = file.readBytes();
        if (bytes.length < HEADER_SIZE) {
            throw new IllegalStateException("Invalid beat visualization file: " + file.path());
        }

        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int magic = header.getInt();
        int fps = header.getInt();
        int frames = header.getInt();
        int bandCount = header.getInt();
        long requiredSize = HEADER_SIZE + (long) frames * FRAME_SIZE;
        if (magic != 0x56495A31 || fps <= 0 || frames <= 0 || bandCount != BANDS
            || requiredSize > bytes.length) {
            throw new IllegalStateException("Invalid beat visualization file: " + file.path());
        }
        return new BeatViz(fps, frames, bytes);
    }

    public boolean isReady() {
        return data != null;
    }

    public float band(int index) {
        return bands[MathUtils.clamp(index, 0, BANDS - 1)];
    }

    private float raw(int frame, int field) {
        int safeFrame = MathUtils.clamp(frame, 0, frames - 1);
        return (data[HEADER_SIZE + safeFrame * FRAME_SIZE + field] & 0xff) / 255f;
    }

    public void update(float delta, float musicPosition, boolean playing) {
        if (!isReady()) return;

        float targetEnergy = 0f;
        float targetBass = 0f;
        float hit = 0f;
        if (playing) {
            if (Math.abs(musicPosition - clock) > 0.15f) {
                clock = musicPosition;
                lastFrame = -1;
            } else {
                clock += delta;
                clock += (musicPosition - clock) * Math.min(1f, delta * 12f);
            }

            float framePosition = Math.max(0f, clock + SYNC_OFFSET) * fps;
            int frame = Math.min((int) framePosition, frames - 1);
            float fraction = framePosition - (int) framePosition;
            targetEnergy = MathUtils.lerp(raw(frame, 0), raw(frame + 1, 0), fraction);
            targetBass = MathUtils.lerp(raw(frame, 2), raw(frame + 1, 2), fraction);
            for (int i = 0; i < BANDS; i++) {
                targetBands[i] = MathUtils.lerp(raw(frame, 3 + i), raw(frame + 1, 3 + i), fraction);
            }

            int from = lastFrame < 0 || lastFrame > frame ? frame : lastFrame + 1;
            for (int i = from; i <= frame; i++) hit = Math.max(hit, raw(i, 1));
            lastFrame = frame;
        } else {
            lastFrame = -1;
        }

        energy += (targetEnergy - energy) * Math.min(1f, delta * 4f);
        bass += (targetBass - bass) * Math.min(1f, delta * 14f);
        float punchSpeed = hit > punch ? 14f : 5f;
        punch += (hit - punch) * Math.min(1f, delta * punchSpeed);
        for (int i = 0; i < BANDS; i++) {
            float target = playing ? targetBands[i] : 0f;
            float speed = target > bands[i] ? 20f : 7f;
            bands[i] += (target - bands[i]) * Math.min(1f, delta * speed);
        }
    }

    private float mirroredBand(float position) {
        float p = Math.abs(position * 2f - 1f) * (BANDS - 1);
        int index = (int) p;
        return MathUtils.lerp(bands[index], bands[Math.min(BANDS - 1, index + 1)], p - index);
    }

    public void drawWave(SpriteBatch batch, TextureRegion pixel, float x, float y, float width,
                         float time, float intensity) {
        if (!isReady()) return;
        final int segments = 64;
        float amplitude = (22f + 210f * energy) * intensity;
        float previousX = x, previousY = y;
        for (int i = 0; i <= segments; i++) {
            float u = i / (float) segments;
            float edge = MathUtils.sin(u * MathUtils.PI);
            float calm = MathUtils.sin(u * 9f + time * 1.4f) * 7f * (1f - energy * 0.6f);
            float spike = mirroredBand(u) * amplitude * (0.55f + 1.15f * punch);
            float jitter = MathUtils.sin(u * 40f - time * 12f) * punch * 8f * energy * intensity;
            float currentX = x + u * width;
            float currentY = y + (calm + spike + jitter) * edge;
            if (i > 0) {
                float dx = currentX - previousX, dy = currentY - previousY;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                float alpha = Math.min(1f, 0.4f + 0.7f * energy + 0.45f * punch);
                batch.setColor(0.82f, 0.82f, 0.82f, 0.28f * alpha);
                batch.draw(pixel, previousX, previousY - 8f, 0f, 8f, length + 1f, 16f,
                    1f, 1f, angle);
                batch.setColor(0.92f, 0.92f, 0.92f, 0.42f * alpha);
                batch.draw(pixel, previousX, previousY - 4f, 0f, 4f, length + 1f, 8f,
                    1f, 1f, angle);
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(pixel, previousX, previousY - 1.5f, 0f, 1.5f, length + 1f, 3f,
                    1f, 1f, angle);
            }
            previousX = currentX;
            previousY = currentY;
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void drawBars(SpriteBatch batch, TextureRegion pixel, float x, float y, float width,
                         float maxHeight, float intensity) {
        if (!isReady()) return;
        final int count = BANDS * 2;
        float barWidth = width / count;
        for (int i = 0; i < count; i++) {
            int bandIndex = i < BANDS ? BANDS - 1 - i : i - BANDS;
            float value = bands[bandIndex];
            float height = (3f + value * value * maxHeight * (0.4f + 0.8f * energy)) * intensity;
            batch.setColor(0.8f, 0.8f, 0.8f, (0.1f + 0.28f * value) * intensity);
            batch.draw(pixel, x + i * barWidth + 1f, y, barWidth - 2f, height);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
