package com.burstmeman.midi2keys.domain.valueobjects;

import java.util.Objects;

/**
 * Represents a note shift configuration for a MIDI file.
 * Shifts can be -4 to +4 semitones.
 */
public final class NoteShift {
    
    public static final int MIN_SHIFT = -4;
    public static final int MAX_SHIFT = 4;
    public static final NoteShift NONE = new NoteShift(0);
    
    private final int semitones;
    
    /**
     * Creates a note shift.
     * 
     * @param semitones Number of semitones to shift (-4 to +4)
     */
    public NoteShift(int semitones) {
        if (semitones < MIN_SHIFT || semitones > MAX_SHIFT) {
            throw new IllegalArgumentException(
                    "Note shift must be between " + MIN_SHIFT + " and " + MAX_SHIFT + ", got: " + semitones);
        }
        this.semitones = semitones;
    }
    
    /**
     * Creates a note shift, clamping to valid range.
     * 
     * @param semitones Number of semitones
     * @return NoteShift clamped to valid range
     */
    public static NoteShift ofClamped(int semitones) {
        int clamped = Math.max(MIN_SHIFT, Math.min(MAX_SHIFT, semitones));
        return new NoteShift(clamped);
    }
    
    /**
     * Creates a note shift from a string (e.g., "+2", "-3", "0").
     * 
     * @param input String representation
     * @return NoteShift instance
     */
    public static NoteShift parse(String input) {
        if (input == null || input.isBlank()) {
            return NONE;
        }
        
        try {
            int value = Integer.parseInt(input.trim().replace("+", ""));
            return new NoteShift(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid note shift: " + input);
        }
    }
    
    /**
     * Gets the shift amount in semitones.
     * 
     * @return Semitones (-4 to +4)
     */
    public int getSemitones() {
        return semitones;
    }
    
    /**
     * Checks if this is a zero (no) shift.
     * 
     * @return true if no shift
     */
    public boolean isNone() {
        return semitones == 0;
    }
    
    /**
     * Checks if this is an upward shift.
     * 
     * @return true if positive shift
     */
    public boolean isUp() {
        return semitones > 0;
    }
    
    /**
     * Checks if this is a downward shift.
     * 
     * @return true if negative shift
     */
    public boolean isDown() {
        return semitones < 0;
    }
    
    /**
     * Applies this shift to a note number.
     * 
     * @param noteNumber Original MIDI note number
     * @return Shifted note number (clamped to 0-127)
     */
    public int apply(int noteNumber) {
        int shifted = noteNumber + semitones;
        return Math.max(0, Math.min(127, shifted));
    }
    
    /**
     * Reverses this shift (for finding original note from shifted note).
     * 
     * @param shiftedNoteNumber Shifted MIDI note number
     * @return Original note number (clamped to 0-127)
     */
    public int reverse(int shiftedNoteNumber) {
        int original = shiftedNoteNumber - semitones;
        return Math.max(0, Math.min(127, original));
    }
    
    /**
     * Gets a display string (e.g., "+2", "-3", "0").
     * 
     * @return Display string with sign
     */
    public String getDisplayString() {
        if (semitones == 0) {
            return "0";
        }
        return String.format("%+d", semitones);
    }
    
    /**
     * Gets a descriptive string (e.g., "2 up", "3 down", "None").
     * 
     * @return Descriptive string
     */
    public String getDescription() {
        if (semitones == 0) {
            return "None";
        }
        return Math.abs(semitones) + (semitones > 0 ? " up" : " down");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteShift noteShift = (NoteShift) o;
        return semitones == noteShift.semitones;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(semitones);
    }
    
    @Override
    public String toString() {
        return getDisplayString();
    }
}

