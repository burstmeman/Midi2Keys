package com.burstmeman.midi2keys;

import com.burstmeman.midi2keys.application.services.GlobalHotkeyService;
import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import com.burstmeman.midi2keys.infrastructure.di.ServiceLocator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JavaFX Application class for Midi2Keys.
 * Handles application lifecycle, JMetro theme configuration, and primary stage setup.
 */
public class Application extends javafx.application.Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    private static final int MIN_WIDTH = 1024;
    private static final int MIN_HEIGHT = 768;
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 800;
    
    private ServiceLocator serviceLocator;
    
    @Override
    public void init() throws Exception {
        super.init();
        logger.info("Initializing Midi2Keys application...");
        
        // Ensure application directories exist
        ensureDirectoriesExist();
        
        // Initialize service locator (which handles database and all services)
        serviceLocator = ServiceLocator.getInstance();
        serviceLocator.initialize();
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Midi2Keys UI...");
        
        // Load main view
        Parent root = loadMainView();
        
        // Configure primary stage and create scene
        Scene scene = configureStage(primaryStage, root);
        
        // Apply JMetro theme (must be done after scene creation)
        ApplicationConfig.applyJMetroTheme(scene);
        
        // Register global hotkeys with the scene
        GlobalHotkeyService hotkeyService = serviceLocator.getGlobalHotkeyService();
        hotkeyService.registerWithScene(scene);
        
        // Show the stage
        primaryStage.show();
        
        logger.info("Midi2Keys application started successfully");
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Shutting down Midi2Keys application...");
        
        // Shutdown all services
        if (serviceLocator != null) {
            serviceLocator.shutdown();
        }
        
        super.stop();
        logger.info("Midi2Keys application stopped");
    }
    
    /**
     * Ensures all required application directories exist.
     */
    private void ensureDirectoriesExist() throws IOException {
        Path appDataDir = ApplicationConfig.getAppDataDirectory();
        Path profilesDir = ApplicationConfig.getProfilesDirectory();
        
        if (!Files.exists(appDataDir)) {
            Files.createDirectories(appDataDir);
            logger.info("Created application data directory: {}", appDataDir);
        }
        
        if (!Files.exists(profilesDir)) {
            Files.createDirectories(profilesDir);
            logger.info("Created profiles directory: {}", profilesDir);
        }
    }
    
    /**
     * Loads the main view FXML.
     * 
     * @return The root node of the main view
     * @throws IOException if FXML cannot be loaded
     */
    private Parent loadMainView() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/burstmeman/midi2keys/ui/views/main-view.fxml")
        );
        return loader.load();
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
    
    /**
     * Static method to launch the application.
     * Called from Main class.
     * 
     * @param args Command line arguments
     */
    public static void launch(String[] args) {
        javafx.application.Application.launch(Application.class, args);
    }
}
