package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.services.ProfileService;
import com.burstmeman.midi2keys.domain.entities.NoteMapping;
import com.burstmeman.midi2keys.domain.entities.PlaybackOptions;
import com.burstmeman.midi2keys.domain.entities.PlaybackOptions.Quantization;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.valueobjects.KeyCombination;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller for the Profile Editor view.
 * Allows editing note mappings and playback options.
 */
@Slf4j
public class ProfileEditorController implements Initializable {

    private final CheckBox[] channelToggles = new CheckBox[16];
    private final ObservableList<NoteMapping> mappings = FXCollections.observableArrayList();
    // Profile info
    @FXML
    private TextField profileNameField;
    @FXML
    private TextField descriptionField;
    // Mappings section
    @FXML
    private ListView<NoteMapping> mappingListView;
    @FXML
    private TextField noteNumberField;
    @FXML
    private TextField keyField;
    @FXML
    private ComboBox<String> channelCombo;
    @FXML
    private Button addMappingButton;
    @FXML
    private Button removeMappingButton;
    // Playback options
    @FXML
    private Slider tempoSlider;
    @FXML
    private Label tempoValueLabel;
    @FXML
    private ComboBox<Quantization> quantizationCombo;
    @FXML
    private Slider velocityThresholdSlider;
    @FXML
    private Label velocityValueLabel;
    @FXML
    private Slider transposeSlider;
    @FXML
    private Label transposeValueLabel;
    @FXML
    private Slider keyDurationSlider;
    @FXML
    private Label keyDurationValueLabel;
    // Channel toggles
    @FXML
    private VBox channelTogglesContainer;
    // Actions
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    private ProfileService profileService;
    private Profile profile;
    private Stage stage;
    private Consumer<Profile> onSaved;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing ProfileEditorController");

        // Setup mapping list
        if (mappingListView != null) {
            mappingListView.setItems(mappings);
            mappingListView.setCellFactory(lv -> new MappingListCell());
            mappingListView.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldVal, newVal) -> updateMappingButtons());
        }

        // Setup channel combo
        if (channelCombo != null) {
            List<String> channels = new ArrayList<>();
            channels.add("Any Channel");
            for (int i = 1; i <= 16; i++) {
                channels.add("Channel " + i);
            }
            channelCombo.getItems().addAll(channels);
            channelCombo.getSelectionModel().selectFirst();
        }

        // Setup quantization combo
        if (quantizationCombo != null) {
            quantizationCombo.getItems().addAll(Quantization.values());
            quantizationCombo.getSelectionModel().selectFirst();
        }

        // Setup sliders
        setupSliders();

        // Setup channel toggles
        setupChannelToggles();
    }

    /**
     * Sets dependencies and profile to edit.
     */
    public void setDependencies(ProfileService profileService, Profile profile) {
        this.profileService = profileService;
        this.profile = profile;
        loadProfile();
    }

    /**
     * Sets the stage for this dialog.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Sets callback for when profile is saved.
     */
    public void setOnSaved(Consumer<Profile> callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onAddMapping() {
        try {
            String noteStr = noteNumberField != null ? noteNumberField.getText() : null;
            String keyStr = keyField != null ? keyField.getText() : null;

            if (noteStr == null || noteStr.isBlank()) {
                ErrorHandler.showWarning("Invalid Input", "Please enter a MIDI note number (0-127).");
                return;
            }

            if (keyStr == null || keyStr.isBlank()) {
                ErrorHandler.showWarning("Invalid Input", "Please enter a keyboard key.");
                return;
            }

            int noteNumber;
            try {
                noteNumber = Integer.parseInt(noteStr.trim());
                if (noteNumber < 0 || noteNumber > 127) {
                    throw new NumberFormatException("Out of range");
                }
            } catch (NumberFormatException e) {
                ErrorHandler.showWarning("Invalid Input", "Note number must be 0-127.");
                return;
            }

            int channel = -1; // Any channel
            if (channelCombo != null) {
                int selectedIndex = channelCombo.getSelectionModel().getSelectedIndex();
                if (selectedIndex > 0) {
                    channel = selectedIndex - 1;
                }
            }

            MidiNote midiNote = new MidiNote(noteNumber);
            KeyCombination keyComb = KeyCombination.parse(keyStr.trim());
            NoteMapping mapping = new NoteMapping(midiNote, keyComb, channel);

            // Check for conflicts
            for (NoteMapping existing : mappings) {
                if (existing.conflictsWith(mapping)) {
                    ErrorHandler.showWarning("Conflict",
                            "This mapping conflicts with: " + existing.getDescription());
                    return;
                }
            }

            mappings.add(mapping);
            clearMappingInputs();

        } catch (IllegalArgumentException e) {
            ErrorHandler.showWarning("Invalid Input", e.getMessage());
        }
    }

    @FXML
    private void onRemoveMapping() {
        NoteMapping selected = mappingListView != null ?
                mappingListView.getSelectionModel().getSelectedItem() : null;

        if (selected != null) {
            mappings.remove(selected);
        }
    }

    @FXML
    private void onSave() {
        try {
            // Update profile name
            if (profileNameField != null && !profileNameField.getText().isBlank()) {
                profile.setName(profileNameField.getText().trim());
            }

            // Update description
            if (descriptionField != null) {
                profile.setDescription(descriptionField.getText());
            }

            // Update mappings
            profile.clearMappings();
            for (NoteMapping mapping : mappings) {
                profile.addMapping(mapping);
            }

            // Update playback options
            PlaybackOptions options = buildPlaybackOptions();
            profile.setPlaybackOptions(options);

            // Validate
            List<String> errors = profile.validate();
            if (!errors.isEmpty() && !errors.get(0).contains("no mappings")) {
                ErrorHandler.showWarning("Validation Errors", String.join("\n", errors));
                return;
            }

            // Save
            Profile saved = profileService.updateProfile(profile);

            if (onSaved != null) {
                onSaved.accept(saved);
            }

            if (stage != null) {
                stage.close();
            }

        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onCancel() {
        if (stage != null) {
            stage.close();
        }
    }

    private void loadProfile() {
        if (profile == null) return;

        // Load basic info
        if (profileNameField != null) {
            profileNameField.setText(profile.getName());
        }

        if (descriptionField != null) {
            descriptionField.setText(profile.getDescription());
        }

        // Load mappings
        mappings.setAll(profile.getNoteMappings());

        // Load playback options
        PlaybackOptions opts = profile.getPlaybackOptions();

        if (tempoSlider != null) {
            tempoSlider.setValue(opts.getTempoMultiplier() * 100);
        }

        if (quantizationCombo != null) {
            quantizationCombo.setValue(opts.getQuantization());
        }

        if (velocityThresholdSlider != null) {
            velocityThresholdSlider.setValue(opts.getMinVelocityThreshold());
        }

        if (transposeSlider != null) {
            transposeSlider.setValue(opts.getTranspose());
        }

        if (keyDurationSlider != null) {
            keyDurationSlider.setValue(opts.getKeyPressDurationMs());
        }

        // Load ignored channels
        Set<Integer> ignored = opts.getIgnoredChannels();
        for (int i = 0; i < 16; i++) {
            if (channelToggles[i] != null) {
                channelToggles[i].setSelected(!ignored.contains(i));
            }
        }
    }

    private PlaybackOptions buildPlaybackOptions() {
        PlaybackOptions opts = new PlaybackOptions();

        if (tempoSlider != null) {
            opts.setTempoMultiplier(tempoSlider.getValue() / 100.0);
        }

        if (quantizationCombo != null && quantizationCombo.getValue() != null) {
            opts.setQuantization(quantizationCombo.getValue());
        }

        if (velocityThresholdSlider != null) {
            opts.setMinVelocityThreshold((int) velocityThresholdSlider.getValue());
        }

        if (transposeSlider != null) {
            opts.setTranspose((int) transposeSlider.getValue());
        }

        if (keyDurationSlider != null) {
            opts.setKeyPressDurationMs((int) keyDurationSlider.getValue());
        }

        // Ignored channels
        Set<Integer> ignored = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            if (channelToggles[i] != null && !channelToggles[i].isSelected()) {
                ignored.add(i);
            }
        }
        opts.setIgnoredChannels(ignored);

        return opts;
    }

    private void setupSliders() {
        // Tempo slider: 25% to 400%
        if (tempoSlider != null) {
            tempoSlider.setMin(25);
            tempoSlider.setMax(400);
            tempoSlider.setValue(100);
            tempoSlider.valueProperty().addListener((obs, old, val) -> {
                if (tempoValueLabel != null) {
                    tempoValueLabel.setText(String.format("%.0f%%", val.doubleValue()));
                }
            });
        }

        // Velocity threshold: 1-127
        if (velocityThresholdSlider != null) {
            velocityThresholdSlider.setMin(1);
            velocityThresholdSlider.setMax(127);
            velocityThresholdSlider.setValue(1);
            velocityThresholdSlider.valueProperty().addListener((obs, old, val) -> {
                if (velocityValueLabel != null) {
                    velocityValueLabel.setText(String.format("%.0f", val.doubleValue()));
                }
            });
        }

        // Transpose: -24 to +24
        if (transposeSlider != null) {
            transposeSlider.setMin(-24);
            transposeSlider.setMax(24);
            transposeSlider.setValue(0);
            transposeSlider.valueProperty().addListener((obs, old, val) -> {
                if (transposeValueLabel != null) {
                    int v = val.intValue();
                    transposeValueLabel.setText(v > 0 ? "+" + v : String.valueOf(v));
                }
            });
        }

        // Key duration: 10-500ms
        if (keyDurationSlider != null) {
            keyDurationSlider.setMin(10);
            keyDurationSlider.setMax(500);
            keyDurationSlider.setValue(50);
            keyDurationSlider.valueProperty().addListener((obs, old, val) -> {
                if (keyDurationValueLabel != null) {
                    keyDurationValueLabel.setText(String.format("%.0fms", val.doubleValue()));
                }
            });
        }
    }

    private void setupChannelToggles() {
        if (channelTogglesContainer == null) return;

        HBox row1 = new HBox(8);
        HBox row2 = new HBox(8);

        for (int i = 0; i < 16; i++) {
            CheckBox toggle = new CheckBox(String.valueOf(i + 1));
            toggle.setSelected(true); // All channels enabled by default
            channelToggles[i] = toggle;

            if (i < 8) {
                row1.getChildren().add(toggle);
            } else {
                row2.getChildren().add(toggle);
            }
        }

        channelTogglesContainer.getChildren().addAll(row1, row2);
    }

    private void updateMappingButtons() {
        boolean hasSelection = mappingListView != null &&
                mappingListView.getSelectionModel().getSelectedItem() != null;

        if (removeMappingButton != null) {
            removeMappingButton.setDisable(!hasSelection);
        }
    }

    private void clearMappingInputs() {
        if (noteNumberField != null) noteNumberField.clear();
        if (keyField != null) keyField.clear();
        if (channelCombo != null) channelCombo.getSelectionModel().selectFirst();
    }

    /**
     * List cell for displaying note mappings.
     */
    private static class MappingListCell extends ListCell<NoteMapping> {
        @Override
        protected void updateItem(NoteMapping mapping, boolean empty) {
            super.updateItem(mapping, empty);

            if (empty || mapping == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            setText(mapping.getDescription());
        }
    }
}
