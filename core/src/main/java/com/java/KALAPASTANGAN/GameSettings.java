package com.java.KALAPASTANGAN;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

public final class GameSettings {
    private static final String PREFERENCES_NAME = "kalapastangan-settings";

    public static float music = 1f;
    public static float sfx = 1f;
    public static float textSpeed = 1f;
    // control sa laban: false = WASD, true = cursor
    public static boolean battleMouse;

    private GameSettings() {
    }

    public static void load() {
        Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        music = MathUtils.clamp(preferences.getFloat("music", 1f), 0f, 1f);
        sfx = MathUtils.clamp(preferences.getFloat("sfx", 1f), 0f, 1f);
        textSpeed = MathUtils.clamp(preferences.getFloat("textSpeed", 1f), 0.5f, 2f);
        battleMouse = preferences.getBoolean("battleMouse", false);
    }

    public static void save() {
        Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        preferences.putFloat("music", MathUtils.clamp(music, 0f, 1f));
        preferences.putFloat("sfx", MathUtils.clamp(sfx, 0f, 1f));
        preferences.putFloat("textSpeed", MathUtils.clamp(textSpeed, 0.5f, 2f));
        preferences.putBoolean("battleMouse", battleMouse);
        preferences.flush();
    }
}
