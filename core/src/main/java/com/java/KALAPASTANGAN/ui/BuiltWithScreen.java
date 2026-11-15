package com.java.KALAPASTANGAN.ui;
import com.java.KALAPASTANGAN.Main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// built with: java, libgdx, vscode. isa-isa

public class BuiltWithScreen extends ScreenAdapter {
    // Tweak these
    private static final String[] FILES = {
        "Images/Branding/BuiltWith/java.png",
        "Images/Branding/BuiltWith/libgdx.png",
        "Images/Branding/BuiltWith/vscode.png"
    };
    // taas ng logo. java/vscode malaki, libgdx mahaba kaya maliit
    private static final float[] HEIGHTS = { 520f, 300f, 520f };
    private static final float MAX_WIDTH = 1100f;    // any logo wider than this is shrunk to fit

    private static final float START_DELAY = 0.3f;   // black pause before the first logo
    private static final float FADE_IN =   0.7f;       // seconds
    private static final float HOLD = 1.0f;          // seconds fully visible
    private static final float FADE_OUT = 0.7f;      // seconds
    private static final float BLACK_BETWEEN = 0.2f; // black pause between logos

    private final Main game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);

    private final Texture[] logos = new Texture[FILES.length];
    private final float[] x = new float[FILES.length];
    private final float[] y = new float[FILES.length];
    private final float[] w = new float[FILES.length];
    private final float[] h = new float[FILES.length];

    private float time = 0f;
    private boolean transitionStarted = false;

    public BuiltWithScreen(Main game) {
        this.game = game;

        for (int i = 0; i < FILES.length; i++) {
            // Mipmaps keep the logos smooth when they are scaled down.
            logos[i] = new Texture(Gdx.files.internal(FILES[i]), true);
            logos[i].setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

            h[i] = HEIGHTS[i];
            w[i] = h[i] * logos[i].getWidth() / logos[i].getHeight(); // keep the original proportions

            if (w[i] > MAX_WIDTH) { // too wide for the screen: shrink, keeping proportions
                float shrink = MAX_WIDTH / w[i];
                w[i] *= shrink;
                h[i] *= shrink;
            }

            // Centre each logo on the screen.
            x[i] = (Main.WORLD_WIDTH - w[i]) / 2f;
            y[i] = (Main.WORLD_HEIGHT - h[i]) / 2f;
        }
    }

    @Override
    public void render(float delta) {
        time += delta;

        float slot = FADE_IN + HOLD + FADE_OUT + BLACK_BETWEEN; // time given to each logo
        float t = time - START_DELAY;

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        if (t >= 0f) {
            int index = (int) (t / slot);
            if (index >= logos.length) {
                if (!transitionStarted) {
                    transitionStarted = true;
                    game.transitionTo(new MenuScreen(game));
                }
                return;
            }

            float local = t - index * slot; // seconds into this logo's slot
            float alpha;
            if (local < FADE_IN) {
                alpha = local / FADE_IN;
            } else if (local < FADE_IN + HOLD) {
                alpha = 1f;
            } else if (local < FADE_IN + HOLD + FADE_OUT) {
                alpha = 1f - (local - FADE_IN - HOLD) / FADE_OUT;
            } else {
                alpha = 0f; // black pause before the next logo
            }
            alpha = Interpolation.smooth.apply(alpha); // softer start and end

            viewport.apply();
            game.batch.setProjectionMatrix(camera.combined);
            game.batch.begin();
            game.batch.setColor(1f, 1f, 1f, alpha);
            game.batch.draw(logos[index], x[index], y[index], w[index], h[index]);
            game.batch.setColor(Color.WHITE);
            game.batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        for (Texture logo : logos) logo.dispose();
    }
}