# KALAPASTANGAN

KALAPASTANGAN is a tarot-themed narrative game built with Java and [libGDX](https://libgdx.com/). Explore tarot readings and branching dialogue in the desktop demo.

The official development repository for KALAPASTANGAN contains its source code, game assets, documentation, and project updates. This is a non-commercial, non-profit fan project created for educational and entertainment purposes, with no intention of infringing on existing copyrights.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Source layout

Everything lives under `core/src/main/java/com/java/KALAPASTANGAN`:

```
KALAPASTANGAN/          app shell: Main (game + screen transitions), GameScreen, GameSettings
  battle/               the fights: AustineBossBattle (screens, phases, HUD, glitches),
                        BattleArena (bullets, bones, fx), BattleChart (the phase tables plus
                        the beat/section timing and camera-fit maths), BeatViz (baked visualizer)
  dialogue/             the script engine: DialogueScript (parser + nodes), DialogueRunner
                        (playback state), RichText (inline markup layout)
  ui/                   everything outside a fight: MenuScreen, MenuIntro, MenuTextures (the
                        procedurally drawn textures), WarningScreen, BuiltWithScreen, CardReading
```

Supporting folders:

- `assets/` — runtime content, paths are relative to it.
- `tools/` — offline Python analysis: `analyze_beats.py` / `analyze_song.py` for song timing and
  `make_vis.py` / `bake_viz.py` for the baked visualizer tracks.
- `archive/` — local-only history: old source snapshots (`archive/old-sources`), pre-refactor copies
  (`archive/pre-refactor`) and design snapshots (`archive/legacy-snapshots`). It is git-ignored and
  never compiled.

The repository is not initialised yet; run `git init` in the project root when you want to publish it.
The included `.gitignore` already covers Gradle output, IDE files, OS junk and `archive/`.

## Assets

Assets are grouped by type under `assets/`: `Audio/Music`, `Audio/SFX` (split into `Cards` and `Menu`), `Dialogue`, `Fonts`, `Images` (organized into `Backgrounds`, `Branding`, `Cards`, `Characters`, and `UI`), and `Video/Backgrounds`. Asset paths in the game are relative to the `assets` folder.

### Characters

Character art and voice lines live in one folder per character:

```
assets/Images/Characters/<NAME>/Sprites/<expression>.png    e.g. MARIA/Sprites/normal.png
assets/Images/Characters/<NAME>/Portraits/<speaker>.png     e.g. MARIA/Portraits/minipfp.png
assets/Audio/Voice/<NAME>/<line>.wav
```

`<NAME>` is one of `AUSTINE`, `BASILYO`, `LUNA`, `MARIA`, `MARK`. The folders are all created (kept in git
with `.gitkeep`); the two sprites and the portrait in the repo are Maria's, and the others are ready for their
art and voice lines. Sprite file names follow the `!expr` value in the dialogue script (`normal`, `nakikinig`),
and portraits are registered per speaker in `AustineBossBattle` (`portraits.put("MARIA", ...)`) so a new
character is one file plus one line. The Liyab fight uses `Austine Phase 1.mp3` and its baked `Austine_Phase_1.vis` in `assets/Audio/Music`; regenerate the visualizer with `python tools/make_vis.py "assets/Audio/Music/Austine Phase 1.mp3" assets/Audio/Music/Austine_Phase_1.vis`.

## Tests

`./gradlew core:test` runs the unit tests: the chart timings (every phase's section and menu
boundaries against the measured beats) and the dialogue parser (commands, choices, `!timed`, and the
error cases, plus the shipped `scene1.txt`). `.github/workflows/build.yml` compiles, tests and builds
the jar on every push and pull request.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.

## Running the built game

`./gradlew lwjgl3:jar` builds `lwjgl3/build/libs/KALAPASTANGAN-1.0.0.jar` - a runnable jar with every asset inside it.

Double-click `PLAY KALAPASTANGAN.bat` to start it. The launcher looks for the jar beside itself or in
`lwjgl3/build/libs/`, looks for a Java runtime (in `JAVA_HOME`, then on `PATH`), and then starts the game.
If Java is missing or older than 8 it shows a dialog that can open the Java download page, instead of
failing silently - install Java 21 (LTS) from [Adoptium](https://adoptium.net/temurin/releases/),
then double-click the game again. `PLAY KALAPASTANGAN.bat --check` prints which Java and which jar it
found without starting anything.

A Java program cannot run its own "is Java installed" check - that is why the check lives in the launcher
script. For a build that needs no Java at all, package it with Construo (`./gradlew lwjgl3:packageWinX64`),
which bundles its own runtime.

## Dialogue demo controls

- **Dialogue:** Space, Enter, or click to advance. `A` toggles auto, hold Ctrl to skip, `L` opens the history log, and Esc opens the return-to-menu confirmation.
- **Choices:** Cards fan out like a held hand above the dialogue box. Hover or use W/S and the arrow keys to pop a card above the others, reveal its text, and show the bobbing pointing-hand indicator. Press `1`-`4`, Enter, Space, or click to choose.
- **History log:** Scroll with the mouse wheel or W/S; close with Esc, `L`, or a click.

The scene script is [assets/Dialogue/scene1.txt](./assets/Dialogue/scene1.txt). The demo opens on its FOOL chapter card, then follows Maria and Mark's hallway conversation and its three branching responses. The shadow reveal uses the script's `!shake` command to shake the full scene. The scene's ending branches trigger the Liyab battle: its track starts immediately as the box closes in, followed by a quiet fake-out and a beat-34 drop at about 11.91 seconds. The battle then moves through Ignition, Lattice, Spin, Collapse, and the LIYAB finale, with three timed Attack/Defend/Reason menus. The timer and pressure bars sit above the choices; timeout selects Defend. Three attacks can defeat the 24-HP boss, while three Reason choices spare it. The fake-out attacks can damage you; press F to toggle temporary god mode (the heart turns blue) while viewing the chart. During dodge rounds, move with WASD and hold Shift to slow down. Press F3 during the battle to cycle screen effects through full, half, and off. Camera shake is reduced, tilt/zoom/pan settle smoothly, and the arena is fitted within the screen during camera movement.

## Phase 1 ('LIYAB')

Phase 1 follows five beat-synced dodge sequences. `IGNITION` introduces falling attacks, aimed shots, and sweeping beams. `LATTICE` adds beam patterns and spinning crosses. `STANDSTILL` mixes timed blasts with a brief pause before the attacks resume. `COLLAPSE` fills the arena with imploding rings, weaving shots, and spinning attacks. `INFERNO`
 brings the patterns together at higher speed, with little room to rest. Three Attack/Defend/Reason menus appear between sequences.

## Phase 2 (`DILIM`)

 Phase 2 (!battle dilim, Austine Phase 2.mp3, 183.58 BPM): 120 HP / 5 hearts boss, beatable in ~4 ATTACK or 3 REASON picks. Four dodge sequences — DESCENT, THRALL, VORTEX, COLLAPSE — land exactly on the song’s section boundaries, separated by synced menus. Finale (from 1:43): the arena box itself spins and teleports around the screen while projectiles/heart stay unrotated for readability; red screen-edge burn, shake/zoom/ring on kicks, cold palette (bone, magenta, lilac, violet), and monochrome impact frames at key moments.


## Phase 3 (`SIGAW`)

Phase 3 (SIGAW, Austine Phase 3.mp3, 99.75 BPM, 175s long): five sequences — MAYDAY, RUPTURE, STATIC, SIGNAL, SIGAW — charted with menus in between. Signature effects: 9 timed “signal break” glitches (mono static + heart zoom), a cinematic MAYDAY broadcast intro (0:10–0:20), a bullet-free “bone lane” corridor segment at 1:36 (~26 scrolling walls with a shifting gap), and a 5-cycle “wall squeeze” (0:78–1:43) where the arena breathes inward/outward. Dead-channel steel palette; loudest music of the three phases.


## Main menu


Phase 3 (SIGAW, Austine Phase 3.mp3, 99.75 BPM, 175s long): five sequences — MAYDAY, RUPTURE, STATIC, SIGNAL, SIGAW — charted with menus in between. Signature effects: 9 timed “signal break” glitches (mono static + heart zoom), a cinematic MAPhase 2 (!battle dilim, Austine Phase 2.mp3, 183.58 BPM): 120 HP / 5 hearts boss, beatable in ~4 ATTACK or 3 REASON picks. Four dodge sequences — DESCENT, THRALL, VORTEX, COLLAPSE — land exactly on the song’s section boundaries, separated by synced menus. Finale (from 1:43): the arena box itself spins and teleports around the screen while projectiles/heart stay unrotated for readability; red screen-edge burn, shake/zoom/ring on kicks, cold palette (bone, magenta, lilac, violet), and monochrome impact frames at key moments
YDAY broadcast intro (0:10–0:20), a bullet-free “bone lane” corridor segment at 1:36 (~26 scrolling walls with a shifting gap), and a 5-cycle “wall squeeze” (0:78–1:43) where the arena breathes inward/outward. Dead-channel steel palette; loudest music of the three phases.

- Use W/S or the mouse to select a menu item; Enter, Space, or click to activate it.
- Click a rotating tarot card to open its reading; use A/D or the arrow keys to browse cards. Press Esc, click X, or click outside the reading to close it.
- New Game opens the dialogue demo with a fade; Continue is disabled until a save system is added.
