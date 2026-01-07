package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.NoteMapping;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing test mode functionality.
 * In test mode, key mappings are previewed without sending actual keystrokes.
 */
public class TestModeService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestModeService.class);
    
    private final BooleanProperty testModeEnabled = new SimpleBooleanProperty(false);
    private final List<TestModeEvent> eventLog = Collections.synchronizedList(new ArrayList<>());
    
    private Consumer<TestModeEvent> onEventLogged;
    private Consumer<String> onKeyPressed;
    private Profile currentProfile;
    private int maxLogSize = 1000;
    
    public TestModeService() {
        testModeEnabled.addListener((obs, wasEnabled, isEnabled) -> {
            logger.info("Test mode {}", isEnabled ? "enabled" : "disabled");
            if (!isEnabled) {
                eventLog.clear();
            }
        });
    }
    
    public void setTestModeEnabled(boolean enabled) {
        testModeEnabled.set(enabled);
    }
    
    public boolean isTestModeEnabled() {
        return testModeEnabled.get();
    }
    
    public BooleanProperty testModeEnabledProperty() {
        return testModeEnabled;
    }
    
    public void setCurrentProfile(Profile profile) {
        this.currentProfile = profile;
    }
    
    /**
     * Simulate a MIDI note event in test mode.
     */
    public Optional<String> simulateNote(int noteNumber, int velocity, boolean noteOn) {
        if (!testModeEnabled.get()) {
            logger.warn("simulateNote called but test mode is not enabled");
            return Optional.empty();
        }
        
        if (currentProfile == null) {
            logEvent(TestModeEvent.noMapping(noteNumber, velocity, noteOn, "No profile selected"));
            return Optional.empty();
        }
        
        MidiNote midiNote = new MidiNote(noteNumber);
        Optional<NoteMapping> mappingOpt = currentProfile.getNoteMappings().stream()
            .filter(m -> m.getMidiNote().equals(midiNote))
            .findFirst();
        
        if (mappingOpt.isEmpty()) {
            logEvent(TestModeEvent.noMapping(noteNumber, velocity, noteOn, "No mapping for note"));
            return Optional.empty();
        }
        
        NoteMapping mapping = mappingOpt.get();
        
        // Check velocity threshold
        if (noteOn && velocity < mapping.getMinVelocity()) {
            String keyName = mapping.getKeyCombination().getDisplayString();
            logEvent(TestModeEvent.velocityFiltered(noteNumber, velocity, noteOn, keyName, mapping.getMinVelocity()));
            return Optional.empty();
        }
        
        String keyName = mapping.getKeyCombination().getDisplayString();
        logEvent(TestModeEvent.mapped(noteNumber, velocity, noteOn, keyName));
        
        // Notify key press callback (only for note on events)
        if (noteOn && onKeyPressed != null) {
            try {
                onKeyPressed.accept(keyName);
            } catch (Exception e) {
                logger.error("Error in key pressed callback", e);
            }
        }
        
        return Optional.of(keyName);
    }
    
    private void logEvent(TestModeEvent event) {
        while (eventLog.size() >= maxLogSize) {
            eventLog.remove(0);
        }
        
        eventLog.add(event);
        
        if (onEventLogged != null) {
            try {
                onEventLogged.accept(event);
            } catch (Exception e) {
                logger.error("Error in event logged callback", e);
            }
        }
        
        logger.debug("Test mode event: {}", event);
    }
    
    public List<TestModeEvent> getEventLog() {
        return new ArrayList<>(eventLog);
    }
    
    public void clearEventLog() {
        eventLog.clear();
    }
    
    public void setOnEventLogged(Consumer<TestModeEvent> callback) {
        this.onEventLogged = callback;
    }
    
    /**
     * Sets callback for when a key is pressed in test mode.
     * This is called with the display string of the key (e.g., "Q", "Ctrl+E").
     */
    public void setOnKeyPressed(Consumer<String> callback) {
        this.onKeyPressed = callback;
    }
    
    /**
     * Directly logs a key press from the playback system.
     * Used when the playback service simulates a key press in test mode.
     * 
     * @param keyDescription The display string of the pressed key
     */
    public void logKeyPress(String keyDescription) {
        if (!testModeEnabled.get()) {
            return;
        }
        
        if (onKeyPressed != null) {
            try {
                onKeyPressed.accept(keyDescription);
            } catch (Exception e) {
                logger.error("Error in key pressed callback", e);
            }
        }
        
        logger.debug("Test mode key press logged: {}", keyDescription);
    }
    
    public void setMaxLogSize(int maxSize) {
        this.maxLogSize = Math.max(10, maxSize);
    }
    
    /**
     * Represents a test mode event.
     */
    public static class TestModeEvent {
        public enum EventType {
            MAPPED,
            NO_MAPPING,
            VELOCITY_FILTERED
        }
        
        private final long timestamp;
        private final int noteNumber;
        private final int velocity;
        private final boolean noteOn;
        private final EventType type;
        private final String mappedKey;
        private final String reason;
        private final int velocityThreshold;
        
        private TestModeEvent(int noteNumber, int velocity, boolean noteOn, 
                              EventType type, String mappedKey, String reason, int velocityThreshold) {
            this.timestamp = System.currentTimeMillis();
            this.noteNumber = noteNumber;
            this.velocity = velocity;
            this.noteOn = noteOn;
            this.type = type;
            this.mappedKey = mappedKey;
            this.reason = reason;
            this.velocityThreshold = velocityThreshold;
        }
        
        static TestModeEvent mapped(int noteNumber, int velocity, boolean noteOn, String key) {
            return new TestModeEvent(noteNumber, velocity, noteOn, EventType.MAPPED, key, null, 0);
        }
        
        static TestModeEvent noMapping(int noteNumber, int velocity, boolean noteOn, String reason) {
            return new TestModeEvent(noteNumber, velocity, noteOn, EventType.NO_MAPPING, null, reason, 0);
        }
        
        static TestModeEvent velocityFiltered(int noteNumber, int velocity, boolean noteOn, String key, int threshold) {
            return new TestModeEvent(noteNumber, velocity, noteOn, EventType.VELOCITY_FILTERED, key, "Velocity below threshold", threshold);
        }
        
        public long getTimestamp() { return timestamp; }
        public int getNoteNumber() { return noteNumber; }
        public int getVelocity() { return velocity; }
        public boolean isNoteOn() { return noteOn; }
        public EventType getType() { return type; }
        public String getMappedKey() { return mappedKey; }
        public String getReason() { return reason; }
        public int getVelocityThreshold() { return velocityThreshold; }
        
        @Override
        public String toString() {
            String noteState = noteOn ? "ON" : "OFF";
            String noteName = formatNoteName(noteNumber);
            
            return switch (type) {
                case MAPPED -> String.format("[%s] %s (vel=%d) → %s", noteState, noteName, velocity, mappedKey);
                case NO_MAPPING -> String.format("[%s] %s (vel=%d) → NO MAPPING (%s)", noteState, noteName, velocity, reason);
                case VELOCITY_FILTERED -> String.format("[%s] %s (vel=%d) → FILTERED (threshold=%d)", noteState, noteName, velocity, velocityThreshold);
            };
        }
        
        private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        
        private static String formatNoteName(int noteNumber) {
            int octave = (noteNumber / 12) - 1;
            int noteIndex = noteNumber % 12;
            return NOTE_NAMES[noteIndex] + octave;
        }
    }
}
