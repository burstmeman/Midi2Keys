package com.burstmeman.midi2keys.domain.valueobjects;

import java.util.Objects;

/**
 * Represents a MIDI note number with human-readable name conversion.
 * MIDI note numbers range from 0-127, with 60 being Middle C (C4).
 */
public final class MidiNote {
    
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int NOTES_PER_OCTAVE = 12;
    private static final int MIDDLE_C = 60;
    
    private final int noteNumber;
    
    /**
     * Creates a MIDI note from a note number.
     * 
     * @param noteNumber MIDI note number (0-127)
     */
    public MidiNote(int noteNumber) {
        if (noteNumber < 0 || noteNumber > 127) {
            throw new IllegalArgumentException("MIDI note number must be 0-127, got: " + noteNumber);
        }
        this.noteNumber = noteNumber;
    }
    
    /**
     * Creates a MIDI note from a note name and octave.
     * 
     * @param noteName Note name (C, C#, D, D#, E, F, F#, G, G#, A, A#, B)
     * @param octave   Octave number (-1 to 9)
     * @return MidiNote instance
     */
    public static MidiNote fromName(String noteName, int octave) {
        int noteIndex = -1;
        String normalizedName = noteName.toUpperCase().replace("♯", "#").replace("♭", "b");
        
        // Handle flats by converting to sharps
        normalizedName = normalizedName.replace("Db", "C#")
                                       .replace("Eb", "D#")
                                       .replace("Gb", "F#")
                                       .replace("Ab", "G#")
                                       .replace("Bb", "A#");
        
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(normalizedName)) {
                noteIndex = i;
                break;
            }
        }
        
        if (noteIndex == -1) {
            throw new IllegalArgumentException("Invalid note name: " + noteName);
        }
        
        int noteNumber = (octave + 1) * NOTES_PER_OCTAVE + noteIndex;
        
        if (noteNumber < 0 || noteNumber > 127) {
            throw new IllegalArgumentException(
                    String.format("Note %s%d is out of MIDI range (0-127)", noteName, octave));
        }
        
        return new MidiNote(noteNumber);
    }
    
    /**
     * Creates Middle C (C4, note 60).
     */
    public static MidiNote middleC() {
        return new MidiNote(MIDDLE_C);
    }
    
    /**
     * Gets the MIDI note number.
     * 
     * @return Note number (0-127)
     */
    public int getNoteNumber() {
        return noteNumber;
    }
    
    /**
     * Gets the note name (without octave).
     * 
     * @return Note name (C, C#, D, etc.)
     */
    public String getNoteNameWithoutOctave() {
        return NOTE_NAMES[noteNumber % NOTES_PER_OCTAVE];
    }
    
    /**
     * Gets the octave number.
     * 
     * @return Octave (-1 to 9)
     */
    public int getOctave() {
        return (noteNumber / NOTES_PER_OCTAVE) - 1;
    }
    
    /**
     * Gets the full note name with octave.
     * 
     * @return Note name with octave (e.g., "C4", "F#3")
     */
    public String getNoteName() {
        return getNoteNameWithoutOctave() + getOctave();
    }
    
    /**
     * Checks if this is Middle C.
     * 
     * @return true if note number is 60
     */
    public boolean isMiddleC() {
        return noteNumber == MIDDLE_C;
    }
    
    /**
     * Gets the frequency in Hz (A4 = 440Hz standard tuning).
     * 
     * @return Frequency in Hz
     */
    public double getFrequency() {
        return 440.0 * Math.pow(2.0, (noteNumber - 69) / 12.0);
    }
    
    /**
     * Creates a new note shifted by semitones.
     * 
     * @param semitones Number of semitones to shift (positive = up, negative = down)
     * @return New MidiNote or null if result would be out of range
     */
    public MidiNote transpose(int semitones) {
        int newNote = noteNumber + semitones;
        if (newNote < 0 || newNote > 127) {
            return null;
        }
        return new MidiNote(newNote);
    }
    
    /**
     * Calculates the interval in semitones from another note.
     * 
     * @param other The other note
     * @return Interval in semitones (positive = this note is higher)
     */
    public int intervalFrom(MidiNote other) {
        return this.noteNumber - other.noteNumber;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MidiNote midiNote = (MidiNote) o;
        return noteNumber == midiNote.noteNumber;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(noteNumber);
    }
    
    @Override
    public String toString() {
        return getNoteName() + " (" + noteNumber + ")";
    }
}

