package com.burstmeman.midi2keys.domain.entities;

import com.burstmeman.midi2keys.domain.valueobjects.KeyCombination;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;

import java.util.Objects;

/**
 * Represents a mapping from a MIDI note to a keyboard key combination.
 */
public class NoteMapping {
    
    public static final int ANY_CHANNEL = -1;
    public static final int DEFAULT_MIN_VELOCITY = 1;
    public static final int MAX_VELOCITY = 127;
    
    private final MidiNote midiNote;
    private final KeyCombination keyCombination;
    private final int channel; // -1 means any channel
    private final int minVelocity; // Minimum velocity to trigger (1-127)
    private final int maxVelocity; // Maximum velocity to trigger (1-127)
    
    /**
     * Creates a simple note mapping (any channel, any velocity).
     * 
     * @param midiNote       The MIDI note to map from
     * @param keyCombination The keyboard key to map to
     */
    public NoteMapping(MidiNote midiNote, KeyCombination keyCombination) {
        this(midiNote, keyCombination, ANY_CHANNEL, DEFAULT_MIN_VELOCITY, MAX_VELOCITY);
    }
    
    /**
     * Creates a note mapping with channel filter.
     * 
     * @param midiNote       The MIDI note to map from
     * @param keyCombination The keyboard key to map to
     * @param channel        MIDI channel filter (-1 for any)
     */
    public NoteMapping(MidiNote midiNote, KeyCombination keyCombination, int channel) {
        this(midiNote, keyCombination, channel, DEFAULT_MIN_VELOCITY, MAX_VELOCITY);
    }
    
    /**
     * Creates a fully configured note mapping.
     * 
     * @param midiNote       The MIDI note to map from
     * @param keyCombination The keyboard key to map to
     * @param channel        MIDI channel filter (-1 for any)
     * @param minVelocity    Minimum velocity to trigger
     * @param maxVelocity    Maximum velocity to trigger
     */
    public NoteMapping(MidiNote midiNote, KeyCombination keyCombination, 
                       int channel, int minVelocity, int maxVelocity) {
        this.midiNote = Objects.requireNonNull(midiNote, "MIDI note cannot be null");
        this.keyCombination = Objects.requireNonNull(keyCombination, "Key combination cannot be null");
        this.channel = validateChannel(channel);
        this.minVelocity = validateVelocity(minVelocity);
        this.maxVelocity = validateVelocity(maxVelocity);
        
        if (this.minVelocity > this.maxVelocity) {
            throw new IllegalArgumentException("minVelocity cannot be greater than maxVelocity");
        }
    }
    
    // Getters
    
    public MidiNote getMidiNote() {
        return midiNote;
    }
    
    public KeyCombination getKeyCombination() {
        return keyCombination;
    }
    
    public int getChannel() {
        return channel;
    }
    
    public int getMinVelocity() {
        return minVelocity;
    }
    
    public int getMaxVelocity() {
        return maxVelocity;
    }
    
    /**
     * Checks if this mapping matches a given MIDI note event.
     * 
     * @param noteNumber MIDI note number (0-127)
     * @param channel    MIDI channel (0-15)
     * @return true if this mapping should be triggered
     */
    public boolean matches(int noteNumber, int channel) {
        return midiNote.getNoteNumber() == noteNumber &&
               (this.channel == ANY_CHANNEL || this.channel == channel);
    }
    
    /**
     * Checks if this mapping should trigger for a given velocity.
     * 
     * @param velocity Note velocity (1-127)
     * @return true if velocity is within range
     */
    public boolean velocityInRange(int velocity) {
        return velocity >= minVelocity && velocity <= maxVelocity;
    }
    
    /**
     * Checks if this mapping conflicts with another mapping.
     * A conflict occurs when two mappings would be triggered by the same MIDI event.
     * 
     * @param other The other mapping to check
     * @return true if there's a conflict
     */
    public boolean conflictsWith(NoteMapping other) {
        if (other == null) return false;
        
        // Same note number
        if (midiNote.getNoteNumber() != other.midiNote.getNoteNumber()) {
            return false;
        }
        
        // Check channel overlap
        if (channel != ANY_CHANNEL && other.channel != ANY_CHANNEL && channel != other.channel) {
            return false;
        }
        
        // Check velocity range overlap
        return !(maxVelocity < other.minVelocity || minVelocity > other.maxVelocity);
    }
    
    /**
     * Creates a copy of this mapping.
     * 
     * @return New mapping instance with same values
     */
    public NoteMapping copy() {
        return new NoteMapping(
                new MidiNote(midiNote.getNoteNumber()),
                keyCombination.copy(),
                channel,
                minVelocity,
                maxVelocity
        );
    }
    
    /**
     * Gets a human-readable description of this mapping.
     * 
     * @return Description string
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(midiNote.getNoteName());
        
        if (channel != ANY_CHANNEL) {
            sb.append(" (Ch ").append(channel + 1).append(")");
        }
        
        sb.append(" → ").append(keyCombination.getDisplayString());
        
        if (minVelocity > DEFAULT_MIN_VELOCITY || maxVelocity < MAX_VELOCITY) {
            sb.append(" [").append(minVelocity).append("-").append(maxVelocity).append("]");
        }
        
        return sb.toString();
    }
    
    private int validateChannel(int channel) {
        if (channel < -1 || channel > 15) {
            throw new IllegalArgumentException("Channel must be -1 (any) or 0-15, got: " + channel);
        }
        return channel;
    }
    
    private int validateVelocity(int velocity) {
        if (velocity < 1 || velocity > 127) {
            throw new IllegalArgumentException("Velocity must be 1-127, got: " + velocity);
        }
        return velocity;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteMapping that = (NoteMapping) o;
        return channel == that.channel &&
               minVelocity == that.minVelocity &&
               maxVelocity == that.maxVelocity &&
               Objects.equals(midiNote, that.midiNote) &&
               Objects.equals(keyCombination, that.keyCombination);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(midiNote, keyCombination, channel, minVelocity, maxVelocity);
    }
    
    @Override
    public String toString() {
        return getDescription();
    }
}

