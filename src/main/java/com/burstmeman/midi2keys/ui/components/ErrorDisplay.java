package com.burstmeman.midi2keys.ui.components;

import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Reusable component for displaying error states and messages.
 */
public class ErrorDisplay extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorDisplay.class);
    
    public enum ErrorType {
        INFO("info-message"),
        WARNING("warning-message"),
        ERROR("error-message"),
        CRITICAL("critical-message");
        
        private final String styleClass;
        
        ErrorType(String styleClass) {
            this.styleClass = styleClass;
        }
        
        String getStyleClass() {
            return styleClass;
        }
    }
    
    private Label iconLabel;
    private Label titleLabel;
    private Label messageLabel;
    private Button dismissButton;
    private Button retryButton;
    private HBox buttonBox;
    
    private Consumer<Void> onDismiss;
    private Consumer<Void> onRetry;
    private PauseTransition autoDismissTimer;
    
    public ErrorDisplay() {
        initializeComponents();
        setupLayout();
        applyStyles();
        setVisible(false);
        setManaged(false);
    }
    
    private void initializeComponents() {
        iconLabel = new Label();
        iconLabel.getStyleClass().add("error-icon");
        
        titleLabel = new Label();
        titleLabel.getStyleClass().add("error-title");
        
        messageLabel = new Label();
        messageLabel.getStyleClass().add("error-message-text");
        messageLabel.setWrapText(true);
        
        dismissButton = new Button("Dismiss");
        dismissButton.getStyleClass().add("dismiss-button");
        dismissButton.setOnAction(e -> dismiss());
        
        retryButton = new Button("Retry");
        retryButton.getStyleClass().add("retry-button");
        retryButton.setOnAction(e -> retry());
        retryButton.setVisible(false);
        retryButton.setManaged(false);
        
        buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(retryButton, dismissButton);
    }
    
    private void setupLayout() {
        setSpacing(8);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_LEFT);
        
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerBox.getChildren().addAll(iconLabel, titleLabel);
        
        VBox contentBox = new VBox(4);
        contentBox.getChildren().addAll(headerBox, messageLabel);
        
        HBox mainBox = new HBox(8);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        mainBox.getChildren().addAll(contentBox, buttonBox);
        
        getChildren().add(mainBox);
    }
    
    private void applyStyles() {
        getStyleClass().add("error-display");
    }
    
    public void showError(ErrorType type, String title, String message) {
        showError(type, title, message, false, 0);
    }
    
    public void showError(ErrorType type, String title, String message, int autoDismissSeconds) {
        showError(type, title, message, false, autoDismissSeconds);
    }
    
    public void showError(ErrorType type, String title, String message, boolean showRetry, int autoDismissSeconds) {
        if (autoDismissTimer != null) {
            autoDismissTimer.stop();
        }
        
        titleLabel.setText(title != null ? title : "Error");
        messageLabel.setText(message != null ? message : "An error occurred");
        iconLabel.setText(getIconForType(type));
        
        getStyleClass().removeAll("info-message", "warning-message", "error-message", "critical-message");
        getStyleClass().add(type.getStyleClass());
        
        retryButton.setVisible(showRetry);
        retryButton.setManaged(showRetry);
        
        setVisible(true);
        setManaged(true);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        if (autoDismissSeconds > 0) {
            autoDismissTimer = new PauseTransition(Duration.seconds(autoDismissSeconds));
            autoDismissTimer.setOnFinished(e -> dismiss());
            autoDismissTimer.play();
        }
        
        logger.debug("Showing {} error: {} - {}", type, title, message);
    }
    
    public void showException(Exception exception) {
        ErrorType type = ErrorType.ERROR;
        String title = "Error";
        String message = exception.getMessage();
        
        if (exception instanceof ApplicationException) {
            title = exception.getClass().getSimpleName().replace("Exception", " Error");
        }
        
        showError(type, title, message, true, 0);
    }
    
    public void dismiss() {
        if (autoDismissTimer != null) {
            autoDismissTimer.stop();
            autoDismissTimer = null;
        }
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
            if (onDismiss != null) {
                onDismiss.accept(null);
            }
        });
        fadeOut.play();
    }
    
    private void retry() {
        if (onRetry != null) {
            dismiss();
            onRetry.accept(null);
        }
    }
    
    public void setOnDismiss(Consumer<Void> callback) {
        this.onDismiss = callback;
    }
    
    public void setOnRetry(Consumer<Void> callback) {
        this.onRetry = callback;
    }
    
    public boolean isShowing() {
        return isVisible();
    }
    
    private String getIconForType(ErrorType type) {
        return switch (type) {
            case INFO -> "ℹ";
            case WARNING -> "⚠";
            case ERROR -> "✕";
            case CRITICAL -> "⛔";
        };
    }
    
    public void showInfo(String title, String message) {
        showError(ErrorType.INFO, title, message, 5);
    }
    
    public void showWarning(String title, String message) {
        showError(ErrorType.WARNING, title, message, 0);
    }
}
