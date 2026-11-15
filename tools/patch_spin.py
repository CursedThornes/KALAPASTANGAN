#!/usr/bin/env python3
"""One-off: the phase-2 finale spins the arena box only - no screen rotation, no camera drift."""
import sys

A = "core/src/main/java/com/java/KALAPASTANGAN/BattleArena.java"
B = "core/src/main/java/com/java/KALAPASTANGAN/AustineBossBattle.java"

def patch(path, edits, slices=None):
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()
    if slices:
        for start_marker, end_marker, body in slices:
            s = src.find(start_marker)
            e = src.find(end_marker)
            if s < 0 or e < 0 or e < s:
                sys.exit("slice not found in %s: %r" % (path, start_marker[:80]))
            src = src[:s] + body + src[e:]
    for item in edits:
        old, new = item[0], item[1]
        count = item[2] if len(item) > 2 else 1
        found = src.count(old)
        if found != count:
            sys.exit("MISMATCH in %s (%d != %d):\n%s" % (path, found, count, old[:200]))
        src = src.replace(old, new)
    with open(path, "w", encoding="utf-8", newline="\r\n") as f:
        f.write(src)

# ------------------------------------------------------------------ BattleArena
spin_body = '''    /**
     * The last sequence turns the arena box itself, and only the box: the camera stays where it
     * is and nothing outside the square turns. A zoom-in boom lands on every kick.
     */
    private void tickSpinFinale(float d) {
        spinT += d;
        tpTX = tpTY = 0f;
        spinAng += d * (190f + 150f * secLevel);

        spinBoomT = Math.max(0f, spinBoomT - d);
        if (thumpV > 0.7f && spinBoomT <= 0f) {
            spinBoomT = 0.26f;
            addTrauma(0.55f);
            fxZoom += 0.09f;
            flashV = Math.max(flashV, 0.45f);
            fxImpact(0.8f);
            addRing(L / 2f, L / 2f, 24f, 560f, 0.5f, 1, 0.9f, false);
            addRing(L / 2f, L / 2f, 60f, 380f, 0.65f, 3, 0.6f, true);
        }
    }

'''
patch(A, [
    # the spin no longer touches the world transform: it is applied to the arena only
    ('''        outRot = fs * (fxTilt + tpR + spinAng + sh * FX_SHAKE_ROT * noise(3f, animT));''',
     '''        outRot = fs * (fxTilt + tpR + sh * FX_SHAKE_ROT * noise(3f, animT));'''),
    # expose the box angle
    ('''    public float fxRot() { return outRot; }''',
     '''    public float fxRot() { return outRot; }

    /** Extra rotation applied to the arena box alone (the 1:43 spin finale). */
    public float spinAngle() { return spinAng; }'''),
], [("    /** 1:43: the last sequence turns the arena loose. */", "    private void tickTeleport(float d, float st) {", spin_body)])

# ------------------------------------------------------------------ AustineBossBattle
patch(B, [
    ('''    private final Matrix4 worldTransform''', '''    private final Matrix4 arenaTransform = new Matrix4();
    private final Matrix4 worldTransform'''),
    # extent must account for the spinning box so it stays on screen
    ('''            float radians = Math.abs(rot) * MathUtils.degreesToRadians;''',
     '''            float radians = Math.abs(rot + arena.spinAngle()) * MathUtils.degreesToRadians;'''),
    # build the box-only transform
    ('''            arenaScreenTop = viewCy + (acy - pivotY) * worldScale + extent * worldScale;''',
     '''            arenaScreenTop = viewCy + (acy - pivotY) * worldScale + extent * worldScale;
            arenaTransform.set(worldTransform);
            float spin = arena.spinAngle();
            if (spin != 0f) {
                arenaTransform.translate(pivotX, pivotY, 0f).rotate(0f, 0f, 1f, spin)
                    .translate(-pivotX, -pivotY, 0f);
            }
            arenaScreenTop = viewCy + (acy - pivotY) * worldScale + extent * worldScale;'''),
    # the panel / frame / contents use the box transform
    ('''        if (fighting) batch.setTransformMatrix(worldTransform);

        // Panel (shrinks into the battle square)''',
     '''        if (fighting) batch.setTransformMatrix(arenaTransform);

        // Panel (shrinks into the battle square)'''),
    # hand the upright transform back before the HUD
    ('''                arena.draw(batch, pixel, 1f);
            }
            drawBattleHud(batch, battlePhase == B_P2_INTRO ? 0f : battleUi);''',
     '''                arena.draw(batch, pixel, 1f);
            }
            batch.setTransformMatrix(worldTransform);
            drawBattleHud(batch, battlePhase == B_P2_INTRO ? 0f : battleUi);'''),
])
print("spin finale: box only, camera untouched")
