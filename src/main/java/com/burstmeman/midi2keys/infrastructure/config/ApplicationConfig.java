package com.burstmeman.midi2keys.infrastructure.config;

import javafx.scene.Scene;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application-wide configuration settings.
 * Handles JMetro theme initialization and path configuration.
 */
@Slf4j
public final class ApplicationConfig {

    // Default settings
    public static final int DEFAULT_COUNTDOWN_SECONDS = 3;
    public static final int MIN_COUNTDOWN_SECONDS = 1;
    public static final int MAX_COUNTDOWN_SECONDS = 10;
    // Panic stop default hotkey
    public static final String DEFAULT_PANIC_STOP_HOTKEY = "Ctrl+Shift+Escape";
    // Application paths
    private static final String APP_NAME = "Midi2Keys";
    private static final String APP_DATA_DIR = "midi2keys";
    private static final String DATABASE_NAME = "midi2keys.db";
    private static final String PROFILES_DIR = "profiles";
    // JMetro instance
    private static JMetro jMetro;

    private ApplicationConfig() {
        // Utility class - prevent instantiation
    }

    /**
     * Applies JMetro dark theme to the given scene.
     * JMetro provides a Fluent Design inspired theme for JavaFX.
     *
     * @param scene The scene to apply the theme to
     */
    public static void applyJMetroTheme(Scene scene) {
        log.info("Applying JMetro dark theme...");

        jMetro = new JMetro(Style.DARK);
        jMetro.setScene(scene);

        log.info("JMetro theme applied successfully");
    }

    /**
     * Gets the current JMetro instance.
     *
     * @return The JMetro instance, or null if not initialized
     */
    public static JMetro getJMetro() {
        return jMetro;
    }

    /**
     * Switches between light and dark themes.
     *
     * @param dark true for dark theme, false for light theme
     */
    public static void setDarkMode(boolean dark) {
        if (jMetro != null) {
            jMetro.setStyle(dark ? Style.DARK : Style.LIGHT);
            log.info("Switched to {} theme", dark ? "dark" : "light");
        }
    }

    /**
     * Gets the application data directory path.
     * Creates the directory if it doesn't exist.
     *
     * @return Path to application data directory
     */
    public static Path getAppDataDirectory() {
        String userHome = System.getProperty("user.home");
        Path appDataPath;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: %APPDATA%\Midi2Keys
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                appDataPath = Paths.get(appData, APP_DATA_DIR);
            } else {
                appDataPath = Paths.get(userHome, "AppData", "Roaming", APP_DATA_DIR);
            }
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/Midi2Keys
            appDataPath = Paths.get(userHome, "Library", "Application Support", APP_DATA_DIR);
        } else {
            // Linux/Unix: ~/.midi2keys
            appDataPath = Paths.get(userHome, "." + APP_DATA_DIR);
        }

        return appDataPath;
    }

    /**
     * Gets the database file path.
     *
     * @return Path to SQLite database file
     */
    public static Path getDatabasePath() {
        return getAppDataDirectory().resolve(DATABASE_NAME);
    }

    /**
     * Gets the profiles directory path.
     *
     * @return Path to profiles directory
     */
    public static Path getProfilesDirectory() {
        return getAppDataDirectory().resolve(PROFILES_DIR);
    }

    /**
     * Gets the application name.
     *
     * @return Application name
     */
    public static String getAppName() {
        return APP_NAME;
    }

    /**
     * Gets the application version.
     *
     * @return Application version string
     */
    public static String getAppVersion() {
        return "1.0-SNAPSHOT";
    }
}
