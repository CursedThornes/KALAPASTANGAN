package com.java.KALAPASTANGAN.dialogue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assume;
import org.junit.Test;

public class DialogueScriptTest {
    private static final String MINIMAL = String.join("\n",
        "[start]",
        "MARIA: Hello there.",
        "   ",
        "# comment, dapat balewala",
        "* Go -> a",
        "* Stay -> b",
        "[a]",
        "!set met_maria",
        "MARK: Coming.",
        "-> end",
        "[b]",
        "!trust maria +1",
        "[end]",
        "!end"
    );

    @Test
    public void parsesLinesChoicesAndLabels() {
        DialogueScript s = DialogueScript.parse(MINIMAL);

        assertEquals(4, s.labels.size());
        assertEquals(Integer.valueOf(0), s.labels.get("start"));
        assertEquals(Integer.valueOf(2), s.labels.get("a"));
        assertEquals(Integer.valueOf(5), s.labels.get("b"));

        DialogueScript.Node line = s.nodes.get(0);
        assertEquals(DialogueScript.Type.LINE, line.type);
        assertEquals("MARIA", line.speaker);
        assertEquals("Hello there.", line.text);
        assertEquals(2, line.lineNo);

        DialogueScript.Node choice = s.nodes.get(1);
        assertEquals(DialogueScript.Type.CHOICE, choice.type);
        assertEquals(2, choice.choices.size());
        assertEquals("Go", choice.choices.get(0).text);
        assertEquals("a", choice.choices.get(0).target);
        assertEquals("b", choice.choices.get(1).target);

        assertEquals(DialogueScript.Type.SET, s.nodes.get(2).type);
        assertEquals("met_maria", s.nodes.get(2).flag);
        assertEquals(DialogueScript.Type.GOTO, s.nodes.get(4).type);
        assertEquals("end", s.nodes.get(4).target);
        assertEquals(DialogueScript.Type.TRUST, s.nodes.get(5).type);
        assertEquals("maria", s.nodes.get(5).flag);
        assertEquals("+", s.nodes.get(5).cmp);
        assertEquals(1, s.nodes.get(5).value);
        assertEquals(DialogueScript.Type.END, s.nodes.get(6).type);
    }

    @Test
    public void parsesTimedChoice() {
        DialogueScript s = DialogueScript.parse(String.join("\n",
            "[start]",
            "!timed 3.5 -> timeout",
            "* Yes -> yes",
            "* No -> no",
            "[yes]",
            "MARIA: Yes.",
            "[no]",
            "MARIA: No.",
            "[timeout]",
            "!end"
        ));
        DialogueScript.Node choice = s.nodes.get(0);
        assertEquals(DialogueScript.Type.CHOICE, choice.type);
        assertEquals(3.5f, choice.timeLimit, 1e-4f);
        assertEquals("timeout", choice.target);
        assertEquals(2, choice.choices.size());
        assertEquals("no", choice.choices.get(1).target);
    }

    @Test
    public void parsesEveryCommand() {
        DialogueScript s = DialogueScript.parse(String.join("\n",
            "[start]",
            "!shake",
            "!flash",
            "!expr angry",
            "!battle liyab",
            "!chapter FOOL | The beginning",
            "!if met_maria -> a",
            "[a]",
            "!iftrust maria >= 3 -> b",
            "!ifmind mark <= 2 -> b",
            "!mind mark =2",
            "[b]",
            "!end"
        ));
        int i = 0;
        assertEquals(DialogueScript.Type.SHAKE, s.nodes.get(i++).type);
        assertEquals(DialogueScript.Type.FLASH, s.nodes.get(i++).type);
        assertEquals("angry", s.nodes.get(i++).text);
        assertEquals("liyab", s.nodes.get(i++).text);
        assertEquals("FOOL | The beginning", s.nodes.get(i++).text);

        DialogueScript.Node ifNode = s.nodes.get(i++);
        assertEquals(DialogueScript.Type.IF, ifNode.type);
        assertEquals("met_maria", ifNode.flag);
        assertEquals("a", ifNode.target);

        assertEquals(DialogueScript.Type.IFTRUST, s.nodes.get(i++).type);
        assertEquals(DialogueScript.Type.IFMIND, s.nodes.get(i++).type);
        DialogueScript.Node stat = s.nodes.get(i++);
        assertEquals(DialogueScript.Type.MIND, stat.type);
        assertEquals("=", stat.cmp);
        assertEquals(2, stat.value);
        assertEquals(DialogueScript.Type.END, s.nodes.get(i).type);
    }

    @Test
    public void rejectsUnknownCommand() {
        assertError(String.join("\n", "[start]", "!dance wildly"), "hindi kilalang utos");
    }

    @Test
    public void rejectsMissingStartLabel() {
        assertError("MARIA: Hello.", "Walang [start]");
    }

    @Test
    public void rejectsDuplicateLabel() {
        assertError(String.join("\n", "[start]", "[start]", "MARIA: Hi."), "doble ang label");
    }

    @Test
    public void rejectsUnknownJumpTarget() {
        assertError(String.join("\n", "[start]", "-> nowhere"), "walang label na [nowhere]");
    }

    @Test
    public void rejectsChoiceWithoutArrow() {
        assertError(String.join("\n", "[start]", "* Just text"), "kulang ang '->'");
    }

    @Test
    public void rejectsEmptyLine() {
        assertError(String.join("\n", "[start]", "MARIA:"), "walang laman ang linya");
    }

    @Test
    public void rejectsLineWithoutColon() {
        assertError(String.join("\n", "[start]", "MARIA Hello"), "walang ':'");
    }

    @Test
    public void rejectsUnknownBattlePattern() {
        assertError(String.join("\n", "[start]", "!battle fireworks"), "hindi kilalang battle pattern");
    }

    @Test
    public void rejectsTimedWithoutChoices() {
        assertError(String.join("\n", "[start]", "!timed 2 -> done", "[done]", "!end"),
            "may !timed na walang pagpipilian");
    }

    @Test
    public void rejectsBadComparison() {
        assertError(String.join("\n", "[start]", "!iftrust maria ~= 3 -> start"),
            "hindi kilalang comparison");
    }

    @Test
    public void handlesCrlfBomAndBlankFirstLine() {
        String src = "\uFEFF\r\n[start]\r\nMARIA: Hi.\r\n* Go -> a\r\n[a]\r\n!end\r\n";
        DialogueScript s = DialogueScript.parse(src);
        assertEquals(DialogueScript.Type.LINE, s.nodes.get(0).type);
        assertEquals("Hi.", s.nodes.get(0).text);
        assertEquals(3, s.nodes.get(0).lineNo);
    }

    @Test
    public void keepsDialogueTextIntact() {
        DialogueScript s = DialogueScript.parse(String.join("\n",
            "[start]",
            "MARIA: Did you hear that? -> yes, really",
            "!end"
        ));
        assertEquals("Did you hear that? -> yes, really", s.nodes.get(0).text);
    }

    /** Ang tunay na script ng demo, para hindi ito mabasag nang tahimik. */
    @Test
    public void demoScriptStillParses() throws IOException {
        File file = new File("../assets/Dialogue/scene1.txt");
        Assume.assumeTrue("scene1.txt not available from the test working directory", file.exists());

        DialogueScript s = DialogueScript.parse(
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));

        assertNotNull(s.labels.get("start"));
        assertTrue("script needs at least one choice", hasChoice(s));
    }

    private static boolean hasChoice(DialogueScript s) {
        for (DialogueScript.Node n : s.nodes) {
            if (n.type == DialogueScript.Type.CHOICE) return true;
        }
        return false;
    }

    private static void assertError(String source, String expectedFragment) {
        try {
            DialogueScript.parse(source);
            fail("expected a dialogue error containing: " + expectedFragment);
        } catch (GdxRuntimeException e) {
            assertTrue("message was: " + e.getMessage(), e.getMessage().contains(expectedFragment));
        }
    }
}
