package com.java.KALAPASTANGAN.ui;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

// clock intro bago ang menu music
public final class MenuIntro {
    private static final float LENGTH = 5f;
    private static final float SPIN_START = 1.25f;
    private static final float ZOOM_START = 2.8f;
    private static final float ZOOM_END = 4.55f;
    private static final float CLOCK_RADIUS = 300f;

    private static final String[] ROMAN = {
        "XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"
    };

    private final TextureRegion pixel;
    private final TextureRegion soft;
    private final TextureRegion ring;
    private final TextureRegion longHand;
    private final TextureRegion mediumHand;
    private final float longPivotX, longPivotY, mediumPivotX, mediumPivotY;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final Sound clickSfx;
    private final Sound introSfx;

    private float elapsed;
    private float nextTick = 0.6f;
    private float secondAngle = 90f;
    private float minuteAngle = 90f;
    private float hourAngle = 90f;
    private long soundId = -1L;
    private int tickCount;
    private boolean started;

    public MenuIntro(TextureRegion pixel, TextureRegion soft, TextureRegion ring,
                     TextureRegion longHand, TextureRegion mediumHand,
                     float longPivotX, float longPivotY, float mediumPivotX, float mediumPivotY,
                     BitmapFont font,
                     Sound clickSfx, Sound introSfx) {
        this.pixel = pixel;
        this.soft = soft;
        this.ring = ring;
        this.longHand = longHand;
        this.mediumHand = mediumHand;
        this.longPivotX = longPivotX;
        this.longPivotY = longPivotY;
        this.mediumPivotX = mediumPivotX;
        this.mediumPivotY = mediumPivotY;
        this.font = font;
        this.clickSfx = clickSfx;
        this.introSfx = introSfx;
    }

    public void update(float delta) {
        if (isDone()) return;
        if (!started) {
            started = true;
            if (introSfx != null) soundId = introSfx.play(1f, 1f, 0f);
        }

        elapsed = Math.min(LENGTH, elapsed + delta);
        while (nextTick <= elapsed) {
            float tickTime = nextTick;
            float tickSpin = MathUtils.clamp((tickTime - SPIN_START) / (ZOOM_END - SPIN_START), 0f, 1f);
            float speed = MathUtils.lerp(0.6f, 0.075f, tickSpin * tickSpin);
            secondAngle -= 6f;
            minuteAngle -= speed * 7f;
            hourAngle -= speed * 1.4f;
            tickCount++;
            if (clickSfx != null) {
                float pitch = MathUtils.lerp(0.58f, 0.82f, tickSpin);
                clickSfx.play(0.45f, pitch, 0f);
            }
            nextTick += speed;
        }

        if (elapsed >= ZOOM_START) {
            float progress = MathUtils.clamp((elapsed - ZOOM_START) / (ZOOM_END - ZOOM_START), 0f, 1f);
            float zoom = (float) Math.exp(progress * 2.1f);
            secondAngle -= delta * (90f + 1500f * progress) * zoom;
            minuteAngle -= delta * (30f + 420f * progress) * zoom;
            hourAngle -= delta * (6f + 70f * progress) * zoom;
        } else if (elapsed > SPIN_START) {
            float progress = (elapsed - SPIN_START) / (ZOOM_START - SPIN_START);
            secondAngle -= delta * progress * progress * 520f;
            minuteAngle -= delta * progress * progress * 90f;
            hourAngle -= delta * progress * progress * 18f;
        }

    }

    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        float progress = MathUtils.clamp((elapsed - ZOOM_START) / (ZOOM_END - ZOOM_START), 0f, 1f);
        float zoom = elapsed < ZOOM_START ? 1f : (float) Math.exp(progress * 2.1f);
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float radius = CLOCK_RADIUS * zoom;
        float fadeIn = MathUtils.clamp(elapsed / 0.8f, 0f, 1f);
        float alpha = fadeIn * (1f - white());

        batch.setColor(1f, 1f, 1f, 0.16f * alpha);
        batch.draw(soft, centerX - radius * 0.8f, centerY - radius * 0.8f,
            radius * 1.6f, radius * 1.6f);
        batch.setColor(0.85f, 0.85f, 0.85f, alpha);
        float rim = radius * 2.04f;
        batch.draw(ring, centerX - rim / 2f, centerY - rim / 2f, rim, rim);

        for (int i = 0; i < 60; i++) {
            float angle = 90f - i * 6f;
            boolean major = i % 5 == 0;
            float length = major ? 30f : 14f;
            float thickness = major ? 3.2f : 1.6f;
            float startX = centerX + MathUtils.cosDeg(angle) * radius;
            float startY = centerY + MathUtils.sinDeg(angle) * radius;
            float shade = major ? 0.9f : 0.68f;
            batch.setColor(shade, shade, shade, (major ? 0.72f : 0.35f) * alpha);
            drawLine(batch, startX, startY, angle + 180f, length * zoom, thickness * zoom);
        }

        float numeralScale = MathUtils.clamp(zoom, 0.65f, 1.4f) / 2f;
        font.getData().setScale(numeralScale);
        for (int i = 0; i < ROMAN.length; i++) {
            float angle = 90f - i * 30f;
            float numeralX = centerX + MathUtils.cosDeg(angle) * radius * 0.8f;
            float numeralY = centerY + MathUtils.sinDeg(angle) * radius * 0.8f;
            layout.setText(font, ROMAN[i]);
            batch.setColor(0.9f, 0.9f, 0.9f, 0.85f * alpha);
            font.draw(batch, ROMAN[i], numeralX - layout.width / 2f, numeralY + layout.height / 2f);
        }
        font.getData().setScale(1f / 2f);

        float windup = MathUtils.clamp((elapsed - SPIN_START) / (ZOOM_END - SPIN_START), 0f, 1f);
        drawHand(batch, mediumHand, centerX, centerY, hourAngle, radius * 0.42f,
            mediumPivotX, mediumPivotY, 0.49f * alpha);
        drawHand(batch, mediumHand, centerX, centerY, minuteAngle, radius * 0.62f,
            mediumPivotX, mediumPivotY, 0.585f * alpha);
        float secondHandLength = radius * 0.76f;
        drawHand(batch, longHand, centerX, centerY, secondAngle, secondHandLength,
            longPivotX, longPivotY, 0.65f * alpha);

        if (windup > 0.05f) {
            float tipX = centerX + MathUtils.cosDeg(secondAngle) * secondHandLength;
            float tipY = centerY + MathUtils.sinDeg(secondAngle) * secondHandLength;
            for (int i = 0; i < 5; i++) {
                float sparkAngle = secondAngle + 150f + i * 21f + tickCount * 13f;
                float sparkLength = (5f + i * 2f) * windup * zoom;
                batch.setColor(1f, 1f, 1f, (0.25f + 0.12f * i) * windup * alpha);
                drawLine(batch, tipX, tipY, sparkAngle, sparkLength, Math.max(0.7f, zoom));
            }
        }
        batch.setColor(1f, 1f, 1f, 0.9f * alpha);
        batch.draw(soft, centerX - 9f * zoom, centerY - 9f * zoom, 18f * zoom, 18f * zoom);
        batch.setColor(Color.WHITE);
    }

    private void drawHand(SpriteBatch batch, TextureRegion hand, float centerX, float centerY,
                          float angle, float height, float pivotXFraction, float pivotYFraction,
                          float alpha) {
        float scale = height / hand.getRegionHeight();
        float width = hand.getRegionWidth() * scale;
        float originX = width * pivotXFraction;
        float originY = height * (1f - pivotYFraction);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(hand, centerX - originX, centerY - originY, originX, originY, width, height,
            1f, 1f, angle - 90f);
    }

    private void drawLine(SpriteBatch batch, float x, float y, float angle, float length, float thickness) {
        batch.draw(pixel, x, y - thickness / 2f, 0f, thickness / 2f,
            length, thickness, 1f, 1f, angle);
    }

    public float white() {
        return MathUtils.clamp((elapsed - 4.48f) / (LENGTH - 4.48f), 0f, 1f);
    }

    public boolean isDone() {
        return elapsed >= LENGTH;
    }

    public void skip() {
        elapsed = LENGTH;
        if (soundId >= 0L && introSfx != null) {
            introSfx.stop(soundId);
            soundId = -1L;
        }
    }
}
