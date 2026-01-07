package com.burstmeman.midi2keys.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

/**
 * Playback configuration options for a profile.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PlaybackOptions {

    // Default values
    public static final double DEFAULT_TEMPO_MULTIPLIER = 1.0;
    public static final int DEFAULT_MIN_VELOCITY = 1;
    public static final int DEFAULT_TRANSPOSE = 0;
    public static final int DEFAULT_KEY_PRESS_DURATION_MS = 50;
    public static final Quantization DEFAULT_QUANTIZATION = Quantization.NONE;

    private double tempoMultiplier;      // Speed multiplier (0.25 - 4.0)
    private Quantization quantization;    // Note quantization setting
    private int minVelocityThreshold;     // Minimum velocity to trigger (1-127)
    private Set<Integer> ignoredChannels; // MIDI channels to ignore (0-15)
    private int transpose;                // Semitones to transpose (-24 to +24)
    private int keyPressDurationMs;       // Duration of key press in ms (10-500)

    /**
     * Creates playback options with default values.
     */
    public PlaybackOptions() {
        this.tempoMultiplier = DEFAULT_TEMPO_MULTIPLIER;
        this.quantization = DEFAULT_QUANTIZATION;
        this.minVelocityThreshold = DEFAULT_MIN_VELOCITY;
        this.ignoredChannels = new HashSet<>();
        this.transpose = DEFAULT_TRANSPOSE;
        this.keyPressDurationMs = DEFAULT_KEY_PRESS_DURATION_MS;
    }

    /**
     * Creates playback options with all values specified.
     */
    public PlaybackOptions(double tempoMultiplier, Quantization quantization,
                           int minVelocityThreshold, Set<Integer> ignoredChannels,
                           int transpose, int keyPressDurationMs) {
        this.tempoMultiplier = validateTempoMultiplier(tempoMultiplier);
        this.quantization = quantization != null ? quantization : DEFAULT_QUANTIZATION;
        this.minVelocityThreshold = validateVelocity(minVelocityThreshold);
        this.ignoredChannels = ignoredChannels != null ? new HashSet<>(ignoredChannels) : new HashSet<>();
        this.transpose = validateTranspose(transpose);
        this.keyPressDurationMs = validateKeyPressDuration(keyPressDurationMs);
    }

    public Set<Integer> getIgnoredChannels() {
        return Collections.unmodifiableSet(ignoredChannels);
    }

    public void setIgnoredChannels(Set<Integer> ignoredChannels) {
        this.ignoredChannels = new HashSet<>();
        if (ignoredChannels != null) {
            for (int channel : ignoredChannels) {
                addIgnoredChannel(channel);
            }
        }
    }

    public void setTempoMultiplier(double tempoMultiplier) {
        this.tempoMultiplier = validateTempoMultiplier(tempoMultiplier);
    }

    public void setQuantization(Quantization quantization) {
        this.quantization = quantization != null ? quantization : DEFAULT_QUANTIZATION;
    }

    public void setMinVelocityThreshold(int minVelocityThreshold) {
        this.minVelocityThreshold = validateVelocity(minVelocityThreshold);
    }

    public void addIgnoredChannel(int channel) {
        if (channel >= 0 && channel <= 15) {
            ignoredChannels.add(channel);
        }
    }

    public void removeIgnoredChannel(int channel) {
        ignoredChannels.remove(channel);
    }

    public void setTranspose(int transpose) {
        this.transpose = validateTranspose(transpose);
    }

    public void setKeyPressDurationMs(int keyPressDurationMs) {
        this.keyPressDurationMs = validateKeyPressDuration(keyPressDurationMs);
    }

    // Utility methods

    /**
     * Checks if a channel should be ignored.
     *
     * @param channel MIDI channel (0-15)
     * @return true if channel is ignored
     */
    public boolean isChannelIgnored(int channel) {
        return ignoredChannels.contains(channel);
    }

    /**
     * Checks if a velocity passes the threshold.
     *
     * @param velocity Note velocity (0-127)
     * @return true if velocity is at or above threshold
     */
    public boolean velocityPassesThreshold(int velocity) {
        return velocity >= minVelocityThreshold;
    }

    /**
     * Applies transpose to a note number.
     *
     * @param noteNumber Original note number
     * @return Transposed note number (clamped to 0-127)
     */
    public int applyTranspose(int noteNumber) {
        int transposed = noteNumber + transpose;
        return Math.max(0, Math.min(127, transposed));
    }

    /**
     * Calculates the adjusted time based on tempo multiplier.
     *
     * @param timeMs Original time in milliseconds
     * @return Adjusted time
     */
    public long adjustTimeForTempo(long timeMs) {
        if (tempoMultiplier == 1.0) {
            return timeMs;
        }
        return Math.round(timeMs / tempoMultiplier);
    }

    /**
     * Quantizes a time value based on the quantization setting.
     *
     * @param timeMs        Time in milliseconds
     * @param quarterNoteMs Length of quarter note in milliseconds
     * @return Quantized time
     */
    public long quantizeTime(long timeMs, double quarterNoteMs) {
        if (quantization == Quantization.NONE || quarterNoteMs <= 0) {
            return timeMs;
        }

        double gridSize = quarterNoteMs * quantization.getMultiplier();
        return Math.round(timeMs / gridSize) * (long) gridSize;
    }

    /**
     * Validates the configuration.
     *
     * @return List of validation errors
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (tempoMultiplier < 0.25 || tempoMultiplier > 4.0) {
            errors.add("Tempo multiplier must be between 0.25 and 4.0");
        }

        if (minVelocityThreshold < 1 || minVelocityThreshold > 127) {
            errors.add("Minimum velocity must be between 1 and 127");
        }

        if (transpose < -24 || transpose > 24) {
            errors.add("Transpose must be between -24 and +24 semitones");
        }

        if (keyPressDurationMs < 10 || keyPressDurationMs > 500) {
            errors.add("Key press duration must be between 10 and 500 milliseconds");
        }

        for (int channel : ignoredChannels) {
            if (channel < 0 || channel > 15) {
                errors.add("Invalid ignored channel: " + channel);
            }
        }

        return errors;
    }

    /**
     * Creates a copy of these options.
     *
     * @return New PlaybackOptions instance
     */
    public PlaybackOptions copy() {
        return new PlaybackOptions(
                tempoMultiplier,
                quantization,
                minVelocityThreshold,
                new HashSet<>(ignoredChannels),
                transpose,
                keyPressDurationMs
        );
    }

    // Validation helpers

    private double validateTempoMultiplier(double value) {
        if (value < 0.25) return 0.25;
        if (value > 4.0) return 4.0;
        return value;
    }

    private int validateVelocity(int value) {
        if (value < 1) return 1;
        if (value > 127) return 127;
        return value;
    }

    private int validateTranspose(int value) {
        if (value < -24) return -24;
        if (value > 24) return 24;
        return value;
    }

    private int validateKeyPressDuration(int value) {
        if (value < 10) return 10;
        if (value > 500) return 500;
        return value;
    }

    /**
     * Quantization options for note timing.
     */
    public enum Quantization {
        NONE("None", 0),
        QUARTER("1/4 Note", 1.0),
        EIGHTH("1/8 Note", 0.5),
        SIXTEENTH("1/16 Note", 0.25),
        THIRTY_SECOND("1/32 Note", 0.125);

        private final String displayName;
        private final double multiplier; // Multiplier of quarter note length

        Quantization(String displayName, double multiplier) {
            this.displayName = displayName;
            this.multiplier = multiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getMultiplier() {
            return multiplier;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}

