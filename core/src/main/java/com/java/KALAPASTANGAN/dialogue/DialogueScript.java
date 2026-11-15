package com.java.KALAPASTANGAN.dialogue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// basahin ang text file, gawing nodes at labels
public class DialogueScript {
    public enum Type {
        LINE, CHOICE, GOTO, SET, IF, TRUST, MIND, IFTRUST, IFMIND,
        SHAKE, FLASH, EXPR, BATTLE, CHAPTER, END
    }
    private static final Set<String> BATTLE_PATTERNS = new HashSet<String>(
        Arrays.asList("default", "yabag", "anino", "sweep", "fan", "liyab", "dilim", "sigaw"));

    public static class Choice {
        public final String text;
        public final String target;

        Choice(String text, String target) {
            this.text = text;
            this.target = target;
        }
    }

    public static class Node {
        public final Type type;
        public final int lineNo;
        public String speaker = "";
        public String text = "";
        public String target = "";
        public String flag = "";
        public String cmp = "";
        public int value;
        public float timeLimit;
        public final List<Choice> choices = new ArrayList<Choice>();

        Node(Type type, int lineNo) {
            this.type = type;
            this.lineNo = lineNo;
        }
    }

    public final List<Node> nodes = new ArrayList<Node>();
    public final Map<String, Integer> labels = new HashMap<String, Integer>();

    public static DialogueScript load(FileHandle file) {
        if (!file.exists()) {
            throw new GdxRuntimeException("Hindi mahanap ang script: " + file.path());
        }
        return parse(file.readString("UTF-8"));
    }

    public static DialogueScript parse(String source) {
        DialogueScript s = new DialogueScript();
        String[] rows = source.split("\\r?\\n");
        Node openChoice = null;
        float pendingTime = 0f;
        String pendingTimeout = "";

        for (int i = 0; i < rows.length; i++) {
            int lineNo = i + 1;
            String row = rows[i].trim();
            if (i == 0 && row.startsWith("\uFEFF")) row = row.substring(1).trim();
            if (row.isEmpty()) {
                openChoice = null;
                continue;
            }
            if (row.startsWith("#")) continue;

            if (row.startsWith("*")) {
                if (openChoice == null) {
                    openChoice = new Node(Type.CHOICE, lineNo);
                    openChoice.timeLimit = pendingTime;
                    openChoice.target = pendingTimeout;
                    pendingTime = 0f;
                    pendingTimeout = "";
                    s.nodes.add(openChoice);
                }
                String body = row.substring(1).trim();
                int arrow = body.lastIndexOf("->");
                if (arrow < 0) throw error(lineNo, "kulang ang '->' sa pagpipilian");
                String text = body.substring(0, arrow).trim();
                String target = body.substring(arrow + 2).trim();
                if (text.isEmpty() || target.isEmpty()) {
                    throw error(lineNo, "kailangan ang teksto at label ng pagpipilian");
                }
                openChoice.choices.add(new Choice(text, target));
                continue;
            }
            openChoice = null;

            if (row.startsWith("[") && row.endsWith("]")) {
                String label = row.substring(1, row.length() - 1).trim();
                if (label.isEmpty()) throw error(lineNo, "walang laman ang label");
                if (s.labels.containsKey(label)) throw error(lineNo, "doble ang label [" + label + "]");
                s.labels.put(label, s.nodes.size());
            } else if (row.startsWith("->")) {
                Node n = new Node(Type.GOTO, lineNo);
                n.target = row.substring(2).trim();
                if (n.target.isEmpty()) throw error(lineNo, "kailangan ang target ng goto");
                s.nodes.add(n);
            } else if (row.startsWith("!")) {
                Node command = parseCommand(row.substring(1).trim(), lineNo);
                if (command.type == Type.CHOICE) {
                    pendingTime = command.timeLimit;
                    pendingTimeout = command.target;
                    if (pendingTime <= 0f || pendingTimeout.isEmpty()) {
                        throw error(lineNo, "dapat ay: !timed segundo -> label");
                    }
                } else {
                    s.nodes.add(command);
                }
            } else {
                int colon = row.indexOf(':');
                if (colon < 0) throw error(lineNo, "walang ':' sa linya ng usapan");
                Node n = new Node(Type.LINE, lineNo);
                n.speaker = row.substring(0, colon).trim();
                n.text = row.substring(colon + 1).trim();
                if (n.text.isEmpty()) throw error(lineNo, "walang laman ang linya ng usapan");
                s.nodes.add(n);
            }
        }
        if (pendingTime > 0f) {
            throw error(rows.length, "may !timed na walang pagpipilian pagkatapos");
        }
        s.validate();
        return s;
    }

    private static Node parseCommand(String body, int lineNo) {
        String[] parts = body.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";
        Node n;

        if (cmd.equals("set")) {
            if (arg.isEmpty()) throw error(lineNo, "walang pangalan ang !set");
            n = new Node(Type.SET, lineNo);
            n.flag = arg;
        } else if (cmd.equals("if")) {
            int arrow = arg.indexOf("->");
            if (arrow < 0) throw error(lineNo, "dapat ay: !if bandila -> label");
            n = new Node(Type.IF, lineNo);
            n.flag = arg.substring(0, arrow).trim();
            n.target = arg.substring(arrow + 2).trim();
            if (n.flag.isEmpty() || n.target.isEmpty()) {
                throw error(lineNo, "kailangan ang bandila at target ng !if");
            }
        } else if (cmd.equals("trust") || cmd.equals("mind")) {
            n = parseStat(cmd.equals("trust") ? Type.TRUST : Type.MIND, arg, lineNo);
        } else if (cmd.equals("iftrust") || cmd.equals("ifmind")) {
            n = parseStatIf(cmd.equals("iftrust") ? Type.IFTRUST : Type.IFMIND, arg, lineNo);
        } else if (cmd.equals("timed")) {
            n = parseTimed(arg, lineNo);
        } else if (cmd.equals("shake")) {
            n = new Node(Type.SHAKE, lineNo);
        } else if (cmd.equals("flash")) {
            n = new Node(Type.FLASH, lineNo);
        } else if (cmd.equals("expr")) {
            n = new Node(Type.EXPR, lineNo);
            n.text = arg.isEmpty() ? "normal" : arg;
        } else if (cmd.equals("battle")) {
            n = new Node(Type.BATTLE, lineNo);
            n.text = arg.isEmpty() ? "default" : arg.toLowerCase(Locale.ROOT);
            if (!BATTLE_PATTERNS.contains(n.text)) {
                throw error(lineNo, "hindi kilalang battle pattern: " + n.text);
            }
        } else if (cmd.equals("chapter")) {
            n = new Node(Type.CHAPTER, lineNo);
            n.text = arg.isEmpty() ? "FOOL | ..." : arg;
        } else if (cmd.equals("end")) {
            n = new Node(Type.END, lineNo);
        } else {
            throw error(lineNo, "hindi kilalang utos: !" + cmd);
        }
        return n;
    }

    private static Node parseTimed(String arg, int lineNo) {
        int arrow = arg.indexOf("->");
        if (arrow < 0) throw error(lineNo, "dapat ay: !timed segundo -> label");
        Node n = new Node(Type.CHOICE, lineNo);
        try {
            n.timeLimit = Float.parseFloat(arg.substring(0, arrow).trim());
        } catch (NumberFormatException ignored) {
            throw error(lineNo, "hindi numero ang oras ng !timed");
        }
        n.target = arg.substring(arrow + 2).trim();
        if (n.timeLimit <= 0f || n.target.isEmpty()) {
            throw error(lineNo, "dapat ay: !timed segundo -> label");
        }
        return n;
    }

    private static Node parseStat(Type type, String arg, int lineNo) {
        String[] bits = arg.split("\\s+");
        if (bits.length < 2) {
            throw error(lineNo, "dapat ay: !" + type.name().toLowerCase(Locale.ROOT)
                + " character +1");
        }
        Node n = new Node(type, lineNo);
        n.flag = bits[0].toLowerCase(Locale.ROOT);
        String amount = bits[1];
        if (amount.startsWith("=")) {
            n.cmp = "=";
            amount = amount.substring(1);
        } else {
            n.cmp = "+";
        }
        try {
            n.value = Integer.parseInt(amount);
        } catch (NumberFormatException ignored) {
            throw error(lineNo, "hindi numero ang value ng !" + type.name().toLowerCase(Locale.ROOT));
        }
        return n;
    }

    private static Node parseStatIf(Type type, String arg, int lineNo) {
        int arrow = arg.indexOf("->");
        if (arrow < 0) {
            throw error(lineNo, "dapat ay: !" + type.name().toLowerCase(Locale.ROOT)
                + " character >= 3 -> label");
        }
        String left = arg.substring(0, arrow).trim();
        String[] bits = left.split("\\s+");
        if (bits.length < 3) {
            throw error(lineNo, "dapat ay: !" + type.name().toLowerCase(Locale.ROOT)
                + " character >= 3 -> label");
        }
        Node n = new Node(type, lineNo);
        n.flag = bits[0].toLowerCase(Locale.ROOT);
        n.cmp = bits[1];
        if (!n.cmp.equals(">=") && !n.cmp.equals("<=") && !n.cmp.equals(">")
            && !n.cmp.equals("<") && !n.cmp.equals("==")) {
            throw error(lineNo, "hindi kilalang comparison: " + n.cmp);
        }
        try {
            n.value = Integer.parseInt(bits[2]);
        } catch (NumberFormatException ignored) {
            throw error(lineNo, "hindi numero ang value ng comparison");
        }
        n.target = arg.substring(arrow + 2).trim();
        if (n.flag.isEmpty() || n.target.isEmpty()) {
            throw error(lineNo, "kailangan ang character at target");
        }
        return n;
    }

    private void validate() {
        if (!labels.containsKey("start")) {
            throw new GdxRuntimeException("Walang [start] na label sa dialogue script.");
        }
        for (Node n : nodes) {
            if (n.type == Type.GOTO || n.type == Type.IF
                || n.type == Type.IFTRUST || n.type == Type.IFMIND) {
                check(n.target, n.lineNo);
            }
            if (n.type == Type.CHOICE) {
                if (n.choices.isEmpty()) throw error(n.lineNo, "walang pagpipilian");
                for (Choice c : n.choices) check(c.target, n.lineNo);
                if (n.timeLimit > 0f) {
                    if (n.target.isEmpty()) throw error(n.lineNo, "kailangan ang timeout label");
                    check(n.target, n.lineNo);
                }
            }
        }
    }

    private void check(String target, int lineNo) {
        if (!labels.containsKey(target)) {
            throw error(lineNo, "walang label na [" + target + "]");
        }
    }

    private static GdxRuntimeException error(int lineNo, String message) {
        return new GdxRuntimeException("Dialogue script, linya " + lineNo + ": " + message);
    }
}
