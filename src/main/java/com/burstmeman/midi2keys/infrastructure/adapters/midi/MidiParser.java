package com.burstmeman.midi2keys.infrastructure.adapters.midi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for parsing MIDI files.
 * Abstracts MIDI file reading and event extraction.
 */
public interface MidiParser {

    /**
     * Parses a MIDI file and returns its information.
     *
     * @param path Path to the MIDI file
     * @return Parsed MIDI file information
     * @throws IOException        if file cannot be read
     * @throws MidiParseException if file format is invalid
     */
    MidiFileInfo parse(Path path) throws IOException, MidiParseException;

    /**
     * Checks if a file is a valid MIDI file.
     *
     * @param path Path to check
     * @return true if file appears to be valid MIDI
     */
    boolean isValidMidiFile(Path path);

    /**
     * Container for parsed MIDI file information.
     */
    record MidiFileInfo(
            int formatType,           // 0, 1, or 2
            int trackCount,
            int resolution,           // Ticks per quarter note (or SMPTE)
            long durationMs,
            float initialTempoBpm,
            int tempoChangeCount,
            String timeSignature,
            List<MidiTrackInfo> tracks,
            List<MidiNoteEvent> noteEvents
    ) {
    }

    /**
     * Information about a MIDI track.
     */
    record MidiTrackInfo(
            int trackNumber,
            String name,
            int noteCount,
            int eventCount
    ) {
    }

    /**
     * Represents a MIDI note event (Note On or Note Off).
     */
    record MidiNoteEvent(
            long tickPosition,        // Position in MIDI ticks
            long timeMs,              // Position in milliseconds
            int channel,              // MIDI channel (0-15)
            int noteNumber,           // MIDI note number (0-127)
            int velocity,             // Velocity (0-127, 0 = note off)
            boolean isNoteOn,         // true for Note On, false for Note Off
            int trackNumber
    ) {
    }

    /**
     * Exception for MIDI parsing errors.
     */
    class MidiParseException extends Exception {
        public MidiParseException(String message) {
            super(message);
        }

        public MidiParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

