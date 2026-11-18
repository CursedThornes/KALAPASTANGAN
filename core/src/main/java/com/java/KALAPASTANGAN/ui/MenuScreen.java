package com.java.KALAPASTANGAN.ui;
import com.java.KALAPASTANGAN.GameScreen;
import com.java.KALAPASTANGAN.battle.BeatViz;
import com.java.KALAPASTANGAN.battle.AustineBossBattle;
import com.java.KALAPASTANGAN.GameSettings;
import com.java.KALAPASTANGAN.Main;

import java.io.FileNotFoundException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

// main menu: title sa taas, limang baraha, isa kada option
public class MenuScreen extends ScreenAdapter {
    // ===================== Tweak these =====================
    private static final String[] OPTIONS = { "CONTINUE", "NEW GAME", "SETTINGS", "CREDITS", "EXIT" };
    private static final int CONTINUE = 0, NEW_GAME = 1, SETTINGS = 2, CREDITS = 3, EXIT = 4;
    // isang card kada option, pareho ng order sa OPTIONS
    private static final String[] CARD_NAMES = { "FOOL", "MAGICIAN", "HIGHPRIEST", "EMPRESS", "DEATH" };
    private static final String[] TAROT_NAMES = {
        "THE FOOL", "THE MAGICIAN", "THE HIGH PRIESTESS", "THE EMPRESS", "DEATH"
    };
    private static final String[] ROMAN = {
        "XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"
    };

    private static final String CARD_DIR = "Images/Cards/Tarot/";
    private static final float LONG_HAND_PIVOT_X = 0.4762f, LONG_HAND_PIVOT_Y = 0.9204f;
    private static final float MEDIUM_HAND_PIVOT_X = 0.4901f, MEDIUM_HAND_PIVOT_Y = 0.8817f;
    private static final String CARD_DRAW_SOUND = "Audio/SFX/Cards/CardDraw.MP3";
    private static final String CHOSEN_SOUND = "Audio/SFX/Menu/CHOSEN.MP3";
    private static final String OPTION_FONT_PATH = "Fonts/Trajan Pro Bold.ttf";
    private static final float FONT_QUALITY = 2f;

    // Title
    private static final float TITLE_W = 1150f;        // much bigger title
    private static final float TITLE_TOP = 30f;        // gap between the top of the screen and the title

    // corners, mirrored
    private static final float TOP_BORDER_W = 312f;
    private static final float BOTTOM_BORDER_W = 303f;
    private static final float BORDER_MARGIN = 3f;

    // Fan
    private static final float CARD_H = 450f;
    private static final float FAN_STEP = 15f;         // degrees between neighbouring cards
    private static final float FAN_SHIFT_X = 0f;      // move the whole fan left or right
    private static final float FAN_GAP = 26f;          // space between the title and the lifted card
    private static final float FAN_MIN_Y = 95f;        // limits for the height of the card centres
    private static final float FAN_MAX_Y = 300f;
    private static final float LIFT = 80f;             // how far the hovered card is pulled up
    private static final float SINK = 64f;             // how far the unhovered cards go down
    private static final float SPREAD_X = 34f;         // how far the others slide away sideways
    private static final float POP_SCALE = 0.16f;     // how much the hovered card scales up
    private static final float HOVER_DWELL = 0.05f;   // snappy hover response

    private static final float PAR_CARD = 14f;
    private static final float PAR_TITLE = 18f;
    private static final float PAR_BG = 24f;

    // Background
    private static final int SNOW_COUNT = 90;          // set to 0 for no snow particles
    private static final boolean SHOW_LIGHTNING = true;
    private static final int LIGHTNING_SEGMENT_COUNT = 7;

    private static final float FADE_IN = 0.4f;
    private static final float FADE_OUT = 0.35f;
    private static final int GRAIN_SIZE = 256;
    private static final String BEAT_VIZ_PATH = "Audio/Music/MenuOST.viz";
    private static final float GOTH_INTENSITY = 1f;
    private static final boolean PLAY_INTRO = true;
    private static boolean introPlayed;
    private static final float MONO = 1f;
    private static final String MONO_VERT =
        "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\nvarying vec4 v_color;\nvarying vec2 v_texCoords;\n"
            + "void main(){v_color=a_color;v_color.a=v_color.a*(255.0/254.0);"
            + "v_texCoords=a_texCoord0;gl_Position=u_projTrans*a_position;}";
    private static final String MONO_FRAG =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
            + "varying vec4 v_color;\nvarying vec2 v_texCoords;\nuniform sampler2D u_texture;\n"
            + "uniform float u_mono;\n"
            + "void main(){vec4 c=v_color*texture2D(u_texture,v_texCoords);"
            + "float g=clamp((dot(c.rgb,vec3(0.299,0.587,0.114))-0.5)*1.15+0.5,0.0,1.0);"
            + "gl_FragColor=vec4(mix(c.rgb,vec3(g),u_mono),c.a);}";
    // =======================================================

    private static class Art {
        Texture texture;
        TextureRegion region;
    }

    private final Main game;
    private final Music menuMusic;
    private final BeatViz beatViz;
    private final ShaderProgram monoShader;
    private final MenuIntro intro;
    private final Sound cardDrawSfx, chosenSfx;
    private final Sound introSfx, introTickSfx;
    private final VideoPlayer snowBackground;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new ExtendViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);

    private final Art titleArt, menuIndicator, topBorder, bottomBorder;
    private final Texture longClockHandTexture, mediumClockHandTexture;
    private final TextureRegion longClockHand, mediumClockHand;
    private final Texture[] cardTextures = new Texture[CARD_NAMES.length];
    private final TextureRegion[] cardRegions = new TextureRegion[CARD_NAMES.length];
    private final float[] cardW = new float[CARD_NAMES.length];
    private final float[] cardPop = new float[CARD_NAMES.length];
    private final float[] cardSink = new float[CARD_NAMES.length];
    private final float[] sinkDir = new float[CARD_NAMES.length];
    private final Texture soft, grain, vignette, pixel, ring, bloodVignette;
    private final TextureRegion softRegion;
    private final TextureRegion pixelRegion, ringRegion;
    private final BitmapFont optionFont, smallFont;
    private final CardReading reading;
    private final Sound hoverSfx, clickSfx;

    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mouse = new Vector2();
    private final InputAdapter inputProcessor = new InputAdapter() {
        @Override
        public boolean keyDown(int k) {
            if (!introHandedOff) { intro.skip(); return true; }
            if (chosen >= 0) return true;
            if (exitConfirmation) {
                if (k == Input.Keys.ESCAPE || k == Input.Keys.BACKSPACE) {
                    exitConfirmation = false;
                } else if (k == Input.Keys.LEFT || k == Input.Keys.RIGHT
                    || k == Input.Keys.A || k == Input.Keys.D) {
                    exitSelection = 1 - exitSelection;
                    play(hoverSfx, 0.35f, 1f);
                } else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) {
                    confirmExitSelection();
                } else if (k == Input.Keys.Y) {
                    Gdx.app.exit();
                } else if (k == Input.Keys.N) {
                    exitConfirmation = false;
                } else {
                    return false;
                }
                return true;
            }
            if (reading.isActive()) {
                reading.key(k);
                return true;
            }
            if (k == Input.Keys.LEFT || k == Input.Keys.A) moveFocus(-1);
            else if (k == Input.Keys.RIGHT || k == Input.Keys.D) moveFocus(1);
            else if ((k == Input.Keys.ENTER || k == Input.Keys.SPACE) && focus >= 0) activate(focus);
            else return false;
            return true;
        }

        @Override
        public boolean touchDown(int sx, int sy, int pointer, int button) {
            if (!introHandedOff) { intro.skip(); return true; }
            if (chosen >= 0) return true;
            toWorld(sx, sy);
            if (exitConfirmation) {
                if (button == Input.Buttons.LEFT) {
                    int selection = exitButtonAt(mouse.x, mouse.y);
                    if (selection >= 0) {
                        exitSelection = selection;
                        confirmExitSelection();
                    }
                }
                return true;
            }
            if (reading.isActive()) {
                if (button == Input.Buttons.LEFT) reading.click(mouse.x, mouse.y);
                return true;
            }
            int c = cardAt();
            if (c < 0) return true;
            focus = c;
            if (button == Input.Buttons.RIGHT) {
                play(clickSfx, 0.4f, 0.8f);
                openWindow(c, CardReading.READING);
            }
            else if (button == Input.Buttons.LEFT) activate(c);
            return true;
        }

        @Override
        public boolean touchDragged(int sx, int sy, int pointer) {
            if (reading.isActive()) {
                toWorld(sx, sy);
                reading.drag(mouse.x);
            }
            return true;
        }

        @Override
        public boolean touchUp(int sx, int sy, int pointer, int button) {
            reading.release();
            return true;
        }
    };
    private final Matrix4 beatTransform = new Matrix4();
    private final float[] geo = new float[5]; // cx, cy, w, h, rotation of one fan card

    private final float[] snowX = new float[SNOW_COUNT], snowY = new float[SNOW_COUNT];
    private final float[] snowV = new float[SNOW_COUNT], snowS = new float[SNOW_COUNT];
    private final float[] snowP = new float[SNOW_COUNT];
    private final float[] snowKick = new float[SNOW_COUNT];

    private final float[] lightningX = new float[LIGHTNING_SEGMENT_COUNT + 1];
    private final float[] lightningY = new float[LIGHTNING_SEGMENT_COUNT + 1];
    private float lightningCooldown = 6f, lightningTime = -1f;

    private float visX = 0f, visY = 0f, visW = Main.WORLD_WIDTH, visH = Main.WORLD_HEIGHT;
    private float titleW, titleH, titleCy, fanCy;
    private boolean layoutDirty = true;
    private float time, grainU, grainV, labelA, noSaveT, clickFlash, parX, parY;
    private float beatShakeX, beatShakeY, beatZoom = 1f;
    private float lastSnowPunch;
    private float dropT, dropSlow, dropCooldown, goPunch, sigilRot;
    private float gothR = 330f, clockJolt;
    private float introFlash, entryT = 10f, clockDir = 1f, hourSpin, minSpin, secSpin, secKick;
    private boolean clockInitialized;
    private boolean introHandedOff;
    private float flickerCooldown = 5f, flickerTime;
    private int lastMx = -1, lastMy = -1, lastFocus = -1;
    private float hoverTimer = 0f;
    private int hoverTarget = -1;
    private boolean hasSave = false; // set to true once you have a save system
    private int focus = -1;          // the card that is popped out, or -1
    private boolean keyboardFocus;
    private int chosen = -1;
    private float chosenTimer;
    private boolean choiceFinished;
    private boolean snowBackgroundStarted;
    private boolean exitConfirmation;
    private int exitSelection = 1;

    public MenuScreen(Main game) {
        this.game = game;
        GameSettings.load();
        snowBackground = loadSnowBackground();
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("Audio/Music/MenuOST.mp3"));
        beatViz = BeatViz.load(BEAT_VIZ_PATH);
        cardDrawSfx = loadSound(CARD_DRAW_SOUND);
        chosenSfx = loadSound(CHOSEN_SOUND);

        titleArt = loadArt("Images/Branding/GameTitle/KALAPASTANGAN TEXT.png");
        menuIndicator = loadArt("Images/UI/MenuIndicator/IndicatorMenu.png");
        topBorder = loadArt("Images/UI/Menu/TOPBORDER1.png");
        bottomBorder = loadArt("Images/UI/Menu/BOTTOMBORDER1.png");
        longClockHandTexture = new Texture(Gdx.files.internal("Images/UI/Menu/LongHand.png"), true);
        mediumClockHandTexture = new Texture(Gdx.files.internal("Images/UI/Menu/MediumHand.png"), true);
        longClockHandTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        mediumClockHandTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        longClockHand = new TextureRegion(longClockHandTexture);
        mediumClockHand = new TextureRegion(mediumClockHandTexture);

        for (int i = 0; i < CARD_NAMES.length; i++) {
            Texture t = new Texture(Gdx.files.internal(CARD_DIR + CARD_NAMES[i] + ".jpg"), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            cardTextures[i] = t;
            cardRegions[i] = new TextureRegion(t);
            cardW[i] = CARD_H * t.getWidth() / t.getHeight();
        }

        soft = MenuTextures.softCircle();
        softRegion = new TextureRegion(soft);
        grain = MenuTextures.grain(GRAIN_SIZE);
        vignette = MenuTextures.vignette();
        ring = MenuTextures.ring();
        ringRegion = new TextureRegion(ring);
        bloodVignette = MenuTextures.bloodVignette();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        pixelRegion = new TextureRegion(pixel);

        FileHandle fontFile = findFont("trajan pro bold", OPTION_FONT_PATH);
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fontFile);
        optionFont = makeFont(gen, 30);
        smallFont = makeFont(gen, 15);
        gen.dispose();
        monoShader = createMonoShader();

        hoverSfx = loadSound("Audio/SFX/Menu/hover.wav");
        clickSfx = loadSound("Audio/SFX/Menu/click.wav");
        introSfx = loadSound("Audio/SFX/Menu/intro.wav");
        introTickSfx = loadSound("Audio/SFX/Menu/clock-tick.wav");
        intro = new MenuIntro(pixelRegion, softRegion, ringRegion, longClockHand, mediumClockHand,
            LONG_HAND_PIVOT_X, LONG_HAND_PIVOT_Y, MEDIUM_HAND_PIVOT_X, MEDIUM_HAND_PIVOT_Y,
            optionFont, introTickSfx, introSfx);
        introHandedOff = !(PLAY_INTRO && !introPlayed);
        introPlayed = true;

        for (int i = 0; i < SNOW_COUNT; i++) {
            snowX[i] = MathUtils.random();
            snowY[i] = MathUtils.random();
            snowV[i] = MathUtils.random(0.03f, 0.09f);
            snowS[i] = MathUtils.random(1.5f, 3.2f);
            snowP[i] = MathUtils.random(6.28f);
        }

        reading = new CardReading(cardTextures);
        layoutDirty = true;
    }

    private VideoPlayer loadSnowBackground() {
        FileHandle file = Gdx.files.internal("Video/Backgrounds/SnowBackground.webm");
        VideoPlayer player = VideoPlayerCreator.createVideoPlayer();
        if (player == null) {
            throw new IllegalStateException("Unable to create the menu background video player");
        }

        player.setLooping(true);
        player.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        try {
            if (!player.load(file)) {
                player.dispose();
                throw new IllegalStateException("Unable to load menu background video: " + file.path());
            }
            player.setVolume(0f);
        } catch (FileNotFoundException exception) {
            player.dispose();
            throw new IllegalStateException("Unable to load menu background video: " + file.path(), exception);
        }
        return player;
    }

    // assets

    private ShaderProgram createMonoShader() {
        boolean previousPedantic = ShaderProgram.pedantic;
        ShaderProgram shader;
        ShaderProgram.pedantic = false;
        try {
            shader = new ShaderProgram(MONO_VERT, MONO_FRAG);
        } finally {
            ShaderProgram.pedantic = previousPedantic;
        }
        if (shader.isCompiled()) return shader;

        Gdx.app.error("MenuScreen", "Monochrome shader failed to compile: " + shader.getLog());
        shader.dispose();
        return null;
    }

    private Sound loadSound(String path) {
        FileHandle file = Gdx.files.internal(path);
        return file.exists() ? Gdx.audio.newSound(file) : null;
    }

    private void play(Sound sound, float volume, float pitch) {
        if (sound != null) sound.play(volume * GameSettings.sfx, pitch, 0f);
    }

    private Art loadArt(String path) {
        Pixmap pm = new Pixmap(Gdx.files.internal(path));
        int w = pm.getWidth(), h = pm.getHeight();
        int[] box = findBounds(pm, false);
        boolean whole = box[0] == 0 && box[1] == 0 && box[2] == w - 1 && box[3] == h - 1;
        if (box[2] < 0 || whole) {
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

    private static int[] findBounds(Pixmap pm, boolean useBrightness) {
        int w = pm.getWidth(), h = pm.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int px = pm.getPixel(x, y);
                if ((px & 0xff) <= 16) continue;
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

    private FileHandle findFont(String keyword, String defaultPath) {
        FileHandle exact = Gdx.files.internal(defaultPath);
        if (exact.exists()) return exact;
        FileHandle dir = Gdx.files.internal("Fonts");
        if (dir.exists()) {
            for (FileHandle f : dir.list()) {
                String name = f.name().toLowerCase();
                if (name.contains(keyword) && (name.endsWith(".ttf") || name.endsWith(".otf"))) return f;
            }
        }
        return exact;
    }

    private BitmapFont makeFont(FreeTypeFontGenerator generator, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = Math.round(size * FONT_QUALITY);
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u201C\u201D\u2019\u2014";
        p.genMipMaps = false;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(p);
        font.getData().setScale(1f / FONT_QUALITY);
        font.setUseIntegerPositions(false);
        return font;
    }

    // helpers

    private void updateVisibleArea() {
        visW = viewport.getWorldWidth();
        visH = viewport.getWorldHeight();
        visX = Main.WORLD_WIDTH / 2f - visW / 2f;
        visY = Main.WORLD_HEIGHT / 2f - visH / 2f;
    }

    private void computeLayout() {
        titleW = Math.min(TITLE_W, visW * 0.95f);
        titleH = artHeight(titleArt, titleW);
        titleCy = visY + visH - TITLE_TOP - titleH / 2f;
        float titleBottom = titleCy - titleH / 2f;
        float restTop = titleBottom - FAN_GAP - LIFT - CARD_H * POP_SCALE / 2f;
        fanCy = MathUtils.clamp(restTop - CARD_H / 2f, visY + FAN_MIN_Y, visY + FAN_MAX_Y);
    }

    private void toWorld(int sx, int sy) {
        mouse.set(sx, sy);
        viewport.unproject(mouse);
        float centerX = visX + visW / 2f, centerY = visY + visH / 2f;
        mouse.x = centerX + (mouse.x - centerX - beatShakeX) / beatZoom;
        mouse.y = centerY + (mouse.y - centerY - beatShakeY) / beatZoom;
    }

    private boolean pointerAvailable() {
        int x = Gdx.input.getX(), y = Gdx.input.getY();
        return game.isWindowActive() && x >= 0 && y >= 0
            && x < Gdx.graphics.getWidth() && y < Gdx.graphics.getHeight();
    }

    private static float artHeight(Art art, float width) {
        return width * art.region.getRegionHeight() / art.region.getRegionWidth();
    }

    private static float ease(float v) {
        float c = MathUtils.clamp(v, 0f, 1f);
        float i = 1f - c;
        return 1f - i * i * i;
    }

    private void drawArtCentered(Art art, float cx, float cy, float w, float alpha) {
        float h = artHeight(art, w);
        game.batch.setColor(1f, 1f, 1f, alpha);
        game.batch.draw(art.region, cx - w / 2f, cy - h / 2f, w, h);
    }

    private void drawBorderCorner(SpriteBatch batch, Art art, boolean right, boolean top, float width) {
        float height = artHeight(art, width);
        float x = right ? visX + visW - width - BORDER_MARGIN : visX + BORDER_MARGIN;
        float y = top ? visY + visH - height - BORDER_MARGIN : visY + BORDER_MARGIN;
        batch.setColor(Color.WHITE);
        for (int pass = 0; pass < 3; pass++) {
            batch.draw(art.region, x, y, width / 2f, height / 2f, width, height,
                right ? -1f : 1f, 1f, 0f);
        }
    }

    private void drawMenuBorders(SpriteBatch batch) {
        drawBorderCorner(batch, topBorder, false, true, TOP_BORDER_W);
        drawBorderCorner(batch, topBorder, true, true, TOP_BORDER_W);
        drawBorderCorner(batch, bottomBorder, false, false, BOTTOM_BORDER_W);
        drawBorderCorner(batch, bottomBorder, true, false, BOTTOM_BORDER_W);
    }

    private void rectLines(SpriteBatch b, float x, float y, float w, float h, float th, float a) {
        b.setColor(0.86f, 0.86f, 0.86f, a);
        b.draw(pixelRegion, x, y, w, th);
        b.draw(pixelRegion, x, y + h - th, w, th);
        b.draw(pixelRegion, x, y, th, h);
        b.draw(pixelRegion, x + w - th, y, th, h);
    }

    // the fan

    // san na ang card i, walang bobbing
    private void fanGeo(int i) {
        float pop = cardPop[i], sink = cardSink[i];
        float theta = (i - 2) * FAN_STEP;
        float radius = CARD_H * 1.25f;
        float pivotX = visX + visW / 2f + FAN_SHIFT_X;
        float pivotY = fanCy - radius;
        float rad = theta * MathUtils.degreesToRadians;
        float scale = 1f + POP_SCALE * pop - 0.03f * sink;
        geo[0] = pivotX + MathUtils.sin(rad) * radius + sinkDir[i] * SPREAD_X * sink - parX * PAR_CARD;
        geo[1] = pivotY + MathUtils.cos(rad) * radius + pop * LIFT - sink * SINK - parY * PAR_CARD * 0.5f;
        geo[2] = cardW[i] * scale;
        geo[3] = CARD_H * scale;
        geo[4] = -theta * (1f - 0.9f * pop);
    }

    private boolean hitCard(int i) {
        fanGeo(i);
        float dx = mouse.x - geo[0], dy = mouse.y - geo[1];
        float cos = MathUtils.cosDeg(geo[4]), sin = MathUtils.sinDeg(geo[4]);
        float lx = dx * cos + dy * sin;
        float ly = -dx * sin + dy * cos;
        return Math.abs(lx) <= geo[2] / 2f && Math.abs(ly) <= geo[3] / 2f;
    }

    // card sa ilalim ng mouse, pinakamalapit na center ang panalo
    private int cardAt() {
        if (time < 1.7f) return -1; // still dealing
        int n = CARD_NAMES.length;
        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (!hitCard(i)) continue;
            float dx = mouse.x - geo[0], dy = mouse.y - geo[1];
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private void drawCard(SpriteBatch b, int i, float t) {
        float deal = ease((time - 0.5f - i * 0.12f) / 0.7f);
        if (deal <= 0.01f) return;
        float pop = cardPop[i];
        fanGeo(i);
        float cx = geo[0], cy = geo[1], w = geo[2], h = geo[3], rot = geo[4];
        cy += (float) Math.sin(time * 1.3f + i * 1.1f) * 3f * deal * (1f - pop) - (1f - deal) * 560f;
        rot += (float) Math.sin(time * 0.8f + i * 0.7f) * 0.7f * (1f - pop);
        float alpha = 1f;

        if (chosen >= 0) {
            if (i == chosen && chosen == NEW_GAME) {
                float g = ease(t);
                cx = MathUtils.lerp(cx, visX + visW / 2f, g);
                cy = MathUtils.lerp(cy, visY + visH / 2f, g);
                float s = MathUtils.lerp(1f, 2.6f, g);
                w *= s;
                h *= s;
                rot = MathUtils.lerp(rot, 0f, g);
            } else {
                alpha = 1f - t;
            }
        }
        if (i == CONTINUE && !hasSave && noSaveT > 0f) {
            cx += (float) Math.sin(time * 70f) * 7f * Math.min(1f, noSaveT * 3f);
        }
        float band = beatViz.band((i * 5 + 2) % BeatViz.BANDS);
        float cardShake = beatViz.punch * (4f + band * 9f) + beatViz.energy * band * 3f;
        cx += (float) Math.sin(time * (5f + i * 0.45f) + i) * cardShake;
        cy += (float) Math.cos(time * (5.8f + i * 0.35f) + i * 0.7f) * cardShake;
        rot += (float) Math.sin(time * 4.2f + i * 1.4f) * beatViz.punch * 1.35f;
        float pulse = beatViz.energy * (0.01f + band * 0.016f) + beatViz.punch * 0.014f;
        w *= 1f + pulse;
        h *= 1f + pulse;

        float kick = beatViz.punch;
        if (kick > 0.05f || pop > 0.02f) {
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            b.setColor(0.82f, 0.82f, 0.82f,
                (0.10f * beatViz.energy + 0.45f * kick + 0.3f * pop) * alpha);
            b.draw(soft, cx - w * 0.9f, cy - h * 0.75f, w * 1.8f, h * 1.5f);
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        float tint = (i == CONTINUE && !hasSave) ? 0.3f + 0.2f * pop : 0.78f + 0.22f * pop;
        if (pop > 0.02f) {
            b.setColor(1f, 1f, 1f, 0.22f * pop * alpha);
            b.draw(soft, cx - w * 1.0f, cy - h * 0.8f, w * 2f, h * 1.6f);
        }
        b.setColor(0f, 0f, 0f, 0.65f * alpha);
        b.draw(pixelRegion, cx - w / 2f + 10f + 8f * pop, cy - h / 2f - 12f - 12f * pop,
            w / 2f, h / 2f, w, h, 1f, 1f, rot);
        float borderShade = 0.92f - 0.3f * kick;
        b.setColor(borderShade, borderShade, borderShade,
            Math.min(1f, (0.18f + 0.72f * pop + 0.5f * kick) * alpha));
        b.draw(pixelRegion, cx - w / 2f - 3f, cy - h / 2f - 3f, w / 2f + 3f, h / 2f + 3f,
            w + 6f, h + 6f, 1f, 1f, rot);
        b.setColor(tint, tint, tint, alpha);
        b.draw(cardRegions[i], cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, rot);
    }

    private void drawFan(SpriteBatch b, float t) {
        int n = CARD_NAMES.length;
        int ref = focus >= 0 ? focus : 2;
        for (int d = n; d >= 0; d--) {
            for (int i = 0; i < n; i++) {
                if (Math.abs(i - ref) == d) drawCard(b, i, t);
            }
        }
        b.setColor(Color.WHITE);
    }

    private void drawLabel(SpriteBatch b) {
        if (labelA <= 0.01f || focus < 0) return;
        boolean dead = focus == CONTINUE && !hasSave;
        String s = dead ? "CONTINUE  -  NO SAVE DATA" : OPTIONS[focus];
        layout.setText(optionFont, s);
        float w = layout.width + 150f, h = 48f;
        float x = visX + visW / 2f - w / 2f, y = visY + 66f;
        b.setColor(0f, 0f, 0f, 0.9f * labelA);
        b.draw(pixelRegion, x, y, w, h);
        rectLines(b, x, y, w, h, 1.5f, 0.9f * labelA);
        rectLines(b, x + 5f, y + 5f, w - 10f, h - 10f, 1f, 0.25f * labelA);
        float iw = 38f, ih = artHeight(menuIndicator, iw);
        b.setColor(1f, 1f, 1f, labelA);
        b.draw(menuIndicator.region, x + 18f, y + (h - ih) / 2f, iw, ih);
        b.draw(menuIndicator.region, x + w - 18f, y + (h - ih) / 2f, -iw, ih);
        optionFont.setColor(1f, 1f, 1f, (dead ? 0.55f : 1f) * labelA);
        optionFont.draw(b, s, x, y + h / 2f + optionFont.getCapHeight() / 2f, w, Align.center, false);
        smallFont.setColor(1f, 1f, 1f, 0.5f * labelA);
        smallFont.draw(b, "RIGHT CLICK  -  READ  " + TAROT_NAMES[focus],
            visX, y - 14f, visW, Align.center, false);
    }

    // logic

    private void moveFocus(int dir) {
        focus = focus < 0 ? 1 : (focus + dir + OPTIONS.length) % OPTIONS.length;
        keyboardFocus = true;
    }

    private void openWindow(int i, int mode) {
        fanGeo(i);
        reading.open(i, geo[0], geo[1], geo[2], geo[3], geo[4], mode);
    }

    private void activate(int i) {
        clickFlash = 1f;
        play(clickSfx, 0.6f, 1f);
        if (i == EXIT) {
            exitSelection = 1;
            exitConfirmation = true;
        } else if (i == CONTINUE) {
            if (!hasSave) noSaveT = 1.2f;
            else startChoice(i);
        } else if (i == SETTINGS) {
            openWindow(i, CardReading.SETTINGS);
        } else if (i == CREDITS) {
            openWindow(i, CardReading.CREDITS);
        } else {
            startChoice(i);
        }
    }

    private void confirmExitSelection() {
        if (exitSelection == 0) Gdx.app.exit();
        else exitConfirmation = false;
    }

    private float exitButtonX(int selection) {
        return visX + visW / 2f + (selection == 0 ? -205f : 15f);
    }

    private float exitButtonY() {
        return visY + visH / 2f - 72f;
    }

    private int exitButtonAt(float x, float y) {
        if (y < exitButtonY() || y > exitButtonY() + 52f) return -1;
        for (int i = 0; i < 2; i++) {
            float buttonX = exitButtonX(i);
            if (x >= buttonX && x <= buttonX + 190f) return i;
        }
        return -1;
    }

    private void startChoice(int i) {
        if (i == NEW_GAME) {
            menuMusic.stop();
            play(chosenSfx, 1f, 1f);
        }
        chosen = i;
        chosenTimer = 0f;
    }

    private void finishChoice() {
        choiceFinished = true;
        if (chosen == CONTINUE) game.transitionTo(new GameScreen(game));
        else if (chosen == NEW_GAME) game.transitionTo(new AustineBossBattle(game));
        else if (chosen == EXIT) Gdx.app.exit();
    }

    // screen

    @Override
    public void show() {
        if (snowBackgroundStarted) {
            snowBackground.resume();
        } else {
            snowBackground.play();
            snowBackgroundStarted = true;
        }
        menuMusic.setLooping(true);
        if (introHandedOff) menuMusic.play();
        Gdx.input.setInputProcessor(inputProcessor);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        if (!introHandedOff) {
            if (!intro.isDone()) { renderIntro(delta); return; }
            handOffIntro();
        }
        time += delta;
        grainU += delta * 0.015f;
        grainV += delta * 0.01f;
        noSaveT = Math.max(0f, noSaveT - delta);
        clickFlash = Math.max(0f, clickFlash - delta * 3f);
        updateVisibleArea();
        if (layoutDirty) {
            computeLayout();
            layoutDirty = false;
        }

        int mxs = Gdx.input.getX(), mys = Gdx.input.getY();
        boolean moved = mxs != lastMx || mys != lastMy;
        lastMx = mxs;
        lastMy = mys;
        toWorld(mxs, mys);
        boolean pointerAvailable = pointerAvailable();
        if (exitConfirmation && pointerAvailable) {
            int hoveredButton = exitButtonAt(mouse.x, mouse.y);
            if (hoveredButton >= 0) exitSelection = hoveredButton;
        }

        float targetParX = pointerAvailable
            ? MathUtils.clamp((mouse.x - (visX + visW / 2f)) / visW, -0.5f, 0.5f) : 0f;
        float targetParY = pointerAvailable
            ? MathUtils.clamp((mouse.y - (visY + visH / 2f)) / visH, -0.5f, 0.5f) : 0f;
        float parallaxSpeed = Math.min(1f, delta * 3f);
        parX += (targetParX - parX) * parallaxSpeed;
        parY += (targetParY - parY) * parallaxSpeed;

        menuMusic.setVolume(GameSettings.music * 0.9f);
        beatViz.update(delta, menuMusic.getPosition(), menuMusic.isPlaying());
        updateGothic(delta);
        snowBackground.update();
        reading.update(delta, mouse.x, mouse.y, visX, visY, visW, visH);
        updateWeather(delta);
        updateSnow(delta);

        float t = 0f;
        if (chosen >= 0) {
            chosenTimer += delta;
            t = MathUtils.clamp(chosenTimer / FADE_OUT, 0f, 1f);
            if (chosenTimer >= FADE_OUT && !choiceFinished) {
                finishChoice();
                return;
            }
        }

        boolean idle = chosen < 0 && !reading.isActive() && !exitConfirmation;
        if (idle) {
            if (moved && pointerAvailable) keyboardFocus = false;
            if (!keyboardFocus) {
                int under = pointerAvailable ? cardAt() : -1;
                if (under != hoverTarget) {
                    hoverTarget = under;
                    hoverTimer = 0f;
                }
                if (under >= 0) {
                    hoverTimer += delta;
                    if (hoverTimer >= HOVER_DWELL) focus = under;
                } else {
                    hoverTimer = 0f;
                    focus = -1; // drop the card as soon as the cursor leaves
                }
            }
        }
        if (focus != lastFocus) {
            if (focus >= 0 && idle) {
                play(hoverSfx, 0.35f, 0.95f + focus * 0.04f);
                play(cardDrawSfx, 0.65f, 1f);
            }
            lastFocus = focus;
        }
        if (focus >= 0) {
            for (int i = 0; i < sinkDir.length; i++) sinkDir[i] = Math.signum((float) (i - focus));
        }
        for (int i = 0; i < cardPop.length; i++) {
            boolean up = idle && i == focus;
            boolean down = idle && focus >= 0 && i != focus;
            cardPop[i] += ((up ? 1f : 0f) - cardPop[i]) * Math.min(1f, delta * 12f);
            cardSink[i] += ((down ? 1f : 0f) - cardSink[i]) * Math.min(1f, delta * 8f);
        }
        labelA += ((idle && focus >= 0 ? 1f : 0f) - labelA) * Math.min(1f, delta * 12f);

        flickerCooldown -= delta;
        if (flickerCooldown <= 0f) {
            flickerTime = 0.28f;
            flickerCooldown = MathUtils.random(4f, 9f);
        }
        flickerTime = Math.max(0f, flickerTime - delta);
        float flicker = flickerTime > 0f && MathUtils.randomBoolean() ? 0.5f : 0f;
        beatShakeX = ((float) Math.sin(time * 16f) + (float) Math.sin(time * 11f))
            * beatViz.punch * 7f + (float) Math.sin(time * 2.5f) * beatViz.energy * 1.7f;
        beatShakeY = ((float) Math.sin(time * 14f) + (float) Math.sin(time * 9f))
            * beatViz.punch * 5.5f + (float) Math.cos(time * 2.2f) * beatViz.energy * 1.4f;
        beatZoom = 1f + beatViz.energy * 0.01f + beatViz.punch * 0.045f;
        beatShakeX += (float) Math.sin(time * 47f) * dropT * 14f * GOTH_INTENSITY;
        beatShakeY += (float) Math.cos(time * 53f) * dropT * 11f * GOTH_INTENSITY;
        beatZoom += dropT * 0.05f * GOTH_INTENSITY;
        entryT += delta;
        introFlash = Math.max(0f, introFlash - delta * 1.1f);
        float entry = 1f - ease(entryT / 1.2f);
        beatShakeX += (float) Math.sin(time * 61f) * entry * 14f;
        beatShakeY += (float) Math.cos(time * 57f) * entry * 10f;
        beatZoom += entry * 0.2f;

        // draw
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply();
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(camera.combined);
        b.setTransformMatrix(beatTransform.idt()
            .translate(visX + visW / 2f + beatShakeX, visY + visH / 2f + beatShakeY, 0f)
            .scale(beatZoom, beatZoom, 1f)
            .translate(-(visX + visW / 2f), -(visY + visH / 2f), 0f));
        if (monoShader != null) b.setShader(monoShader);
        b.begin();
        if (monoShader != null) monoShader.setUniformf("u_mono", MONO);

        drawSnowBackground(b);
        if (SHOW_LIGHTNING) drawLightning(b);
        drawSnow(b);

        // title: inks in, glow, flicker, sumusunod sa mouse
        float ui = 1f - t;
        float cx = visX + visW / 2f - parX * PAR_TITLE;
        float titleCenterY = titleCy + (float) Math.sin(time * 0.7f) * 2f
            - parY * PAR_TITLE * 0.4f + beatViz.bass * 12f + beatViz.punch * 16f;
        float titleAlpha = ease((time - 0.3f) / 1.4f) * (1f - flicker * 0.3f) * ui;
        float glow = MathUtils.clamp(beatViz.energy * 0.6f + beatViz.bass * 0.4f
            + beatViz.punch * 1.1f + dropT, 0f, 1f);
        float titlePulse = 1f + beatViz.energy * 0.04f + beatViz.bass * 0.03f
            + beatViz.punch * 0.09f + dropT * 0.08f;
        float rtw = titleW * titlePulse, rth = titleH * titlePulse;
        float gothFlicker = 0.85f + 0.15f * (float) Math.sin(time * 13f) * (float) Math.sin(time * 5.3f);

        drawGothicBack(b, cx, titleCenterY, ui);
        drawClock(b, cx, titleCenterY, ui);
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        b.setColor(1f, 1f, 1f, (0.035f + glow * 0.1f) * gothFlicker * titleAlpha);
        b.draw(soft, cx - rtw, titleCenterY - rth * 1.6f, rtw * 2f, rth * 3.2f);
        b.setColor(1f, 1f, 1f, (0.025f + glow * 0.07f) * titleAlpha);
        b.draw(titleArt.region, cx - rtw * 0.54f, titleCenterY - rth * 0.54f,
            rtw * 1.08f, rth * 1.08f);
        float split = glow * 3f + dropT * 6f;
        b.setColor(1f, 1f, 1f, glow * 0.18f * titleAlpha);
        b.draw(titleArt.region, cx - rtw / 2f - split, titleCenterY - rth / 2f, rtw, rth);
        b.setColor(1f, 1f, 1f, glow * 0.12f * titleAlpha);
        b.draw(titleArt.region, cx - rtw / 2f + split, titleCenterY - rth / 2f, rtw, rth);
        b.setColor(1f, 1f, 1f, beatViz.punch * 0.12f * titleAlpha);
        b.draw(titleArt.region, cx - rtw / 2f, titleCenterY - rth / 2f, rtw, rth);
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawArtCentered(titleArt, cx, titleCenterY,
            titleW * (1f + (titlePulse - 1f) * 0.5f), titleAlpha);

        drawFan(b, t);
        if (chosen < 0) drawLabel(b);

        reading.draw(b);

        b.setColor(1f, 1f, 1f, 0.35f);
        b.draw(grain, visX, visY, visW, visH, grainU, grainV,
            grainU + visW / GRAIN_SIZE, grainV + visH / GRAIN_SIZE);
        b.setColor(Color.WHITE);
        b.draw(vignette, visX, visY, visW, visH);
        float heart = MathUtils.clamp(0.12f + beatViz.bass * 0.35f
            + beatViz.punch * 0.45f + dropT * 0.6f, 0f, 1f);
        b.setColor(1f, 1f, 1f, heart * GOTH_INTENSITY * ui);
        b.draw(bloodVignette, visX, visY, visW, visH);

        if (clickFlash > 0.01f) {
            b.setColor(1f, 1f, 1f, clickFlash * 0.1f);
            b.draw(pixel, visX, visY, visW, visH);
        }

        if (dropT > 0.01f || beatViz.punch > 0.6f) {
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            float flash = Math.min(0.12f,
                dropT * dropT * 0.12f + beatViz.punch * beatViz.punch * beatViz.energy * 0.035f);
            b.setColor(1f, 1f, 1f, flash);
            b.draw(pixel, visX, visY, visW, visH);
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        float leave = chosen == NEW_GAME ? MathUtils.clamp((t - 0.55f) / 0.45f, 0f, 1f) : t;
        float overlay = Math.max(1f - MathUtils.clamp(time / FADE_IN, 0f, 1f), leave);
        if (overlay > 0f) {
            b.setColor(0f, 0f, 0f, overlay);
            b.draw(pixel, visX, visY, visW, visH);
        }
        if (introFlash > 0.01f) {
            b.setColor(1f, 1f, 1f, introFlash);
            b.draw(pixel, visX, visY, visW, visH);
        }
        if (exitConfirmation) drawExitConfirmation(b);
        b.setColor(Color.WHITE);
        b.end();
        b.setTransformMatrix(beatTransform.idt());
        b.setShader(null);
    }

    private void renderIntro(float delta) {
        intro.update(delta);
        grainU += delta * 0.015f;
        grainV += delta * 0.01f;
        updateVisibleArea();
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply();
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(camera.combined);
        b.setTransformMatrix(beatTransform.idt());
        if (monoShader != null) b.setShader(monoShader);
        b.begin();
        if (monoShader != null) monoShader.setUniformf("u_mono", MONO);
        intro.draw(b, visX, visY, visW, visH);
        b.setColor(1f, 1f, 1f, 0.35f);
        b.draw(grain, visX, visY, visW, visH, grainU, grainV,
            grainU + visW / GRAIN_SIZE, grainV + visH / GRAIN_SIZE);
        b.setColor(Color.WHITE);
        b.draw(vignette, visX, visY, visW, visH);
        if (intro.white() > 0.002f) {
            b.setColor(1f, 1f, 1f, intro.white());
            b.draw(pixel, visX, visY, visW, visH);
        }
        b.setColor(Color.WHITE);
        b.end();
        b.setShader(null);
    }

    private void handOffIntro() {
        introHandedOff = true;
        introFlash = 1f;
        entryT = 0f;
        menuMusic.setLooping(true);
        menuMusic.play();
        dropT = 1f;
        dropCooldown = 8f;
        lightningCooldown = 0f;
        clockDir = -6f;
    }

    private void drawExitConfirmation(SpriteBatch batch) {
        float centerX = visX + visW / 2f;
        float centerY = visY + visH / 2f;
        float panelW = 580f, panelH = 230f;
        float panelX = centerX - panelW / 2f, panelY = centerY - panelH / 2f;

        batch.setColor(0f, 0f, 0f, 0.78f);
        batch.draw(pixelRegion, visX, visY, visW, visH);
        batch.setColor(0f, 0f, 0f, 0.96f);
        batch.draw(pixelRegion, panelX, panelY, panelW, panelH);
        rectLines(batch, panelX, panelY, panelW, panelH, 2f, 0.95f);
        rectLines(batch, panelX + 7f, panelY + 7f, panelW - 14f, panelH - 14f, 1f, 0.35f);

        optionFont.setColor(Color.WHITE);
        optionFont.draw(batch, "ARE YOU SURE YOU WANT TO EXIT?",
            panelX + 20f, centerY + 34f, panelW - 40f, Align.center, false);

        String[] labels = { "YES, EXIT", "CANCEL" };
        for (int i = 0; i < labels.length; i++) {
            float buttonX = exitButtonX(i), buttonY = exitButtonY();
            batch.setColor(1f, 1f, 1f, i == exitSelection ? 0.2f : 0.06f);
            batch.draw(pixelRegion, buttonX, buttonY, 190f, 52f);
            rectLines(batch, buttonX, buttonY, 190f, 52f, 1.5f, i == exitSelection ? 0.9f : 0.35f);
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, labels[i], buttonX, buttonY + 32f, 190f, Align.center, false);
        }
        batch.setColor(Color.WHITE);
    }

    // background

    private void drawSnowBackground(SpriteBatch batch) {
        Texture texture = snowBackground.getTexture();
        if (texture == null) return;

        float scale = Math.max(visW / texture.getWidth(), visH / texture.getHeight()) * 1.06f;
        float width = texture.getWidth() * scale;
        float height = texture.getHeight() * scale;
        batch.setColor(Color.WHITE);
        batch.draw(texture, visX + (visW - width) / 2f - parX * PAR_BG,
            visY + (visH - height) / 2f - parY * PAR_BG * 0.5f, width, height);
    }

    private void updateSnow(float delta) {
        float beatRise = Math.max(0f, beatViz.punch - lastSnowPunch);
        lastSnowPunch = beatViz.punch;
        if (beatRise > 0.035f) {
            float burst = Math.min(1f, beatViz.punch * 1.8f);
            for (int i = 0; i < SNOW_COUNT; i++) {
                snowKick[i] = Math.max(snowKick[i], burst);
            }
        }

        for (int i = 0; i < SNOW_COUNT; i++) {
            float dx = snowX[i] - 0.5f;
            float dy = snowY[i] - 0.5f;
            float distance = Math.max(0.18f, (float) Math.sqrt(dx * dx + dy * dy));
            snowX[i] += dx / distance * snowKick[i] * delta * 0.28f;
            snowY[i] += dy / distance * snowKick[i] * delta * 0.22f;
            snowY[i] -= snowV[i] * delta;
            snowKick[i] *= (float) Math.exp(-delta * 2.8f);
            if (snowY[i] < -0.02f) {
                snowY[i] = 1.02f;
                snowX[i] = MathUtils.random();
                snowKick[i] = 0f;
            } else if (snowY[i] > 1.02f || snowX[i] < -0.02f || snowX[i] > 1.02f) {
                snowY[i] = MathUtils.clamp(snowY[i], 0f, 1f);
                snowX[i] = MathUtils.clamp(snowX[i], 0f, 1f);
            }
        }
    }

    private void updateGothic(float delta) {
        if (!clockInitialized) {
            java.time.LocalTime now = java.time.LocalTime.now();
            secSpin = now.getSecond() * 6f;
            minSpin = (now.getMinute() + now.getSecond() / 60f) * 6f;
            hourSpin = (now.getHour() % 12 + now.getMinute() / 60f) * 30f;
            clockInitialized = true;
        }
        sigilRot += delta * (8f + beatViz.energy * 25f + beatViz.punch * 60f);

        dropSlow += (beatViz.energy - dropSlow) * Math.min(1f, delta * 0.5f);
        dropCooldown -= delta;
        if (beatViz.energy - dropSlow > 0.35f && dropCooldown <= 0f) {
            dropT = 1f;
            clockDir = -6f;
            dropCooldown = 8f;
            lightningCooldown = 0f;
        }
        dropT = Math.max(0f, dropT - delta * 1.1f);

        boolean strongHit = beatViz.punch > 0.8f && goPunch <= 0.8f;
        goPunch = beatViz.punch;
        clockJolt = Math.max(0f, clockJolt - delta * 6f);
        if (strongHit) {
            secKick = 14f;
            clockJolt = 1f;
        }
        clockDir += (1f - clockDir) * (1f - (float) Math.exp(-delta * 1.6f));
        secKick *= (float) Math.exp(-delta * 14f);
        float spin = (0.25f + beatViz.energy * 1f + beatViz.punch * 1.4f) * clockDir;
        hourSpin = (hourSpin + delta * 14f * spin) % 360f;
        minSpin = (minSpin + delta * 90f * spin) % 360f;
        secSpin = (secSpin + delta * 200f * spin) % 360f;
    }

    private void drawGothicBack(SpriteBatch batch, float centerX, float centerY, float ui) {
        float intensity = GOTH_INTENSITY * ui;
        float flick = 0.88f + 0.12f * (float) Math.sin(time * 13f) * (float) Math.sin(time * 5.3f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (int i = 0; i < 6; i++) {
            float u = (i + 0.5f) / 6f;
            float sway = (float) Math.sin(time * 0.35f + i * 1.7f) * 40f;
            float width = 150f + beatViz.band(i * 5) * 160f + beatViz.punch * 90f;
            float height = visH * 1.25f;
            float alpha = (0.05f + beatViz.energy * 0.10f + beatViz.band(i * 5) * 0.12f
                + beatViz.punch * 0.10f) * intensity * flick;
            batch.setColor(0.8f, 0.8f, 0.8f, alpha);
            batch.draw(softRegion, visX + u * visW + sway - width / 2f, visY + visH - height * 0.9f,
                width / 2f, height * 0.9f, width, height, 1f, 1f, (u - 0.5f) * -16f);
        }

        float radius = gothR = 330f * (1f + beatViz.bass * 0.10f + beatViz.punch * 0.22f + dropT * 0.3f);
        batch.setColor(0.85f, 0.85f, 0.85f,
            (0.12f + beatViz.energy * 0.3f + beatViz.punch * 0.4f) * intensity * flick);
        batch.draw(softRegion, centerX - radius * 1.6f, centerY - radius * 1.6f,
            radius * 3.2f, radius * 3.2f);

        for (int r = 0; r < 3; r++) {
            float size = radius * (2.3f - r * 0.55f);
            batch.setColor(1f, 1f, 1f,
                (0.18f + beatViz.energy * 0.25f + beatViz.punch * 0.35f - r * 0.04f)
                    * intensity * flick);
            batch.draw(ringRegion, centerX - size / 2f, centerY - size / 2f, size, size);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawClockHand(SpriteBatch batch, TextureRegion hand, float centerX, float centerY,
                               float angle, float height, float pivotXFraction, float pivotYFraction,
                               float alpha) {
        float scale = height / hand.getRegionHeight();
        float width = hand.getRegionWidth() * scale;
        float originX = width * pivotXFraction;
        float originY = height * (1f - pivotYFraction);
        batch.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha);
        batch.draw(hand, centerX - originX, centerY - originY, originX, originY, width, height,
            1f, 1f, angle - 90f);
    }

    private void drawClock(SpriteBatch batch, float centerX, float centerY, float ui) {
        float radius = gothR;
        float intensity = GOTH_INTENSITY * ui;
        float flick = 0.88f + 0.12f * (float) Math.sin(time * 13f) * (float) Math.sin(time * 5.3f);
        float hourAngle = 90f - hourSpin;
        float minuteAngle = 90f - minSpin;
        float secondAngle = 90f - (secSpin - secKick)
            + (float) Math.sin(time * 70f) * clockJolt * 2.5f;

        float alpha = (0.55f + beatViz.energy * 0.3f + beatViz.punch * 0.15f) * intensity;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float rim = radius * 2.04f;
        // fade ang dial kasabay ng kamay
        batch.setColor(0.85f, 0.85f, 0.85f, alpha * 0.55f * flick);
        batch.draw(ringRegion, centerX - rim / 2f, centerY - rim / 2f, rim, rim);
        int litTick = Math.floorMod(Math.round(secSpin / 6f), 60);
        for (int i = 0; i < 60; i++) {
            float angle = 90f - i * 6f;
            boolean major = i % 5 == 0;
            float distance = Math.max(0f,
                1f - Math.abs(Math.floorMod(i - litTick + 30, 60) - 30) / 4f);
            float length = (major ? 30f : 14f) + distance * 18f
                + beatViz.band((i * 3) % BeatViz.BANDS) * (major ? 22f : 12f);
            float thickness = major ? 3.2f : 1.6f;
            float x = centerX + MathUtils.cosDeg(angle) * radius;
            float y = centerY + MathUtils.sinDeg(angle) * radius;
            float tickShade = 0.72f + 0.28f * distance;
            batch.setColor(tickShade, tickShade, tickShade,
                Math.min(1f, (0.25f + distance * 0.6f + beatViz.punch * 0.3f
                    + (major ? 0.2f : 0f)) * intensity * flick));
            batch.draw(pixelRegion, x, y - thickness / 2f, 0f, thickness / 2f,
                length, thickness, 1f, 1f, angle + 180f);
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        int nearHour = Math.floorMod(Math.round(secSpin / 30f), 12);
        for (int i = 0; i < 12; i++) {
            float angle = 90f - i * 30f;
            float near = i == nearHour ? 1f : 0f;
            float scale = (i == 0 ? 1.25f : 1f)
                * (0.85f + beatViz.band((i * 2 + 1) % BeatViz.BANDS) * 0.35f
                    + near * 0.25f + beatViz.punch * 0.15f);
            optionFont.getData().setScale(scale / FONT_QUALITY);
            layout.setText(optionFont, ROMAN[i]);
            float x = centerX + MathUtils.cosDeg(angle) * radius * 0.80f - layout.width / 2f;
            float y = centerY + MathUtils.sinDeg(angle) * radius * 0.80f + layout.height / 2f;
            float numeralShade = 0.88f - 0.35f * near;
            optionFont.setColor(numeralShade, numeralShade, numeralShade,
                Math.min(1f, (0.45f + 0.5f * near + 0.3f * beatViz.punch) * intensity));
            optionFont.draw(batch, ROMAN[i], x, y);
        }
        optionFont.getData().setScale(1f / FONT_QUALITY);
        optionFont.setColor(Color.WHITE);

        drawClockHand(batch, mediumClockHand, centerX, centerY, hourAngle, radius * 0.42f,
            MEDIUM_HAND_PIVOT_X, MEDIUM_HAND_PIVOT_Y, alpha * 0.49f);
        drawClockHand(batch, mediumClockHand, centerX, centerY, minuteAngle, radius * 0.62f,
            MEDIUM_HAND_PIVOT_X, MEDIUM_HAND_PIVOT_Y, alpha * 0.585f);
        float secondHandLength = radius * 0.76f;
        drawClockHand(batch, longClockHand, centerX, centerY, secondAngle, secondHandLength,
            LONG_HAND_PIVOT_X, LONG_HAND_PIVOT_Y, alpha * 0.62f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        // hub sa gitna ng kamay
        float hubShade = 0.82f;
        batch.setColor(hubShade, hubShade, hubShade, alpha * 0.5f);
        batch.draw(softRegion, centerX - 26f, centerY - 26f, 52f, 52f);
        batch.setColor(1f, 1f, 1f, alpha * 0.62f);
        batch.draw(softRegion, centerX - 9f, centerY - 9f, 18f, 18f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawSnow(SpriteBatch b) {
        for (int i = 0; i < SNOW_COUNT; i++) {
            float x = visX + (snowX[i] + (float) Math.sin(time * 0.6f + snowP[i]) * 0.012f) * visW;
            float y = visY + snowY[i] * visH;
            b.setColor(1f, 1f, 1f, 0.12f + 0.1f * snowS[i]);
            b.draw(pixel, x, y, snowS[i], snowS[i]);
        }
        b.setColor(Color.WHITE);
    }

    private void updateWeather(float delta) {
        if (lightningTime >= 0f) {
            lightningTime += delta;
            if (lightningTime >= 0.55f) lightningTime = -1f;
        } else {
            lightningCooldown -= delta;
            if (lightningCooldown <= 0f) {
                lightningCooldown = MathUtils.random(5f, 9f);
                lightningTime = 0f;
                lightningX[0] = MathUtils.random(visX + visW * 0.2f, visX + visW * 0.8f);
                lightningY[0] = visY + visH;
                for (int i = 1; i <= LIGHTNING_SEGMENT_COUNT; i++) {
                    lightningX[i] = lightningX[i - 1] + MathUtils.random(-32f, 32f);
                    lightningY[i] = lightningY[i - 1] - visH / LIGHTNING_SEGMENT_COUNT;
                }
            }
        }
    }

    private void drawLightning(SpriteBatch batch) {
        float flashAlpha = 0f, boltAlpha = 0f;
        if (lightningTime >= 0f) {
            if (lightningTime < 0.08f) { flashAlpha = 0.045f; boltAlpha = 0.28f; }
            else if (lightningTime < 0.16f) { flashAlpha = 0.012f; boltAlpha = 0.08f; }
            else if (lightningTime < 0.25f) { flashAlpha = 0.03f; boltAlpha = 0.2f; }
            else {
                float fade = 1f - (lightningTime - 0.25f) / 0.3f;
                flashAlpha = 0.02f * fade;
                boltAlpha = 0.12f * fade;
            }
        }
        if (flashAlpha > 0f) {
            batch.setColor(0.9f, 0.9f, 0.9f, flashAlpha * 3f);
            batch.draw(pixel, visX, visY, visW, visH);
            batch.setColor(1f, 1f, 1f, Math.min(1f, boltAlpha * 3f));
            for (int i = 0; i < LIGHTNING_SEGMENT_COUNT; i++) {
                float dx = lightningX[i + 1] - lightningX[i];
                float dy = lightningY[i + 1] - lightningY[i];
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                batch.draw(pixelRegion, lightningX[i], lightningY[i], 0f, 0f, length, 5f, 1f, 1f, angle);
            }
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        camera.position.set(Main.WORLD_WIDTH / 2f, Main.WORLD_HEIGHT / 2f, 0f);
        camera.update();
        updateVisibleArea();
        layoutDirty = true;
    }

    // pause pag minimized
    public void pauseMusic() {
        if (menuMusic != null) menuMusic.pause();
        snowBackground.pause();
    }

    // resume pag balik
    public void resumeMusic() {
        if (menuMusic != null && chosen < 0 && introHandedOff) menuMusic.play();
        if (snowBackgroundStarted) snowBackground.resume();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        pauseMusic();
        GameSettings.save();
    }

    @Override
    public void dispose() {
        menuMusic.dispose();
        snowBackground.dispose();
        titleArt.texture.dispose();
        menuIndicator.texture.dispose();
        topBorder.texture.dispose();
        bottomBorder.texture.dispose();
        for (Texture t : cardTextures) t.dispose();
        soft.dispose();
        grain.dispose();
        vignette.dispose();
        ring.dispose();
        bloodVignette.dispose();
        pixel.dispose();
        longClockHandTexture.dispose();
        mediumClockHandTexture.dispose();
        if (monoShader != null) monoShader.dispose();
        optionFont.dispose();
        smallFont.dispose();
        reading.dispose();
        if (cardDrawSfx != null) cardDrawSfx.dispose();
        if (chosenSfx != null) chosenSfx.dispose();
        if (hoverSfx != null) hoverSfx.dispose();
        if (clickSfx != null) clickSfx.dispose();
        if (introSfx != null) introSfx.dispose();
        if (introTickSfx != null) introTickSfx.dispose();
    }
}
