package com.java.KALAPASTANGAN.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

// mga texture na ginagawa sa code, hindi galing sa assets
public final class MenuTextures {
    private MenuTextures() {
    }

    public static Texture softCircle() {
        final int size = 32;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f) / size * 2f - 1f;
                float dy = (y + 0.5f) / size * 2f - 1f;
                float alpha = MathUtils.clamp(1f - (float) Math.sqrt(dx * dx + dy * dy), 0f, 1f);
                pm.setColor(1f, 1f, 1f, alpha * alpha);
                pm.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return texture;
    }

    public static Texture grain(int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float shade = MathUtils.randomBoolean() ? 1f : 0f;
                pm.setColor(shade, shade, shade, MathUtils.random(0f, 0.08f));
                pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm);
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    public static Texture vignette() {
        final int vw = 256, vh = 144;
        Pixmap pm = new Pixmap(vw, vh, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < vh; y++) {
            for (int x = 0; x < vw; x++) {
                float nx = (x + 0.5f) / vw * 2f - 1f;
                float ny = (y + 0.5f) / vh * 2f - 1f;
                float d = (float) Math.sqrt(nx * nx * 0.75f + ny * ny * 0.9f);
                float a = MathUtils.clamp((d - 0.55f) / 0.8f, 0f, 1f);
                pm.setColor(0f, 0f, 0f, a * a * 0.55f);
                pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    public static Texture ring() {
        final int size = 512;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f) / size * 2f - 1f;
                float dy = (y + 0.5f) / size * 2f - 1f;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = MathUtils.clamp(1f - Math.abs(distance - 0.92f) / 0.025f, 0f, 1f);
                pm.setColor(1f, 1f, 1f, alpha * alpha);
                pm.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return texture;
    }

    public static Texture bloodVignette() {
        final int width = 256, height = 144;
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float nx = (x + 0.5f) / width * 2f - 1f;
                float ny = (y + 0.5f) / height * 2f - 1f;
                float distance = (float) Math.sqrt(nx * nx * 0.75f + ny * ny * 0.9f);
                float alpha = MathUtils.clamp((distance - 0.4f) / 0.75f, 0f, 1f);
                pm.setColor(0.08f, 0.08f, 0.08f, alpha * alpha * 0.85f);
                pm.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return texture;
    }
}
