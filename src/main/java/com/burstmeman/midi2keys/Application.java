package com.burstmeman.midi2keys;

import com.burstmeman.midi2keys.application.services.GlobalHotkeyService;
import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import com.burstmeman.midi2keys.infrastructure.di.ShutdownHandler;
import com.burstmeman.midi2keys.infrastructure.di.SpringConfig;
import com.burstmeman.midi2keys.ui.SpringFXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JavaFX Application class for Midi2Keys.
 * Handles application lifecycle, JMetro theme configuration, and primary stage setup.
 */
@Slf4j
public class Application extends javafx.application.Application {

    private static final int MIN_WIDTH = 1024;
    private static final int MIN_HEIGHT = 768;
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 800;

    private static ApplicationContext applicationContext;
    private GlobalHotkeyService globalHotkeyService;

    /**
     * Static method to launch the application.
     * Called from Main class.
     *
     * @param args Command line arguments
     */
    public static void launch(String[] args) {
        javafx.application.Application.launch(Application.class, args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        log.info("Initializing Midi2Keys application...");

        // Ensure application directories exist
        ensureDirectoriesExist();

        // Initialize Spring application context with explicit configuration class
        // This avoids component scanning which can fail with class version issues
        applicationContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        
        log.info("Spring application context initialized");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting Midi2Keys UI...");

        // Load main view
        Parent root = loadMainView();

        // Configure primary stage and create scene
        Scene scene = configureStage(primaryStage, root);

        // Apply JMetro theme (must be done after scene creation)
        ApplicationConfig.applyJMetroTheme(scene);

        // Register global hotkeys with the scene
        globalHotkeyService = applicationContext.getBean(GlobalHotkeyService.class);
        globalHotkeyService.registerWithScene(scene);

        // Show the stage
        primaryStage.show();

        log.info("Midi2Keys application started successfully");
    }

    @Override
    public void stop() throws Exception {
        log.info("Shutting down Midi2Keys application...");

        // Explicitly shutdown all services before closing Spring context
        if (applicationContext != null) {
            try {
                ShutdownHandler shutdownHandler = applicationContext.getBean(ShutdownHandler.class);
                shutdownHandler.shutdown();
            } catch (Exception e) {
                log.error("Error during explicit shutdown", e);
            }

            // Close Spring context
            if (applicationContext instanceof AnnotationConfigApplicationContext) {
                ((AnnotationConfigApplicationContext) applicationContext).close();
            }
        }

        super.stop();
        log.info("Midi2Keys application stopped");
    }

    /**
     * Gets the Spring application context.
     * Used by controllers to access beans.
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Ensures all required application directories exist.
     */
    private void ensureDirectoriesExist() throws IOException {
        Path appDataDir = ApplicationConfig.getAppDataDirectory();
        Path profilesDir = ApplicationConfig.getProfilesDirectory();

        if (!Files.exists(appDataDir)) {
            Files.createDirectories(appDataDir);
            log.info("Created application data directory: {}", appDataDir);
        }

        if (!Files.exists(profilesDir)) {
            Files.createDirectories(profilesDir);
            log.info("Created profiles directory: {}", profilesDir);
        }
    }

    /**
     * Loads the main view FXML.
     *
     * @return The root node of the main view
     * @throws IOException if FXML cannot be loaded
     */
    private Parent loadMainView() throws IOException {
        SpringFXMLLoader loader = new SpringFXMLLoader(applicationContext);
        return loader.load("/com/burstmeman/midi2keys/ui/views/main-view.fxml");
    }

    /**
     * Configures the primary stage with appropriate settings.
     *
     * @param stage The primary stage
     * @param root  The root node to display
     * @return The configured scene (for theme and hotkey registration)
     */
    private Scene configureStage(Stage stage, Parent root) {
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        stage.setTitle(ApplicationConfig.getAppName() + " - MIDI to Keyboard Converter");
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        // Center on screen
        stage.centerOnScreen();

        return scene;
    }
}
