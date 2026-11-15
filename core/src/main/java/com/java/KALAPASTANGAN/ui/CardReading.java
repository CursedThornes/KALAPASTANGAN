package com.java.KALAPASTANGAN.ui;
import com.java.KALAPASTANGAN.GameSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

// card window: reading, settings, credits
public class CardReading implements Disposable {
    public static final int READING = 0, SETTINGS = 1, CREDITS = 2;

    private static final String BACKGROUND_PATH = "Images/Backgrounds/Tarot/BACKGROUNDTAROT.png";
    private static final float FONT_QUALITY = 2f;
    private static final float TEXT_SPEED = 55f;

    private static final String[] NAMES = {
        "THE FOOL", "THE MAGICIAN", "THE HIGH PRIESTESS", "THE EMPRESS", "DEATH"
    };
    private static final String[] KEYWORDS = {
        "SIMULA  -  WALANG-MALAY  -  TIWALA",
        "KAPANGYARIHAN  -  KAPALIT  -  PAGBABAGO",
        "LIHIM  -  KATAHIMIKAN  -  BULONG",
        "KALINGA  -  KASAGANAAN  -  KULUNGAN",
        "WAKAS  -  PAGSISIMULA  -  PAGBABALIK"
    };
    private static final String[] TEXTS = {
        "Nakatayo ang Hangal sa gilid ng bangin at nakatingin sa langit. Hindi niya alam kung ano ang "
            + "naghihintay sa ibaba. Simula ito ng lahat ng paglalakbay: walang dalang kaalaman, walang takot... "
            + "at walang nagbabala sa kanya.",
        "Hawak niya ang lahat ng kailangan: tasa, espada, barya at tungkod. Kaya niyang baguhin ang anyo ng "
            + "mundo kung may sapat na lakas ng loob. Ngunit may kapalit ang bawat salamangka, at laging may "
            + "nagbabayad.",
        "Nakaupo siya sa pagitan ng dalawang haligi, hawak ang kasulatang hindi mo maaaring basahin. Alam niya "
            + "ang mga lihim na ayaw mong marinig. Kapag tahimik ang paligid, siya ang bumubulong.",
        "Ang ina ng lahat ng tumutubo. Kalinga, kasaganaan, at pagmamahal na hindi marunong bumitaw. "
            + "Ngunit minsan, ang pag-aaruga ay nagiging kulungan, at ang tahanan ay hindi na makalabas.",
        "Hindi ito ang katapusan, kundi ang pagsasara ng isang pinto. May mga bagay na kailangang mamatay "
            + "para may magsimula. Ngunit may mga bagay ring ayaw mamatay, at bumabalik sila sa dilim."
    };

    private static final String[] ROW_LABELS = { "MUSIC", "SFX", "TEXT SPEED", "FULLSCREEN" };
    private static final String[][] CREDIT_LINES = {
        { "STORY", "Andrei" },
        { "ART", "Andrei" },
        { "CODE", "Andrei" },
        { "MUSIC", "Your name here" }
    };

    private final Texture[] cards;
    private final TextureRegion[] regions;
    private final Texture background, soft, pixel;
    private final TextureRegion pixelRegion;
    private final BitmapFont titleFont, bodyFont, smallFont;
    private final GlyphLayout glyph = new GlyphLayout();

    private int mode = READING;
    private int index = -1;
    private boolean open;
    private float progress, time, textTime;
    private float startX, startY, startW, startH, startRotation;
    private boolean flipping, swapped;
    private float flipProgress;
    private int nextIndex;
    private List<String> lines;
    private float wrapWidth = -1f;
    private float visibleX, visibleY, visibleW, visibleH, mouseX, mouseY, lastMouseX = -1f, lastMouseY = -1f;
    private float windowX, windowY, windowW, windowH;
    private float cardX, cardY, cardW, cardH, panelX, panelY, panelW, panelH;
    private float sliderX, sliderW;
    private int selRow, dragRow = -1;

    public CardReading(Texture[] cards) {
        this.cards = cards;
        regions = new TextureRegion[cards.length];
        for (int i = 0; i < cards.length; i++) regions[i] = new TextureRegion(cards[i]);

        background = new Texture(Gdx.files.internal(BACKGROUND_PATH), true);
        background.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
        pixelRegion = new TextureRegion(pixel);

        soft = createSoft();
        titleFont = loadFont("Fonts/Trajan Pro Bold.ttf", 34);
        bodyFont = loadFont("Fonts/Type Machine.ttf", 22);
        smallFont = loadFont("Fonts/Trajan Pro Bold.ttf", 15);
    }

    private BitmapFont loadFont(String path, int size) {
        FileHandle file = Gdx.files.internal(path);
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(file);
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = Math.round(size * FONT_QUALITY);
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            BitmapFont font = generator.generateFont(parameter);
            font.getData().setScale(1f / FONT_QUALITY);
            font.setUseIntegerPositions(false);
            return font;
        } finally {
            generator.dispose();
        }
    }

    private Texture createSoft() {
        final int size = 32;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f) / size * 2f - 1f;
                float dy = (y + 0.5f) / size * 2f - 1f;
                float alpha = MathUtils.clamp(1f - (float) Math.sqrt(dx * dx + dy * dy), 0f, 1f);
                pixmap.setColor(1f, 1f, 1f, alpha * alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    // control

    public boolean isActive() {
        return open || progress > 0.001f;
    }

    // buksan, galing sa fan ang card
    public void open(int index, float x, float y, float width, float height, float rotation, int mode) {
        this.index = index;
        this.mode = mode;
        open = true;
        flipping = false;
        textTime = 0f;
        lines = null;
        selRow = 0;
        dragRow = -1;
        startX = x;
        startY = y;
        startW = width;
        startH = height;
        startRotation = rotation % 360f;
        if (startRotation > 180f) startRotation -= 360f;
        if (startRotation < -180f) startRotation += 360f;
    }

    public void close() {
        if (open && mode == SETTINGS) GameSettings.save();
        open = false;
        dragRow = -1;
    }

    private void shift(int direction) {
        if (mode != READING || flipping || progress < 0.9f || !open) return;
        nextIndex = (index + direction + cards.length) % cards.length;
        flipping = true;
        swapped = false;
        flipProgress = 0f;
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(1280, 720);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    private void adjust(int row, int dir) {
        if (row == 0) GameSettings.music = MathUtils.clamp(GameSettings.music + dir * 0.05f, 0f, 1f);
        else if (row == 1) GameSettings.sfx = MathUtils.clamp(GameSettings.sfx + dir * 0.05f, 0f, 1f);
        else if (row == 2) GameSettings.textSpeed = MathUtils.clamp(GameSettings.textSpeed + dir * 0.1f, 0.5f, 2f);
        else toggleFullscreen();
        GameSettings.save();
    }

    private void setSlider(int row, float x) {
        float n = MathUtils.clamp((x - sliderX) / sliderW, 0f, 1f);
        if (row == 0) GameSettings.music = n;
        else if (row == 1) GameSettings.sfx = n;
        else if (row == 2) GameSettings.textSpeed = 0.5f + n * 1.5f;
    }

    public void key(int k) {
        if (k == Input.Keys.ESCAPE) {
            close();
            return;
        }
        if (mode == SETTINGS) {
            if (k == Input.Keys.UP || k == Input.Keys.W) selRow = (selRow + 3) % 4;
            else if (k == Input.Keys.DOWN || k == Input.Keys.S) selRow = (selRow + 1) % 4;
            else if (k == Input.Keys.LEFT || k == Input.Keys.A) adjust(selRow, -1);
            else if (k == Input.Keys.RIGHT || k == Input.Keys.D) adjust(selRow, 1);
            else if ((k == Input.Keys.ENTER || k == Input.Keys.SPACE) && selRow == 3) adjust(3, 1);
        } else if (mode == READING) {
            if (k == Input.Keys.LEFT || k == Input.Keys.A) shift(-1);
            else if (k == Input.Keys.RIGHT || k == Input.Keys.D) shift(1);
        }
    }

    private static boolean contains(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    public void click(float x, float y) {
        if (!open || progress < 0.9f) return;
        if (mode == READING) {
            if (contains(x, y, windowX - 56f, windowY + windowH / 2f - 24f, 40f, 48f)) { shift(-1); return; }
            if (contains(x, y, windowX + windowW + 16f, windowY + windowH / 2f - 24f, 40f, 48f)) { shift(1); return; }
        }
        if (contains(x, y, windowX + windowW - 34f, windowY + windowH + 14f, 34f, 34f)) {
            close();
            return;
        }
        if (mode == SETTINGS) {
            for (int i = 0; i < 4; i++) {
                float cy = rowCenter(i);
                if (contains(x, y, panelX + 14f, cy - 24f, panelW - 28f, 48f)) {
                    selRow = i;
                    if (i < 3 && x >= sliderX - 14f && x <= sliderX + sliderW + 14f) {
                        dragRow = i;
                        setSlider(i, x);
                    } else if (i == 3) {
                        adjust(3, 1);
                    }
                    return;
                }
            }
        }
        if (!contains(x, y, windowX, windowY, windowW, windowH)) close();
    }

    public void drag(float x) {
        if (dragRow >= 0) setSlider(dragRow, x);
    }

    public void release() {
        if (dragRow >= 0) {
            dragRow = -1;
            GameSettings.save();
        }
    }

    private float rowCenter(int i) {
        return panelY + panelH - 140f - i * 58f;
    }

    private void layout() {
        windowW = Math.min(visibleW * 0.8f, 1040f);
        windowH = windowW * 0.41f;
        if (windowH > visibleH * 0.8f) {
            windowH = visibleH * 0.8f;
            windowW = windowH / 0.41f;
        }
        windowX = visibleX + (visibleW - windowW) / 2f;
        windowY = visibleY + (visibleH - windowH) / 2f;

        cardH = windowH * 0.9f;
        Texture texture = cards[index % cards.length];
        cardW = cardH * texture.getWidth() / texture.getHeight();
        cardX = windowX + windowH * 0.05f;
        cardY = windowY + (windowH - cardH) / 2f;
        panelX = cardX + cardW + 28f;
        panelW = windowX + windowW - windowH * 0.05f - panelX;
        panelY = cardY;
        panelH = cardH;
        sliderX = panelX + panelW * 0.40f;
        sliderW = panelW * 0.38f;
    }

    public void update(float delta, float mx, float my, float vx, float vy, float vw, float vh) {
        time += delta;
        boolean moved = mx != lastMouseX || my != lastMouseY;
        lastMouseX = mx;
        lastMouseY = my;
        mouseX = mx;
        mouseY = my;
        visibleX = vx;
        visibleY = vy;
        visibleW = vw;
        visibleH = vh;
        if (index < 0) return;
        layout();

        progress = MathUtils.clamp(progress + (open ? delta / 0.9f : -delta / 0.55f), 0f, 1f);
        if (flipping) {
            flipProgress += delta / 0.5f;
            if (!swapped && flipProgress >= 0.5f) {
                index = nextIndex;
                swapped = true;
                textTime = 0f;
                lines = null;
                layout();
            }
            if (flipProgress >= 1f) flipping = false;
        }
        if (progress > 0.9f) textTime += delta;

        if (mode == SETTINGS && open && progress > 0.9f && moved) {
            for (int i = 0; i < 4; i++) {
                if (contains(mx, my, panelX + 14f, rowCenter(i) - 24f, panelW - 28f, 48f)) selRow = i;
            }
        }
    }

    // drawing

    private static float ease(float value) {
        float clamped = MathUtils.clamp(value, 0f, 1f);
        float inverse = 1f - clamped;
        return 1f - inverse * inverse * inverse;
    }

    private void frame(SpriteBatch batch, float x, float y, float width, float height,
                       float thickness, float alpha) {
        batch.setColor(0.97f, 0.97f, 0.95f, alpha);
        batch.draw(pixel, x, y, width, thickness);
        batch.draw(pixel, x, y + height - thickness, width, thickness);
        batch.draw(pixel, x, y, thickness, height);
        batch.draw(pixel, x + width - thickness, y, thickness, height);
    }

    private void rotatedRect(SpriteBatch batch, float cx, float cy, float width, float height, float rotation) {
        batch.draw(pixelRegion, cx - width / 2f, cy - height / 2f,
            width / 2f, height / 2f, width, height, 1f, 1f, rotation);
    }

    private void diamond(SpriteBatch batch, float x, float y, float size, float alpha) {
        batch.setColor(0.97f, 0.97f, 0.95f, alpha);
        rotatedRect(batch, x, y, size, size, 45f);
    }

    private void cover(SpriteBatch batch, Texture texture, float x, float y, float width, float height) {
        float scale = Math.max(width / texture.getWidth(), height / texture.getHeight());
        float sourceW = width / scale, sourceH = height / scale;
        float u = (texture.getWidth() - sourceW) / (2f * texture.getWidth());
        float v = (texture.getHeight() - sourceH) / (2f * texture.getHeight());
        batch.draw(texture, x, y, width, height, u, v + sourceH / texture.getHeight(),
            u + sourceW / texture.getWidth(), v);
    }

    private void ensureWrap(float width) {
        if (lines != null && Math.abs(wrapWidth - width) < 0.5f) return;
        wrapWidth = width;
        lines = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String word : TEXTS[index % TEXTS.length].split(" ")) {
            String test = current.length() == 0 ? word : current + " " + word;
            glyph.setText(bodyFont, test);
            if (glyph.width > width && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
    }

    private void smallButton(SpriteBatch batch, float x, float y, float width, float height,
                             String label, float alpha) {
        boolean hover = contains(mouseX, mouseY, x, y, width, height);
        batch.setColor(0f, 0f, 0f, 0.7f * alpha);
        batch.draw(pixel, x, y, width, height);
        frame(batch, x, y, width, height, 1.5f, (hover ? 1f : 0.55f) * alpha);
        smallFont.setColor(1f, 1f, 1f, (hover ? 1f : 0.7f) * alpha);
        smallFont.draw(batch, label, x, y + height / 2f + smallFont.getCapHeight() / 2f,
            width, Align.center, false);
    }

    private void drawSettings(SpriteBatch batch, float offsetY, float alpha, float textX) {
        for (int i = 0; i < 4; i++) {
            float cy = rowCenter(i) + offsetY;
            boolean on = i == selRow;
            float ra = (on ? 1f : 0.62f) * alpha;
            if (on) {
                batch.setColor(1f, 1f, 1f, 0.07f * alpha);
                batch.draw(pixel, panelX + 14f, cy - 24f, panelW - 28f, 48f);
                frame(batch, panelX + 14f, cy - 24f, panelW - 28f, 48f, 1f, 0.35f * alpha);
                diamond(batch, panelX + 30f, cy, 8f, ra);
            }
            bodyFont.setColor(0.9f, 0.88f, 0.82f, ra);
            bodyFont.draw(batch, ROW_LABELS[i], textX + 10f, cy + bodyFont.getCapHeight() / 2f);

            if (i == 3) {
                bodyFont.draw(batch, Gdx.graphics.isFullscreen() ? "ON" : "OFF",
                    sliderX, cy + bodyFont.getCapHeight() / 2f);
                continue;
            }
            float n = i == 0 ? GameSettings.music
                : i == 1 ? GameSettings.sfx : (GameSettings.textSpeed - 0.5f) / 1.5f;
            batch.setColor(0.25f, 0.25f, 0.27f, ra);
            batch.draw(pixel, sliderX, cy - 2f, sliderW, 4f);
            batch.setColor(0.95f, 0.93f, 0.88f, ra);
            batch.draw(pixel, sliderX, cy - 2f, sliderW * n, 4f);
            batch.draw(pixel, sliderX + sliderW * n - 4f, cy - 10f, 8f, 20f);
            String value = i == 2 ? (Math.round(GameSettings.textSpeed * 10f) / 10f) + "x"
                : Math.round(n * 100f) + "%";
            bodyFont.draw(batch, value, sliderX + sliderW + 18f, cy + bodyFont.getCapHeight() / 2f);
        }
        bodyFont.setColor(0.9f, 0.88f, 0.82f, 1f);
    }

    private void drawCredits(SpriteBatch batch, float py, float alpha, float textX) {
        float y = py + panelH - 124f;
        for (int k = 0; k < CREDIT_LINES.length; k++) {
            float la = ease((textTime - 0.2f - k * 0.3f) / 0.5f) * alpha;
            smallFont.setColor(0.72f, 0.72f, 0.75f, la);
            smallFont.draw(batch, CREDIT_LINES[k][0], textX, y);
            bodyFont.setColor(0.95f, 0.93f, 0.88f, la);
            bodyFont.draw(batch, CREDIT_LINES[k][1], textX, y - 26f);
            y -= 62f;
        }
        float fa = ease((textTime - 1.8f) / 0.6f) * alpha;
        smallFont.setColor(0.72f, 0.72f, 0.75f, 0.8f * fa);
        smallFont.draw(batch, "MADE WITH LIBGDX", textX, py + 34f);
    }

    public void draw(SpriteBatch batch) {
        if (index < 0 || progress <= 0.001f) return;
        float alpha = ease(progress / 0.5f);
        float offsetY = (1f - alpha) * -18f;
        float windowY = this.windowY + offsetY;

        batch.setColor(0f, 0f, 0f, 0.82f * alpha);
        batch.draw(pixel, visibleX, visibleY, visibleW, visibleH);

        batch.setColor(1f, 1f, 1f, 0.92f * alpha);
        cover(batch, background, windowX, windowY, windowW, windowH);

        for (int i = 0; i < 18; i++) {
            float x = windowX + (i * 137.5f) % windowW;
            float y = windowY + (i * 71.3f + time * (8f + (i % 4) * 5f)) % windowH;
            batch.setColor(1f, 1f, 1f, (0.14f + 0.12f * (float) Math.sin(time * 2f + i)) * alpha);
            batch.draw(pixel, x, y, 2f, 2f);
        }

        frame(batch, windowX, windowY, windowW, windowH, 3f, alpha);
        frame(batch, windowX - 10f, windowY - 10f, windowW + 20f, windowH + 20f, 1f, 0.3f * alpha);

        // the card
        float cardProgress = ease((progress - 0.05f) / 0.7f);
        float bob = (float) Math.sin(time * 1.6f) * 4f * cardProgress;
        float targetX = cardX + cardW / 2f;
        float targetY = cardY + cardH / 2f + offsetY + bob;
        float cx = MathUtils.lerp(startX, targetX, cardProgress);
        float cy = MathUtils.lerp(startY, targetY, cardProgress);
        float width = MathUtils.lerp(startW, cardW, cardProgress);
        float height = MathUtils.lerp(startH, cardH, cardProgress);
        float tilt = MathUtils.clamp((mouseX - targetX) / 400f, -1f, 1f) * 3f * cardProgress;
        float rotation = MathUtils.lerp(startRotation, tilt, cardProgress);
        float flipScale = flipping
            ? Math.max(0.03f, Math.abs(MathUtils.cos(flipProgress * MathUtils.PI))) : 1f;
        width *= flipScale;

        batch.setColor(1f, 1f, 1f, (0.16f + 0.06f * (float) Math.sin(time * 2f)) * cardProgress);
        batch.draw(soft, cx - width * 1.1f, cy - height * 0.75f, width * 2.2f, height * 1.5f);
        batch.setColor(0f, 0f, 0f, 0.6f * cardProgress);
        rotatedRect(batch, cx + 10f, cy - 12f, width + 16f, height + 16f, rotation);
        batch.setColor(0.97f, 0.97f, 0.95f, cardProgress);
        rotatedRect(batch, cx, cy, width + 16f, height + 16f, rotation);
        batch.setColor(0.02f, 0.02f, 0.03f, cardProgress);
        rotatedRect(batch, cx, cy, width + 8f, height + 8f, rotation);
        batch.setColor(Color.WHITE);
        batch.draw(regions[index], cx - width / 2f, cy - height / 2f,
            width / 2f, height / 2f, width, height, 1f, 1f, rotation);

        // the panel
        float panelAlpha = ease((progress - 0.45f) / 0.45f);
        if (panelAlpha > 0.01f) {
            float panelWidth = panelW * panelAlpha;
            float panelY = this.panelY + offsetY;
            batch.setColor(0.02f, 0.02f, 0.03f, 0.97f);
            batch.draw(pixel, panelX, panelY, panelWidth, panelH);
            frame(batch, panelX, panelY, panelWidth, panelH, 3f, 1f);
            if (panelWidth > 40f) {
                frame(batch, panelX + 8f, panelY + 8f, panelWidth - 16f, panelH - 16f, 1f, 0.28f);
            }
            diamond(batch, panelX, panelY, 10f, 0.9f);
            diamond(batch, panelX + panelWidth, panelY, 10f, 0.9f);
            diamond(batch, panelX, panelY + panelH, 10f, 0.9f);
            diamond(batch, panelX + panelWidth, panelY + panelH, 10f, 0.9f);

            float contentAlpha = ease((progress - 0.8f) / 0.2f) * flipScale;
            if (contentAlpha > 0.01f) {
                float textX = panelX + 32f;
                String title = mode == READING ? NAMES[index % NAMES.length]
                    : mode == SETTINGS ? "SETTINGS" : "CREDITS";
                String keywords = mode == READING ? KEYWORDS[index % KEYWORDS.length]
                    : mode == SETTINGS ? "SOUND  -  TEXT  -  SCREEN" : "ANG MGA NASA LIKOD NG MGA BARAHA";

                titleFont.setColor(1f, 1f, 1f, contentAlpha);
                titleFont.draw(batch, title, textX, panelY + panelH - 34f);
                batch.setColor(1f, 1f, 1f, 0.6f * contentAlpha);
                batch.draw(pixel, textX, panelY + panelH - 62f, 200f * contentAlpha, 1.5f);
                smallFont.setColor(0.72f, 0.72f, 0.75f, contentAlpha);
                smallFont.draw(batch, keywords, textX, panelY + panelH - 80f);

                if (mode == READING) {
                    ensureWrap(panelW - 64f);
                    int remaining = (int) (textTime * TEXT_SPEED);
                    float lineY = panelY + panelH - 120f;
                    float lineHeight = bodyFont.getLineHeight() * 1.1f;
                    bodyFont.setColor(0.9f, 0.88f, 0.82f, contentAlpha);
                    for (int i = 0; i < lines.size() && remaining > 0; i++) {
                        String line = lines.get(i);
                        int count = Math.min(remaining, line.length());
                        bodyFont.draw(batch, line.substring(0, count), textX, lineY - i * lineHeight);
                        remaining -= line.length();
                    }
                } else if (mode == SETTINGS) {
                    drawSettings(batch, offsetY, contentAlpha, panelX + 20f);
                } else {
                    drawCredits(batch, panelY, contentAlpha, textX);
                }
            }
        }

        // buttons and hint
        float buttonAlpha = ease((progress - 0.85f) / 0.15f);
        if (buttonAlpha > 0.01f) {
            if (mode == READING) {
                smallButton(batch, windowX - 56f, windowY + windowH / 2f - 24f, 40f, 48f, "<", buttonAlpha);
                smallButton(batch, windowX + windowW + 16f, windowY + windowH / 2f - 24f, 40f, 48f, ">", buttonAlpha);
            }
            smallButton(batch, windowX + windowW - 34f, windowY + windowH + 14f, 34f, 34f, "X", buttonAlpha);
            String hint = mode == READING ? "A D  -  IBANG BARAHA          ESC  -  ISARA"
                : mode == SETTINGS ? "W S  -  PUMILI          A D  -  BAGUHIN          ESC  -  ISARA"
                : "ESC  -  ISARA";
            smallFont.setColor(1f, 1f, 1f, 0.55f * buttonAlpha);
            smallFont.draw(batch, hint, windowX, windowY - 22f, windowW, Align.center, false);
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        background.dispose();
        soft.dispose();
        pixel.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        smallFont.dispose();
    }
}
