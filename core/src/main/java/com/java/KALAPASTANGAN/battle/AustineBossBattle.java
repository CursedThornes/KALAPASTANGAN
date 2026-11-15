package com.java.KALAPASTANGAN.battle;
import com.java.KALAPASTANGAN.dialogue.RichText;
import com.java.KALAPASTANGAN.dialogue.DialogueScript;
import com.java.KALAPASTANGAN.dialogue.DialogueRunner;
import com.java.KALAPASTANGAN.ui.MenuScreen;
import com.java.KALAPASTANGAN.GameSettings;
import com.java.KALAPASTANGAN.Main;

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
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// buong laban, pati dialogue at choices. good luck
public class AustineBossBattle extends ScreenAdapter {
    // files
    private static final String TYPEWRITER_FONT = "Fonts/Type Machine.ttf";
    private static final String NAME_FONT = "Fonts/Trajan Pro Regular.ttf";
    private static final String SCRIPT_FILE = "Dialogue/scene1.txt";
    private static final String BLIP_FILE = "Audio/SFX/Gameplay/blip.wav";
    private static final String CARD_DRAW_SOUND = "Audio/SFX/Cards/CardDraw.MP3";
    private static final String CARD_FLIP_SOUND = "Audio/SFX/Cards/CardFlip.MP3";
    private static final String CHOSEN_SOUND = "Audio/SFX/Menu/CHOSEN.MP3";
    private static final String BATTLE_OST = "Audio/Music/Austine Phase 1.mp3";
    private static final String BATTLE_VIS = "Audio/Music/Austine_Phase_1.vis";
    private static final String BATTLE_OST_2 = "Audio/Music/Austine Phase 2.mp3";
    private static final String BATTLE_VIS_2 = "Audio/Music/Austine_Phase_2.vis";
    private static final String BATTLE_OST_3 = "Audio/Music/Austine Phase 3.mp3";
    private static final String BATTLE_VIS_3 = "Audio/Music/Austine_Phase_3.vis";
    private static final String[] CHOICE_CARD_RANKS = { "JACK", "KING", "QUEEN" };
    private static final String[] TAROT_CARD_NAMES = { "FOOL", "MAGICIAN", "HIGHPRIEST", "EMPRESS", "DEATH" };

    // tuning
    private static final float TEXT_SPEED = 42f;
    private static final float SHAKE_TIME = 0.45f;
    private static final float AUTO_DELAY = 1.1f;
    private static final float SKIP_INTERVAL = 0.07f;
    private static final float INTRO_TIME = 1f;
    private static final float CHAPTER_TIME = 3.6f;
    private static final float BOX_SHRINK_TIME = 0.5f;
    private static final float BOX_GROW_TIME = 0.45f;
    // libGDX caps volume sa 1.0, kaya phase 3 naka-pin sa dulo
    private static final float BATTLE_MUSIC_VOLUME = 0.88f;
    private static final float PHASE3_MUSIC_VOLUME = 1f;
    private static final float BATTLE_BPM = 172f;
    private static final float BEAT_OFFSET = 0.051f;
    private static final String ENEMY_NAME = "Austin E. Araw";
    private static final int ENEMY_MAX = 80;
    private static final int ENEMY_MAX_2 = 120;
    private static final int ENEMY_MAX_3 = 160;
    // phase 2: 183.58 bpm, body sa bar 16 (20.94s). gothic title sa tahimik
    private static final float P2_INTRO_TIME = 3.6f;
    private static final float P3_INTRO_TIME = 3.6f;
    // naka-align sa loudness peaks. 27.74s at 42.75s, dati sumasabog bago ang hit
    private static final float[] P3_GLITCH_HITS = {
        27.720f, 37.370f, 42.720f, 46.995f, 51.810f, 56.615f, 61.425f, 65.785f, 70.750f
    };
    private static final float P3_GLITCH_DURATION = 0.36f;
    private static final float P3_MAYDAY_START = 10f, P3_MAYDAY_END = 20f, P3_MAYDAY_STROBE = 0.48f;
    private static final float P3_MAYDAY_CYCLE = 3.15f, P3_MAYDAY_SWEEP = 2.4f;
    // !battle dilim ang pumipili ng phase 2
    private static final String PHASE_TWO_PATTERN = "dilim";
    private static final String PHASE_THREE_PATTERN = "sigaw";
    private static final int TRUST_NEEDED = 3;
    private static final int GRAIN_SIZE = 256;

    // isang 1:52 pass: 4 menu sa pagitan ng 5 sequence
    private static final float PICK_MIN = 2.2f, PICK_MAX = 4.5f;
    private static final float RESULT_SLACK = 2.3f;
    private static final float RESULT_HOLD = 0.5f;
    private static final int MENU_HEAL = 5;                 // HP restored each time the menu opens
    private static final float BATTLE_SHAKE_GAIN = 1.0f;    // scene shake multiplier during the fight
    private static final float FOCUS_ZOOM = 2.1f;           // how hard the camera dives on the heart
    private static final float BATTLE_CAMERA_MARGIN = 6f;
    private static final float SEQ_TITLE_TIME = 2.6f;
    private static final String[] ROMAN = { "I", "II", "III", "IV", "V" };

    private static final Color TEXT = new Color(0.88f, 0.86f, 0.8f, 1f);
    private static final Color NARRATION = new Color(0.7f, 0.69f, 0.65f, 1f);
    private static final Color CHOICE_ON = new Color(0.98f, 0.95f, 0.88f, 1f);
    private static final Color CHOICE_OFF = new Color(0.62f, 0.6f, 0.57f, 1f);

    private static final String[] MENU_LABELS = { "ATTACK", "DEFEND", "REASON" };
    private static final String[] QUICK_LABELS = { "LOG", "AUTO", "SKIP", "PHASE2", "PHASE3", "MENU" };
    private static final float QM_PHASE2_W = 116f;
    private static final float QM_PHASE3_W = 116f;
    private static final String MONO_VERT =
        "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\nvarying vec4 v_color;\nvarying vec2 v_texCoords;\n"
            + "void main(){v_color=a_color;v_color.a=v_color.a*(255.0/254.0);"
            + "v_texCoords=a_texCoord0;gl_Position=u_projTrans*a_position;}";
    private static final String MONO_FRAG =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
            + "varying vec4 v_color;\nvarying vec2 v_texCoords;\nuniform sampler2D u_texture;\n"
            + "uniform float u_mono;\nuniform float u_static;\nuniform float u_seed;\n"
            + "float h21(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\n"
            + "void main(){\n"
            + " vec2 uv=v_texCoords;\n"
            + " vec2 sp=gl_FragCoord.xy/vec2(1280.0,720.0);\n"
            + " float band=floor(sp.y*64.0+u_seed*9.0);\n"
            + " float tear=(h21(vec2(band,u_seed*3.1))-0.5)*0.24*u_static;\n"
            + " uv.x=fract(uv.x+tear);\n"
            + " vec4 c=v_color*texture2D(u_texture,uv);\n"
            + " float g=clamp((dot(c.rgb,vec3(0.299,0.587,0.114))-0.5)*1.15+0.5,0.0,1.0);\n"
            + " vec3 base=mix(c.rgb,vec3(g),u_mono);\n"
            + " float n=h21(floor(gl_FragCoord.xy)+vec2(u_seed*17.0,u_seed*7.0));\n"
            + " float scan=0.5+0.5*sin(gl_FragCoord.y*2.4+u_seed*38.0);\n"
            + " vec3 st=vec3(n*0.9+0.14*scan);\n"
            + " base=mix(base,st,u_static*(0.55+0.45*n));\n"
            + " gl_FragColor=vec4(base,c.a);\n"
            + "}";
    private static final String[] FLAVOR = {
        "* Lumapit ang anino. Amoy-basa ang hangin.",
        "* May yabag sa likod mo. Mabagal. Papalapit.",
        "* Hindi gumagalaw ang anino, pero lumalaki ito.",
        "* Tahimik. Masyadong tahimik."
    };
    private static final String[] REASON_LINES = {
        "* Kinausap mo ang anino. Parang may nakikinig.",
        "* Ipinaliwanag mo ang lahat. Bumagal ang yabag.",
        "* Inabot mo ang kamay mo. Tumigil ang anino."
    };

    // battle phases
    private static final int B_NONE = 0, B_INTRO = 1, B_MENU = 2, B_RESULT = 3, B_SHRINK = 4,
        B_DODGE = 5, B_GROW = 6,         B_DEAD = 7, B_END = 8, B_P2_INTRO = 9, B_P3_INTRO = 10;

    // objects
    private final Main game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new ExtendViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);
    private final Matrix4 arenaTransform = new Matrix4();
    private final Matrix4 worldTransform = new Matrix4();
    private final Matrix4 identityTransform = new Matrix4();
    private final Texture background, topLine, choiceIndicator, choiceCardBack, grain, vignette, pixel;
    private final ShaderProgram monoShader;
    private final TextureRegion pixelRegion, topLineRegion;
    private final Map<String, Texture> sprites = new HashMap<String, Texture>();
    private final Map<String, Texture> portraits = new HashMap<String, Texture>();
    private final Map<String, Texture> cardTex = new HashMap<String, Texture>();
    private final BitmapFont dialogueFont, nameFont, uiFont, choiceFont, titleFont;
    private final Sound blip, cardDrawSfx, cardFlipSfx, chosenSfx;
    private final Music battleMusic;
    private final Music phaseTwoMusic;
    private final Music phaseThreeMusic;
    private final GlyphLayout glyph = new GlyphLayout();
    private final Vector2 mouse = new Vector2();
    private final Color tempColor = new Color();
    private final DialogueRunner runner;
    private final BattleArena arena = new BattleArena();

    // dialogue state
    private DialogueScript.Node line, choiceNode, chapterNode;
    private RichText rich;
    private float layoutWidth = -1f, textTime;
    private int revealed;
    private String lastSpeaker = "", tagText = "";
    private Texture portraitTexture;
    private DialogueScript.Node portraitNode;
    private Texture portraitLookup;
    private int selectedChoice;
    private int hoveredChoice = -1;
    private boolean choiceKeyboardFocused;
    private boolean auto, skipMode, leaving, transitioned, confirmQuit, logOpen;
    private int logScroll;
    private final List<String> logSpeaker = new ArrayList<String>();
    private final List<String> logText = new ArrayList<String>();

    // chapter card
    private float chapterT;
    private String chapterCard = "FOOL", chapterTitle = "";

    // battle state
    private int battlePhase;
    private float battleMix, battleUi, phaseT, hitFlash, shakeAmount, battleMusicMix, cameraFxBlend;
    private float arenaScreenTop;
    private int menuSel, enemyHp = ENEMY_MAX, trust, round;
    private boolean defending, battleMusicStarted, openingPending, openingRoundActive;
    private boolean phaseTwo, phaseThree;
    private boolean phaseTwoOpened, phaseThreeOpened;
    private float impactT, impactStrength, fxTime, finaleImpactT, finaleBars, monoT, monoLen, monoStrength;
    private int p3HitIdx, maydayStrobe = -1;
    private float glitchTextT;
    private float maydayCinema;
    private String battlePattern = "default", outcome;
    private RichText bRich;
    private float bLayoutW = -1f, bTextT;
    private int nextSeq, curSeq;                    // next sequence to start / sequence last started
    private float pickSeconds = 4f, seqTitleT;
    private String seqTitle = "";
    private volatile boolean battleMusicEnded;
    private boolean musicHeld;

    // animation state
    private String shownExpression;
    private Texture previousSprite;
    private float crossFade = 1f, time, intro, tagVisibility, portraitVisibility, choiceTime,
        selectionAnimation, talkHop, flash, fadeOut, autoTimer, skipTimer, characterShakeTime,
        dialogueShakeTime,
        grainU, grainV;
    private int lastMx = -1, lastMy = -1;

    // layout
    private float visX, visY, visW = Main.WORLD_WIDTH, visH = Main.WORLD_HEIGHT;
    private float sceneX, sceneY, sceneW, sceneH;
    private float panelX, panelY, panelW, panelH;
    private float portX, portY, portW, portH;
    private float arenaX, arenaY, arenaS;
    private float battleSafeX, battleSafeY, battleSafeW, battleSafeH;
    private float qmX0, qmY, cqYesX, cqNoX, cqY;
    private static final float QM_W = 72f, QM_H = 26f, QM_GAP = 6f, CQ_W = 150f, CQ_H = 44f;
    private final float[] rect = new float[4];
    private final float[] choiceGeo = new float[5];
    private float[] choicePop = new float[8];
    private boolean[] choiceDrawPlayed = new boolean[8];
    private float[] choiceFlipProgress = new float[8];
    private float pressureT;

    public AustineBossBattle(Main game) {
        this.game = game;

        background = loadTexture("Images/Backgrounds/Gameplay/BackgroundTest.png");
        topLine = loadTexture("Images/UI/Menu/TOPLINE1.png");
        topLineRegion = new TextureRegion(topLine, 8, 151, 650, 47);
        choiceIndicator = loadTexture("Images/UI/ChoiceIndicator/TarotCardIndicator.png");
        choiceCardBack = loadTexture("Images/Cards/Choice/BackCards.png");
        grain = createGrain();
        vignette = createVignette();

        // isang folder kada character: Images/Characters/<NAME>/{Sprites,Portraits}
        sprites.put("normal", loadTexture("Images/Characters/MARIA/Sprites/normal.png"));
        sprites.put("nakikinig", loadTexture("Images/Characters/MARIA/Sprites/nakikinig.png"));
        portraits.put("MARIA", loadTexture("Images/Characters/MARIA/Portraits/minipfp.png"));
        // portrait by speaker name, dagdag lang ng file

        for (String name : TAROT_CARD_NAMES) {
            cardTex.put(name, loadTexture("Images/Cards/Tarot/" + name + ".jpg"));
        }
        for (String rank : CHOICE_CARD_RANKS) {
            cardTex.put(rank, loadTexture("Images/Cards/Choice/" + rank + ".png"));
        }

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        pixelRegion = new TextureRegion(pixel);
        monoShader = createMonoShader();

        FreeTypeFontGenerator typeGen = new FreeTypeFontGenerator(Gdx.files.internal(TYPEWRITER_FONT));
        dialogueFont = makeFont(typeGen, 26);
        typeGen.dispose();
        FreeTypeFontGenerator nameGen = new FreeTypeFontGenerator(Gdx.files.internal(NAME_FONT));
        nameFont = makeFont(nameGen, 22);
        uiFont = makeFont(nameGen, 16);
        choiceFont = makeFont(nameGen, 17);
        titleFont = makeFont(nameGen, 44);
        nameGen.dispose();

        FileHandle blipFile = Gdx.files.internal(BLIP_FILE);
        blip = blipFile.exists() ? Gdx.audio.newSound(blipFile) : null;
        cardDrawSfx = loadSound(CARD_DRAW_SOUND);
        cardFlipSfx = loadSound(CARD_FLIP_SOUND);
        chosenSfx = loadSound(CHOSEN_SOUND);

        FileHandle ost = Gdx.files.internal(BATTLE_OST);
        if (ost.exists()) {
            battleMusic = Gdx.audio.newMusic(ost);
            battleMusic.setLooping(false);          // one pass: the fight is scheduled against the song
            battleMusic.setVolume(0f);
            battleMusic.setOnCompletionListener(new Music.OnCompletionListener() {
                @Override
                public void onCompletion(Music music) {
                    battleMusicEnded = true;
                }
            });
        } else {
            Gdx.app.error("AustineBossBattle",
                "Battle music not found: " + ost.path() + ". Add Austine Phase 1.mp3 to Audio/Music.");
            battleMusic = null;
        }
        FileHandle ost2 = Gdx.files.internal(BATTLE_OST_2);
        if (ost2.exists()) {
            phaseTwoMusic = Gdx.audio.newMusic(ost2);
            phaseTwoMusic.setLooping(false);
            phaseTwoMusic.setVolume(0f);
            phaseTwoMusic.setOnCompletionListener(new Music.OnCompletionListener() {
                @Override
                public void onCompletion(Music music) {
                    battleMusicEnded = true;
                }
            });
        } else {
            Gdx.app.error("AustineBossBattle",
                "Phase 2 music not found: " + ost2.path() + ". Add Austine Phase 2.mp3 to Audio/Music.");
            phaseTwoMusic = null;
        }
        FileHandle ost3 = Gdx.files.internal(BATTLE_OST_3);
        if (ost3.exists()) {
            phaseThreeMusic = Gdx.audio.newMusic(ost3);
            phaseThreeMusic.setLooping(false);
            phaseThreeMusic.setVolume(0f);
            phaseThreeMusic.setOnCompletionListener(new Music.OnCompletionListener() {
                @Override
                public void onCompletion(Music music) {
                    battleMusicEnded = true;
                }
            });
        } else {
            Gdx.app.error("AustineBossBattle",
                "Phase 3 music not found: " + ost3.path() + ". Add Austine Phase 3.mp3 to Audio/Music.");
            phaseThreeMusic = null;
        }

        arena.configure(BATTLE_BPM, BEAT_OFFSET);
        arena.loadVis(Gdx.files.internal(BATTLE_VIS));
        runner = new DialogueRunner(DialogueScript.load(Gdx.files.internal(SCRIPT_FILE)));
        shownExpression = runner.expression();
    }

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

        Gdx.app.error("AustineBossBattle", "Monochrome shader failed to compile: " + shader.getLog());
        shader.dispose();
        return null;
    }

    private Texture loadTexture(String path) {
        Texture t = new Texture(Gdx.files.internal(path), true);
        t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        return t;
    }

    private Sound loadSound(String path) {
        FileHandle file = Gdx.files.internal(path);
        return file.exists() ? Gdx.audio.newSound(file) : null;
    }

    private void playCardSound(Sound sound, float volume) {
        if (sound != null) sound.play(volume * GameSettings.sfx);
    }

    private BitmapFont makeFont(FreeTypeFontGenerator generator, int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size * 2;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u201C\u201D\u2019\u2014\u2026";
        p.genMipMaps = true;
        p.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(p);
        font.getData().setScale(0.5f);
        font.setUseIntegerPositions(false);
        font.setColor(TEXT);
        return font;
    }

    // input

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int k) {
                if (leaving) return true;
                if (confirmQuit) {
                    if (k == Input.Keys.Y || k == Input.Keys.ENTER) leave();
                    else if (k == Input.Keys.N || k == Input.Keys.ESCAPE) confirmQuit = false;
                    return true;
                }
                if (logOpen) {
                    if (k == Input.Keys.ESCAPE || k == Input.Keys.L) logOpen = false;
                    else if (k == Input.Keys.UP || k == Input.Keys.W) scrollLog(1);
                    else if (k == Input.Keys.DOWN || k == Input.Keys.S) scrollLog(-1);
                    return true;
                }
                if (k == Input.Keys.ESCAPE) {
                    confirmQuit = true;
                    return true;
                }
                if (chapterActive()) {
                    if (k == Input.Keys.SPACE || k == Input.Keys.ENTER) skipChapter();
                    return true;
                }
                if (battlePhase != B_NONE) return battleKey(k);
                if (k == Input.Keys.L) {
                    openLog();
                    return true;
                }
                if (choicesOpen()) {
                    int n = runner.current().choices.size();
                    if (k == Input.Keys.UP || k == Input.Keys.W || k == Input.Keys.LEFT) {
                        selectedChoice = (selectedChoice + n - 1) % n;
                        choiceKeyboardFocused = true;
                        return true;
                    }
                    if (k == Input.Keys.DOWN || k == Input.Keys.S || k == Input.Keys.RIGHT || k == Input.Keys.D) {
                        selectedChoice = (selectedChoice + 1) % n;
                        choiceKeyboardFocused = true;
                        return true;
                    }
                    if (k >= Input.Keys.NUM_1 && k < Input.Keys.NUM_1 + n) {
                        chooseOption(k - Input.Keys.NUM_1);
                        return true;
                    }
                    if (k == Input.Keys.SPACE || k == Input.Keys.ENTER) {
                        chooseOption(selectedChoice);
                        return true;
                    }
                    return false;
                }
                if (k == Input.Keys.A) {
                    auto = !auto;
                    autoTimer = 0f;
                    return true;
                }
                if (k == Input.Keys.SPACE || k == Input.Keys.ENTER) {
                    confirm();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int sx, int sy, int pointer, int button) {
                if (leaving) return true;
                toWorld(sx, sy);
                if (confirmQuit) {
                    if (hit(cqYesX, cqY, CQ_W, CQ_H)) leave();
                    else if (hit(cqNoX, cqY, CQ_W, CQ_H)) confirmQuit = false;
                    return true;
                }
                if (logOpen) {
                    logOpen = false;
                    return true;
                }
                if (quickVisible()) {
                    for (int i = 0; i < QUICK_LABELS.length; i++) {
                        float buttonX = quickButtonX(i);
                        if (hit(buttonX, qmY, quickButtonWidth(i), QM_H)) {
                            quickAction(i);
                            return true;
                        }
                    }
                }
                if (chapterActive()) {
                    skipChapter();
                    return true;
                }
                if (battlePhase != B_NONE) {
                    battleClick();
                    return true;
                }
                if (choicesOpen()) {
                    int c = choiceAt();
                    if (c >= 0) chooseOption(c);
                    return true;
                }
                confirm();
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (logOpen) scrollLog(amountY < 0 ? 1 : -1);
                return true;
            }
        });
    }

    private void toWorld(int sx, int sy) {
        mouse.set(sx, sy);
        viewport.unproject(mouse);
    }

    private boolean pointerAvailable() {
        int x = Gdx.input.getX(), y = Gdx.input.getY();
        return game.isWindowActive() && x >= 0 && y >= 0
            && x < Gdx.graphics.getWidth() && y < Gdx.graphics.getHeight();
    }

    private boolean hit(float x, float y, float w, float h) {
        return mouse.x >= x && mouse.x <= x + w && mouse.y >= y && mouse.y <= y + h;
    }

    private void leave() {
        leaving = true;
        confirmQuit = false;
    }

    private void openLog() {
        logOpen = true;
        logScroll = 0;
    }

    private void scrollLog(int dir) {
        logScroll = MathUtils.clamp(logScroll + dir, 0, Math.max(0, logText.size() - 1));
    }

    private boolean quickVisible() {
        return battlePhase == B_NONE && !chapterActive() && intro >= 0.9f && !leaving;
    }

    private void quickAction(int i) {
        if (i == 0) openLog();
        else if (i == 1) { auto = !auto; autoTimer = 0f; }
        else if (i == 2) skipMode = !skipMode;
        else if (i == 3) startPhaseTwo(true);
        else if (i == 4) startPhaseThree(true);
        else confirmQuit = true;
    }

    private float quickButtonWidth(int i) {
        if (i == 3) return QM_PHASE2_W;
        if (i == 4) return QM_PHASE3_W;
        return QM_W;
    }

    private float quickButtonX(int i) {
        float x = qmX0;
        for (int button = 0; button < i; button++) {
            x += quickButtonWidth(button) + QM_GAP;
        }
        return x;
    }

    private boolean chapterActive() {
        DialogueScript.Node c = runner.current();
        return c != null && c.type == DialogueScript.Type.CHAPTER;
    }

    private void skipChapter() {
        if (chapterT > 1f && chapterT < CHAPTER_TIME - 0.6f) chapterT = CHAPTER_TIME - 0.6f;
    }

    private boolean choicesOpen() {
        DialogueScript.Node c = runner.current();
        return c != null && c.type == DialogueScript.Type.CHOICE && intro >= 0.8f && battlePhase == B_NONE;
    }

    private void confirm() {
        if (leaving || intro < 0.8f || line == null || runner.isFinished() || choicesOpen()) return;
        DialogueScript.Node c = runner.current();
        if (c == null || c.type != DialogueScript.Type.LINE) return;
        if (rich != null && textTime < rich.totalTime) {
            textTime = rich.totalTime;
            revealed = rich.chars.size();
        } else {
            runner.next();
        }
    }

    private void chooseOption(int index) {
        DialogueScript.Node node = runner.current();
        if (node != null && index >= 0 && index < node.choices.size()) {
            playCardSound(chosenSfx, 1f);
            logSpeaker.add("");
            logText.add("> " + node.choices.get(index).text);
        }
        runner.choose(index);
        selectedChoice = 0;
    }

    // choice cards: naka-fan na baraha
    private void cardGeo(int i, int n) {
        float maxW = visW * 0.78f / (1f + (n - 1) * 0.5f);
        float baseW = Math.min(196f, maxW);
        float baseH = baseW * 1.5f;
        float pop = choicePop[i];
        float mid = (n - 1) / 2f;
        float theta = (i - mid) * (n > 1 ? 15f : 0f);
        float radius = baseH * 1.5f;
        float restCy = panelY + panelH + 34f + baseH / 2f;
        float pivotX = visX + visW / 2f;
        float pivotY = restCy - radius;
        float distance = radius + pop * 60f;
        float radians = theta * MathUtils.degreesToRadians;
        float scale = 1f + 0.14f * pop;
        choiceGeo[0] = pivotX + MathUtils.sin(radians) * distance;
        choiceGeo[1] = pivotY + MathUtils.cos(radians) * distance;
        choiceGeo[2] = baseW * scale;
        choiceGeo[3] = baseH * scale;
        choiceGeo[4] = -theta * (1f - pop);
    }

    private boolean hitChoice(int i, int n) {
        cardGeo(i, n);
        float dx = mouse.x - choiceGeo[0], dy = mouse.y - choiceGeo[1];
        float cos = MathUtils.cosDeg(choiceGeo[4]), sin = MathUtils.sinDeg(choiceGeo[4]);
        float localX = dx * cos + dy * sin;
        float localY = -dx * sin + dy * cos;
        return Math.abs(localX) <= choiceGeo[2] / 2f && Math.abs(localY) <= choiceGeo[3] / 2f;
    }

    private int choiceAt() {
        if (!choicesOpen()) return -1;
        int n = runner.current().choices.size();
        int focus = choiceKeyboardFocused ? selectedChoice : hoveredChoice;
        if (focus >= 0 && focus < n && hitChoice(focus, n)) return focus;
        for (int i = n - 1; i >= 0; i--) {
            if (i != focus && hitChoice(i, n)) return i;
        }
        return -1;
    }

    // layout

    private void computeLayout() {
        panelW = Math.min(visW * 0.86f, 1160f);
        panelH = visH * 0.215f;
        panelX = visX + (visW - panelW) / 2f;
        panelY = visY + visH * 0.035f;

        sceneX = panelX;
        sceneY = panelY + panelH + visH * 0.065f;
        sceneW = panelW;
        sceneH = visH * 0.56f;

        portH = panelH * 0.98f;
        portW = portH;
        portX = panelX + 20f;
        portY = panelY + 14f;

        arenaS = Math.min(visH * 0.4f, 300f);
        arenaX = visX + (visW - arenaS) / 2f;
        battleSafeX = visX + 32f;
        battleSafeY = panelY + panelH + 55f;
        battleSafeW = Math.max(1f, visW - 64f);
        float safeTop = visY + visH - 130f;
        battleSafeH = Math.max(arenaS + 16f, safeTop - battleSafeY);
        arena.setSafeRect(battleSafeX, battleSafeY, battleSafeW, battleSafeH);
        arenaY = panelY;

        qmX0 = visX + visW - 28f
            - (4 * QM_W + QM_PHASE2_W + QM_PHASE3_W + 5 * QM_GAP);
        qmY = visY + visH - 34f;

        cqYesX = visX + visW / 2f - 190f;
        cqNoX = visX + visW / 2f + 40f;
        cqY = visY + visH / 2f - 50f;
    }

    private void updateVisibleArea() {
        visW = viewport.getWorldWidth();
        visH = viewport.getWorldHeight();
        visX = Main.WORLD_WIDTH / 2f - visW / 2f;
        visY = Main.WORLD_HEIGHT / 2f - visH / 2f;
    }

    private Texture portraitFor(DialogueScript.Node n) {
        if (n == null || n.speaker.isEmpty()) return null;
        if (n == portraitNode) return portraitLookup;   // isang lookup kada linya, hindi kada frame
        portraitNode = n;
        portraitLookup = portraits.get(n.speaker.toUpperCase(Locale.ROOT));
        return portraitLookup;
    }

    // helpers

    private static float ease(float v) {
        float c = MathUtils.clamp(v, 0f, 1f);
        float i = 1f - c;
        return 1f - i * i * i;
    }

    private static final String[] CHAR_CACHE = new String[256];

    private static String str(char c) {
        if (c < CHAR_CACHE.length) {
            if (CHAR_CACHE[c] == null) CHAR_CACHE[c] = String.valueOf(c);
            return CHAR_CACHE[c];
        }
        return String.valueOf(c);
    }

    private void startLine(DialogueScript.Node n) {
        line = n;
        rich = RichText.parse(n.text, n.speaker.isEmpty() ? NARRATION : TEXT, TEXT_SPEED * GameSettings.textSpeed);
        layoutWidth = -1f;
        textTime = 0f;
        revealed = 0;
        autoTimer = 0f;
        skipTimer = 0f;
        logSpeaker.add(n.speaker.toUpperCase(Locale.ROOT));
        logText.add(RichText.strip(n.text));
        if (!n.speaker.equals(lastSpeaker)) {
            lastSpeaker = n.speaker;
            tagText = n.speaker.isEmpty() ? "" : n.speaker.toUpperCase(Locale.ROOT);
            tagVisibility = 0f;
            if (portraitFor(n) != null) portraitVisibility = 0f;
        }
        if (!n.speaker.isEmpty()) talkHop = 1f;
    }

    // chapter

    private void updateChapter(float d, DialogueScript.Node cur) {
        if (cur == null || cur.type != DialogueScript.Type.CHAPTER) return;
        if (cur != chapterNode) {
            chapterNode = cur;
            chapterT = 0f;
            String[] parts = cur.text.split("\\|", 2);
            chapterCard = parts[0].trim().toUpperCase(Locale.ROOT);
            chapterTitle = parts.length > 1 ? parts[1].trim() : "";
        }

        chapterT += d;
        if (chapterT >= CHAPTER_TIME) runner.next();
    }

    private float chapterAlpha() {
        return Math.min(ease(chapterT / 0.6f), ease((CHAPTER_TIME - chapterT) / 0.6f));
    }

    // battle

    // kanta ng kasalukuyang phase
    private Music activeMusic() {
        return phaseThree ? phaseThreeMusic : phaseTwo ? phaseTwoMusic : battleMusic;
    }

    private int enemyMax() {
        return phaseThree ? ENEMY_MAX_3 : phaseTwo ? ENEMY_MAX_2 : ENEMY_MAX;
    }

    private float songPosition() {
        Music m = activeMusic();
        if (m != null && m.isPlaying()) return m.getPosition();
        return -1f;
    }

    private void startBattle(String pattern) {
        battlePattern = pattern;
        phaseTwo = PHASE_TWO_PATTERN.equals(pattern);
        phaseThree = PHASE_THREE_PATTERN.equals(pattern);
        phaseTwoOpened = false;
        phaseThreeOpened = false;
        if (phaseThree) {
            arena.usePhaseThree();
            arena.loadVis(Gdx.files.internal(BATTLE_VIS_3));
        } else if (phaseTwo) {
            arena.usePhaseTwo();
            arena.loadVis(Gdx.files.internal(BATTLE_VIS_2));
        } else {
            arena.usePhaseOne();
            arena.loadVis(Gdx.files.internal(BATTLE_VIS));
        }
        battleMusicStarted = false;
        if (phaseTwoMusic != null) phaseTwoMusic.stop();
        if (phaseThreeMusic != null) phaseThreeMusic.stop();
        if (battleMusic != null) {
            battleMusic.stop();
            battleMusic.setVolume(0f);
        }
        battlePhase = B_SHRINK;
        phaseT = 0f;
        monoT = monoLen = monoStrength = 0f;
        p3HitIdx = 0;
        glitchTextT = 0f;
        maydayStrobe = -1;
        battleMix = 0f;
        battleUi = 0f;
        enemyHp = enemyMax();
        trust = 0;
        round = 0;
        nextSeq = 0;
        curSeq = 0;
        seqTitleT = 0f;
        finaleImpactT = 1.2f;
        finaleBars = 0f;
        pickSeconds = 4f;
        defending = false;
        openingPending = !phaseTwo && !phaseThree;
        openingRoundActive = false;
        outcome = null;
        menuSel = 0;
        arena.resetHp();
        auto = false;
        skipMode = false;
        characterShakeTime = SHAKE_TIME;
        bRich = null;
        // phase 2/3 simula pag sara ng box, para sabay ang title at chart
        if (!phaseTwo && !phaseThree) startBattleMusic();
    }

    private void retryBattle() {
        if (phaseThree) {
            startPhaseThree(false);
            return;
        }
        if (phaseTwo) {
            startPhaseTwo(false);
            return;
        }
        arena.resetHp();
        startBattle(battlePattern);
    }

    private void setBattleText(String s) {
        bRich = RichText.parse(s, TEXT, TEXT_SPEED * GameSettings.textSpeed);
        bLayoutW = -1f;
        bTextT = 0f;
    }

    private boolean battleTextDone() {
        return bRich == null || bTextT >= bRich.totalTime;
    }

    private String menuFlavor() {
        return FLAVOR[round % FLAVOR.length];
    }

    // phase 3 pinakamalakas, isang hakbang pababa ang iba
    private float battleMusicGain() {
        return phaseThree ? PHASE3_MUSIC_VOLUME : BATTLE_MUSIC_VOLUME;
    }

    private void startBattleMusic() {
        Music m = activeMusic();
        if (m == null) return;
        battleMusicStarted = true;
        battleMusicEnded = false;
        musicHeld = false;
        m.stop();
        m.setPosition(0f);
        battleMusicMix = 1f;
        m.setVolume(battleMusicGain() * GameSettings.music);
        m.play();
    }

    private void updateBattleMusic(float delta) {
        Music m = activeMusic();
        if (m == null) return;
        // song-locked, kaya patayin ang kanta pag umalis
        if (confirmQuit || logOpen) {
            if (m.isPlaying()) {
                m.pause();
                musicHeld = true;
            }
            return;
        }
        if (musicHeld) {
            musicHeld = false;
            if (battleMusicStarted && !battleMusicEnded && battlePhase != B_NONE && battlePhase != B_END) {
                m.play();
            }
        }
        boolean on = battleMusicStarted && battlePhase != B_NONE && battlePhase != B_END && !leaving;
        if (on) {
            battleMusicMix = Math.min(1f, battleMusicMix + delta / 0.35f);
            if (!m.isPlaying() && !battleMusicEnded) m.play();
        } else {
            battleMusicMix = Math.max(0f, battleMusicMix - delta / 0.45f);
            if (battleMusicMix <= 0f) {
                if (m.isPlaying()) m.stop();
                battleMusicStarted = false;
            }
        }
        m.setVolume(battleMusicMix * battleMusicGain() * GameSettings.music);
    }

    // oras para pumili = natitira bago ang susunod na boundary
    private float computePickSeconds() {
        float pos = songPosition();
        if (pos < 0f || nextSeq >= arena.sequenceCount()) return 4f;
        float remain = arena.sequenceStartTime(nextSeq) - pos;
        return MathUtils.clamp(remain - RESULT_SLACK, PICK_MIN, PICK_MAX);
    }

    private void enterMenu() {
        battlePhase = B_MENU;
        phaseT = 0f;
        menuSel = 0;
        pickSeconds = computePickSeconds();
        arena.heal(MENU_HEAL);
        setBattleText(menuFlavor());
    }

    // kung walang natapos, kung sino ang mas nauna, sya na
    private void resolveOutcome() {
        float trustFrac = trust / (float) TRUST_NEEDED;
        float harmFrac = (enemyMax() - enemyHp) / (float) enemyMax();
        outcome = trustFrac >= harmFrac ? "spared" : "killed";
    }

    private void chooseBattleAction(int i) {
        int optionCount = battleOptionCount();
        if (i < 0 || i >= optionCount) return;
        menuSel = i;
        String result;
        if (i == 0) {
            int dmg = MathUtils.random(24, 32);
            enemyHp = Math.max(0, enemyHp - dmg);
            characterShakeTime = SHAKE_TIME;
            flash = 0.5f;
            result = "* Sinaktan mo ang anino. (-" + dmg + ")";
            if (enemyHp <= 0) {
                outcome = "killed";
                result += " Naglaho ito.";
            }
        } else if (i == 1) {
            defending = true;
            result = "* Nagkubli ka. Mas mahina ang susunod na hampas.";
        } else if (i == 2) {
            trust++;
            result = REASON_LINES[Math.min(trust, REASON_LINES.length) - 1];
            if (trust >= TRUST_NEEDED) outcome = "spared";
        } else {
            return;
        }
        setBattleText(result);
        battlePhase = B_RESULT;
        phaseT = 0f;
    }

    private void afterResult() {
        phaseT = 0f;
        if (outcome != null) {
            battlePhase = B_END;
        } else {
            battlePhase = B_SHRINK;
            round++;
        }
    }

    private void finishBattle() {
        DialogueScript.Node cur = runner.current();
        if (outcome != null) runner.setFlag(outcome);
        battlePhase = B_NONE;
        battleMix = 0f;
        battleUi = 0f;
        monoT = monoLen = monoStrength = 0f;
        p3HitIdx = 0;
        glitchTextT = 0f;
        maydayStrobe = -1;
        // pwedeng mag-umpisa ng laban sa labas ng script (PHASE2/PHASE3). i-check muna bago complete
        if (cur != null && cur.type == DialogueScript.Type.BATTLE) runner.completeBattle();
    }

    private boolean battleKey(int k) {
        if (battlePhase == B_P2_INTRO || battlePhase == B_P3_INTRO) {
            if (phaseT > 0.4f) phaseT = battlePhase == B_P3_INTRO ? P3_INTRO_TIME : P2_INTRO_TIME;
            return true;
        }
        if (battlePhase == B_MENU) {
            if (k == Input.Keys.LEFT || k == Input.Keys.A) menuSel = (menuSel + MENU_LABELS.length - 1) % MENU_LABELS.length;
            else if (k == Input.Keys.RIGHT || k == Input.Keys.D) menuSel = (menuSel + 1) % MENU_LABELS.length;
            else if (k >= Input.Keys.NUM_1 && k < Input.Keys.NUM_1 + MENU_LABELS.length) {
                chooseBattleAction(k - Input.Keys.NUM_1);
            }
            else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) chooseBattleAction(menuSel);
        } else if (battlePhase == B_RESULT) {
            if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) {
                if (battleTextDone()) afterResult();
                else bTextT = bRich.totalTime;
            }
        } else if (battlePhase == B_DEAD && phaseT > 0.8f) {
            if (k == Input.Keys.R || k == Input.Keys.ENTER || k == Input.Keys.SPACE) retryBattle();
            else if (k == Input.Keys.M) leave();
        }
        return true;
    }

    private void battleClick() {
        if (battlePhase == B_P2_INTRO || battlePhase == B_P3_INTRO) {
            if (phaseT > 0.4f) phaseT = battlePhase == B_P3_INTRO ? P3_INTRO_TIME : P2_INTRO_TIME;
            return;
        }
        if (battlePhase == B_MENU) {
            int i = battleOptionAt();
            if (i >= 0) chooseBattleAction(i);
        } else if (battlePhase == B_RESULT) {
            if (battleTextDone()) afterResult();
            else bTextT = bRich.totalTime;
        } else if (battlePhase == B_DEAD && phaseT > 0.8f) {
            retryBattle();
        }
    }

    private float optionCenterX(int i) {
        return panelX + panelW * (0.2f + 0.3f * i);
    }

    private int battleOptionCount() {
        return MENU_LABELS.length;
    }

    private int battleOptionAt() {
        for (int i = 0; i < battleOptionCount(); i++) {
            if (hit(optionCenterX(i) - 100f, panelY + 10f, 200f, 60f)) return i;
        }
        return -1;
    }

    // phase 2 mula zero: title sa tahimik na 15 bars, sabog sa bar 16
    private void startPhaseTwo(boolean withIntro) {
        phaseTwo = true;
        phaseThree = false;
        phaseThreeOpened = false;
        battleMusicStarted = false;
        if (battleMusic != null) battleMusic.stop();
        if (phaseThreeMusic != null) phaseThreeMusic.stop();
        if (phaseTwoMusic != null) {
            phaseTwoMusic.stop();
            phaseTwoMusic.setPosition(0f);
        }
        enemyHp = ENEMY_MAX_2;
        trust = 0;
        round = 0;
        nextSeq = 0;
        curSeq = 0;
        seqTitleT = 0f;
        finaleImpactT = 1.2f;
        finaleBars = 0f;
        pickSeconds = 4f;
        defending = false;
        openingPending = false;
        openingRoundActive = true;
        outcome = null;
        menuSel = 0;
        bRich = null;
        phaseTwoOpened = true;
        arena.usePhaseTwo();
        arena.loadVis(Gdx.files.internal(BATTLE_VIS_2));
        arena.resetHp();
        battleUi = 1f;
        battleMix = 1f;
        phaseT = 0f;
        hitFlash = 0f;
        monoT = monoLen = monoStrength = 0f;
        p3HitIdx = 0;
        glitchTextT = 0f;
        maydayStrobe = -1;
        startBattleMusic();
        float pos = songPosition();
        arena.startOpening(1f, pos >= 0f ? pos : 0f,
            arenaX + 4f, arenaY + 4f, arenaS - 8f, arenaS - 8f);
        battlePhase = withIntro ? B_P2_INTRO : B_DODGE;
    }

    // phase 2 galing sa dialogue (!battle dilim), nakasara na ang box
    private void beginPhaseTwoOpening() {
        phaseTwo = true;
        phaseThree = false;
        phaseThreeOpened = false;
        if (battleMusic != null) battleMusic.stop();
        if (phaseThreeMusic != null) phaseThreeMusic.stop();
        if (phaseTwoMusic != null) {
            phaseTwoMusic.stop();
            phaseTwoMusic.setPosition(0f);
        }
        phaseTwoOpened = true;
        battlePhase = B_P2_INTRO;
        phaseT = 0f;
        curSeq = 0;
        nextSeq = 0;
        openingPending = false;
        openingRoundActive = true;
        battleMusicEnded = false;
        battleMix = 1f;
        battleUi = 1f;
        startBattleMusic();
        arena.startOpening(1f, songPosition() >= 0f ? songPosition() : 0f,
            arenaX + 4f, arenaY + 4f, arenaS - 8f, arenaS - 8f);
    }

    private void startPhaseThree(boolean withIntro) {
        battlePattern = PHASE_THREE_PATTERN;
        phaseTwo = false;
        phaseThree = true;
        phaseTwoOpened = false;
        battleMusicStarted = false;
        if (battleMusic != null) battleMusic.stop();
        if (phaseTwoMusic != null) phaseTwoMusic.stop();
        if (phaseThreeMusic != null) {
            phaseThreeMusic.stop();
            phaseThreeMusic.setPosition(0f);
        }
        enemyHp = ENEMY_MAX_3;
        trust = 0;
        round = 0;
        nextSeq = 0;
        curSeq = 0;
        seqTitleT = 0f;
        finaleImpactT = 1.2f;
        finaleBars = 0f;
        pickSeconds = 4f;
        defending = false;
        openingPending = false;
        openingRoundActive = true;
        outcome = null;
        menuSel = 0;
        bRich = null;
        phaseThreeOpened = true;
        monoT = monoLen = monoStrength = 0f;
        p3HitIdx = 0;
        glitchTextT = 0f;
        maydayStrobe = -1;
        arena.usePhaseThree();
        arena.loadVis(Gdx.files.internal(BATTLE_VIS_3));
        arena.resetHp();
        battleUi = 1f;
        battleMix = 1f;
        phaseT = 0f;
        hitFlash = 0f;
        startBattleMusic();
        arena.startOpening(1f, songPosition() >= 0f ? songPosition() : 0f,
            arenaX + 4f, arenaY + 4f, arenaS - 8f, arenaS - 8f);
        battlePhase = withIntro ? B_P3_INTRO : B_DODGE;
    }

    private void beginPhaseThreeOpening() {
        phaseTwo = false;
        phaseThree = true;
        if (battleMusic != null) battleMusic.stop();
        if (phaseTwoMusic != null) phaseTwoMusic.stop();
        if (phaseThreeMusic != null) {
            phaseThreeMusic.stop();
            phaseThreeMusic.setPosition(0f);
        }
        phaseThreeOpened = true;
        battlePhase = B_P3_INTRO;
        phaseT = 0f;
        curSeq = 0;
        nextSeq = 0;
        openingPending = false;
        openingRoundActive = true;
        battleMusicEnded = false;
        battleMix = 1f;
        battleUi = 1f;
        startBattleMusic();
        arena.usePhaseThree();
        arena.loadVis(Gdx.files.internal(BATTLE_VIS_3));
        arena.startOpening(1f, songPosition() >= 0f ? songPosition() : 0f,
            arenaX + 4f, arenaY + 4f, arenaS - 8f, arenaS - 8f);
    }

    private boolean fxPhase() {
        return battlePhase == B_SHRINK || battlePhase == B_DODGE || battlePhase == B_GROW || battlePhase == B_DEAD;
    }

    // signature ng phase 3: 9 mono hit, shake, impact frame, zoom sa puso
    private void updatePhaseThreeHits(float d) {
        glitchTextT = Math.max(0f, glitchTextT - d);
        if (battlePhase == B_NONE || battlePhase == B_END) return;
        float pos = songPosition();
        if (pos < 0f) return;
        while (p3HitIdx < P3_GLITCH_HITS.length && pos >= P3_GLITCH_HITS[p3HitIdx]) {
            p3HitIdx++;
            firePhaseThreeGlitch();
        }
        boolean mayday = pos >= P3_MAYDAY_START && pos < P3_MAYDAY_END;
        int strobe = mayday ? (int) ((pos - P3_MAYDAY_START) / P3_MAYDAY_STROBE) : -1;
        if (strobe >= 0 && strobe != maydayStrobe && blip != null) {
            blip.play(0.8f * GameSettings.sfx, (strobe & 1) == 0 ? 0.30f : 0.48f,
                (strobe & 1) == 0 ? -0.7f : 0.7f);
        }
        maydayStrobe = strobe;
    }

    private void firePhaseThreeGlitch() {
        monoLen = P3_GLITCH_DURATION;
        monoT = monoLen;
        monoStrength = 1f;
        triggerImpactFrame(1f);
        shakeAmount = Math.max(shakeAmount, 6f);
        flash = Math.max(flash, 1f);
        hitFlash = Math.max(hitFlash, 0.9f);
        glitchTextT = P3_GLITCH_DURATION;
        arena.signalBurst();
        if (blip != null) blip.play(1f * GameSettings.sfx, 0.18f, 0f);
    }

    // 0..1 window ng MAYDAY, may slide in at out
    private float maydayCinemaTarget() {
        if (battlePhase == B_NONE || battlePhase == B_END) return 0f;
        float pos = songPosition();
        if (pos < P3_MAYDAY_START || pos >= P3_MAYDAY_END) return 0f;
        float u = Math.min((pos - P3_MAYDAY_START) / 0.6f, (P3_MAYDAY_END - pos) / 0.6f);
        return MathUtils.clamp(u, 0f, 1f);
    }

    // MAYDAY at SIGNAL LOST, sadyang basag ang dating
    private void drawPhaseThreeSignal(SpriteBatch batch) {
        float a = battleUi;
        if (a <= 0.01f) return;
        float pos = songPosition();
        boolean mayday = pos >= P3_MAYDAY_START && pos < P3_MAYDAY_END;
        if (mayday) {
            float t = pos - P3_MAYDAY_START;
            for (int i = 0; i < 3; i++) {
                float cycleT = t - 0.2f - i * P3_MAYDAY_CYCLE;
                drawMaydaySwipe(batch, cycleT, 0f, 0.36f,
                    P3_MAYDAY_SWEEP - 0.48f, P3_MAYDAY_SWEEP,
                    true, 0.70f, a);
                drawMaydaySwipe(batch, cycleT, 0f, 0.36f,
                    P3_MAYDAY_SWEEP - 0.48f, P3_MAYDAY_SWEEP,
                    false, 0.32f, a);
            }
        }
        if (glitchTextT > 0f) {
            float g = MathUtils.clamp(glitchTextT / P3_GLITCH_DURATION, 0f, 1f);
            drawGlitchWord(batch, "SIGNAL LOST", visX + visW / 2f, visY + visH * 0.52f, g * a);
        }
        batch.setColor(Color.WHITE);
    }

    // malaking text, chroma split at punit
    // isang side ng broadcast: pasok, strobe, swipe pabalik
    private void drawMaydaySwipe(SpriteBatch batch, float t, float in0, float in1,
                                 float out0, float out1, boolean fromLeft, float yFrac, float a) {
        if (t < in0 || t > out1) return;
        float k;
        if (t < in1) k = ease((t - in0) / Math.max(0.01f, in1 - in0));
        else if (t > out0) k = 1f - ease((t - out0) / Math.max(0.01f, out1 - out0));
        else k = 1f;
        if (k <= 0.001f) return;
        float w = visW * 0.72f;
        float slide = (1f - k) * (w + 80f);
        float x = fromLeft ? visX + visW * 0.05f - slide : visX + visW * 0.95f - w + slide;
        float y = visY + visH * yFrac - 34f;
        float flick = 0.72f + 0.28f * (float) Math.sin(fxTime * 41f);
        float vy = (float) Math.sin(fxTime * 57f) * 2f;
        batch.setColor(0f, 0f, 0f, 0.92f * k * a);
        batch.draw(pixel, x - 18f, y - 16f + vy, w + 36f, 96f);
        batch.setColor(0.88f, 0.93f, 1f, 0.6f * k * a * flick);
        batch.draw(pixel, x - 18f, y + 78f + vy, w + 36f, 2f);
        batch.draw(pixel, x - 18f, y - 16f + vy, w + 36f, 2f);
        float oldScaleX = titleFont.getData().scaleX;
        float oldScaleY = titleFont.getData().scaleY;
        titleFont.getData().setScale(oldScaleX * 1.8f, oldScaleY * 1.8f);
        glyph.setText(titleFont, "MAYDAY!");
        titleFont.setColor(1f, 1f, 1f, k * a * flick);
        titleFont.draw(batch, "MAYDAY!", x + (w - glyph.width) / 2f + vy, y + 65f + vy);
        titleFont.setColor(TEXT);
        titleFont.getData().setScale(oldScaleX, oldScaleY);
        uiFont.setColor(0.88f, 0.93f, 1f, 0.7f * k * a);
        uiFont.draw(batch, fromLeft ? "ERR 0x1F  //  NO SIGNAL" : "CHANNEL LOST  //  0:10", x, y + 23f + vy);
        uiFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private void drawGlitchWord(SpriteBatch batch, String text, float cx, float y, float alpha) {
        float oldScaleX = titleFont.getData().scaleX;
        float oldScaleY = titleFont.getData().scaleY;
        titleFont.getData().setScale(oldScaleX * 2.1f, oldScaleY * 2.1f);
        glyph.setText(titleFont, text);
        float w = glyph.width;
        float h = titleFont.getCapHeight();
        float x = cx - w / 2f;
        float jx = (float) Math.sin(fxTime * 61f) * 12f + (float) Math.sin(fxTime * 143f) * 6f;
        float jy = (float) Math.cos(fxTime * 53f) * 7f;
        titleFont.setColor(1f, 0.04f, 0.12f, alpha * 0.82f);
        titleFont.draw(batch, text, x - 13f + jx, y + jy);
        titleFont.setColor(0.05f, 0.82f, 1f, alpha * 0.82f);
        titleFont.draw(batch, text, x + 13f + jx, y + jy);
        titleFont.setColor(1f, 1f, 1f, alpha);
        titleFont.draw(batch, text, x + jx, y + jy);
        batch.setColor(0f, 0f, 0f, alpha * 0.95f);
        batch.draw(pixel, x - 20f + jx, y + h * 0.42f, w + 40f, Math.max(8f, h * 0.18f));
        batch.setColor(1f, 1f, 1f, alpha * 0.95f);
        batch.draw(pixel, x - 20f - jx, y + h * 0.72f, w + 40f, Math.max(3f, h * 0.04f));
        batch.setColor(Color.WHITE);
        titleFont.setColor(TEXT);
        titleFont.getData().setScale(oldScaleX, oldScaleY);
    }

    private void updateBattle(float d, float realDelta, DialogueScript.Node cur, boolean moved) {
        fxTime += d;
        impactT = Math.max(0f, impactT - d);
        monoT = Math.max(0f, monoT - d);
        if (phaseTwo && battlePhase == B_DODGE && arena.spinFinale()) {
            finaleImpactT -= d;
            if (finaleImpactT <= 0f) {
                triggerImpactFrame(0.6f + 0.3f * arena.finaleProgress());
                finaleImpactT = 3.4f - 1.8f * arena.finaleProgress();
            }
        } else {
            finaleImpactT = 1.2f;
        }
        float barTarget = (phaseTwo && battlePhase == B_DODGE && arena.spinFinale()) ? 1f : 0f;
        finaleBars += (barTarget - finaleBars) * Math.min(1f,
            d * (barTarget > finaleBars ? 18f : 10f));
        arena.setFxLive(fxPhase());
        float cameraFxTarget = fxPhase() ? 1f : 0f;        cameraFxBlend += (cameraFxTarget - cameraFxBlend) * Math.min(1f, d * (cameraFxTarget > cameraFxBlend ? 6f : 3.5f));
        if (battlePhase != B_NONE) {
            syncArena();
            arena.tickVisual(d, songPosition());
        }
        hitFlash = Math.max(0f, hitFlash - d * 3f);
        shakeAmount = Math.max(0f, shakeAmount - d * 3.5f);
        seqTitleT = Math.max(0f, seqTitleT - d);
        updateBattleMusic(realDelta);
        if (phaseThree) updatePhaseThreeHits(d);
        float cinT = phaseThree ? maydayCinemaTarget() : 0f;
        maydayCinema += (cinT - maydayCinema) * Math.min(1f, d * (cinT > maydayCinema ? 9f : 6f));

        if (battlePhase == B_NONE) {
            if (cur != null && cur.type == DialogueScript.Type.BATTLE && intro >= 0.8f && !leaving) {
                startBattle(cur.text);
            }
            return;
        }

        float uiTarget = battlePhase == B_END ? 0f : 1f;
        battleUi = MathUtils.clamp(battleUi + (uiTarget > battleUi ? d / 0.4f : -d / 0.5f), 0f, 1f);
        if (bRich != null && bTextT < bRich.totalTime) bTextT += d;

        if (battlePhase == B_MENU && moved && !confirmQuit && !logOpen) {
            int i = battleOptionAt();
            if (i >= 0) menuSel = i;
        }

        switch (battlePhase) {
            case B_INTRO:
                phaseT += d;
                if (phaseT > 1.6f && battleTextDone()) enterMenu();
                break;
            case B_MENU:
                phaseT += d;
                if (phaseT >= pickSeconds) chooseBattleAction(1);
                break;
            case B_RESULT:
                if (battleTextDone()) {
                    phaseT += d;
                    if (phaseT > RESULT_HOLD) afterResult();
                }
                break;
            case B_SHRINK:
                battleMix = Math.min(1f, battleMix + d / BOX_SHRINK_TIME);
                if (battleMix >= 1f) {
                    battlePhase = B_DODGE;
                    if (phaseThree && !phaseThreeOpened) {
                        beginPhaseThreeOpening();
                    } else if (phaseTwo && !phaseTwoOpened) {
                        beginPhaseTwoOpening();
                    } else if (openingPending) {
                        openingPending = false;
                        openingRoundActive = true;
                        curSeq = 0;
                        arena.startOpening(1f, songPosition(),
                            arenaX + 4f, arenaY + 4f, arenaS - 8f, arenaS - 8f);
                    } else {
                        int idx = Math.min(nextSeq, arena.sequenceCount() - 1);
                        arena.startSequence(idx, defending ? 0.5f : 1f, songPosition(),
                            arenaX + 4f, arenaY + 4f, arenaS - 8f, arenaS - 8f);
                        curSeq = idx;
                        nextSeq = idx + 1;
                        defending = false;
                    }
                }
                break;
            case B_DODGE:
                syncArena();
                BattleArena.Result r = arena.update(d, songPosition());
                int started = arena.consumeSeqStart();
                if (started >= 0) {
                    String roman = ROMAN[Math.min(started, ROMAN.length - 1)];
                    seqTitle = "[ " + roman + " - " + arena.sequenceName(started) + " ]";
                    seqTitleT = SEQ_TITLE_TIME;
                    if (phaseTwo || phaseThree) triggerImpactFrame(0.85f);
                }
                if (phaseThree) arena.consumeMonochromeCue();
                if (arena.consumeHit()) {
                    hitFlash = 1f;
                    shakeAmount = 1.0f;
                }
                if (arena.consumeDrop()) {
                    if (phaseTwo || phaseThree) triggerImpactFrame(1f);
                    shakeAmount = 2.4f;
                    flash = 1f;
                    hitFlash = 0.5f;
                    characterShakeTime = SHAKE_TIME;
                    dialogueShakeTime = SHAKE_TIME;
                }
                if (r == BattleArena.Result.DEAD) {
                    battlePhase = B_DEAD;
                    phaseT = 0f;
                } else if (r == BattleArena.Result.DONE) {
                    if (openingRoundActive) {
                        openingRoundActive = false;
                        nextSeq = 1;
                        round++;
                    }
                    if (nextSeq >= arena.sequenceCount() && outcome == null) resolveOutcome();
                    battlePhase = B_GROW;
                }
                break;
            case B_GROW:
                battleMix = Math.max(0f, battleMix - d / BOX_GROW_TIME);
                if (battleMix <= 0f) {
                    if (outcome != null) {
                        battlePhase = B_END;
                        phaseT = 0f;
                    } else {
                        enterMenu();
                    }
                }
                break;
            case B_DEAD:
                phaseT += d;
                break;
            case B_END:
                phaseT += d;
                if (phaseT > 0.7f) finishBattle();
                break;
            case B_P2_INTRO:
            case B_P3_INTRO: {
                phaseT += d;
                syncArena();
                BattleArena.Result pr = arena.update(d, songPosition());
                int pstarted = arena.consumeSeqStart();
                if (pstarted >= 0) {
                    curSeq = pstarted;
                }
                if (phaseThree) arena.consumeMonochromeCue();
                if (pr == BattleArena.Result.DEAD) {
                    battlePhase = B_DEAD;
                    phaseT = 0f;
                } else if (phaseT >= (battlePhase == B_P3_INTRO ? P3_INTRO_TIME : P2_INTRO_TIME)) {
                    battlePhase = B_DODGE;
                }
                break;
            }
            default:
                break;
        }
    }

    // camera

    private float fitWorldScale(float desired, float rot) {
        float half = arenaS / 2f;
        float radians = Math.abs(rot) * MathUtils.degreesToRadians;
        float extent = half * (Math.abs(MathUtils.cos(radians)) + Math.abs(MathUtils.sin(radians)));
        float maxX = (visW / 2f - BATTLE_CAMERA_MARGIN) / extent;
        float maxY = (visH / 2f - BATTLE_CAMERA_MARGIN) / extent;
        return MathUtils.clamp(desired, 1f, Math.max(1f, Math.min(maxX, maxY)));
    }

    private float fitWorldCenter(float center, float start, float size, float extent, float scale) {
        float inset = extent * scale + BATTLE_CAMERA_MARGIN;
        float min = start + inset;
        float max = start + size - inset;
        return min > max ? start + size / 2f : MathUtils.clamp(center, min, max);
    }

    // render

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        float gd = (confirmQuit || logOpen) ? 0f : delta; // game time stops while a popup is open
        time += delta;
        grainU += delta * 0.015f;
        grainV += delta * 0.01f;
        intro = Math.min(1f, intro + delta / INTRO_TIME);
        characterShakeTime = Math.max(0f, characterShakeTime - gd);
        dialogueShakeTime = Math.max(0f, dialogueShakeTime - gd);
        talkHop = Math.max(0f, talkHop - gd * 3.5f);
        flash = Math.max(0f, flash - gd * 2.2f);
        if (crossFade < 1f) {
            crossFade = Math.min(1f, crossFade + gd / 0.3f);
            if (crossFade >= 1f) previousSprite = null;
        }

        camera.position.set(Main.WORLD_WIDTH / 2f, Main.WORLD_HEIGHT / 2f, 0f);
        camera.update();
        updateVisibleArea();
        computeLayout();

        int mxs = Gdx.input.getX(), mys = Gdx.input.getY();
        boolean pointerAvailable = pointerAvailable();
        boolean moved = pointerAvailable && (mxs != lastMx || mys != lastMy);
        lastMx = mxs;
        lastMy = mys;
        toWorld(mxs, mys);

        if (runner.consumeShake()) {
            characterShakeTime = SHAKE_TIME;
            dialogueShakeTime = SHAKE_TIME;
        }
        if (runner.consumeMindSpike()) {
            characterShakeTime = Math.max(characterShakeTime, SHAKE_TIME * 0.85f);
            dialogueShakeTime = Math.max(dialogueShakeTime, SHAKE_TIME * 0.6f);
        }
        if (runner.consumeFlash()) flash = 1f;
        if (!runner.expression().equals(shownExpression)) {
            previousSprite = sprites.get(shownExpression);
            shownExpression = runner.expression();
            crossFade = 0f;
        }

        DialogueScript.Node current = runner.current();
        updateChapter(gd, current);
        current = runner.current();
        updateBattle(gd, delta, current, moved);
        current = runner.current();

        if (current != null && current.type == DialogueScript.Type.LINE && current != line) startLine(current);
        if (current != null && current.type == DialogueScript.Type.CHOICE && current != choiceNode) {
            choiceNode = current;
            selectedChoice = 0;
            hoveredChoice = -1;
            choiceKeyboardFocused = false;
            selectionAnimation = -1f;
            choiceTime = 0f;
            pressureT = 0f;
            if (choicePop.length < current.choices.size()) {
                int size = current.choices.size();
                choicePop = Arrays.copyOf(choicePop, size);
                choiceDrawPlayed = Arrays.copyOf(choiceDrawPlayed, size);
                choiceFlipProgress = Arrays.copyOf(choiceFlipProgress, size);
            }
            for (int i = 0; i < choicePop.length; i++) {
                choicePop[i] = 0f;
                choiceDrawPlayed[i] = false;
                choiceFlipProgress[i] = 0f;
            }
            skipMode = false;
            auto = false;
        }
        if (runner.isFinished()) leave();
        boolean fighting = battlePhase != B_NONE;
        boolean choices = choicesOpen();
        boolean chapter = chapterActive();
        if (choices && !confirmQuit && !logOpen) {
            if (moved) choiceKeyboardFocused = false;
            if (!choiceKeyboardFocused) {
                hoveredChoice = pointerAvailable ? choiceAt() : -1;
                if (hoveredChoice >= 0) selectedChoice = hoveredChoice;
            }
        } else {
            hoveredChoice = -1;
            choiceKeyboardFocused = false;
        }

        Texture currentPortrait = portraitFor(line);
        if (currentPortrait != null) portraitTexture = currentPortrait;
        boolean showPortrait = !fighting && currentPortrait != null;
        float textX = showPortrait ? portX + portW + 28f : panelX + 40f;
        float rightLimit = panelX + panelW - 48f;
        float textWidth = Math.max(50f, rightLimit - textX);

        if (rich != null && Math.abs(layoutWidth - textWidth) > 0.5f) {
            rich.layout(dialogueFont, textWidth);
            layoutWidth = textWidth;
        }
        float battleTextW = panelW - 88f;
        if (bRich != null && Math.abs(bLayoutW - battleTextW) > 0.5f) {
            bRich.layout(dialogueFont, battleTextW);
            bLayoutW = battleTextW;
        }

        // Typewriter for dialogue
        if (rich != null && textTime < rich.totalTime && !fighting && !chapter) textTime += gd;
        boolean typing = rich != null && textTime < rich.totalTime;
        if (rich != null && !fighting) {
            int before = revealed;
            boolean playBlip = false;
            while (revealed < rich.chars.size() && rich.chars.get(revealed).revealTime <= textTime) {
                if (!Character.isWhitespace(rich.chars.get(revealed).value) && (revealed & 1) == 0) {
                    playBlip = true;
                }
                revealed++;
            }
            if (playBlip && blip != null && revealed - before <= 3 && gd > 0f) {
                blip.play(0.25f * GameSettings.sfx, 0.92f + MathUtils.random() * 0.16f, 0f);
            }
        }

        // Skip and auto
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean skipping = (ctrl || skipMode) && !fighting;
        if (!leaving && !fighting && gd > 0f && intro >= 0.8f && current != null && current == line
            && current.type == DialogueScript.Type.LINE) {
            if (skipping) {
                if (typing) {
                    textTime = rich.totalTime;
                    revealed = rich.chars.size();
                }
                skipTimer += gd;
                if (skipTimer >= SKIP_INTERVAL) {
                    skipTimer = 0f;
                    runner.next();
                }
            } else if (auto && !typing) {
                autoTimer += gd;
                if (autoTimer >= AUTO_DELAY + line.text.length() * 0.02f) {
                    autoTimer = 0f;
                    runner.next();
                }
            }
        }

        // Animations for the tag, portrait and choices
        boolean tagWanted = !fighting && line != null && !line.speaker.isEmpty();
        tagVisibility = MathUtils.clamp(tagVisibility + (tagWanted ? gd / 0.25f : -gd / 0.2f), 0f, 1f);
        boolean portraitWanted = showPortrait;
        portraitVisibility = MathUtils.clamp(
            portraitVisibility + (portraitWanted ? gd / 0.3f : -gd / 0.2f), 0f, 1f);
        if (choices) {
            choiceTime += gd;
            if (current != null) {
                for (int i = 0; i < current.choices.size(); i++) {
                    if (!choiceDrawPlayed[i] && choiceTime >= i * 0.1f) {
                        playCardSound(cardDrawSfx, 0.65f);
                        choiceDrawPlayed[i] = true;
                    }
                }
            }
            if (!confirmQuit && !logOpen && current != null && current.timeLimit > 0f
                && choiceTime > 0.4f) {
                pressureT += gd;
                if (pressureT >= current.timeLimit) {
                    logSpeaker.add("");
                    logText.add("> (You stayed silent.)");
                    runner.timeout();
                    pressureT = 0f;
                    choiceNode = null;
                    choiceTime = 0f;
                    hoveredChoice = -1;
                    selectedChoice = 0;
                    choiceKeyboardFocused = false;
                    current = runner.current();
                    if (current != null && current.type == DialogueScript.Type.LINE && current != line) {
                        startLine(current);
                    }
                }
            }
        } else {
            choiceTime = 0f;
        }
        float choiceTarget = choiceKeyboardFocused ? selectedChoice : hoveredChoice;
        selectionAnimation += (choiceTarget - selectionAnimation) * Math.min(1f, delta * 14f);
        int popFocus = choiceKeyboardFocused ? selectedChoice : hoveredChoice;
        for (int i = 0; i < choicePop.length; i++) {
            float target = choices && i == popFocus ? 1f : 0f;
            choicePop[i] += (target - choicePop[i]) * Math.min(1f, delta * 12f);
            float flip = MathUtils.clamp(choicePop[i], 0f, 1f);
            float previousFlip = choiceFlipProgress[i];
            if (choices && ((previousFlip < 0.5f && flip >= 0.5f)
                || (previousFlip >= 0.5f && flip < 0.5f))) {
                playCardSound(cardFlipSfx, 0.55f);
            }
            choiceFlipProgress[i] = flip;
        }

        if (leaving) {
            fadeOut = Math.min(1f, fadeOut + delta / 0.2f);
            if (fadeOut >= 1f && !transitioned) {
                transitioned = true;
                game.transitionTo(new MenuScreen(game));
            }
        }

        // drawing
        float boxEase = ease((intro - 0.15f) / 0.6f);
        float boxOffsetY = -(1f - boxEase) * (panelH + 48f);
        float be = ease(battleMix);
        float charAlpha = ease((intro - 0.3f) / 0.7f) * MathUtils.lerp(1f, 0.55f, be);

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        float sceneShake = Math.max(shakeAmount, dialogueShakeTime / SHAKE_TIME)
            * (fighting ? BATTLE_SHAKE_GAIN : 1f);
        float shakeX = 0f, shakeY = 0f;
        if (sceneShake > 0.01f) {
            shakeX = MathUtils.random(-8f, 8f) * sceneShake;
            shakeY = MathUtils.random(-6f, 6f) * sceneShake;
        }
        float offX = 0f, offY = 0f, rot = 0f, scale = 1f, focus = 0f;
        if (fighting && cameraFxBlend > 0.001f) {
            offX += arena.fxX() * cameraFxBlend;
            offY += arena.fxY() * cameraFxBlend;
            rot = arena.fxRot() * cameraFxBlend;
            scale = MathUtils.lerp(1f, arena.fxScale(), cameraFxBlend);
            focus = arena.fxFocus() * cameraFxBlend;
        }
        // 0:10-0:20: hinahatak ng broadcast ang camera sa puso
        float camFocus = Math.max(focus, maydayCinema * 0.62f);
        camera.update();
        viewport.apply(false);
        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(camera.combined);
        float worldScale = 1f, pivotX = 0f, pivotY = 0f;
        if (fighting) {
            float acx = arenaX + arenaS / 2f + arena.arenaShiftX();
            float acy = arenaY + arenaS / 2f + arena.arenaShiftY();
            // box ang umiikot sa sariling axes, kaya kasama ang casing sa fit
            float boxSpin = be >= 0.99f ? arena.spinAngle() : 0f;
            float radians = Math.abs(rot + boxSpin) * MathUtils.degreesToRadians;
            float extent = arenaS / 2f * (Math.abs(MathUtils.cos(radians)) + Math.abs(MathUtils.sin(radians)));
            worldScale = fitWorldScale(scale * MathUtils.lerp(1f, FOCUS_ZOOM, camFocus), rot + boxSpin);
            float baseX = fitWorldCenter(acx + offX, visX, visW, extent, worldScale);
            float baseY = fitWorldCenter(acy + offY, visY, visH, extent, worldScale);
            pivotX = MathUtils.lerp(acx, arena.heartX(), camFocus);
            pivotY = MathUtils.lerp(acy, arena.heartY(), camFocus);
            float viewCx = MathUtils.lerp(baseX, visX + visW / 2f, camFocus) + shakeX;
            float viewCy = MathUtils.lerp(baseY, visY + visH / 2f, camFocus) + shakeY;
            worldTransform.idt()
                .translate(viewCx, viewCy, 0f)
                .rotate(0f, 0f, 1f, rot)
                .scale(worldScale, worldScale, 1f)
                .translate(-pivotX, -pivotY, 0f);
            arenaScreenTop = viewCy + (acy - pivotY) * worldScale + extent * worldScale;
            arenaTransform.set(worldTransform);
            if (boxSpin != 0f) {
                arenaTransform.translate(acx, acy, 0f).rotate(0f, 0f, 1f, boxSpin)
                    .translate(-acx, -acy, 0f);
            }
            arenaScreenTop = viewCy + (acy - pivotY) * worldScale + extent * worldScale;
        }

        batch.begin();
        // full-screen mono, 9 hit lang
        boolean monochromeActive = phaseThree && monoShader != null && monoT > 0f;
        if (monochromeActive) {
            batch.setShader(monoShader);
            float mf = monoLen > 0f ? MathUtils.clamp(monoT / monoLen, 0f, 1f) : 0f;
            monoShader.setUniformf("u_mono", monoStrength * mf);
            monoShader.setUniformf("u_static", mf);
            monoShader.setUniformf("u_seed", fxTime);
        }
        if (fighting) batch.setTransformMatrix(worldTransform);

        // Scene
        float zoom = 1.04f + 0.015f * (float) Math.sin(time * 0.22f);
        batch.setColor(0.74f, 0.7f, 0.66f, 1f);
        drawCover(batch, background, sceneX, sceneY, sceneW, sceneH, zoom,
            (float) Math.sin(time * 0.11f) * 0.0035f, (float) Math.cos(time * 0.09f) * 0.0025f);
        drawFrame(batch, sceneX, sceneY, sceneW, sceneH, 1.5f, 0.82f);
        drawCharacter(batch, charAlpha);
        batch.setTransformMatrix(identityTransform);
        float mindFear = runner.mind("maria") / (float) DialogueRunner.STAT_MAX;
        if (mindFear > 0.35f && !chapter) {
            batch.setColor(0.05f, 0f, 0.02f, (mindFear - 0.35f) * 0.55f);
            batch.draw(pixel, visX, visY, visW, visH);
        }
        if (flash > 0.01f) {
            batch.setColor(1f, 1f, 1f, flash * 0.75f);
            batch.draw(pixel, visX, visY, visW, visH);
        }
        if (hitFlash > 0.01f) {
            batch.setColor(0.8f, 0f, 0f, hitFlash * 0.35f);
            batch.draw(pixel, visX, visY, visW, visH);
        }
        if (choices) {
            batch.setColor(0f, 0f, 0f, 0.4f * ease(choiceTime / 0.25f));
            batch.draw(pixel, visX, visY, visW, visH);
        }
        if (fighting) batch.setTransformMatrix(worldTransform);

        // panel, pumapasok sa battle square
        float pY = panelY + boxOffsetY;
        float bx = MathUtils.lerp(panelX, arenaX, be);
        float bw = MathUtils.lerp(panelW, arenaS, be);
        float bh = MathUtils.lerp(panelH, arenaS, be);
        float by = MathUtils.lerp(pY, arenaY, be);
        // phase 3: humihinga ang pader, sabay ang casing at playfield
        if (fighting && be > 0.99f) {
            float ins = arenaInset();
            bx += ins; by += ins; bw -= ins * 2f; bh -= ins * 2f;
        }
        float boxX = bx + (fighting ? be * arena.arenaShiftX() : 0f);
        float boxY = by + (fighting ? be * arena.arenaShiftY() : 0f);
        float pulse = arena.pulse();
        // 1:43: box pagkatapos ng HUD
        boolean arenaOnTop = fighting && arena.spinFinale();

        if (fighting) {
            float bs = worldScale * 0.9f;
            arena.drawBackdrop(batch, pixel, pivotX - visW / 2f / bs, pivotY - visH / 2f / bs,
                visW / bs, visH / bs, battleUi);
        }

        // umiikot ang box, hindi ang bullets
        if (!arenaOnTop) {
            if (fighting) batch.setTransformMatrix(arenaTransform);
            drawBattleBox(batch, be, boxEase, boxX, boxY, bw, bh, pulse);
            if (fighting) batch.setTransformMatrix(worldTransform);
        }

        // Battle contents
        if (fighting) {
            if (!arenaOnTop
                && (battlePhase == B_DODGE || battlePhase == B_DEAD
                    || battlePhase == B_P2_INTRO || battlePhase == B_P3_INTRO)) {
                arena.draw(batch, pixel, 1f);
            }
            batch.setTransformMatrix(identityTransform);
            float textA = battleUi * (1f - be);
            if (bRich != null && textA > 0.01f && battlePhase != B_DODGE && battlePhase != B_DEAD) {
                drawRichText(batch, bRich, bTextT, panelX + 44f, pY + panelH - 34f,
                    dialogueFont.getLineHeight(), textA);
            }
            if (battlePhase == B_MENU) drawBattleMenu(batch, battleUi);
        }

        // fx: sya na ang mag-fade
        if (fighting) arena.drawOverlay(batch, pixel, visX, visY, visW, visH, worldTransform);
        if (fighting) {
            batch.setTransformMatrix(identityTransform);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            drawFinaleVignette(batch);
            drawImpactFrames(batch);
            drawLetterbox(batch);
            drawMaydayCinema(batch);
            drawSeqTitle(batch);
            batch.setTransformMatrix(worldTransform);
            drawBattleHud(batch,
                battlePhase == B_P2_INTRO || battlePhase == B_P3_INTRO ? 0f : battleUi);
            batch.setTransformMatrix(identityTransform);
        }
        // text ng broadcast sa taas
        if (phaseThree && (battlePhase == B_DODGE || battlePhase == B_P3_INTRO))
            drawPhaseThreeSignal(batch);

        // finale box, ibabaw ng lahat
        if (arenaOnTop) {
            batch.setTransformMatrix(arenaTransform);
            drawBattleBox(batch, be, boxEase, boxX, boxY, bw, bh, pulse);
            batch.setTransformMatrix(worldTransform);
            arena.draw(batch, pixel, 1f);
            batch.setTransformMatrix(identityTransform);
        }

        // Portrait, name plate, dialogue text
        if (!fighting) {
            drawPortrait(batch, boxOffsetY);
            drawNameTag(batch, pY, boxEase, showPortrait ? portX + portW + 28f : panelX + 40f);
            drawStateMeters(batch, boxEase * (chapter ? 0f : 1f));
            if (rich != null && line != null) {
                drawRichText(batch, rich, textTime, textX, pY + panelH - 62f,
                    dialogueFont.getLineHeight(), boxEase);
                if (!typing && !leaving && intro >= 0.9f && !choices
                    && (current == null || current.type == DialogueScript.Type.LINE)) {
                    float bob = (float) Math.sin(time * 5f) * 3f;
                    drawArrow(batch, panelX + panelW - 40f, pY + 24f + bob);
                }
            }
        }

        if (quickVisible()) drawQuickMenu(batch, skipping);

        if (choices && current != null && current.type == DialogueScript.Type.CHOICE) {
            drawChoiceCards(batch, current);
            if (current.timeLimit > 0f) drawPressure(batch, current);
        }

        if (battlePhase == B_DEAD) drawGameOver(batch);
        if (battlePhase == B_P2_INTRO) drawPhaseTwoIntro(batch);
        else if (battlePhase == B_P3_INTRO) drawPhaseThreeIntro(batch);

        batch.setColor(1f, 1f, 1f, 0.32f);
        batch.draw(grain, visX, visY, visW, visH,
            grainU, grainV, grainU + visW / GRAIN_SIZE, grainV + visH / GRAIN_SIZE);
        batch.setColor(Color.WHITE);
        batch.draw(vignette, visX, visY, visW, visH);

        if (chapter) drawChapter(batch);
        if (logOpen) drawLog(batch);
        if (confirmQuit) drawConfirm(batch);

        float black = Math.max(1f - MathUtils.clamp(intro / 0.5f, 0f, 1f), fadeOut);
        if (black > 0.01f) {
            batch.setColor(0f, 0f, 0f, black);
            batch.draw(pixel, visX, visY, visW, visH);
        }
        batch.setColor(Color.WHITE);
        if (monochromeActive) batch.setShader(null);
        batch.end();
        resetCamera();
    }

    private void resetCamera() {
        camera.up.set(0f, 1f, 0f);
        camera.direction.set(0f, 0f, -1f);
        camera.zoom = 1f;
        camera.position.set(Main.WORLD_WIDTH / 2f, Main.WORLD_HEIGHT / 2f, 0f);
        camera.update();
    }

    // drawing pieces

    private void drawSeqTitle(SpriteBatch batch) {
        if (seqTitleT <= 0f || seqTitle.isEmpty() || battlePhase == B_DEAD) return;
        float in = ease((SEQ_TITLE_TIME - seqTitleT) / 0.35f);
        float out = MathUtils.clamp(seqTitleT / 0.6f, 0f, 1f);
        float a = Math.min(in, out) * battleUi;
        if (a <= 0.01f) return;
        glyph.setText(nameFont, seqTitle);
        float x = clampHudX(visX + visW / 2f - glyph.width / 2f, glyph.width);
        float y = clampHudY(arenaScreenTop + 16f + (1f - in) * 12f, nameFont.getLineHeight());
        nameFont.setColor(TEXT.r, TEXT.g, TEXT.b, a);
        nameFont.draw(batch, seqTitle, x, y);
        batch.setColor(0.78f, 0.09f, 0.14f, 0.8f * a);
        batch.draw(pixel, x, y - 8f - nameFont.getCapHeight(), glyph.width * in, 1.5f);
        nameFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private void drawRichText(SpriteBatch batch, RichText r, float t, float x0, float y0,
                              float lineH, float alpha) {
        List<RichText.Ch> cs = r.chars;
        for (int i = 0; i < cs.size(); i++) {
            RichText.Ch ch = cs.get(i);
            float age = t - ch.revealTime;
            if (age < 0f) break;
            if (Character.isWhitespace(ch.value)) continue;

            float pop = Math.min(1f, age / 0.14f);
            float x = x0 + ch.x;
            float y = y0 - ch.line * lineH + (1f - pop) * 5f;
            if (ch.shake) {
                x += (float) Math.sin(time * 60f + i * 12.9f) * 1.3f;
                y += (float) Math.cos(time * 53f + i * 7.1f) * 1.3f;
            }
            if (ch.wave) y += (float) Math.sin(time * 7f + ch.x * 0.05f) * 2.6f;
            dialogueFont.setColor(ch.color.r, ch.color.g, ch.color.b, pop * alpha);
            dialogueFont.draw(batch, str(ch.value), x, y);
        }
        dialogueFont.setColor(TEXT);
    }

    private void drawPortrait(SpriteBatch batch, float offY) {
        float v = ease(portraitVisibility);
        if (portraitTexture == null || v <= 0.01f) return;
        float s = 0.92f + 0.08f * v;
        float w = portW * s, h = portH * s;
        float x = portX + (portW - w) / 2f;
        float y = portY + (portH - h) / 2f + offY;
        float inset = 5f;
        batch.setColor(0f, 0f, 0f, v);
        batch.draw(pixel, x - 5f, y - 5f, w + 10f, h + 10f);
        float fear = runner.mind("maria") / (float) DialogueRunner.STAT_MAX;
        batch.setColor(1f, 1f - fear * 0.28f, 1f - fear * 0.4f, v);
        drawFit(batch, portraitTexture, x + inset, y + inset, w - inset * 2f, h - inset * 2f);
        drawFrame(batch, x - 5f, y - 5f, w + 10f, h + 10f, 1.5f, 0.9f * v);
        drawFrame(batch, x, y, w, h, 1f, 0.45f * v);
    }

    private void drawNameTag(SpriteBatch batch, float pY, float boxEase, float x0) {
        float v = ease(tagVisibility) * boxEase;
        if (v <= 0.01f || tagText.isEmpty()) return;
        glyph.setText(nameFont, tagText);
        float w = Math.max(90f, glyph.width + 44f);
        float h = 36f;
        float x = x0 - (1f - v) * 16f;
        float y = pY + panelH - h / 2f;
        batch.setColor(0.02f, 0.02f, 0.03f, 0.97f * v);
        batch.draw(pixel, x, y, w, h);
        drawFrame(batch, x, y, w, h, 1.5f, 0.9f * v);
        drawDiamond(batch, x + 10f, y + h / 2f, 6f, 0.85f * v);
        drawDiamond(batch, x + w - 10f, y + h / 2f, 6f, 0.85f * v);
        nameFont.setColor(TEXT.r, TEXT.g, TEXT.b, v);
        nameFont.draw(batch, tagText, x, y + h / 2f + nameFont.getCapHeight() / 2f, w, Align.center, false);
        if (line != null && "MARIA".equalsIgnoreCase(line.speaker)) {
            String state = DialogueRunner.mindLabel(runner.mind("maria"));
            uiFont.setColor(0.9f, 0.55f + (1f - runner.mind("maria") / 10f) * 0.35f, 0.4f, v * 0.9f);
            uiFont.draw(batch, state, x, y - 6f, w, Align.center, false);
            uiFont.setColor(TEXT);
        }
        nameFont.setColor(TEXT);
    }

    private void drawStateMeters(SpriteBatch batch, float alpha) {
        if (alpha <= 0.01f) return;
        float x = sceneX + 14f;
        float y = sceneY + sceneH - 64f;
        drawMeter(batch, x, y + 28f, "TRUST", runner.trust("maria"),
            DialogueRunner.trustLabel(runner.trust("maria")),
            0.72f, 0.18f, 0.16f, alpha);
        drawMeter(batch, x, y, "STATE", runner.mind("maria"),
            DialogueRunner.mindLabel(runner.mind("maria")),
            0.55f, 0.12f, 0.14f, alpha);
    }

    private void drawMeter(SpriteBatch batch, float x, float y, String name, int value,
                           String label, float r, float g, float b, float alpha) {
        float w = 168f, h = 22f;
        batch.setColor(0f, 0f, 0f, 0.62f * alpha);
        batch.draw(pixel, x, y, w, h);
        drawFrame(batch, x, y, w, h, 1f, 0.55f * alpha);
        float fill = w * value / (float) DialogueRunner.STAT_MAX;
        batch.setColor(r, g, b, 0.85f * alpha);
        if (fill > 0f) batch.draw(pixel, x, y, fill, h);
        uiFont.setColor(TEXT.r, TEXT.g, TEXT.b, alpha);
        uiFont.draw(batch, name + "  " + label, x + 8f, y + h / 2f + uiFont.getCapHeight() / 2f);
        uiFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private void drawPressure(SpriteBatch batch, DialogueScript.Node node) {
        float limit = node.timeLimit;
        if (limit <= 0f) return;
        float shown = MathUtils.clamp(pressureT / limit, 0f, 1f);
        float remain = 1f - shown;
        float w = Math.min(sceneW * 0.42f, 340f);
        float h = 10f;
        float x = visX + (visW - w) / 2f;
        float y = visY + visH - 34f;
        float urgent = remain < 0.34f ? 1f : 0.45f;
        float pulse = remain < 0.34f ? 0.65f + 0.35f * Math.abs((float) Math.sin(time * 14f)) : 1f;
        batch.setColor(0f, 0f, 0f, 0.72f);
        batch.draw(pixel, x - 4f, y - 18f, w + 8f, h + 28f);
        batch.setColor(0.2f, 0.05f, 0.05f, 0.9f);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(1f, 0.18f + remain * 0.4f, 0.12f, pulse);
        batch.draw(pixel, x, y, w * remain, h);
        drawFrame(batch, x, y, w, h, 1f, 0.8f);
        uiFont.setColor(1f, 0.82f, 0.72f, urgent);
        uiFont.draw(batch, remain < 0.34f ? "NOW" : "DECIDE", x, y + h + 14f, w, Align.center, false);
        uiFont.setColor(TEXT);
    }

    private void drawArrow(SpriteBatch batch, float x, float y) {
        batch.setColor(TEXT.r, TEXT.g, TEXT.b, 0.9f);
        for (int i = 0; i < 5; i++) batch.draw(pixel, x + i, y + (4 - i) * 2, 10 - 2 * i, 2f);
    }

    private void drawQuickMenu(SpriteBatch batch, boolean skipping) {
        for (int i = 0; i < QUICK_LABELS.length; i++) {
            float x = quickButtonX(i);
            float w = quickButtonWidth(i);
            boolean on = (i == 1 && auto) || (i == 2 && (skipMode || skipping));
            boolean hover = hit(x, qmY, w, QM_H);
            float thickness = on || hover ? 1.75f : 1.25f;
            batch.setColor(Color.BLACK);
            batch.draw(pixel, x, qmY, w, QM_H);
            drawWhiteFrame(batch, x, qmY, w, QM_H, thickness);
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, QUICK_LABELS[i], x, qmY + QM_H / 2f + uiFont.getCapHeight() / 2f,
                w, Align.center, false);
        }
        uiFont.setColor(TEXT);
    }

    private void drawChoiceCards(SpriteBatch batch, DialogueScript.Node node) {
        int n = node.choices.size();
        int top = choiceKeyboardFocused ? selectedChoice : hoveredChoice;
        for (int i = 0; i < n; i++) {
            if (i != top) drawChoiceCard(batch, node, i);
        }
        if (top >= 0) drawChoiceCard(batch, node, top);
        batch.setColor(Color.WHITE);
    }

    private void sub(SpriteBatch batch, float x, float y, float w, float h, float rotation,
                     float rx, float ry, float rw, float rh) {
        batch.draw(pixelRegion, x + rx, y + ry, w / 2f - rx, h / 2f - ry,
            rw, rh, 1f, 1f, rotation);
    }

    private void subFrame(SpriteBatch batch, float x, float y, float w, float h,
                          float rotation, float inset, float thickness) {
        sub(batch, x, y, w, h, rotation, inset, inset, w - 2f * inset, thickness);
        sub(batch, x, y, w, h, rotation, inset, h - inset - thickness, w - 2f * inset, thickness);
        sub(batch, x, y, w, h, rotation, inset, inset, thickness, h - 2f * inset);
        sub(batch, x, y, w, h, rotation, w - inset - thickness, inset, thickness, h - 2f * inset);
    }

    private void drawChoiceCard(SpriteBatch batch, DialogueScript.Node node, int i) {
        int n = node.choices.size();
        if (i < 0 || i >= n) return;
        float local = ease((choiceTime - i * 0.1f) / 0.3f);
        if (local <= 0.01f) return;
        float pop = choicePop[i];

        cardGeo(i, n);
        float w = choiceGeo[2], h = choiceGeo[3], rotation = choiceGeo[4];
        float cx = choiceGeo[0];
        float cy = choiceGeo[1] - (1f - local) * 90f;
        float x = cx - w / 2f, y = cy - h / 2f;

        float flip = MathUtils.clamp(pop, 0f, 1f);
        float flipScale = Math.max(0.04f, Math.abs(MathUtils.cos(flip * MathUtils.PI)));
        float revealProgress = MathUtils.clamp((flip - 0.72f) / 0.25f, 0f, 1f);
        float reveal = revealProgress * revealProgress * (3f - 2f * revealProgress);
        boolean showingFace = flip >= 0.5f;

        batch.setColor(0f, 0f, 0f, 0.7f * local);
        sub(batch, x, y, w, h, rotation, 8f, -8f, w, h);

        Texture art = showingFace ? cardTex.get(CHOICE_CARD_RANKS[i % CHOICE_CARD_RANKS.length]) : choiceCardBack;
        if (art != null) {
            float tint = showingFace ? MathUtils.lerp(0.5f, 1f, reveal) : 1f;
            batch.setColor(tint, tint, tint, local);
            batch.draw(art, x, y, w / 2f, h / 2f, w, h, flipScale, 1f, rotation,
                0, 0, art.getWidth(), art.getHeight(), false, false);
        }

        if (showingFace && reveal > 0.01f) {
            float bannerH = h * 0.54f;
            batch.setColor(0f, 0f, 0f, 0.9f * reveal * local);
            sub(batch, x, y, w, h, rotation, 1f, 1f, w - 2f, bannerH);

            tempColor.set(CHOICE_ON);
            tempColor.a = reveal * local;
            float textWidth = w - 24f;
            float textHeight = bannerH - 24f;
            float scaleX = choiceFont.getData().scaleX;
            float scaleY = choiceFont.getData().scaleY;
            glyph.setText(choiceFont, node.choices.get(i).text, tempColor, textWidth, Align.center, true);
            if (glyph.height > textHeight) {
                float fitScale = textHeight / glyph.height;
                choiceFont.getData().setScale(scaleX * fitScale, scaleY * fitScale);
                glyph.setText(choiceFont, node.choices.get(i).text, tempColor, textWidth, Align.center, true);
            }
            choiceFont.draw(batch, glyph, x + 12f, y + (bannerH + glyph.height) / 2f);
            choiceFont.getData().setScale(scaleX, scaleY);
            uiFont.setColor(TEXT);
            choiceFont.setColor(CHOICE_ON);
        }

        if (pop > 0.6f) {
            float iw = 48f;
            float ih = iw * choiceIndicator.getHeight() / choiceIndicator.getWidth();
            float bob = (float) Math.sin(time * 5f) * 8f;
            batch.setColor(1f, 1f, 1f, pop * local);
            batch.draw(choiceIndicator, cx - iw / 2f, y + h + 8f + bob, iw, ih);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBattleMenu(SpriteBatch batch, float alpha) {
        int optionCount = battleOptionCount();
        for (int i = 0; i < optionCount; i++) {
            float cx = optionCenterX(i);
            boolean sel = i == menuSel;
            String label = "[ " + MENU_LABELS[i] + " ]";
            glyph.setText(nameFont, label);
            nameFont.setColor(sel ? 0.93f : 0.55f, sel ? 0.90f : 0.53f, sel ? 0.82f : 0.50f, alpha);
            nameFont.draw(batch, label, cx - glyph.width / 2f, panelY + 50f);
            if (sel) {
                BattleArena.drawHeart(batch, pixel, cx - glyph.width / 2f - 24f, panelY + 41f, alpha);
            }
        }
        nameFont.setColor(TEXT);
        String prompt = "CHOOSE IN " + Math.max(0, MathUtils.ceil(pickSeconds - phaseT));
        glyph.setText(uiFont, prompt);
        uiFont.setColor(TEXT.r, TEXT.g, TEXT.b, alpha * 0.85f);
        uiFont.draw(batch, prompt, panelX + (panelW - glyph.width) / 2f, panelY + 91f);
        float barX = panelX + panelW * 0.2f;
        float barY = panelY + 74f;
        float barW = panelW * 0.6f;
        float barH = 4f;
        float left = 1f - MathUtils.clamp(phaseT / pickSeconds, 0f, 1f);
        float urgent = left < 0.35f ? 0.7f + 0.3f * Math.abs((float) Math.sin(time * 16f)) : 1f;
        batch.setColor(0.12f, 0.02f, 0.04f, alpha);
        batch.draw(pixel, barX, barY, barW, barH);
        batch.setColor(0.78f * urgent, 0.09f, 0.14f, alpha);
        batch.draw(pixel, barX, barY, barW * left, barH);
        uiFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private float clampHudX(float x, float width) {
        float min = visX + 12f;
        float max = visX + visW - 12f - width;
        return max < min ? visX + (visW - width) / 2f : MathUtils.clamp(x, min, max);
    }

    private float clampHudY(float y, float height) {
        float min = visY + 8f;
        float max = visY + visH - 8f - height;
        return max < min ? visY + (visH - height) / 2f : MathUtils.clamp(y, min, max);
    }

    // battle square: black field, casing, decorations
    // px na kinakain ng squeeze kada gilid
    private float arenaInset() {
        return arenaS * 0.26f * arena.squeeze();
    }

    // itutok ang arena sa playfield, kasama ang squeeze
    private void syncArena() {
        float ins = arenaInset();
        arena.setArena(arenaX + 4f + ins, arenaY + 4f + ins,
            arenaS - 8f - 2f * ins, arenaS - 8f - 2f * ins);
    }

    private void drawBattleBox(SpriteBatch batch, float be, float boxEase,
                               float boxX, float boxY, float bw, float bh, float pulse) {
        batch.setColor(Color.BLACK);
        batch.draw(pixel, boxX, boxY, bw, bh);
        drawFrame(batch, boxX, boxY, bw, bh, 1.5f + be, (0.75f + 0.25f * pulse * be) * boxEase);

        float deco = boxEase * (1f - be);
        if (deco > 0.01f) {
            drawFrame(batch, boxX + 6f, boxY + 6f, bw - 12f, bh - 12f, 1f, 0.28f * deco);
            drawDiamond(batch, boxX, boxY, 9f, 0.9f * deco);
            drawDiamond(batch, boxX + bw, boxY, 9f, 0.9f * deco);
            drawDiamond(batch, boxX, boxY + bh, 9f, 0.9f * deco);
            drawDiamond(batch, boxX + bw, boxY + bh, 9f, 0.9f * deco);
            float lineW = Math.min(bw * 0.52f, 560f);
            float lineH = lineW * topLineRegion.getRegionHeight() / topLineRegion.getRegionWidth();
            batch.setColor(1f, 1f, 1f, 0.85f * deco);
            float lineY = boxY + bh - lineH * (6f / 47f) - 0.75f;
            batch.draw(topLineRegion, boxX + (bw - lineW) / 2f, lineY, lineW, lineH);
        }
    }

    private void drawBattleHud(SpriteBatch batch, float a) {
        if (a <= 0.01f) return;
        float cx = visX + visW / 2f;
        float top = clampHudY(visY + visH - 26f, nameFont.getLineHeight());

        String boss = "[ " + ENEMY_NAME.toUpperCase(Locale.ROOT) + " ]";
        glyph.setText(nameFont, boss);
        nameFont.setColor(TEXT.r, TEXT.g, TEXT.b, a);
        nameFont.draw(batch, boss, clampHudX(cx - glyph.width / 2f, glyph.width), top);
        float bw = Math.min(260f, Math.max(1f, visW - 24f)), bh = 10f;
        float bx = clampHudX(cx - bw / 2f, bw), by = clampHudY(top - 34f, bh);
        batch.setColor(0.08f, 0.02f, 0.03f, a);
        batch.draw(pixel, bx, by, bw, bh);
        batch.setColor(0.62f, 0.07f, 0.11f, a);
        batch.draw(pixel, bx, by, bw * MathUtils.clamp(enemyHp / (float) enemyMax(), 0f, 1f), bh);
        drawFrame(batch, bx, by, bw, bh, 1f, 0.7f * a);

        String phaseName = phaseThree ? "PHASE 3 - SIGAW   "
            : phaseTwo ? "PHASE 2 - DILIM   " : "PHASE 1 - LIYAB   ";
        String phaseLabel = phaseName + (curSeq + 1) + " / " + arena.sequenceCount();
        glyph.setText(uiFont, phaseLabel);
        uiFont.setColor(TEXT.r, TEXT.g, TEXT.b, a * 0.8f);
        uiFont.draw(batch, phaseLabel, clampHudX(cx - glyph.width / 2f, glyph.width),
            clampHudY(by - 12f, uiFont.getLineHeight()));
        float trustY = clampHudY(by - 44f, 18f);
        for (int i = 0; i < TRUST_NEEDED; i++) {
            float px = cx - (TRUST_NEEDED - 1) * 9f + i * 18f;
            drawDiamond(batch, MathUtils.clamp(px, visX + 12f, visX + visW - 12f), trustY,
                9f, a * (i < trust ? 1f : 0.25f));
        }

        // panel-relative pa rin, caller ang bahala sa transform
        float hx = clampHudX(panelX + 10f, 260f);
        float hy = clampHudY(panelY + panelH + 34f, nameFont.getLineHeight() + 16f);
        float hpBarY = hy - 16f;
        nameFont.setColor(TEXT.r, TEXT.g, TEXT.b, a);
        nameFont.draw(batch, "HP", hx, hy);
        batch.setColor(0.12f, 0.02f, 0.04f, a);
        batch.draw(pixel, hx + 40f, hpBarY, 120f, 14f);
        batch.setColor(0.86f, 0.83f, 0.74f, a);
        batch.draw(pixel, hx + 40f, hpBarY,
            120f * MathUtils.clamp(arena.hp() / (float) BattleArena.MAX_HP, 0f, 1f), 14f);
        drawFrame(batch, hx + 40f, hpBarY, 120f, 14f, 1f, 0.7f * a);
        nameFont.draw(batch, arena.hp() + " / " + BattleArena.MAX_HP, hx + 172f, hy);
        nameFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    // card lang, isang tibok: PHASE 2 DESCENT
    private void drawPhaseTwoIntro(SpriteBatch batch) {
        float t = phaseT;
        float a = Math.min(ease(t / 0.35f), ease((P2_INTRO_TIME - t) / 0.6f));
        if (a <= 0.01f) return;
        batch.setColor(0f, 0f, 0f, 0.45f * a);
        batch.draw(pixel, visX, visY, visW, visH);
        String title = "PHASE 2  -  " + arena.sequenceName(0);
        glyph.setText(titleFont, title);
        titleFont.setColor(TEXT.r, TEXT.g, TEXT.b, a);
        titleFont.draw(batch, title, visX + visW / 2f - glyph.width / 2f,
            visY + visH / 2f + titleFont.getCapHeight() / 2f);
        titleFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private void drawPhaseThreeIntro(SpriteBatch batch) {
        float t = phaseT;
        float a = Math.min(ease(t / 0.35f), ease((P3_INTRO_TIME - t) / 0.6f));
        if (a <= 0.01f) return;
        batch.setColor(0.015f, 0.005f, 0.025f, 0.5f * a);
        batch.draw(pixel, visX, visY, visW, visH);
        String title = "PHASE 3  -  " + arena.sequenceName(0);
        glyph.setText(titleFont, title);
        titleFont.setColor(0.92f, 0.80f, 1f, a);
        titleFont.draw(batch, title, visX + visW / 2f - glyph.width / 2f,
            visY + visH / 2f + titleFont.getCapHeight() / 2f);
        titleFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private static final float IMPACT_FRAME_LEN = 0.1f;

    // 2-3 frames na puti/itim, gaya ng manga impact
    private void triggerImpactFrame(float strength) {
        impactT = IMPACT_FRAME_LEN;
        impactStrength = Math.max(impactStrength, strength);
    }

    private void drawImpactFrames(SpriteBatch batch) {
        if (impactT <= 0f) return;
        int frame = (int) ((IMPACT_FRAME_LEN - impactT) * 34f);
        boolean white = (frame & 1) == 0;
        float a = MathUtils.clamp(impactT / IMPACT_FRAME_LEN, 0f, 1f) * (0.45f + 0.5f * impactStrength);
        batch.setColor(white ? 1f : 0f, white ? 1f : 0f, white ? 1f : 0f, a);
        batch.draw(pixel, visX, visY, visW, visH);
        if (white) {
            // punit na slabs para mukhang printed
            batch.setColor(0f, 0f, 0f, a * 0.55f);
            for (int i = 0; i < 3; i++) {
                batch.draw(pixel, visX, visY + visH * (0.18f + 0.3f * i), visW, visH * 0.02f);
            }
        }
        batch.setColor(Color.WHITE);
        if (impactT <= 0f) impactStrength = 0f;
    }

    // 1:43: namumula ang gilid
    private void drawFinaleVignette(SpriteBatch batch) {
        if (!arena.spinFinale()) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(fxTime * 7.3f);
        float a = (0.18f + 0.2f * pulse) * (0.6f + 0.8f * arena.finaleProgress());
        int steps = 18;
        float step = visW * 0.17f / steps;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps;
            float alpha = a * (1f - t) * (1f - t);
            batch.setColor(0.76f, 0.075f, 0.012f, alpha);
            batch.draw(pixel, visX + i * step, visY, step, visH);
            batch.draw(pixel, visX + visW - (i + 1) * step, visY, step, visH);
        }
        batch.setColor(Color.WHITE);
    }

    // letterbox: pasok agad, slam kada kick
    private void drawLetterbox(SpriteBatch batch) {
        if (finaleBars <= 0.02f) return;
        float prog = arena.finaleProgress();
        float breathe = 0.5f + 0.5f * MathUtils.sin(time * (2.2f + 2.5f * prog));
        float amount = 0.05f + 0.03f * prog
            + 0.02f * breathe
            + (0.035f + 0.025f * prog) * arena.thump();
        float h = visH * amount * finaleBars;
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(pixel, visX, visY, visW, h);
        batch.draw(pixel, visX, visY + visH - h, visW, h);
        batch.setColor(0.98f, 0.28f, 0.025f, (0.3f + 0.5f * arena.thump()) * finaleBars);
        batch.draw(pixel, visX, visY + h, visW, 1.5f);
        batch.draw(pixel, visX, visY + visH - h - 1.5f, visW, 1.5f);
        batch.setColor(Color.WHITE);
    }

    // 0:10-0:20: cinematic bars, pataas at pababa
    private void drawMaydayCinema(SpriteBatch batch) {
        if (maydayCinema <= 0.01f) return;
        float h = visH * 0.115f * maydayCinema;
        batch.setColor(0f, 0f, 0f, 0.97f);
        batch.draw(pixel, visX, visY, visW, h);
        batch.draw(pixel, visX, visY + visH - h, visW, h);
        float line = 0.45f + 0.35f * (float) Math.sin(fxTime * 19f);
        batch.setColor(0.86f, 0.92f, 1f, line * maydayCinema);
        batch.draw(pixel, visX, visY + h, visW, 1.5f);
        batch.draw(pixel, visX, visY + visH - h - 1.5f, visW, 1.5f);
        batch.setColor(Color.WHITE);
    }

    private void drawGameOver(SpriteBatch batch) {
        float a = ease(phaseT / 0.6f);
        batch.setColor(0f, 0f, 0f, 0.65f * a);
        batch.draw(pixel, visX, visY, visW, visH);
        glyph.setText(titleFont, "ARAY KO");
        titleFont.setColor(0.85f, 0.2f, 0.2f, a);
        titleFont.draw(batch, "ARAY KO", visX + visW / 2f - glyph.width / 2f, visY + visH * 0.62f);
        titleFont.setColor(TEXT);
        if (phaseT > 0.8f) {
            String s = "R - SUBUKAN MULI        M - MENU";
            glyph.setText(nameFont, s);
            nameFont.setColor(TEXT.r, TEXT.g, TEXT.b, 0.6f + 0.4f * (float) Math.sin(time * 4f));
            nameFont.draw(batch, s, visX + visW / 2f - glyph.width / 2f, visY + visH * 0.5f);
            nameFont.setColor(TEXT);
        }
    }

    private void drawChapter(SpriteBatch batch) {
        float a = chapterAlpha();
        if (a <= 0.01f) return;
        batch.setColor(0f, 0f, 0f, 0.97f * a);
        batch.draw(pixel, visX, visY, visW, visH);

        float cx = visX + visW * 0.34f, cy = visY + visH / 2f;
        float ch = visH * 0.56f;
        Texture art = cardTex.get(chapterCard);
        float cw = art != null ? ch * art.getWidth() / art.getHeight() : ch * 0.6f;
        float s = 0.94f + 0.06f * ease(chapterT / 1.2f);
        float w = cw * s, h = ch * s;
        if (art != null) {
            batch.setColor(1f, 1f, 1f, a);
            batch.draw(art, cx - w / 2f, cy - h / 2f, w, h);
        }
        float tx = cx + cw / 2f + 70f;
        uiFont.setColor(TEXT.r, TEXT.g, TEXT.b, 0.7f * a);
        uiFont.draw(batch, chapterCard, tx, cy + 70f);
        batch.setColor(TEXT.r, TEXT.g, TEXT.b, 0.8f * a);
        batch.draw(pixel, tx, cy + 40f, 260f * ease(chapterT / 1.0f), 2f);
        tempColor.set(TEXT.r, TEXT.g, TEXT.b, a);
        glyph.setText(titleFont, chapterTitle, tempColor, Math.max(100f, visX + visW - tx - 60f), Align.left, true);
        titleFont.draw(batch, glyph, tx, cy + 22f);
        uiFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private void drawLog(SpriteBatch batch) {
        batch.setColor(0f, 0f, 0f, 0.9f);
        batch.draw(pixel, visX, visY, visW, visH);

        glyph.setText(titleFont, "TALA");
        titleFont.draw(batch, "TALA", visX + visW / 2f - glyph.width / 2f, visY + visH - 40f);
        batch.setColor(TEXT.r, TEXT.g, TEXT.b, 0.7f);
        batch.draw(pixel, panelX, visY + visH - 100f, panelW, 1.5f);
        uiFont.setColor(TEXT.r, TEXT.g, TEXT.b, 0.6f);
        uiFont.draw(batch, "SCROLL / W S - ITAAS AT IBABA        ESC - ISARA", panelX, visY + 40f);

        float top = visY + visH - 116f;
        float cursor = visY + 72f;
        float textW = panelW - 190f;
        for (int idx = logText.size() - 1 - logScroll; idx >= 0; idx--) {
            String speaker = logSpeaker.get(idx);
            String text = logText.get(idx);
            glyph.setText(dialogueFont, text, TEXT, textW, Align.left, true);
            float h = Math.max(glyph.height, 20f) + 16f;
            if (cursor + h > top) break;
            float topY = cursor + h - 8f;
            if (!speaker.isEmpty()) {
                uiFont.setColor(CHOICE_ON.r, CHOICE_ON.g, CHOICE_ON.b, 0.9f);
                uiFont.draw(batch, speaker, panelX, topY);
            }
            dialogueFont.setColor(speaker.isEmpty() ? NARRATION : TEXT);
            dialogueFont.draw(batch, glyph, panelX + 170f, topY);
            cursor += h;
        }
        uiFont.setColor(TEXT);
        dialogueFont.setColor(TEXT);
        titleFont.setColor(TEXT);
        batch.setColor(Color.WHITE);
    }

    private void drawConfirm(SpriteBatch batch) {
        batch.setColor(0f, 0f, 0f, 0.75f);
        batch.draw(pixel, visX, visY, visW, visH);

        float w = 520f, h = 190f;
        float x = visX + visW / 2f - w / 2f, y = visY + visH / 2f - h / 2f - 4f;
        batch.setColor(0.02f, 0.02f, 0.03f, 0.97f);
        batch.draw(pixel, x, y, w, h);
        drawFrame(batch, x, y, w, h, 1.5f, 0.9f);
        drawFrame(batch, x + 6f, y + 6f, w - 12f, h - 12f, 1f, 0.3f);

        String q = "RETURN TO MENU?";
        glyph.setText(nameFont, q);
        nameFont.draw(batch, q, visX + visW / 2f - glyph.width / 2f, y + h - 34f);

        boolean yesHover = hit(cqYesX, cqY, CQ_W, CQ_H);
        boolean noHover = hit(cqNoX, cqY, CQ_W, CQ_H);
        drawButton(batch, cqYesX, "YES", yesHover);
        drawButton(batch, cqNoX, "NO", noHover);
    }

    private void drawButton(SpriteBatch batch, float x, String label, boolean hover) {
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(pixel, x, cqY, CQ_W, CQ_H);
        drawFrame(batch, x, cqY, CQ_W, CQ_H, 1.25f, hover ? 1f : 0.5f);
        nameFont.setColor(TEXT.r, TEXT.g, TEXT.b, hover ? 1f : 0.7f);
        nameFont.draw(batch, label, x, cqY + CQ_H / 2f + nameFont.getCapHeight() / 2f, CQ_W, Align.center, false);
        nameFont.setColor(TEXT);
    }

    private void drawCharacter(SpriteBatch batch, float alpha) {
        Texture cur = sprites.get(shownExpression);
        if (cur == null) cur = sprites.get("normal");
        if (cur == null) return;

        float breath = 1f + (float) Math.sin(time * 1.7f) * 0.006f;
        float hop = talkHop > 0f ? (float) Math.sin((1f - talkHop) * Math.PI) * 5f : 0f;
        float rise = (1f - ease((intro - 0.3f) / 0.7f)) * 30f;
        float fear = runner.mind("maria") / (float) DialogueRunner.STAT_MAX;
        float shake = characterShakeTime > 0f
            ? (float) Math.sin((SHAKE_TIME - characterShakeTime) * 55f) * 8f * characterShakeTime / SHAKE_TIME
            : 0f;
        if (fear > 0.45f) {
            shake += (float) Math.sin(time * (9f + fear * 10f)) * (fear - 0.45f) * 7f;
        }
        float sway = (float) Math.sin(time * 0.9f) * 2f;
        float cx = sceneX + sceneW / 2f + shake + sway;
        float baseY = panelY + panelH * 0.9f + hop - rise;

        if (previousSprite != null && crossFade < 1f) {
            drawSprite(batch, previousSprite, cx, baseY, breath, alpha * (1f - crossFade), fear);
        }
        drawSprite(batch, cur, cx, baseY, breath, alpha * (previousSprite != null ? crossFade : 1f), fear);
    }

    private void drawSprite(SpriteBatch batch, Texture t, float cx, float y, float breath, float alpha,
                            float fear) {
        float h = Math.min(visH * 0.75f, visY + visH - y - 10f);
        float w = h * t.getWidth() / t.getHeight();
        float maxW = sceneW * 0.5f;
        if (w > maxW) {
            w = maxW;
            h = w * t.getHeight() / t.getWidth();
        }
        batch.setColor(1f, 1f - fear * 0.22f, 1f - fear * 0.38f, alpha);
        batch.draw(t, cx - w / 2f, y, w, h * breath);
    }

    private void drawDiamond(SpriteBatch batch, float cx, float cy, float size, float alpha) {
        batch.setColor(TEXT.r, TEXT.g, TEXT.b, alpha);
        float s = size * 0.7f;
        batch.draw(pixelRegion, cx - s / 2f, cy - s / 2f, s / 2f, s / 2f, s, s, 1f, 1f, 45f);
    }

    private void drawCover(SpriteBatch batch, Texture t, float x, float y, float w, float h,
                           float zoom, float driftX, float driftY) {
        float scale = Math.max(w / t.getWidth(), h / t.getHeight()) * zoom;
        float srcW = w / scale, srcH = h / scale;
        float u = (t.getWidth() - srcW) / (2f * t.getWidth()) + driftX;
        float v = (t.getHeight() - srcH) / (2f * t.getHeight()) + driftY;
        u = MathUtils.clamp(u, 0f, 1f - srcW / t.getWidth());
        v = MathUtils.clamp(v, 0f, 1f - srcH / t.getHeight());
        batch.draw(t, x, y, w, h, u, v + srcH / t.getHeight(), u + srcW / t.getWidth(), v);
    }

    private void drawFit(SpriteBatch batch, Texture t, float x, float y, float w, float h) {
        float scale = Math.min(w / t.getWidth(), h / t.getHeight());
        float dw = t.getWidth() * scale, dh = t.getHeight() * scale;
        batch.draw(t, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh);
    }

    private void drawFrame(SpriteBatch batch, float x, float y, float width, float height,
                           float thickness, float alpha) {
        batch.setColor(0.78f, 0.76f, 0.72f, alpha);
        batch.draw(pixel, x, y, width, thickness);
        batch.draw(pixel, x, y + height - thickness, width, thickness);
        batch.draw(pixel, x, y, thickness, height);
        batch.draw(pixel, x + width - thickness, y, thickness, height);
    }

    private void drawWhiteFrame(SpriteBatch batch, float x, float y, float width, float height,
                                float thickness) {
        batch.setColor(Color.WHITE);
        batch.draw(pixel, x, y, width, thickness);
        batch.draw(pixel, x, y + height - thickness, width, thickness);
        batch.draw(pixel, x, y, thickness, height);
        batch.draw(pixel, x + width - thickness, y, thickness, height);
    }

    private Texture createGrain() {
        Pixmap pm = new Pixmap(GRAIN_SIZE, GRAIN_SIZE, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < GRAIN_SIZE; y++) {
            for (int x = 0; x < GRAIN_SIZE; x++) {
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

    private Texture createVignette() {
        final int w = 256, h = 144;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float nx = (x + 0.5f) / w * 2f - 1f;
                float ny = (y + 0.5f) / h * 2f - 1f;
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

    // screen lifecycle

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        camera.position.set(Main.WORLD_WIDTH / 2f, Main.WORLD_HEIGHT / 2f, 0f);
        camera.update();
        updateVisibleArea();
        computeLayout();
        layoutWidth = -1f;
        bLayoutW = -1f;
    }

    // pause pag nawala ang focus
    public void pauseMusic() {
        if (battleMusic != null) battleMusic.pause();
        if (phaseTwoMusic != null) phaseTwoMusic.pause();
        if (phaseThreeMusic != null) phaseThreeMusic.pause();
    }

    // resume pag bumalik
    public void resumeMusic() {
        Music m = activeMusic();
        if (m != null && battleMusicStarted && !battleMusicEnded) m.play();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        Gdx.input.setCursorCatched(false);
        pauseMusic();
    }

    @Override
    public void dispose() {
        background.dispose();
        topLine.dispose();
        choiceIndicator.dispose();
        choiceCardBack.dispose();
        grain.dispose();
        vignette.dispose();
        pixel.dispose();
        for (Texture t : sprites.values()) t.dispose();
        for (Texture t : portraits.values()) t.dispose();
        for (Texture t : cardTex.values()) t.dispose();
        dialogueFont.dispose();
        nameFont.dispose();
        uiFont.dispose();
        choiceFont.dispose();
        titleFont.dispose();
        if (monoShader != null) monoShader.dispose();
        if (blip != null) blip.dispose();
        if (cardDrawSfx != null) cardDrawSfx.dispose();
        if (cardFlipSfx != null) cardFlipSfx.dispose();
        if (chosenSfx != null) chosenSfx.dispose();
        if (battleMusic != null) battleMusic.dispose();
        if (phaseTwoMusic != null) phaseTwoMusic.dispose();
        if (phaseThreeMusic != null) phaseThreeMusic.dispose();
        arena.dispose();
    }
}