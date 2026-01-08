package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.usecases.ConfigureRootDirectoryUseCase;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the Settings screen.
 * Manages root directory configuration and application settings.
 */
@Slf4j
@Component
@Scope("prototype")
public class SettingsController implements Initializable {

    @Autowired
    private ConfigureRootDirectoryUseCase configureRootDirectoryUseCase;
    @Autowired
    private SettingsRepository settingsRepository;

    @FXML
    private VBox rootDirectoriesContainer;
    @FXML
    private Button addDirectoryButton;
    @FXML
    private Slider countdownSlider;
    @FXML
    private Label countdownValueLabel;
    @FXML
    private TextField panicHotkeyField;
    @FXML
    private CheckBox testModeToggle;
    @FXML
    private Button saveButton;
    @FXML
    private Button closeButton;

    private Consumer<Void> onSettingsChanged;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing SettingsController");

        // Setup countdown slider
        if (countdownSlider != null) {
            countdownSlider.setMin(ApplicationConfig.MIN_COUNTDOWN_SECONDS);
            countdownSlider.setMax(ApplicationConfig.MAX_COUNTDOWN_SECONDS);
            countdownSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (countdownValueLabel != null) {
                    countdownValueLabel.setText(String.format("%.0f seconds", newVal.doubleValue()));
                }
            });
        }
    }

    /**
     * Injects dependencies into the controller.
     * Kept for backward compatibility, but dependencies are now injected via @Autowired.
     */
    public void setDependencies(ConfigureRootDirectoryUseCase configureRootDirectoryUseCase,
                                SettingsRepository settingsRepository) {
        // Dependencies are already injected via @Autowired, just load settings
        loadSettings();
    }

    /**
     * Sets a callback for when settings are changed.
     */
    public void setOnSettingsChanged(Consumer<Void> callback) {
        this.onSettingsChanged = callback;
    }

    /**
     * Loads current settings into the UI.
     */
    public void loadSettings() {
        if (configureRootDirectoryUseCase == null || settingsRepository == null) {
            return;
        }

        // Load root directories
        refreshRootDirectoryList();

        // Load countdown setting
        if (countdownSlider != null) {
            int countdown = settingsRepository.getCountdownSeconds();
            countdownSlider.setValue(countdown);
        }

        // Load panic hotkey
        if (panicHotkeyField != null) {
            panicHotkeyField.setText(settingsRepository.getPanicStopHotkey());
        }

        // Load test mode
        if (testModeToggle != null) {
            testModeToggle.setSelected(settingsRepository.isTestModeEnabled());
        }
    }

    @FXML
    private void onAddDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select MIDI Files Root Directory");

        File selectedDir = chooser.showDialog(getStage());

        if (selectedDir != null) {
            try {
                var result = configureRootDirectoryUseCase.addRootDirectory(
                        selectedDir.getAbsolutePath(),
                        selectedDir.getName()
                );

                log.info("Added root directory: {} with {} files",
                        result.rootDirectory().getPath(), result.midiFileCount());

                refreshRootDirectoryList();
                notifySettingsChanged();

                ErrorHandler.showInfo("Directory Added",
                        String.format("Added '%s' with %d MIDI files found.",
                                result.rootDirectory().getName(), result.midiFileCount()));

            } catch (ApplicationException e) {
                ErrorHandler.handle(e);
            }
        }
    }

    @FXML
    private void onSave() {
        try {
            // Save countdown setting
            if (countdownSlider != null) {
                settingsRepository.saveCountdownSeconds((int) countdownSlider.getValue());
            }

            // Save panic hotkey
            if (panicHotkeyField != null && panicHotkeyField.getText() != null) {
                settingsRepository.savePanicStopHotkey(panicHotkeyField.getText().trim());
            }

            // Save test mode
            if (testModeToggle != null) {
                settingsRepository.setTestModeEnabled(testModeToggle.isSelected());
            }

            log.info("Settings saved");
            notifySettingsChanged();

            if (stage != null) {
                stage.close();
            }

        } catch (Exception e) {
            ErrorHandler.handle("Save Settings", e);
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void refreshRootDirectoryList() {
        if (rootDirectoriesContainer == null) {
            return;
        }

        rootDirectoriesContainer.getChildren().clear();

        List<RootDirectory> directories = configureRootDirectoryUseCase.getAllRootDirectories();

        if (directories.isEmpty()) {
            Label emptyLabel = new Label("No root directories configured. Click 'Add Directory' to get started.");
            emptyLabel.setStyle("-fx-text-fill: gray;");
            rootDirectoriesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (RootDirectory dir : directories) {
            HBox row = createDirectoryRow(dir);
            rootDirectoriesContainer.getChildren().add(row);
        }
    }

    private HBox createDirectoryRow(RootDirectory directory) {
        HBox row = new HBox(16);
        row.setStyle("-fx-padding: 8; -fx-background-color: derive(-fx-base, 10%); -fx-background-radius: 4;");

        VBox info = new VBox(4);
        Label nameLabel = new Label(directory.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label pathLabel = new Label(directory.getPath());
        pathLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        Label statusLabel = new Label(directory.isActive() ? "✓ Active" : "⚠ Inactive");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (directory.isActive() ? "#4caf50" : "#ff9800") + ";");

        info.getChildren().addAll(nameLabel, pathLabel, statusLabel);

        Button rescanButton = new Button("Rescan");
        rescanButton.setOnAction(e -> onRescanDirectory(directory));

        Button removeButton = new Button("Remove");
        removeButton.setStyle("-fx-text-fill: #f44336;");
        removeButton.setOnAction(e -> onRemoveDirectory(directory));

        row.getChildren().addAll(info, rescanButton, removeButton);

        return row;
    }

    private void onRescanDirectory(RootDirectory directory) {
        try {
            int fileCount = configureRootDirectoryUseCase.rescanRootDirectory(directory.getId());
            ErrorHandler.showInfo("Rescan Complete",
                    String.format("Found %d MIDI files in '%s'.", fileCount, directory.getName()));
            notifySettingsChanged();
        } catch (Exception e) {
            ErrorHandler.handle("Rescan Directory", e);
        }
    }

    private void onRemoveDirectory(RootDirectory directory) {
        boolean confirmed = ErrorHandler.showConfirmation("Remove Directory",
                String.format("Are you sure you want to remove '%s'?\n\nThis will not delete any files, only remove it from the application.",
                        directory.getName()));

        if (confirmed) {
            try {
                configureRootDirectoryUseCase.removeRootDirectory(directory.getId());
                refreshRootDirectoryList();
                notifySettingsChanged();
            } catch (Exception e) {
                ErrorHandler.handle("Remove Directory", e);
            }
        }
    }

    private Stage getStage() {
        if (stage != null) {
            return stage;
        }
        if (rootDirectoriesContainer != null && rootDirectoriesContainer.getScene() != null) {
            return (Stage) rootDirectoriesContainer.getScene().getWindow();
        }
        return null;
    }

    /**
     * Sets the stage for this dialog.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void notifySettingsChanged() {
        if (onSettingsChanged != null) {
            Platform.runLater(() -> onSettingsChanged.accept(null));
        }
    }
}
