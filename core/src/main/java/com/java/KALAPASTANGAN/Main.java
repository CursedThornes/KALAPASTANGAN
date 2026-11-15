package com.java.KALAPASTANGAN;
import com.java.KALAPASTANGAN.battle.AustineBossBattle;
import com.java.KALAPASTANGAN.ui.WarningScreen;
import com.java.KALAPASTANGAN.ui.MenuScreen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

// dito nagsisimula lahat
public class Main extends Game {
    private static final float SCREEN_TRANSITION_DURATION = 0.28f;

    // virtual res, lahat ng screen dito naka-base
    public static final float WORLD_WIDTH = 1280f;
    public static final float WORLD_HEIGHT = 720f;

    public SpriteBatch batch;
    private Texture transitionPixel;
    private final Matrix4 transitionProjection = new Matrix4();
    private Screen transitionTarget;
    private float transitionTime;
    private boolean transitionSwitched;
    private boolean maximized;
    private boolean windowActive = true;
    private Runnable maximizeWindowAction;

    @Override
    public void create() {
        batch = new SpriteBatch(4000);   // mas malaking batch, mas kaunting flush pag maraming bala
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        transitionPixel = new Texture(pm);
        pm.dispose();
        maximized = Gdx.app.getPreferences("kalapastangan-settings").getBoolean("maximized", true);
        setScreen(new WarningScreen(this));
    }

    @Override
    public void pause() {
        windowActive = false;
        Screen current = getScreen();
        if (current != null) current.pause();
        // pause pag minimized
        if (current instanceof MenuScreen) {
            ((MenuScreen) current).pauseMusic();
        } else if (current instanceof AustineBossBattle) {
            ((AustineBossBattle) current).pauseMusic();
        }
    }

    @Override
    public void resume() {
        windowActive = true;
        Screen current = getScreen();
        if (current != null) current.resume();
        // resume pag balik
        if (current instanceof MenuScreen) {
            ((MenuScreen) current).resumeMusic();
        } else if (current instanceof AustineBossBattle) {
            ((AustineBossBattle) current).resumeMusic();
        }
    }

    public boolean isWindowActive() {
        return windowActive;
    }

    public void setMaximizeWindowAction(Runnable action) {
        if (action == null) throw new IllegalArgumentException("action cannot be null");
        maximizeWindowAction = action;
    }

    private void setMaximized(boolean enabled) {
        boolean changed = Gdx.graphics.setWindowedMode(1280, 720);
        if (!changed) {
            Gdx.app.error("Main", "Unable to switch to " + (enabled ? "maximized" : "windowed") + " mode");
            return;
        }
        if (enabled) {
            if (maximizeWindowAction == null) {
                throw new IllegalStateException("Maximize window action was not configured");
            }
            maximizeWindowAction.run();
        }
        maximized = enabled;
        Gdx.app.getPreferences("kalapastangan-settings").putBoolean("maximized", enabled).flush();
    }

    public void transitionTo(Screen next) {
        if (next == null) throw new IllegalArgumentException("next screen cannot be null");
        if (transitionTarget != null) {
            throw new IllegalStateException("A screen transition is already in progress");
        }
        transitionTarget = next;
        transitionTime = 0f;
        transitionSwitched = false;
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            setMaximized(!maximized);
        }

        if (transitionTarget != null) {
            transitionTime += Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
            if (!transitionSwitched && transitionTime >= SCREEN_TRANSITION_DURATION) {
                Screen previous = getScreen();
                setScreen(transitionTarget);
                if (previous != null) previous.dispose();
                transitionSwitched = true;
                transitionTime = 0f;
            }
        }

        super.render(); // renders the current screen

        if (transitionTarget != null) {
            float progress = MathUtils.clamp(transitionTime / SCREEN_TRANSITION_DURATION, 0f, 1f);
            float alpha = transitionSwitched ? 1f - progress : progress;
            if (alpha > 0f) {
                int width = Gdx.graphics.getWidth();
                int height = Gdx.graphics.getHeight();
                batch.setProjectionMatrix(transitionProjection.setToOrtho2D(0f, 0f, width, height));
                batch.begin();
                batch.setColor(0f, 0f, 0f, alpha);
                batch.draw(transitionPixel, 0f, 0f, width, height);
                batch.setColor(Color.WHITE);
                batch.end();
            }
            if (transitionSwitched && progress >= 1f) {
                transitionTarget = null;
                transitionSwitched = false;
            }
        }
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) current.dispose();
        if (transitionTarget != null && transitionTarget != current) transitionTarget.dispose();
        transitionPixel.dispose();
        batch.dispose();
    }
}