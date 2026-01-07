package com.burstmeman.midi2keys.ui.components;

import com.burstmeman.midi2keys.domain.entities.NoteMapping;
import com.burstmeman.midi2keys.domain.valueobjects.KeyCombination;
import com.burstmeman.midi2keys.domain.valueobjects.KeyboardKey;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom JavaFX component for editing MIDI note to keyboard key mappings.
 */
@Slf4j
public class NoteMappingEditor extends VBox {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private final ObservableList<NoteMapping> mappings = FXCollections.observableArrayList();
    private final ObjectProperty<NoteMapping> selectedMapping = new SimpleObjectProperty<>();
    private ListView<NoteMapping> mappingListView;
    private ComboBox<Integer> midiNoteCombo;
    private TextField keyInputField;
    private Slider velocityThresholdSlider;
    private Button addButton;
    private Button removeButton;
    private Button captureKeyButton;
    private Consumer<List<NoteMapping>> onMappingsChanged;
    private boolean capturingKey = false;
    private int capturedKeyCode = -1;

    public NoteMappingEditor() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        applyStyles();
    }

    private void initializeComponents() {
        mappingListView = new ListView<>(mappings);
        mappingListView.setPrefHeight(200);
        mappingListView.setCellFactory(lv -> new MappingListCell());
        mappingListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            selectedMapping.set(selected);
            updateEditControls(selected);
        });

        midiNoteCombo = new ComboBox<>();
        midiNoteCombo.setPromptText("Select MIDI Note");
        midiNoteCombo.setPrefWidth(150);
        ObservableList<Integer> noteNumbers = FXCollections.observableArrayList();
        for (int i = 0; i <= 127; i++) {
            noteNumbers.add(i);
        }
        midiNoteCombo.setItems(noteNumbers);
        midiNoteCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer noteNumber) {
                if (noteNumber == null) return "";
                return formatMidiNote(noteNumber);
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        keyInputField = new TextField();
        keyInputField.setPromptText("Press 'Capture'");
        keyInputField.setPrefWidth(120);
        keyInputField.setEditable(false);

        captureKeyButton = new Button("Capture");

        velocityThresholdSlider = new Slider(1, 127, 1);
        velocityThresholdSlider.setShowTickLabels(true);
        velocityThresholdSlider.setShowTickMarks(true);
        velocityThresholdSlider.setMajorTickUnit(32);
        velocityThresholdSlider.setPrefWidth(150);

        addButton = new Button("Add Mapping");

        removeButton = new Button("Remove");
        removeButton.setStyle("-fx-text-fill: #f44336;");
        removeButton.setDisable(true);
    }

    private void setupLayout() {
        setSpacing(16);
        setPadding(new Insets(16));

        Label titleLabel = new Label("Note Mappings");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox noteRow = new HBox(12);
        noteRow.setAlignment(Pos.CENTER_LEFT);
        noteRow.getChildren().addAll(new Label("MIDI Note:"), midiNoteCombo);

        HBox keyRow = new HBox(12);
        keyRow.setAlignment(Pos.CENTER_LEFT);
        keyRow.getChildren().addAll(new Label("Key:"), keyInputField, captureKeyButton);

        HBox velocityRow = new HBox(12);
        velocityRow.setAlignment(Pos.CENTER_LEFT);
        Label velocityLabel = new Label("Min Velocity: 1");
        velocityThresholdSlider.valueProperty().addListener((obs, old, val) -> {
            velocityLabel.setText("Min Velocity: " + val.intValue());
        });
        velocityRow.getChildren().addAll(velocityLabel, velocityThresholdSlider);

        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.getChildren().addAll(addButton, removeButton);

        VBox editorSection = new VBox(12);
        editorSection.setPadding(new Insets(12));
        editorSection.setStyle("-fx-background-color: derive(-fx-base, 10%); -fx-background-radius: 4;");
        editorSection.getChildren().addAll(noteRow, keyRow, velocityRow, buttonRow);

        VBox listSection = new VBox(8);
        Label listLabel = new Label("Current Mappings");
        listLabel.setStyle("-fx-font-size: 14px;");
        VBox.setVgrow(mappingListView, Priority.ALWAYS);
        listSection.getChildren().addAll(listLabel, mappingListView);

        getChildren().addAll(titleLabel, editorSection, listSection);
    }

    private void setupEventHandlers() {
        addButton.setOnAction(e -> addMapping());
        removeButton.setOnAction(e -> removeSelectedMapping());

        selectedMapping.addListener((obs, old, selected) -> {
            removeButton.setDisable(selected == null);
        });

        captureKeyButton.setOnAction(e -> startKeyCapture());
        keyInputField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyCapture);
    }

    private void startKeyCapture() {
        capturingKey = true;
        capturedKeyCode = -1;
        keyInputField.setPromptText("Press a key...");
        keyInputField.setText("");
        keyInputField.requestFocus();
        captureKeyButton.setText("Waiting...");
        captureKeyButton.setDisable(true);
    }

    private void handleKeyCapture(KeyEvent event) {
        if (!capturingKey) return;

        event.consume();

        KeyCode code = event.getCode();
        if (code == KeyCode.ESCAPE) {
            cancelKeyCapture();
            return;
        }

        String keyName = code.getName();
        capturedKeyCode = code.getCode();
        keyInputField.setText(keyName);

        capturingKey = false;
        captureKeyButton.setText("Capture");
        captureKeyButton.setDisable(false);
    }

    private void cancelKeyCapture() {
        capturingKey = false;
        keyInputField.setPromptText("Press 'Capture'");
        captureKeyButton.setText("Capture");
        captureKeyButton.setDisable(false);
    }

    private void addMapping() {
        Integer noteNumber = midiNoteCombo.getValue();
        String keyName = keyInputField.getText();

        if (noteNumber == null) {
            log.warn("Cannot add mapping: no MIDI note selected");
            return;
        }

        if (keyName == null || keyName.trim().isEmpty() || capturedKeyCode < 0) {
            log.warn("Cannot add mapping: no keyboard key captured");
            return;
        }

        boolean duplicateNote = mappings.stream()
                .anyMatch(m -> m.midiNote().noteNumber() == noteNumber);

        if (duplicateNote) {
            log.warn("Duplicate MIDI note mapping detected for note {}", noteNumber);
            return;
        }

        MidiNote midiNote = new MidiNote(noteNumber);
        KeyboardKey keyboardKey = new KeyboardKey(capturedKeyCode);
        KeyCombination keyCombination = new KeyCombination(keyboardKey);
        int minVelocity = (int) velocityThresholdSlider.getValue();

        NoteMapping mapping = new NoteMapping(midiNote, keyCombination, NoteMapping.ANY_CHANNEL, minVelocity, NoteMapping.MAX_VELOCITY);
        mappings.add(mapping);

        notifyMappingsChanged();
        clearEditor();

        log.info("Added mapping: {} -> {}", formatMidiNote(noteNumber), keyName);
    }

    private void removeSelectedMapping() {
        NoteMapping selected = selectedMapping.get();
        if (selected != null) {
            mappings.remove(selected);
            notifyMappingsChanged();
            log.info("Removed mapping: {}", selected.getDescription());
        }
    }

    private void updateEditControls(NoteMapping mapping) {
        if (mapping != null) {
            midiNoteCombo.setValue(mapping.midiNote().noteNumber());
            keyInputField.setText(mapping.keyCombination().getDisplayString());
            capturedKeyCode = mapping.keyCombination().getMainKeyCode();
            velocityThresholdSlider.setValue(mapping.minVelocity());
        }
    }

    private void clearEditor() {
        midiNoteCombo.setValue(null);
        keyInputField.setText("");
        capturedKeyCode = -1;
        velocityThresholdSlider.setValue(1);
    }

    private void notifyMappingsChanged() {
        if (onMappingsChanged != null) {
            onMappingsChanged.accept(new ArrayList<>(mappings));
        }
    }

    private void applyStyles() {
        getStyleClass().add("note-mapping-editor");
    }

    public List<NoteMapping> getMappings() {
        return new ArrayList<>(mappings);
    }

    public void setMappings(List<NoteMapping> noteMappings) {
        mappings.clear();
        if (noteMappings != null) {
            mappings.addAll(noteMappings);
        }
    }

    public void setOnMappingsChanged(Consumer<List<NoteMapping>> callback) {
        this.onMappingsChanged = callback;
    }

    public ObservableList<NoteMapping> mappingsProperty() {
        return mappings;
    }

    private String formatMidiNote(int noteNumber) {
        int octave = (noteNumber / 12) - 1;
        int noteIndex = noteNumber % 12;
        return NOTE_NAMES[noteIndex] + octave + " (" + noteNumber + ")";
    }

    private class MappingListCell extends ListCell<NoteMapping> {
        @Override
        protected void updateItem(NoteMapping mapping, boolean empty) {
            super.updateItem(mapping, empty);

            if (empty || mapping == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(mapping.getDescription());
            }
        }
    }
}
