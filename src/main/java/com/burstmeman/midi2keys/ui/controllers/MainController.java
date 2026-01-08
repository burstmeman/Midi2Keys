package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.services.MidiFileService;
import com.burstmeman.midi2keys.application.services.ProfileService;
import com.burstmeman.midi2keys.application.services.RootDirectoryService;
import com.burstmeman.midi2keys.application.services.TestModeService;
import com.burstmeman.midi2keys.application.usecases.*;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import com.burstmeman.midi2keys.Application;
import com.burstmeman.midi2keys.ui.components.TestModeWindow;
import com.burstmeman.midi2keys.ui.SpringFXMLLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main controller for the primary application view.
 * Coordinates between file browser, preview, playback, and profile management.
 */
@Slf4j
@Component
public class MainController implements Initializable {

    // State
    private final ObservableList<Object> fileListItems = FXCollections.observableArrayList();
    private final ObservableList<Profile> profileItems = FXCollections.observableArrayList();
    // Main layout containers
    @FXML
    private VBox fileBrowserPane;
    @FXML
    private VBox previewPane;
    @FXML
    private VBox controlsPane;
    @FXML
    private Label statusLabel;
    // File browser components
    @FXML
    private ComboBox<RootDirectory> rootDirectoryCombo;
    @FXML
    private TextField searchField;
    @FXML
    private HBox breadcrumbContainer;
    @FXML
    private Button navigateUpButton;
    @FXML
    private ListView<Object> fileListView;
    @FXML
    private Label fileBrowserStatusLabel;
    @FXML
    private VBox emptyStateContainer;
    // Preview components
    @FXML
    private Label fileNameLabel;
    @FXML
    private Label filePathLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private Label trackCountLabel;
    @FXML
    private Label totalNotesLabel;
    @FXML
    private Label tempoLabel;
    @FXML
    private ScrollPane previewContentPane;
    @FXML
    private VBox previewEmptyPane;
    @FXML
    private ComboBox<String> noteShiftCombo;
    // Playback components
    @FXML
    private ComboBox<Profile> profileCombo;
    @FXML
    private Button playButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button stopButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label timeLabel;
    @FXML
    private Label currentKeyLabel;
    @FXML
    private StackPane countdownOverlay;
    @FXML
    private Label countdownLabel;
    // Test mode indicator
    @FXML
    private HBox testModeIndicator;
    // Test mode window
    private TestModeWindow testModeWindow;
    // Services - injected by Spring
    @Autowired
    private RootDirectoryService rootDirectoryService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private MidiFileService midiFileService;
    @Autowired
    private ConfigureRootDirectoryUseCase configureRootDirectoryUseCase;
    @Autowired
    private BrowseMidiFilesUseCase browseMidiFilesUseCase;
    @Autowired
    private CreateProfileUseCase createProfileUseCase;
    @Autowired
    private AnalyzeMidiFileUseCase analyzeMidiFileUseCase;
    @Autowired
    private PlayMidiFileUseCase playMidiFileUseCase;
    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private TestModeService testModeService;
    private List<RootDirectory> rootDirectories;
    private RootDirectory currentRootDirectory;
    private String currentPath = "";
    private MidiFile selectedFile;
    private BrowseMidiFilesUseCase.FolderContents currentContents;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing MainController");

        // Services are injected by Spring via @Autowired

        // Initialize UI components
        initializeFileBrowser();
        initializePreview();
        initializePlayback();

        // Load initial data
        refreshRootDirectories();
        refreshProfiles();

        // Update test mode indicator
        updateTestModeIndicator();

        updateStatus("Ready - Configure a root directory to get started");
    }

    private void initializeFileBrowser() {
        // Setup file list
        if (fileListView != null) {
            fileListView.setItems(fileListItems);
            fileListView.setCellFactory(lv -> new FileListCell());

            fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal instanceof MidiFile midiFile) {
                    onFileSelected(midiFile);
                }
            });

            fileListView.setOnMouseClicked(this::handleFileListClick);
        }

        // Setup search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    performSearch(newVal);
                }
            });
        }

        // Setup root directory combo
        if (rootDirectoryCombo != null) {
            rootDirectoryCombo.setOnAction(e -> {
                RootDirectory selected = rootDirectoryCombo.getValue();
                if (selected != null && !selected.equals(currentRootDirectory)) {
                    switchRootDirectory(selected);
                }
            });
        }
    }

    private void initializePreview() {
        // Setup note shift combo
        if (noteShiftCombo != null) {
            noteShiftCombo.getItems().addAll(
                    "-4 semitones", "-3 semitones", "-2 semitones", "-1 semitone",
                    "No shift",
                    "+1 semitone", "+2 semitones", "+3 semitones", "+4 semitones"
            );
            noteShiftCombo.getSelectionModel().select(4); // Default to "No shift"
            noteShiftCombo.setOnAction(e -> onNoteShiftChanged());
        }

        showPreviewEmptyState();
    }

    private void initializePlayback() {
        // Setup profile combo
        if (profileCombo != null) {
            profileCombo.setItems(profileItems);
            profileCombo.setOnAction(e -> {
                Profile selected = profileCombo.getValue();
                if (selected != null) {
                    onProfileSelected(selected);
                }
            });
        }

        // Setup playback callbacks if use case is available
        if (playMidiFileUseCase != null) {
            playMidiFileUseCase.setOnStateChanged(this::onPlaybackStateChanged);
            playMidiFileUseCase.setOnProgressUpdated(this::onPlaybackProgress);
            playMidiFileUseCase.setOnCountdownTick(this::onCountdownTick);
            playMidiFileUseCase.setOnCountdownComplete(this::hideCountdown);
            playMidiFileUseCase.setOnNotePressed(this::onNotePressed);
        }

        // Initial button state
        updatePlaybackControls(false, false);
        hideCountdown();
    }

    // ========== Action Handlers ==========

    @FXML
    private void onSettingsClicked() {
        log.info("Settings button clicked");
        updateStatus("Opening settings...");
        openSettingsDialog();
    }

    @FXML
    private void onAddRootDirectory() {
        log.info("Add root directory clicked");
        updateStatus("Select a root directory for MIDI files...");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select MIDI Files Root Directory");

        File selectedDir = chooser.showDialog(getWindow());

        if (selectedDir != null) {
            try {
                var result = configureRootDirectoryUseCase.addRootDirectory(
                        selectedDir.getAbsolutePath(),
                        selectedDir.getName()
                );

                log.info("Added root directory: {} with {} files",
                        result.rootDirectory().getPath(), result.midiFileCount());

                refreshRootDirectories();
                switchRootDirectory(result.rootDirectory());

                ErrorHandler.showInfo("Directory Added",
                        String.format("Added '%s' with %d MIDI files found.",
                                result.rootDirectory().getName(), result.midiFileCount()));

                updateStatus("Root directory added: " + result.rootDirectory().getName());

            } catch (ApplicationException e) {
                ErrorHandler.handle(e);
                updateStatus("Failed to add root directory");
            }
        } else {
            updateStatus("Ready");
        }
    }

    @FXML
    private void onCreateProfile() {
        log.info("Create profile clicked");
        updateStatus("Opening profile manager...");
        openProfileManagerDialog();
    }

    @FXML
    private void onManageProfiles() {
        log.info("Manage profiles clicked");
        openProfileManagerDialog();
    }

    @FXML
    private void onNavigateUp() {
        if (currentRootDirectory == null || currentContents == null || !currentContents.canNavigateUp()) {
            return;
        }

        try {
            BrowseMidiFilesUseCase.FolderContents contents = browseMidiFilesUseCase.navigateToParent(
                    currentRootDirectory.getId(), currentPath);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onPlay() {
        if (selectedFile == null) {
            ErrorHandler.showWarning("No File", "Please select a MIDI file to play.");
            return;
        }

        Profile currentProfile = profileCombo != null ? profileCombo.getValue() : null;
        if (currentProfile == null) {
            ErrorHandler.showWarning("No Profile", "Please select a profile before playing.");
            return;
        }

        try {
            if (playMidiFileUseCase.isPaused()) {
                playMidiFileUseCase.resume();
            } else {
                playMidiFileUseCase.play(selectedFile.getId());
            }
            updateStatus("Playing: " + selectedFile.getFileName());
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
            updateStatus("Playback failed");
        }
    }

    @FXML
    private void onPause() {
        if (playMidiFileUseCase != null) {
            playMidiFileUseCase.pause();
            updateStatus("Paused");
        }
    }

    @FXML
    private void onStop() {
        if (playMidiFileUseCase != null) {
            playMidiFileUseCase.stop();
            updateStatus("Stopped");
        }
    }

    @FXML
    private void onToggleTestMode() {
        boolean currentMode = testModeService.isTestModeEnabled();
        boolean newMode = !currentMode;

        testModeService.setTestModeEnabled(newMode);
        updateTestModeIndicator();

        if (newMode) {
            // Enable test mode - open the window
            openTestModeWindow();
        } else {
            // Disable test mode - close the window
            closeTestModeWindow();
        }

        String status = newMode ?
                "Test mode enabled - no actual keystrokes" :
                "Test mode disabled - keystrokes will be sent";
        updateStatus(status);
    }

    private void openTestModeWindow() {
        if (testModeWindow == null) {
            testModeWindow = new TestModeWindow();
            testModeWindow.setOnClose(() -> {
                // When user closes the window, disable test mode
                testModeService.setTestModeEnabled(false);
                updateTestModeIndicator();
                updateStatus("Test mode disabled - keystrokes will be sent");
            });

            // Set up key press callback
            testModeService.setOnKeyPressed(keyDescription -> {
                if (testModeWindow != null) {
                    testModeWindow.logKeyPress(keyDescription);
                }
            });
        }
        testModeWindow.show();
    }

    private void closeTestModeWindow() {
        if (testModeWindow != null) {
            testModeWindow.close();
        }
    }

    // ========== Dialog Methods ==========

    private void openSettingsDialog() {
        try {
            SpringFXMLLoader loader = new SpringFXMLLoader(Application.getApplicationContext());
            Object[] controllerHolder = new Object[1];
            Parent root = loader.load("/com/burstmeman/midi2keys/ui/views/settings-view.fxml", controllerHolder);

            SettingsController controller = (SettingsController) controllerHolder[0];

            Stage dialogStage = createDialogStage("Settings", root, 600, 700);
            controller.setStage(dialogStage);
            controller.setDependencies(configureRootDirectoryUseCase, settingsRepository);
            controller.setOnSettingsChanged(v -> {
                refreshRootDirectories();
                refreshProfiles();
                updateTestModeIndicator();
            });

            dialogStage.showAndWait();

        } catch (IOException e) {
            log.error("Failed to open settings dialog", e);
            ErrorHandler.handle("Open Settings", e);
        }
    }

    private void openProfileManagerDialog() {
        try {
            SpringFXMLLoader loader = new SpringFXMLLoader(Application.getApplicationContext());
            Object[] controllerHolder = new Object[1];
            Parent root = loader.load("/com/burstmeman/midi2keys/ui/views/profile-manager-view.fxml", controllerHolder);

            ProfileManagerController controller = (ProfileManagerController) controllerHolder[0];

            Stage dialogStage = createDialogStage("Profile Manager", root, 700, 500);
            controller.setStage(dialogStage);
            controller.setDependencies(createProfileUseCase, profileService);
            controller.setOnProfileSelected(profile -> {
                refreshProfiles();
                if (profileCombo != null && profile != null) {
                    profileCombo.setValue(profile);
                }
            });

            dialogStage.showAndWait();
            refreshProfiles();

        } catch (IOException e) {
            log.error("Failed to open profile manager dialog", e);
            ErrorHandler.handle("Open Profile Manager", e);
        }
    }

    private Stage createDialogStage(String title, Parent root, double width, double height) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title + " - Midi2Keys");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(getWindow());

        Scene scene = new Scene(root, width, height);

        // Add stylesheet
        URL cssUrl = getClass().getResource("/styles/application.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        dialogStage.setScene(scene);
        dialogStage.setResizable(true);

        return dialogStage;
    }

    // ========== File Browser Logic ==========

    private void refreshRootDirectories() {
        rootDirectories = rootDirectoryService.getAllRootDirectories();

        if (rootDirectoryCombo != null) {
            rootDirectoryCombo.getItems().clear();
            rootDirectoryCombo.getItems().addAll(rootDirectories);

            if (currentRootDirectory != null) {
                rootDirectories.stream()
                        .filter(d -> d.getId().equals(currentRootDirectory.getId()))
                        .findFirst()
                        .ifPresent(d -> rootDirectoryCombo.setValue(d));
            } else if (!rootDirectories.isEmpty()) {
                rootDirectoryCombo.getSelectionModel().selectFirst();
                switchRootDirectory(rootDirectories.get(0));
            }
        }

        updateEmptyState();
    }

    private void switchRootDirectory(RootDirectory rootDirectory) {
        try {
            BrowseMidiFilesUseCase.FolderContents contents =
                    browseMidiFilesUseCase.switchRootDirectory(rootDirectory.getId());
            this.currentRootDirectory = rootDirectory;
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    private void displayFolderContents(BrowseMidiFilesUseCase.FolderContents contents) {
        this.currentContents = contents;
        this.currentPath = contents.currentPath();
        this.currentRootDirectory = contents.rootDirectory();

        // Update navigate up button
        if (navigateUpButton != null) {
            navigateUpButton.setDisable(!contents.canNavigateUp());
        }

        // Update breadcrumbs
        updateBreadcrumbs();

        // Update file list
        fileListItems.clear();

        // Add folders first
        for (String subdir : contents.subdirectories()) {
            fileListItems.add(new FolderItem(subdir));
        }

        // Add files
        fileListItems.addAll(contents.midiFiles());

        // Update status
        String status = String.format("%d folders, %d files",
                contents.subdirectories().size(), contents.midiFiles().size());
        updateFileBrowserStatus(status);

        updateEmptyState();
    }

    private void updateBreadcrumbs() {
        if (breadcrumbContainer == null || currentRootDirectory == null) {
            return;
        }

        breadcrumbContainer.getChildren().clear();

        List<BrowseMidiFilesUseCase.BreadcrumbItem> breadcrumbs =
                browseMidiFilesUseCase.getBreadcrumbs(currentRootDirectory, currentPath);

        for (int i = 0; i < breadcrumbs.size(); i++) {
            BrowseMidiFilesUseCase.BreadcrumbItem item = breadcrumbs.get(i);

            if (i > 0) {
                Label separator = new Label(" / ");
                separator.getStyleClass().add("breadcrumb-separator");
                breadcrumbContainer.getChildren().add(separator);
            }

            if (item.isNavigable()) {
                Button button = new Button(item.name());
                final String path = item.path();
                button.setOnAction(e -> navigateToPath(path));
                breadcrumbContainer.getChildren().add(button);
            } else {
                Label label = new Label(item.name());
                label.getStyleClass().add("breadcrumb-current");
                breadcrumbContainer.getChildren().add(label);
            }
        }
    }

    private void navigateToPath(String path) {
        if (currentRootDirectory == null) return;

        try {
            BrowseMidiFilesUseCase.FolderContents contents =
                    browseMidiFilesUseCase.getFolderContents(currentRootDirectory.getId(), path);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    private void handleFileListClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Object selected = fileListView.getSelectionModel().getSelectedItem();

            if (selected instanceof FolderItem folder) {
                navigateToSubfolder(folder.name());
            } else if (selected instanceof MidiFile midiFile) {
                onFileDoubleClicked(midiFile);
            }
        }
    }

    private void navigateToSubfolder(String folderName) {
        if (currentRootDirectory == null) return;

        try {
            BrowseMidiFilesUseCase.FolderContents contents = browseMidiFilesUseCase.navigateToSubfolder(
                    currentRootDirectory.getId(), currentPath, folderName);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    private void performSearch(String query) {
        if (currentRootDirectory == null || browseMidiFilesUseCase == null) return;

        if (query == null || query.isBlank()) {
            // Clear search, reload current folder
            if (currentContents != null) {
                displayFolderContents(currentContents);
            }
            return;
        }

        List<MidiFile> results = browseMidiFilesUseCase.searchMidiFiles(
                currentRootDirectory.getId(), query);

        fileListItems.clear();
        fileListItems.addAll(results);

        updateFileBrowserStatus(String.format("Found %d files matching '%s'", results.size(), query));
    }

    private void updateEmptyState() {
        boolean showEmpty = rootDirectories == null || rootDirectories.isEmpty();

        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(showEmpty);
            emptyStateContainer.setManaged(showEmpty);
        }

        if (fileListView != null) {
            fileListView.setVisible(!showEmpty);
            fileListView.setManaged(!showEmpty);
        }
    }

    // ========== Preview Logic ==========

    private void onFileSelected(MidiFile midiFile) {
        this.selectedFile = midiFile;
        showPreview(midiFile);
        updatePlaybackControls(true, false);
        updateStatus("Selected: " + midiFile.getFileName());
    }

    private void onFileDoubleClicked(MidiFile midiFile) {
        this.selectedFile = midiFile;
        showPreview(midiFile);

        // Start playback if a profile is selected
        if (profileCombo != null && profileCombo.getValue() != null) {
            onPlay();
        } else {
            ErrorHandler.showWarning("No Profile", "Please select a profile to play this file.");
        }
    }

    private void showPreview(MidiFile midiFile) {
        if (midiFile == null) {
            showPreviewEmptyState();
            return;
        }

        // Show content pane
        if (previewContentPane != null) {
            previewContentPane.setVisible(true);
            previewContentPane.setManaged(true);
        }
        if (previewEmptyPane != null) {
            previewEmptyPane.setVisible(false);
            previewEmptyPane.setManaged(false);
        }

        // Update file info
        if (fileNameLabel != null) {
            fileNameLabel.setText(midiFile.getFileName());
        }
        if (filePathLabel != null) {
            filePathLabel.setText(midiFile.getRelativePath());
        }

        // Load analysis asynchronously
        loadAnalysis(midiFile);

        // Update note shift combo
        if (noteShiftCombo != null && midiFile.getNoteShift() != null) {
            noteShiftCombo.getSelectionModel().select(midiFile.getNoteShift() + 4);
        } else if (noteShiftCombo != null) {
            noteShiftCombo.getSelectionModel().select(4); // "No shift"
        }
    }

    private void loadAnalysis(MidiFile midiFile) {
        // Run analysis in background
        Thread thread = new Thread(() -> {
            try {
                AnalyzeMidiFileUseCase.AnalysisResult result =
                        analyzeMidiFileUseCase.getAnalysis(midiFile.getId());

                Platform.runLater(() -> displayAnalysis(result));
            } catch (Exception e) {
                log.error("Failed to analyze file", e);
                Platform.runLater(() -> {
                    if (durationLabel != null) durationLabel.setText("Error loading");
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void displayAnalysis(AnalyzeMidiFileUseCase.AnalysisResult result) {
        var analysis = result.analysis();

        if (durationLabel != null) {
            durationLabel.setText(analysis.getFormattedDuration());
        }
        if (trackCountLabel != null) {
            trackCountLabel.setText(String.valueOf(analysis.getTrackCount()));
        }
        if (totalNotesLabel != null) {
            totalNotesLabel.setText(String.valueOf(analysis.getTotalNotes()));
        }
        if (tempoLabel != null) {
            tempoLabel.setText(String.format("%.0f BPM", analysis.getTempoBpm()));
        }
    }

    private void showPreviewEmptyState() {
        if (previewContentPane != null) {
            previewContentPane.setVisible(false);
            previewContentPane.setManaged(false);
        }
        if (previewEmptyPane != null) {
            previewEmptyPane.setVisible(true);
            previewEmptyPane.setManaged(true);
        }
    }

    private void onNoteShiftChanged() {
        if (selectedFile == null || noteShiftCombo == null || midiFileService == null) return;

        int selectedIndex = noteShiftCombo.getSelectionModel().getSelectedIndex();
        int noteShift = selectedIndex - 4; // Index 4 is "No shift" (0)

        try {
            midiFileService.updateNoteShift(selectedFile.getId(), noteShift);
            selectedFile.setNoteShift(noteShift);
            log.debug("Updated note shift to {} for file {}", noteShift, selectedFile.getId());
        } catch (Exception e) {
            ErrorHandler.handle("Update Note Shift", e);
        }
    }

    // ========== Playback Logic ==========

    private void refreshProfiles() {
        List<Profile> allProfiles = createProfileUseCase.getAllProfiles();
        profileItems.setAll(allProfiles);

        // Select current profile
        Profile current = profileService.getCurrentProfile();
        if (current != null && profileCombo != null) {
            profileItems.stream()
                    .filter(p -> p.getId().equals(current.getId()))
                    .findFirst()
                    .ifPresent(p -> profileCombo.setValue(p));
        } else if (!profileItems.isEmpty() && profileCombo != null) {
            profileCombo.getSelectionModel().selectFirst();
        }
    }

    private void onProfileSelected(Profile profile) {
        try {
            profileService.setCurrentProfile(profile);
            log.info("Selected profile: {}", profile.getName());
            updateStatus("Profile: " + profile.getName());
        } catch (Exception e) {
            log.error("Failed to select profile", e);
        }
    }

    private void onPlaybackStateChanged(com.burstmeman.midi2keys.application.services.PlaybackService.PlaybackState state) {
        Platform.runLater(() -> {
            boolean playing = state == com.burstmeman.midi2keys.application.services.PlaybackService.PlaybackState.PLAYING;
            boolean paused = state == com.burstmeman.midi2keys.application.services.PlaybackService.PlaybackState.PAUSED;

            updatePlaybackControls(selectedFile != null, playing || paused);

            if (playButton != null) {
                playButton.setVisible(!playing);
                playButton.setManaged(!playing);
                playButton.setText(paused ? "▶ Resume" : "▶ Play");
            }
            if (pauseButton != null) {
                pauseButton.setVisible(playing);
                pauseButton.setManaged(playing);
            }
            if (stopButton != null) {
                stopButton.setDisable(!playing && !paused);
            }

            if (state == com.burstmeman.midi2keys.application.services.PlaybackService.PlaybackState.STOPPED) {
                resetPlaybackProgress();
            }
        });
    }

    private void onPlaybackProgress(Long positionMs) {
        Platform.runLater(() -> {
            if (playMidiFileUseCase == null) return;

            long totalMs = playMidiFileUseCase.getTotalDurationMs();

            if (totalMs > 0 && progressBar != null) {
                progressBar.setProgress((double) positionMs / totalMs);
            }

            if (timeLabel != null) {
                timeLabel.setText(formatTime(positionMs) + " / " + formatTime(totalMs));
            }
        });
    }

    private void onCountdownTick(Integer seconds) {
        Platform.runLater(() -> showCountdown(seconds));
    }

    private void showCountdown(int seconds) {
        if (countdownOverlay != null) {
            countdownOverlay.setVisible(true);
            countdownOverlay.setManaged(true);
        }
        if (countdownLabel != null) {
            countdownLabel.setText(String.valueOf(seconds));
        }
        updateStatus("Starting in " + seconds + "...");
    }

    private void hideCountdown() {
        Platform.runLater(() -> {
            if (countdownOverlay != null) {
                countdownOverlay.setVisible(false);
                countdownOverlay.setManaged(false);
            }
        });
    }

    private void onNotePressed(String keyDescription) {
        Platform.runLater(() -> {
            if (currentKeyLabel != null) {
                currentKeyLabel.setText(keyDescription);
            }

            // If test mode is enabled, log the key press to the test mode window
            if (testModeService.isTestModeEnabled()) {
                // Ensure test mode window is initialized and shown
                if (testModeWindow == null || !testModeWindow.isShowing()) {
                    openTestModeWindow();
                }
                if (testModeWindow != null) {
                    testModeWindow.logKeyPress(keyDescription);
                }
            }
        });
    }

    private void updatePlaybackControls(boolean hasFile, boolean isPlayingOrPaused) {
        if (playButton != null) {
            playButton.setDisable(!hasFile);
        }
        if (stopButton != null) {
            stopButton.setDisable(!isPlayingOrPaused);
        }
    }

    private void resetPlaybackProgress() {
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        if (timeLabel != null) {
            timeLabel.setText("0:00 / 0:00");
        }
        if (currentKeyLabel != null) {
            currentKeyLabel.setText("-");
        }
    }

    private void updateTestModeIndicator() {
        if (testModeIndicator != null) {
            boolean testMode = testModeService.isTestModeEnabled();
            testModeIndicator.setVisible(testMode);
            testModeIndicator.setManaged(testMode);
        }
    }

    // ========== Utility Methods ==========

    public void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    private void updateFileBrowserStatus(String message) {
        if (fileBrowserStatusLabel != null) {
            Platform.runLater(() -> fileBrowserStatusLabel.setText(message));
        }
    }

    private Window getWindow() {
        if (statusLabel != null && statusLabel.getScene() != null) {
            return statusLabel.getScene().getWindow();
        }
        return null;
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Panic stop - immediately stops all playback.
     */
    public void panicStop() {
        log.warn("PANIC STOP triggered");
        if (playMidiFileUseCase != null) {
            playMidiFileUseCase.panicStop();
        }
        hideCountdown();
        updateStatus("STOPPED - All keys released");
    }

    // ========== Inner Classes ==========

    /**
     * Represents a folder item in the file list.
     */
    public record FolderItem(String name) {
        public String getDisplayName() {
            int lastSep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            return lastSep >= 0 ? name.substring(lastSep + 1) : name;
        }
    }

    /**
     * Custom cell for rendering files and folders.
     */
    private static class FileListCell extends ListCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("folder-item", "file-item");
                return;
            }

            if (item instanceof FolderItem folder) {
                setText("📁 " + folder.getDisplayName());
                getStyleClass().add("folder-item");
                getStyleClass().remove("file-item");
            } else if (item instanceof MidiFile file) {
                String display = "🎵 " + file.getFileName();
                if (file.hasNoteShift()) {
                    display += String.format(" [%+d]", file.getNoteShift());
                }
                setText(display);
                getStyleClass().add("file-item");
                getStyleClass().remove("folder-item");
            }
        }
    }
}
