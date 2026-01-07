package com.burstmeman.midi2keys.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents analysis data for a MIDI file.
 * Contains detailed metadata and statistics about the MIDI content.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class MidiAnalysis {

    @EqualsAndHashCode.Include
    private Long id;
    @EqualsAndHashCode.Include
    private Long midiFileId;

    // Basic MIDI info
    private int formatType;           // 0, 1, or 2
    private int trackCount;
    private long durationMs;

    // Tempo and timing
    private float tempoBpm;
    private int tempoChangesCount;
    private String timeSignature;

    // Note statistics
    private int totalNotes;
    private Map<Integer, Integer> channelNoteCounts;  // channel -> note count
    private Map<Integer, Integer> trackNoteCounts;    // track -> note count
    private Map<Integer, Integer> noteHistogram;      // note number -> count

    // Velocity statistics
    private int minVelocity;
    private int maxVelocity;
    private float avgVelocity;

    // Derived data
    private long estimatedMelodyLengthMs;

    private LocalDateTime analyzedAt;

    /**
     * Creates a new MIDI analysis.
     */
    public MidiAnalysis(Long midiFileId) {
        this.midiFileId = Objects.requireNonNull(midiFileId, "MIDI file ID cannot be null");
        this.channelNoteCounts = new HashMap<>();
        this.trackNoteCounts = new HashMap<>();
        this.noteHistogram = new HashMap<>();
        this.timeSignature = "4/4";
        this.analyzedAt = LocalDateTime.now();
    }

    /**
     * Creates a MIDI analysis from database record.
     */
    public MidiAnalysis(Long id, Long midiFileId, int formatType, int trackCount,
                        long durationMs, float tempoBpm, int tempoChangesCount,
                        String timeSignature, int totalNotes,
                        Map<Integer, Integer> channelNoteCounts,
                        Map<Integer, Integer> trackNoteCounts,
                        Map<Integer, Integer> noteHistogram,
                        int minVelocity, int maxVelocity, float avgVelocity,
                        long estimatedMelodyLengthMs, LocalDateTime analyzedAt) {
        this.id = id;
        this.midiFileId = midiFileId;
        this.formatType = formatType;
        this.trackCount = trackCount;
        this.durationMs = durationMs;
        this.tempoBpm = tempoBpm;
        this.tempoChangesCount = tempoChangesCount;
        this.timeSignature = timeSignature;
        this.totalNotes = totalNotes;
        this.channelNoteCounts = channelNoteCounts != null ? new HashMap<>(channelNoteCounts) : new HashMap<>();
        this.trackNoteCounts = trackNoteCounts != null ? new HashMap<>(trackNoteCounts) : new HashMap<>();
        this.noteHistogram = noteHistogram != null ? new HashMap<>(noteHistogram) : new HashMap<>();
        this.minVelocity = minVelocity;
        this.maxVelocity = maxVelocity;
        this.avgVelocity = avgVelocity;
        this.estimatedMelodyLengthMs = estimatedMelodyLengthMs;
        this.analyzedAt = analyzedAt;
    }

    public Map<Integer, Integer> getChannelNoteCounts() {
        return Collections.unmodifiableMap(channelNoteCounts);
    }

    public void setChannelNoteCounts(Map<Integer, Integer> counts) {
        this.channelNoteCounts = counts != null ? new HashMap<>(counts) : new HashMap<>();
    }

    public Map<Integer, Integer> getTrackNoteCounts() {
        return Collections.unmodifiableMap(trackNoteCounts);
    }

    public void setTrackNoteCounts(Map<Integer, Integer> counts) {
        this.trackNoteCounts = counts != null ? new HashMap<>(counts) : new HashMap<>();
    }

    public Map<Integer, Integer> getNoteHistogram() {
        return Collections.unmodifiableMap(noteHistogram);
    }

    public void setNoteHistogram(Map<Integer, Integer> histogram) {
        this.noteHistogram = histogram != null ? new HashMap<>(histogram) : new HashMap<>();
    }

    // Utility methods

    /**
     * Gets a formatted duration string (e.g., "3:45").
     */
    public String getFormattedDuration() {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Gets a description of the MIDI format type.
     */
    public String getFormatDescription() {
        return switch (formatType) {
            case 0 -> "Type 0 (Single Track)";
            case 1 -> "Type 1 (Multi-Track)";
            case 2 -> "Type 2 (Multi-Song)";
            default -> "Unknown Format";
        };
    }

    /**
     * Gets the number of active channels (channels with notes).
     */
    public int getActiveChannelCount() {
        return (int) channelNoteCounts.values().stream()
                .filter(count -> count > 0)
                .count();
    }

    /**
     * Gets the lowest note number in the file.
     */
    public int getLowestNote() {
        return noteHistogram.keySet().stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
    }

    /**
     * Gets the highest note number in the file.
     */
    public int getHighestNote() {
        return noteHistogram.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(127);
    }

    /**
     * Gets the note range (highest - lowest + 1).
     */
    public int getNoteRange() {
        if (noteHistogram.isEmpty()) return 0;
        return getHighestNote() - getLowestNote() + 1;
    }

    /**
     * Gets the most common note (mode).
     */
    public Integer getMostCommonNote() {
        return noteHistogram.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

