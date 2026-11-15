package com.java.KALAPASTANGAN.dialogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// text na may {tags}
public class RichText {
    public static class Ch {
        public char value;
        public boolean shake, wave, slow;
        public Color color;
        public float x;
        public int line;
        public float revealTime;
        float pauseBefore;
    }

    private static final Color RED = new Color(0.85f, 0.25f, 0.22f, 1f);
    private static final Color GRAY = new Color(0.55f, 0.54f, 0.5f, 1f);

    public final List<Ch> chars = new ArrayList<Ch>();
    public int lineCount = 1;
    public float totalTime;

    // tanggalin ang {tags} para sa log
    public static String strip(String src) {
        return src.replaceAll("\\{[^}]*\\}", "");
    }

    public static RichText parse(String src, Color base, float charsPerSecond) {
        if (src == null || base == null) {
            throw new IllegalArgumentException("Text and base color are required");
        }
        if (!Float.isFinite(charsPerSecond) || charsPerSecond <= 0f) {
            throw new IllegalArgumentException("Typing speed must be positive and finite");
        }
        RichText r = new RichText();
        int shake = 0, wave = 0, slow = 0;
        Color tint = null;
        float pending = 0f;

        int i = 0;
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == '{') {
                int end = src.indexOf('}', i);
                if (end > i) {
                    String tag = src.substring(i + 1, end).trim().toLowerCase(Locale.ROOT);
                    boolean recognized = true;
                    if (tag.startsWith("wait=")) {
                        pending += parsePause(tag.substring("wait=".length()));
                    } else if (tag.equals("shake")) shake++;
                    else if (tag.equals("/shake")) shake = Math.max(0, shake - 1);
                    else if (tag.equals("wave")) wave++;
                    else if (tag.equals("/wave")) wave = Math.max(0, wave - 1);
                    else if (tag.equals("slow")) slow++;
                    else if (tag.equals("/slow")) slow = Math.max(0, slow - 1);
                    else if (tag.equals("red")) tint = RED;
                    else if (tag.equals("gray")) tint = GRAY;
                    else if (tag.equals("/red") || tag.equals("/gray")) tint = null;
                    else {
                        recognized = false;
                    }
                    if (recognized) {
                        i = end + 1;
                        continue;
                    }
                }
            }
            Ch ch = new Ch();
            ch.value = c;
            ch.shake = shake > 0;
            ch.wave = wave > 0;
            ch.slow = slow > 0;
            ch.color = tint != null ? tint : base;
            ch.pauseBefore = pending;
            pending = 0f;
            r.chars.add(ch);
            i++;
        }

        float step = 1f / charsPerSecond;
        float acc = 0f;
        for (int k = 0; k < r.chars.size(); k++) {
            Ch ch = r.chars.get(k);
            acc += ch.pauseBefore;
            ch.revealTime = acc;
            float d = step * (ch.slow ? 2.5f : 1f);
            acc += ch.value == ' ' ? d * 0.5f : d;
            boolean wordEnd = k + 1 >= r.chars.size() || r.chars.get(k + 1).value == ' ';
            if (wordEnd) {
                if (".!?".indexOf(ch.value) >= 0) acc += 0.3f;
                else if (",;:".indexOf(ch.value) >= 0) acc += 0.12f;
            }
        }
        r.totalTime = acc + 0.1f;
        return r;
    }

    private static float parsePause(String value) {
        try {
            float pause = Float.parseFloat(value);
            if (!Float.isFinite(pause) || pause < 0f) {
                throw new IllegalArgumentException("Wait duration must be non-negative and finite");
            }
            return pause;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid wait duration: " + value, exception);
        }
    }

    // word wrap
    public void layout(BitmapFont font, float maxWidth) {
        float x = 0f;
        float wordStartX = 0f;
        int wordStart = 0;
        int line = 0;
        for (int i = 0; i < chars.size(); i++) {
            Ch ch = chars.get(i);
            float adv = advance(font, ch.value);
            if (ch.value == ' ') {
                ch.x = x;
                ch.line = line;
                x += adv;
                wordStart = i + 1;
                wordStartX = x;
                continue;
            }
            if (x + adv > maxWidth && wordStartX > 0f) {
                line++;
                for (int j = wordStart; j < i; j++) {
                    Ch w = chars.get(j);
                    w.x -= wordStartX;
                    w.line = line;
                }
                x -= wordStartX;
                wordStartX = 0f;
            }
            ch.x = x;
            ch.line = line;
            x += adv;
        }
        lineCount = line + 1;
    }

    private static float advance(BitmapFont font, char c) {
        BitmapFont.Glyph g = font.getData().getGlyph(c);
        if (g == null) return font.getData().spaceXadvance * font.getScaleX();
        return g.xadvance * font.getScaleX();
    }
}
