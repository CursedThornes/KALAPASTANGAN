package com.java.KALAPASTANGAN.ui;
import com.java.KALAPASTANGAN.Main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// content warning, ito ang unang nakikita
public class WarningScreen extends ScreenAdapter {
    // Tweak these
    private static final float LOCK_TIME = 5f;     // seconds before "press any key" appears
    private static final float MENU_LOCK_TIME = 1f;  // wait when reopened from the menu
    private static final float FADE_IN = 0.8f;     // seconds to fade in from black
    private static final float FADE_OUT = 0.5f;    // seconds to fade out after a key press
    private static final float FLASH_SPEED = 1.2f; // prompt flashes per second
    private static final float TEXT_LEFT = 60f;
    private static final float TEXT_WIDTH = Main.WORLD_WIDTH - TEXT_LEFT * 2f;

    // grey ng title at prompt
    private static final Color GRAY = new Color(0.36f, 0.36f, 0.36f, 1f);

    // corners, lapad ng piraso
    private static final String BORDER_DIR = "Images/UI/WarningFrames/";
    private static final float BORDER_W = 255f;
    private static final float BORDER_MARGIN = 3f;  // gap between a border and the screen edge
    private static final float BORDER_ALPHA = 0.9f; // 1 = full brightness

    // font sa assets, sukat sa world units (1280x720)
    private static final String REGULAR_FONT_PATH = "Fonts/Trajan Pro Regular.ttf";
    private static final String PUNK_FONT_PATH = "Fonts/Helvetica Punk.ttf";
    private static final int TITLE_SIZE = 82;
    private static final int SUBTITLE_SIZE = 30;
    private static final int BODY_SIZE = 34;
    private static final int PROMPT_SIZE = 64;
    private static final float TITLE_TOP_MARGIN = 55f;
    private static final float PROMPT_BOTTOM_MARGIN = 125f;

    // x3 para sharp sa 1080p/1440p. 1f kung tipid sa memory
    private static final float FONT_QUALITY = 2f;

    private static final String TITLE = "CONTENT WARNING";
    private static final String SUBTITLE = "PLEASE READ BEFORE PLAYING";
    private static final String BODY =
        "KALAPASTANGAN is a fictional story that explores sensitive and emotionally difficult subjects.\n"
        + "This game contains themes of bullying, emotional abuse, trauma, social isolation, "
        + "psychological distress, and suicide.\n"
        + "Some scenes and dialogue may be upsetting or uncomfortable. Player discretion is advised.\n"
        + "Please take a break or stop playing if the experience becomes overwhelming.";
    private static final String PROMPT = "! PRESS ANY KEY TO CONTINUE !";

    // picture na naka-crop sa totoong laman
    private static class Art {
        Texture texture;
        TextureRegion region;
    }

    private final Main game;
    private final boolean fromMenu; // true when opened from the menu: short wait, returns to the menu
    private final float lockTime;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new ExtendViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);

    private final Texture pixel; // 1x1 texture used for the black fade overlay
    private final Art topLeft, topRight, bottomLeft, bottomRight;
    private final BitmapFont titleFont, subFont, bodyFont, promptFont;

    // layout, isang beses lang
    private final float textX;
    private final float titleY, subY, bodyY, promptY;

    // visible area, pwedeng lumaki sa 1280x720
    private float visX = 0f, visY = 0f, visW = Main.WORLD_WIDTH, visH = Main.WORLD_HEIGHT;

    private float time = 0f;
    private boolean leaving = false;
    private boolean transitionStarted = false;
    private float leaveTimer = 0f;

    public WarningScreen(Main game) {
        this(game, false);
    }

    public WarningScreen(Main game, boolean fromMenu) {
        this.game = game;
        this.fromMenu = fromMenu;
        this.lockTime = fromMenu ? MENU_LOCK_TIME : LOCK_TIME;

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        topLeft = loadArt(BORDER_DIR + "TOPLEFTWARNING.png");
        topRight = loadArt(BORDER_DIR + "TOPRIGHTWARNING.png");
        bottomLeft = loadArt(BORDER_DIR + "LEFTBOTTOMWARNING.png");
        bottomRight = loadArt(BORDER_DIR + "RIGHTBOTTOMWARNING.png");

        FreeTypeFontGenerator punkGenerator = new FreeTypeFontGenerator(Gdx.files.internal(PUNK_FONT_PATH));
        titleFont = makeFont(punkGenerator, TITLE_SIZE, GRAY);
        promptFont = makeFont(punkGenerator, PROMPT_SIZE, GRAY);
        punkGenerator.dispose();
        FreeTypeFontGenerator regularGenerator = new FreeTypeFontGenerator(Gdx.files.internal(REGULAR_FONT_PATH));
        subFont = makeFont(regularGenerator, SUBTITLE_SIZE, Color.WHITE);
        bodyFont = makeFont(regularGenerator, BODY_SIZE, Color.WHITE);
        regularGenerator.dispose();

        textX = TEXT_LEFT;
        titleY = Main.WORLD_HEIGHT - TITLE_TOP_MARGIN;
        subY = titleY - measure(titleFont, TITLE) - 24f;
        bodyY = subY - measure(subFont, SUBTITLE) - 46f;
        promptY = PROMPT_BOTTOM_MARGIN;
    }

    // assets

    // load at crop sa visible pixels
    private Art loadArt(String path) {
        Pixmap pm = new Pixmap(Gdx.files.internal(path));
        int w = pm.getWidth(), h = pm.getHeight();

        int[] box = findBounds(pm, false);
        boolean wholeImage = box[0] == 0 && box[1] == 0 && box[2] == w - 1 && box[3] == h - 1;
        if (box[2] < 0 || wholeImage) { // no transparency: treat a black background as empty
            int[] bright = findBounds(pm, true);
            if (bright[2] >= 0) box = bright;
        }
        if (box[2] < 0) box = new int[] { 0, 0, w - 1, h - 1 };

        Art art = new Art();
        art.texture = new Texture(pm, true);
        art.texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        art.region = new TextureRegion(art.texture, box[0], box[1], box[2] - box[0] + 1, box[3] - box[1] + 1);
        pm.dispose();
        return art;
    }

    // {minX, minY, maxX, maxY}, -1 kung wala
    private static int[] findBounds(Pixmap pm, boolean useBrightness) {
        int w = pm.getWidth(), h = pm.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int px = pm.getPixel(x, y); // RGBA8888
                int alpha = px & 0xff;
                if (alpha <= 16) continue;
                if (useBrightness) {
                    int sum = ((px >>> 24) & 0xff) + ((px >>> 16) & 0xff) + ((px >>> 8) & 0xff);
                    if (sum / 3 <= 40) continue;
                }
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        return new int[] { minX, minY, maxX, maxY };
    }

    private BitmapFont makeFont(FreeTypeFontGenerator generator, int size, Color color) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = Math.round(size * FONT_QUALITY);
        p.genMipMaps = true;
        p.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        p.magFilter = Texture.TextureFilter.Linear;

        BitmapFont font = generator.generateFont(p);
        font.getData().setScale(1f / FONT_QUALITY);
        font.setUseIntegerPositions(false);
        font.setColor(color);
        return font;
    }

    private float measure(BitmapFont font, String text) {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text, font.getColor(), TEXT_WIDTH, Align.center, true);
        return layout.height;
    }

    // drawing helpers

    private void updateVisibleArea() {
        visW = viewport.getWorldWidth();
        visH = viewport.getWorldHeight();
        visX = Main.WORLD_WIDTH / 2f - visW / 2f;
        visY = Main.WORLD_HEIGHT / 2f - visH / 2f;
    }

    // corner piece
    private void drawCorner(Art art, boolean right, boolean top) {
        float h = BORDER_W * art.region.getRegionHeight() / art.region.getRegionWidth();
        float x = right ? visX + visW - BORDER_W - BORDER_MARGIN : visX + BORDER_MARGIN;
        float y = top ? visY + visH - h - BORDER_MARGIN : visY + BORDER_MARGIN;
        game.batch.setColor(1f, 1f, 1f, BORDER_ALPHA);
        game.batch.draw(art.region, x, y, BORDER_W, h);
    }

    // screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                return tryContinue(keycode);
            }

            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                return tryContinue(-1);
            }
        });
    }

    private boolean tryContinue(int keycode) {
        if (keycode == Input.Keys.F11) return false;       // F11 is handled by Main for the window mode
        if (time < lockTime || leaving) return false;      // still locked
        leaving = true;
        return true;
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        time += delta;
        updateVisibleArea();

        float overlay; // 1 = fully black, 0 = fully visible
        if (leaving) {
            leaveTimer += delta;
            overlay = MathUtils.clamp(leaveTimer / FADE_OUT, 0f, 1f);
            if (leaveTimer >= FADE_OUT && !transitionStarted) {
                transitionStarted = true;
                game.transitionTo(fromMenu ? new MenuScreen(game) : new BuiltWithScreen(game));
                return;
            }
        } else {
            overlay = 1f - MathUtils.clamp(time / FADE_IN, 0f, 1f);
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Corner borders
        drawCorner(topLeft, false, true);
        drawCorner(topRight, true, true);
        drawCorner(bottomLeft, false, false);
        drawCorner(bottomRight, true, false);
        game.batch.setColor(Color.WHITE);

        titleFont.draw(game.batch, TITLE, textX, titleY, TEXT_WIDTH, Align.center, true);
        subFont.draw(game.batch, SUBTITLE, textX, subY, TEXT_WIDTH, Align.center, true);
        bodyFont.draw(game.batch, BODY, textX, bodyY, TEXT_WIDTH, Align.center, true);

        // prompt pagkalipas ng LOCK_TIME, tapos kumikislap
        float t = time - lockTime;
        if (t >= 0f) {
            float appear = MathUtils.clamp(t / 0.4f, 0f, 1f);
            float pulse = 0.5f - 0.5f * MathUtils.cos(t * MathUtils.PI2 * FLASH_SPEED);
            promptFont.setColor(GRAY.r, GRAY.g, GRAY.b, appear * (0.3f + 0.7f * pulse));
            promptFont.draw(game.batch, PROMPT, 0, promptY, Main.WORLD_WIDTH, Align.center, false);
        }

        // fade overlay, huling iguguhit
        if (overlay > 0f) {
            game.batch.setColor(0f, 0f, 0f, overlay);
            game.batch.draw(pixel, visX, visY, visW, visH);
            game.batch.setColor(Color.WHITE);
        }

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        camera.position.set(Main.WORLD_WIDTH / 2f, Main.WORLD_HEIGHT / 2f, 0f);
        camera.update();
        updateVisibleArea();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        pixel.dispose();
        topLeft.texture.dispose();
        topRight.texture.dispose();
        bottomLeft.texture.dispose();
        bottomRight.texture.dispose();
        titleFont.dispose();
        subFont.dispose();
        bodyFont.dispose();
        promptFont.dispose();
    }
}