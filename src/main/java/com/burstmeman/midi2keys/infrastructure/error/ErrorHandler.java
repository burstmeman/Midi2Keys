package com.burstmeman.midi2keys.infrastructure.error;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Centralized error handling for the application.
 * Provides consistent error display and logging.
 */
public final class ErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    
    private static Consumer<ErrorInfo> errorCallback;
    
    private ErrorHandler() {}
    
    /**
     * Handles an exception with user notification.
     * 
     * @param context Context description for logging
     * @param exception The exception to handle
     */
    public static void handle(String context, Throwable exception) {
        logger.error("{}: {}", context, exception.getMessage(), exception);
        
        ErrorInfo errorInfo;
        if (exception instanceof ApplicationException appEx) {
            errorInfo = new ErrorInfo(
                    appEx.getErrorCode().getCategory(),
                    appEx.getUserMessage(),
                    getActionableAdvice(appEx.getErrorCode()),
                    exception
            );
        } else {
            errorInfo = new ErrorInfo(
                    "Unexpected Error",
                    exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred",
                    "Please try again. If the problem persists, check the logs or contact support.",
                    exception
            );
        }
        
        notifyError(errorInfo);
    }
    
    /**
     * Handles an ApplicationException with user notification.
     * 
     * @param exception The application exception
     */
    public static void handle(ApplicationException exception) {
        handle(exception.getErrorCode().getCategory(), exception);
    }
    
    /**
     * Shows an error dialog to the user.
     * Must be called from JavaFX Application Thread.
     * 
     * @param errorInfo Error information to display
     */
    public static void showErrorDialog(ErrorInfo errorInfo) {
        Runnable showDialog = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(errorInfo.title());
            alert.setHeaderText(errorInfo.title());
            alert.setContentText(errorInfo.message() + "\n\n" + errorInfo.advice());
            
            // Add expandable exception details
            if (errorInfo.exception() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                errorInfo.exception().printStackTrace(pw);
                String exceptionText = sw.toString();
                
                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);
                
                GridPane expandableContent = new GridPane();
                expandableContent.setMaxWidth(Double.MAX_VALUE);
                expandableContent.add(textArea, 0, 0);
                
                alert.getDialogPane().setExpandableContent(expandableContent);
            }
            
            alert.showAndWait();
        };
        
        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            Platform.runLater(showDialog);
        }
    }
    
    /**
     * Shows a warning dialog.
     * 
     * @param title Warning title
     * @param message Warning message
     * @return true if user clicked OK
     */
    public static boolean showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Shows a confirmation dialog.
     * 
     * @param title Dialog title
     * @param message Confirmation message
     * @return true if user confirmed
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Shows an information dialog.
     * 
     * @param title Dialog title
     * @param message Information message
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Sets a callback to receive error notifications.
     * Useful for status bar updates.
     * 
     * @param callback Error callback function
     */
    public static void setErrorCallback(Consumer<ErrorInfo> callback) {
        errorCallback = callback;
    }
    
    private static void notifyError(ErrorInfo errorInfo) {
        if (errorCallback != null) {
            if (Platform.isFxApplicationThread()) {
                errorCallback.accept(errorInfo);
            } else {
                Platform.runLater(() -> errorCallback.accept(errorInfo));
            }
        }
    }
    
    private static String getActionableAdvice(ApplicationException.ErrorCode errorCode) {
        return switch (errorCode) {
            case FILE_NOT_FOUND -> "Check if the file exists and the path is correct.";
            case DIRECTORY_NOT_FOUND -> "Check if the directory exists and the path is correct.";
            case FILE_NOT_READABLE -> "Check file permissions or if the file is in use by another application.";
            case PERMISSION_DENIED -> "Run the application with appropriate permissions or check file ownership.";
            case PATH_TRAVERSAL -> "The path contains invalid characters. Please use a valid path.";
            case INVALID_MIDI_FILE -> "The file may be corrupted or not a valid MIDI file.";
            case UNSUPPORTED_MIDI_FORMAT -> "This MIDI format is not supported. Only Standard MIDI Files (SMF) are supported.";
            case MIDI_PARSE_ERROR -> "The MIDI file could not be read. It may be corrupted.";
            case PROFILE_NOT_FOUND -> "The profile may have been deleted or moved.";
            case PROFILE_SAVE_ERROR -> "Check if you have write permissions to the profiles directory.";
            case PROFILE_LOAD_ERROR -> "The profile file may be corrupted. Try creating a new profile.";
            case PROFILE_VALIDATION_ERROR -> "Please check your profile settings and correct any errors.";
            case MAPPING_CONFLICT -> "Two or more mappings conflict with each other. Please resolve the conflict.";
            case PLAYBACK_ERROR -> "Playback was interrupted. Please try again.";
            case KEYBOARD_SIMULATION_ERROR -> "The application cannot simulate keyboard input. Check system permissions.";
            case NO_PROFILE_SELECTED -> "Please select a profile before starting playback.";
            case DATABASE_ERROR -> "Database operation failed. Try restarting the application.";
            case INVALID_CONFIGURATION -> "Please check your application settings.";
            case ROOT_DIRECTORY_INVALID -> "The root directory no longer exists or is not accessible. Please update or remove it in Settings.";
            case UNKNOWN_ERROR -> "An unexpected error occurred. Please check the logs for details.";
            case VALIDATION_ERROR -> "Please correct the validation errors and try again.";
        };
    }
    
    /**
     * Container for error information.
     */
    public record ErrorInfo(String title, String message, String advice, Throwable exception) {}
}

