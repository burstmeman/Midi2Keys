package com.burstmeman.midi2keys.ui.components;

import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A separate window that displays all pressed keys during test mode.
 * Keys are accumulated and displayed one per line.
 */
public class TestModeWindow {
    
    private static final Logger logger = LoggerFactory.getLogger(TestModeWindow.class);
    
    private Stage stage;
    private TextArea keyLogArea;
    private final StringBuilder keyLog = new StringBuilder();
    private int keyCount = 0;
    
    private Runnable onCloseCallback;
    
    /**
     * Creates and shows the test mode window.
     */
    public void show() {
        if (stage != null && stage.isShowing()) {
            stage.requestFocus();
            return;
        }
        
        createWindow();
        stage.show();
        logger.info("Test mode window opened");
    }
    
    /**
     * Closes the test mode window.
     */
    public void close() {
        if (stage != null) {
            stage.close();
            stage = null;
            logger.info("Test mode window closed");
        }
    }
    
    /**
     * Returns whether the window is currently showing.
     */
    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }
    
    /**
     * Sets a callback to be invoked when the window is closed.
     */
    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    /**
     * Logs a key press to the window.
     * 
     * @param keyDescription The key or key combination that was pressed (e.g., "Q", "Ctrl+E")
     */
    public void logKeyPress(String keyDescription) {
        Platform.runLater(() -> {
            keyCount++;
            keyLog.append(keyDescription).append("\n");
            
            if (keyLogArea != null) {
                keyLogArea.setText(keyLog.toString());
                // Auto-scroll to bottom
                keyLogArea.setScrollTop(Double.MAX_VALUE);
                keyLogArea.positionCaret(keyLog.length());
            }
        });
    }
    
    /**
     * Clears all logged keys.
     */
    public void clearLog() {
        Platform.runLater(() -> {
            keyLog.setLength(0);
            keyCount = 0;
            if (keyLogArea != null) {
                keyLogArea.clear();
            }
        });
    }
    
    /**
     * Gets the count of logged keys.
     */
    public int getKeyCount() {
        return keyCount;
    }
    
    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Test Mode - Key Log");
        stage.initStyle(StageStyle.DECORATED);
        stage.setAlwaysOnTop(true);
        
        // Main container
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        
        // Header
        Label headerLabel = new Label("⚠ TEST MODE ACTIVE");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff9800;");
        
        Label descLabel = new Label("No actual keystrokes are being sent. Keys pressed during playback will appear below.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        // Key count label
        Label countLabel = new Label("Keys logged: 0");
        countLabel.setStyle("-fx-font-size: 12px;");
        
        // Key log text area
        keyLogArea = new TextArea();
        keyLogArea.setEditable(false);
        keyLogArea.setWrapText(false);
        keyLogArea.setPromptText("Keys will appear here as they are pressed...");
        keyLogArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 14px;");
        VBox.setVgrow(keyLogArea, Priority.ALWAYS);
        
        // Restore any existing log
        if (keyLog.length() > 0) {
            keyLogArea.setText(keyLog.toString());
        }
        
        // Update count label when log changes
        keyLogArea.textProperty().addListener((obs, old, newText) -> {
            countLabel.setText("Keys logged: " + keyCount);
        });
        
        // Button row
        HBox buttonRow = new HBox(12);
        
        Button clearButton = new Button("Clear Log");
        clearButton.setOnAction(e -> clearLog());
        
        Button closeButton = new Button("Close & Disable Test Mode");
        closeButton.setStyle("-fx-text-fill: #f44336;");
        closeButton.setOnAction(e -> {
            close();
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
        
        buttonRow.getChildren().addAll(clearButton, closeButton);
        
        // Assemble layout
        root.getChildren().addAll(
                headerLabel,
                descLabel,
                countLabel,
                keyLogArea,
                buttonRow
        );
        
        // Create scene
        Scene scene = new Scene(root, 400, 500);
        
        // Apply JMetro theme
        ApplicationConfig.applyJMetroTheme(scene);
        
        stage.setScene(scene);
        stage.setMinWidth(300);
        stage.setMinHeight(300);
        
        // Handle close button
        stage.setOnCloseRequest(e -> {
            logger.info("Test mode window closed by user");
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
    }
}

