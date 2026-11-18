package com.java.KALAPASTANGAN.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Ang chart ang pinakamadaling masira nang tahimik: isang beat na mali, wala nang tugma sa kanta.
 * Dito naka-pin ang mga numerong galing sa README at sa analyze_beats.py.
 */
public class BattleChartTest {
    private static final float EPS = 0.01f;

    // ---- phase 1 ---------------------------------------------------------------------------

    @Test
    public void phaseOneDropIsAtElevenPointNine() {
        // 0.051s + beat 34 at 172 bpm
        assertEquals(11.91f, BattleChart.P1_DROP_TIME, EPS);
    }

    @Test
    public void phaseOneSectionsStartOnTheirBeat() {
        float offset = BattleChart.P1_OFFSET, beat = BattleChart.P1_BEAT;
        assertEquals(offset + 34f * beat,
            BattleChart.sequenceStartTime(offset, beat, BattleChart.P1_SEQ, 0), 1e-4f);
        assertEquals(offset + 244f * beat,
            BattleChart.sequenceStartTime(offset, beat, BattleChart.P1_SEQ, 4), 1e-4f);
        // clamp: walang crash pag labas sa range
        assertEquals(BattleChart.sequenceStartTime(offset, beat, BattleChart.P1_SEQ, 4),
            BattleChart.sequenceStartTime(offset, beat, BattleChart.P1_SEQ, 99), 1e-4f);
    }

    @Test
    public void phaseOneSectionsAreOrderedAndFitTheSong() {
        assertSectionsFitTheSong(BattleChart.P1_SEQ, BattleChart.P1_BEAT, BattleChart.P1_OFFSET,
            BattleChart.P1_SONG_LEN);
    }

    @Test
    public void everyMenuEndsWhereTheNextSectionBegins() {
        assertMenusLandOnBoundaries(BattleChart.P1_SEQ, BattleChart.P1_BEAT, BattleChart.P1_OFFSET);
        assertMenusLandOnBoundaries(BattleChart.P2_SEQ, BattleChart.P2_BEAT, BattleChart.P2_OFFSET);
        assertMenusLandOnBoundaries(BattleChart.P3_SEQ, BattleChart.P3_BEAT, BattleChart.P3_OFFSET);
    }

    @Test
    public void menuCountdownStopsAtZero() {
        float offset = BattleChart.P1_OFFSET, beat = BattleChart.P1_BEAT;
        float open = BattleChart.menuStartTime(offset, beat, BattleChart.P1_SEQ, 0);
        assertTrue(BattleChart.menuLeft(offset, beat, BattleChart.P1_SEQ, 0, open) > 0f);
        assertEquals(0f, BattleChart.menuLeft(offset, beat, BattleChart.P1_SEQ, 0, open + 999f), 1e-4f);
    }

    @Test
    public void phaseOneCueTableIsWellFormed() {
        assertCuesAreWellFormed(BattleChart.P1_CUES, BattleChart.P1_BEAT, BattleChart.P1_OFFSET,
            BattleChart.P1_SONG_LEN);
    }

    @Test
    public void sectionLevelTableStartsAndEndsQuiet() {
        assertEquals(0f, BattleChart.P1_SECT[1], 1e-4f);
        assertEquals(0f, BattleChart.P1_SECT[BattleChart.P1_SECT.length - 1], 1e-4f);
        assertAscending(BattleChart.P1_SECT, 2);
    }

    // ---- phase 2 ---------------------------------------------------------------------------

    @Test
    public void phaseTwoStartsOnBarSixteen() {
        float start = BattleChart.sequenceStartTime(
            BattleChart.P2_OFFSET, BattleChart.P2_BEAT, BattleChart.P2_SEQ, 0);
        assertEquals(20.94f, start, EPS);
    }

    @Test
    public void phaseTwoSectionsFitTheSong() {
        assertSectionsFitTheSong(BattleChart.P2_SEQ, BattleChart.P2_BEAT, BattleChart.P2_OFFSET,
            BattleChart.P2_SONG_LEN);
    }

    @Test
    public void phaseTwoCuesAreWellFormed() {
        assertCuesAreWellFormed(BattleChart.P2_CUES, BattleChart.P2_BEAT, BattleChart.P2_OFFSET,
            BattleChart.P2_SONG_LEN);
    }

    // ---- phase 3 ---------------------------------------------------------------------------

    @Test
    public void phaseThreeDropIsAtTenSeconds() {
        float start = BattleChart.sequenceStartTime(
            BattleChart.P3_OFFSET, BattleChart.P3_BEAT, BattleChart.P3_SEQ, 0);
        assertEquals(10.00f, start, EPS);
    }

    @Test
    public void phaseThreeSectionsFitTheSong() {
        assertSectionsFitTheSong(BattleChart.P3_SEQ, BattleChart.P3_BEAT, BattleChart.P3_OFFSET,
            BattleChart.P3_SONG_LEN);
    }

    @Test
    public void phaseThreeCuesAndSectionsAreWellFormed() {
        assertCuesAreWellFormed(BattleChart.P3_CUES, BattleChart.P3_BEAT, BattleChart.P3_OFFSET,
            BattleChart.P3_SONG_LEN);
        assertEquals(0f, BattleChart.P3_SECT[BattleChart.P3_SECT.length - 1], 1e-4f);
        assertAscending(BattleChart.P3_SECT, 2);
    }

    @Test
    public void phaseThreeWallsAreOrdered() {
        for (int i = 3; i < BattleChart.P3_WALL.length; i += 3) {
            assertTrue("wall windows must not overlap",
                BattleChart.P3_WALL[i] >= BattleChart.P3_WALL[i - 2]);
            assertTrue(BattleChart.P3_WALL[i + 2] > 0f);
            assertTrue(BattleChart.P3_WALL[i + 2] <= 1f);
        }
    }

    // ---- shared tables ---------------------------------------------------------------------

    @Test
    public void everyPhaseHasMatchingTables() {
        for (int phase = 1; phase <= 3; phase++) {
            float[][] seq = BattleChart.sequenceTable(phase);
            assertEquals(seq.length, BattleChart.sequenceNames(phase).length);
            assertEquals(seq.length, BattleChart.sequenceDifficulty(phase).length);
            float[][] palette = BattleChart.palette(phase);
            assertEquals(5, palette.length);
            for (float[] color : palette) assertEquals(3, color.length);
        }
    }

    @Test
    public void palettesAreDistinctPerPhase() {
        assertFalse(samePalette(BattleChart.P1_PALETTE, BattleChart.P2_PALETTE));
        assertFalse(samePalette(BattleChart.P2_PALETTE, BattleChart.P3_PALETTE));
        assertFalse(samePalette(BattleChart.P1_PALETTE, BattleChart.P3_PALETTE));
    }

    @Test
    public void teleportStationsAreInsideTheArena() {
        assertEquals(BattleChart.TP_ST.length, BattleChart.TP_BEATS.length);
        for (float[] station : BattleChart.TP_ST) {
            assertTrue(station[0] >= -1f && station[0] <= 1f);
            assertTrue(station[1] >= -1f && station[1] <= 1f);
        }
    }

    // ---- pure maths ------------------------------------------------------------------------

    @Test
    public void tableInterpolatesAndClamps() {
        float[] t = { 0f, 0f, 10f, 1f };
        assertEquals(0f, BattleChart.table(t, -5f), 1e-4f);
        assertEquals(0.25f, BattleChart.table(t, 2.5f), 1e-4f);
        assertEquals(0.5f, BattleChart.table(t, 5f), 1e-4f);
        assertEquals(1f, BattleChart.table(t, 10f), 1e-4f);
        assertEquals(1f, BattleChart.table(t, 50f), 1e-4f);
    }

    @Test
    public void fitScaleLeavesRoomForTheMargin() {
        float scale = BattleChart.fitScale(300f, 300f, 640f, 360f, 0f, 0f, 0f, 1280f, 720f, 24f);
        assertEquals(2.24f, scale, EPS);
        builtFits(scale, 300f, 300f, 640f, 360f, 0f, 0f, 0f, 0f, 0f, 1280f, 720f, 24f);
    }

    @Test
    public void tiltedArenaNeedsMoreRoom() {
        float straight = BattleChart.fitScale(300f, 300f, 640f, 360f, 0f, 0f, 0f, 1280f, 720f, 24f);
        float tilted = BattleChart.fitScale(300f, 300f, 640f, 360f, 45f, 0f, 0f, 1280f, 720f, 24f);
        assertTrue(tilted < straight);
        builtFits(tilted, 300f, 300f, 640f, 360f, 45f, 0f, 0f, 0f, 0f, 1280f, 720f, 24f);
    }

    @Test
    public void roomFitKeepsTheArenaOnScreen() {
        float room = BattleChart.roomFit(1f, 150f, 0f, 640f, 24f);
        assertEquals(466f, room, EPS);
        assertEquals(-466f, BattleChart.roomFit(-1f, 150f, 0f, 640f, 24f), EPS);
        assertEquals(0f, BattleChart.roomFit(1f, 700f, 0f, 640f, 24f), EPS);
        assertEquals(0f, BattleChart.roomFit(0f, 150f, 0f, 640f, 24f), EPS);
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static void assertSectionsFitTheSong(float[][] seq, float beat, float offset,
                                                 float songLen) {
        for (int i = 0; i < seq.length; i++) {
            assertTrue("section " + i + " ends after it starts", seq[i][1] > seq[i][0]);
            assertTrue("section " + i + " starts after the previous one",
                i == 0 || seq[i][0] > seq[i - 1][1]);
            assertTrue("section " + i + " starts inside the song",
                BattleChart.sequenceStartTime(offset, beat, seq, i) >= 0f);
            assertTrue("section " + i + " must not run past the song",
                offset + seq[i][1] * beat <= songLen + 0.5f);
            assertTrue("section " + i + " needs a positive length",
                BattleChart.sequenceLength(beat, seq, i, BattleChart.TAIL_BEATS) > 0f);
        }
    }

    private static void assertMenusLandOnBoundaries(float[][] seq, float beat, float offset) {
        for (int i = 0; i < seq.length - 1; i++) {
            assertEquals(BattleChart.sequenceStartTime(offset, beat, seq, i + 1),
                BattleChart.menuEndTime(offset, beat, seq, i), 1e-4f);
            assertTrue(BattleChart.menuLength(offset, beat, seq, i) > beat / 2f);
        }
    }

    private static void assertCuesAreWellFormed(float[] cues, float beat, float offset, float songLen) {
        assertEquals(0, cues.length % 3);
        for (int i = 0; i < cues.length; i += 3) {
            assertTrue("cue " + i / 3 + " must be in order", i == 0 || cues[i] > cues[i - 3]);
            float time = BattleChart.cueTime(offset, beat, cues, i / 3);
            assertTrue("cue " + i / 3 + " must land inside the song", time <= songLen);
            assertTrue(cues[i + 1] >= 0f && cues[i + 1] <= 1f);
            int kind = (int) cues[i + 2];
            assertTrue(kind >= 0 && kind <= 2);
        }
    }

    private static void assertAscending(float[] table, int stride) {
        for (int i = stride; i < table.length; i += stride) {
            assertTrue("table must ascend at " + i, table[i] > table[i - stride]);
        }
    }

    private static void builtFits(float scale, float aw, float ah, float cx, float cy, float rot,
                                  float panX, float panY, float scrX, float scrY, float scrW,
                                  float scrH, float margin) {
        float r = (float) Math.toRadians(rot);
        float c = Math.abs((float) Math.cos(r)), s = Math.abs((float) Math.sin(r));
        float hw = (aw / 2f) * c + (ah / 2f) * s;
        float hh = (aw / 2f) * s + (ah / 2f) * c;
        float halfW = hw * scale, halfH = hh * scale;
        assertTrue(cx + panX - halfW >= scrX + margin - 0.5f);
        assertTrue(cx + panX + halfW <= scrX + scrW - margin + 0.5f);
        assertTrue(cy + panY - halfH >= scrY + margin - 0.5f);
        assertTrue(cy + panY + halfH <= scrY + scrH - margin + 0.5f);
    }

    private static boolean samePalette(float[][] a, float[][] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (Math.abs(a[i][j] - b[i][j]) > 1e-4f) return false;
            }
        }
        return true;
    }
}
