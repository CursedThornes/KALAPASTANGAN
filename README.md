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

When the first fight resolves, the story picks straight back up: Mark and Maria talk in the hallway across two three-card choices (the same fanned card hand as the first scene), then `!battle dilim` in the script drops the box back into the arena for phase 2.

Phase 2 is charted against `Austine Phase 2.mp3` and its baked `Austine_Phase_2.vis`. The track was measured at 183.58 BPM with its first downbeat at 0.020s; the body of the song starts on bar 16 (20.94s) after a one-bar hole, and the four dodge sequences sit on that song's real section boundaries — DESCENT (20.94s), THRALL (52.31s, through the 62.8s breakdown), VORTEX (82.38s, launching on the second drop) and COLLAPSE (103.30s). Each menu between them covers a quiet or transitional part of the track and lands exactly on the next phrase, so the countdown reaches zero on the section boundary. Phase 2 opens with a short `PHASE 2 - DESCENT` title (any key skips it), and its last sequence is the spin finale: from 1:43 the arena box itself spins, winding the other way on every leg, and glides from spot to spot anywhere on screen - corners, top, bottom - always inside the frame, with the camera left home so the travel reads. While it travels the box is drawn above the HP bar and the rest of the HUD, so nothing sits on top of it. The edges of the screen burn red, and every kick lands a shake, a zoom-in boom and a ring. The casing turns and travels with the box; the projectiles and the heart are never rotated, so the patterns stay readable. Phase 2 also has its own cold palette (bone, bruised magenta, lilac, violet) and monochrome impact frames — two or three frames of flat white/black with torn slabs, cut on sequence starts, on damage, on the drop, and every few seconds through the finale. with `python tools/make_vis.py "assets/Audio/Music/Austine Phase 2.mp3" assets/Audio/Music/Austine_Phase_2.vis`. Tempo and section analysis for either track: `python tools/analyze_beats.py "assets/Audio/Music/Austine Phase 2.mp3"`.

Phase 2 starts with 120 boss HP and the same five hearts; about four ATTACK picks finish it, while three REASON picks still spare it.

## Phase 3 (`SIGAW`)

The last fight runs the whole of `Austine Phase 3.mp3`. Measured with `python tools/analyze_beats.py "assets/Audio/Music/Austine Phase 3.mp3"`: 99.75 BPM, first downbeat 0.375s, bar 2.406s, 175.03s long. The intro counts in one heavy bomb per beat, the drop lands on bar 4 at 10.00s, and the rest of the track is charted in five sequences — MAYDAY (10.00–38.87), RUPTURE (43.68–70.15), STATIC (74.96–103.83, covering the 1:36 remix and the 1:40 shred), SIGNAL (108.65–135.11, through the 2:14 change) and SIGAW (139.92–173.61) — with the four Attack/Defend/Reason menus in the eight-beat gaps between them.

Phase 3's signature is the signal breaking. At 0:27, 0:37, 0:42, 0:47, 0:52, 0:57, 1:02, 1:06 and 1:11 the entire screen cuts to monochrome, tears into scanline/block static, shakes, cuts an impact frame and snap-zooms onto the heart, then releases. At 0:10–0:20 the broadcast goes cinematic: letterbox bars slam in from the top and bottom, the camera dives onto the heart, and two `MAYDAY!` cards swipe in from the left and right, strobe, then swipe back out. At **1:36** the bullets stop entirely and the box becomes a **bone lane**: bone walls slide in from the right with a single gap each, so the playfield turns into a scrolling corridor that you run through left-to-right, following the gap as it walks up and down the box. About 26 thick walls, a wall every 0.42 beat at 245+ speed so the slabs read as one continuous wave of bone, with the gap snaking top-to-bottom and narrowing as it goes. Nothing else on screen. Phase 3 also owns the **wall squeeze** (0:78–1:43, five windows): the arena casing and playfield breathe smoothly inward up to ~72% and back out, with a slam and edge sparks on each close. Between 0:10 and 0:20 a torn `MAYDAY!` broadcast stutters across the arena with a broken-transmitter beep and an `ERR 0x.. // NO SIGNAL` line, and every glitch stamps `SIGNAL LOST` over the centre. Its palette is dead-channel steel — ash, bone and one cold white highlight — so the mono bursts read as the real picture surfacing. Phase 3's music runs at the engine's volume ceiling, a step above phases 1 and 2.

## Main menu

- Use W/S or the mouse to select a menu item; Enter, Space, or click to activate it.
- Click a rotating tarot card to open its reading; use A/D or the arrow keys to browse cards. Press Esc, click X, or click outside the reading to close it.
- New Game opens the dialogue demo with a fade; Continue is disabled until a save system is added.
