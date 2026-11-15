package com.java.KALAPASTANGAN.dialogue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// naglalakad sa script. hinto sa lines, choices, chapter, battle
public class DialogueRunner {
    public static final int STAT_MIN = 0;
    public static final int STAT_MAX = 10;
    public static final int DEFAULT_TRUST = 4;
    public static final int DEFAULT_MIND = 3;

    private final DialogueScript script;
    private final Set<String> flags = new HashSet<String>();
    private final Map<String, Integer> trust = new HashMap<String, Integer>();
    private final Map<String, Integer> mind = new HashMap<String, Integer>();
    private int pc;
    private DialogueScript.Node current;
    private boolean finished;
    private boolean shakePending;
    private boolean flashPending;
    private String expression = "normal";
    private int lastMind = DEFAULT_MIND;

    public DialogueRunner(DialogueScript script) {
        this.script = script;
        pc = script.labels.get("start");
        step();
    }

    public DialogueScript.Node current() { return current; }

    public boolean isFinished() { return finished; }

    public String expression() { return expression; }

    public boolean hasFlag(String flag) { return flags.contains(flag); }

    public void setFlag(String flag) { flags.add(flag); }

    public int trust(String who) {
        return getStat(trust, who, DEFAULT_TRUST);
    }

    public int mind(String who) {
        return getStat(mind, who, DEFAULT_MIND);
    }

    public static String trustLabel(int value) {
        if (value <= 2) return "DISTANT";
        if (value <= 4) return "WARY";
        if (value <= 6) return "CLOSE";
        if (value <= 8) return "RELIED";
        return "BONDED";
    }

    public static String mindLabel(int value) {
        if (value <= 2) return "CALM";
        if (value <= 4) return "UNEASY";
        if (value <= 6) return "FRIGHTENED";
        if (value <= 8) return "PANICKED";
        return "BREAKING";
    }

    public boolean consumeShake() {
        boolean s = shakePending;
        shakePending = false;
        return s;
    }

    public boolean consumeFlash() {
        boolean f = flashPending;
        flashPending = false;
        return f;
    }

    // true pag lumala ang estado ni Maria
    public boolean consumeMindSpike() {
        int now = mind("maria");
        boolean spiked = now > lastMind;
        lastMind = now;
        return spiked;
    }

    // tapos na ang line/chapter
    public void next() {
        if (finished) return;
        if (current == null || (current.type != DialogueScript.Type.LINE
            && current.type != DialogueScript.Type.CHAPTER)) {
            throw new IllegalStateException("Dialogue can advance only from a line or chapter card.");
        }
        pc++;
        step();
    }

    // tapos na ang laban
    public void completeBattle() {
        if (finished) return;
        if (current == null || current.type != DialogueScript.Type.BATTLE) {
            throw new IllegalStateException("A battle can complete only while a battle is active.");
        }
        pc++;
        step();
    }

    // pumili habang may choice
    public void choose(int index) {
        if (current == null || current.type != DialogueScript.Type.CHOICE) {
            throw new IllegalStateException("A choice can be selected only while choices are active.");
        }
        if (index < 0 || index >= current.choices.size()) {
            throw new IllegalArgumentException("Choice index is out of range: " + index);
        }
        pc = script.labels.get(current.choices.get(index).target);
        step();
    }

    // timeout, tawid sa label
    public void timeout() {
        if (current == null || current.type != DialogueScript.Type.CHOICE || current.timeLimit <= 0f) {
            throw new IllegalStateException("A timeout can fire only during a timed choice.");
        }
        pc = script.labels.get(current.target);
        step();
    }

    private void step() {
        current = null;
        int guard = 0;
        while (guard++ < 1000) {
            if (pc >= script.nodes.size()) {
                finished = true;
                return;
            }
            DialogueScript.Node n = script.nodes.get(pc);
            switch (n.type) {
                case LINE:
                case CHOICE:
                case BATTLE:
                case CHAPTER:
                    current = n;
                    return;
                case GOTO:
                    pc = script.labels.get(n.target);
                    break;
                case SET:
                    flags.add(n.flag);
                    pc++;
                    break;
                case IF:
                    if (flags.contains(n.flag)) pc = script.labels.get(n.target);
                    else pc++;
                    break;
                case TRUST:
                    applyStat(trust, n, DEFAULT_TRUST);
                    pc++;
                    break;
                case MIND:
                    applyStat(mind, n, DEFAULT_MIND);
                    pc++;
                    break;
                case IFTRUST:
                    if (cmp(trust(n.flag), n.cmp, n.value)) pc = script.labels.get(n.target);
                    else pc++;
                    break;
                case IFMIND:
                    if (cmp(mind(n.flag), n.cmp, n.value)) pc = script.labels.get(n.target);
                    else pc++;
                    break;
                case SHAKE:
                    shakePending = true;
                    pc++;
                    break;
                case FLASH:
                    flashPending = true;
                    pc++;
                    break;
                case EXPR:
                    expression = n.text;
                    pc++;
                    break;
                case END:
                    finished = true;
                    return;
            }
        }
        throw new IllegalStateException("Dialogue script exceeded the command-loop limit.");
    }

    private void applyStat(Map<String, Integer> table, DialogueScript.Node n, int fallback) {
        int currentValue = getStat(table, n.flag, fallback);
        int next = "=".equals(n.cmp) ? n.value : currentValue + n.value;
        if (next < STAT_MIN) next = STAT_MIN;
        if (next > STAT_MAX) next = STAT_MAX;
        table.put(n.flag, next);
    }

    private static int getStat(Map<String, Integer> table, String who, int fallback) {
        String key = who == null ? "" : who.toLowerCase(Locale.ROOT);
        Integer value = table.get(key);
        return value == null ? fallback : value;
    }

    private static boolean cmp(int left, String op, int right) {
        if (">=".equals(op)) return left >= right;
        if ("<=".equals(op)) return left <= right;
        if (">".equals(op)) return left > right;
        if ("<".equals(op)) return left < right;
        return left == right;
    }
}
