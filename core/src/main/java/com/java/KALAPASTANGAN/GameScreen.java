package com.java.KALAPASTANGAN;
import com.java.KALAPASTANGAN.ui.MenuScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// placeholder gameplay, wala pa
public class GameScreen extends ScreenAdapter {
    private static final float PLAYER_SIZE = 64f;
    private static final float PLAYER_SPEED = 300f; // units per second

    private final Main game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);
    private final Texture playerTexture;

    private float playerX = Main.WORLD_WIDTH / 2f - PLAYER_SIZE / 2f;
    private float playerY = Main.WORLD_HEIGHT / 2f - PLAYER_SIZE / 2f;

    public GameScreen(Main game) {
        this.game = game;

        // placeholder player. TODO: palitan ng player.png
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        playerTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { // ESC = back to the menu
            game.transitionTo(new MenuScreen(game));
            return;
        }
        handleInput(delta);

        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(playerTexture, playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
        game.batch.end();
    }

    private void handleInput(float delta) {
        // galaw: WASD o arrow
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerX -= PLAYER_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerX += PLAYER_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerY += PLAYER_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerY -= PLAYER_SPEED * delta;
        }

        // wag lumabas sa mundo
        playerX = MathUtils.clamp(playerX, 0, Main.WORLD_WIDTH - PLAYER_SIZE);
        playerY = MathUtils.clamp(playerY, 0, Main.WORLD_HEIGHT - PLAYER_SIZE);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        playerTexture.dispose();
    }
}