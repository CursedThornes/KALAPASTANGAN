package com.java.KALAPASTANGAN.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.java.KALAPASTANGAN.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // Handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        Main game = new Main();
        game.setMaximizeWindowAction(() -> ((Lwjgl3Graphics) Gdx.graphics).getWindow().maximizeWindow());
        return new Lwjgl3Application(game, getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("KALAPASTANGAN");
        configuration.useVsync(true);
        configuration.setForegroundFPS(0);
        configuration.setAutoIconify(false);

        configuration.setWindowedMode(1280, 720);
        configuration.setMaximized(isMaximizedPreferred());
        configuration.setResizable(true);

        return configuration;
    }

    private static boolean isMaximizedPreferred() {
        File preferencesFile = new File(
            System.getProperty("user.home"), ".prefs" + File.separator + "kalapastangan-settings");
        if (!preferencesFile.exists()) return true;

        Properties preferences = new Properties();
        try (FileInputStream input = new FileInputStream(preferencesFile)) {
            preferences.loadFromXML(input);
            return Boolean.parseBoolean(preferences.getProperty("maximized", "true"));
        } catch (IOException exception) {
            System.err.println("Unable to read window preference from " + preferencesFile);
            exception.printStackTrace(System.err);
            return true;
        }
    }
}