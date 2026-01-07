package com.burstmeman.midi2keys.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a MIDI-to-keyboard mapping profile.
 * Contains note mappings and playback configuration options.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Profile {

    @EqualsAndHashCode.Include
    private String id;
    private String name;
    private String description;
    private List<NoteMapping> noteMappings;
    private PlaybackOptions playbackOptions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDefault;

    /**
     * Creates a new empty profile.
     *
     * @param name Profile name
     */
    public Profile(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "Profile name cannot be null");
        this.description = "";
        this.noteMappings = new ArrayList<>();
        this.playbackOptions = new PlaybackOptions();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDefault = false;
    }

    /**
     * Creates a profile with full details (for deserialization).
     */
    public Profile(String id, String name, String description, List<NoteMapping> noteMappings,
                   PlaybackOptions playbackOptions, LocalDateTime createdAt,
                   LocalDateTime updatedAt, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.noteMappings = noteMappings != null ? new ArrayList<>(noteMappings) : new ArrayList<>();
        this.playbackOptions = playbackOptions != null ? playbackOptions : new PlaybackOptions();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDefault = isDefault;
    }

    public List<NoteMapping> getNoteMappings() {
        return Collections.unmodifiableList(noteMappings);
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Profile name cannot be null");
        markUpdated();
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
        markUpdated();
    }

    public void setPlaybackOptions(PlaybackOptions playbackOptions) {
        this.playbackOptions = playbackOptions != null ? playbackOptions : new PlaybackOptions();
        markUpdated();
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
        markUpdated();
    }

    // Mapping Operations

    /**
     * Adds a note mapping to the profile.
     *
     * @param mapping The mapping to add
     * @throws IllegalArgumentException if mapping conflicts with existing
     */
    public void addMapping(NoteMapping mapping) {
        Objects.requireNonNull(mapping, "Mapping cannot be null");

        // Check for conflicts
        for (NoteMapping existing : noteMappings) {
            if (existing.conflictsWith(mapping)) {
                throw new IllegalArgumentException(
                        "Mapping conflicts with existing mapping: " + existing);
            }
        }

        noteMappings.add(mapping);
        markUpdated();
    }

    /**
     * Removes a note mapping from the profile.
     *
     * @param mapping The mapping to remove
     * @return true if removed
     */
    public boolean removeMapping(NoteMapping mapping) {
        boolean removed = noteMappings.remove(mapping);
        if (removed) {
            markUpdated();
        }
        return removed;
    }

    /**
     * Updates an existing mapping.
     *
     * @param index   Index of the mapping to update
     * @param mapping New mapping
     */
    public void updateMapping(int index, NoteMapping mapping) {
        if (index < 0 || index >= noteMappings.size()) {
            throw new IndexOutOfBoundsException("Invalid mapping index: " + index);
        }

        // Check for conflicts (excluding the one being replaced)
        for (int i = 0; i < noteMappings.size(); i++) {
            if (i != index && noteMappings.get(i).conflictsWith(mapping)) {
                throw new IllegalArgumentException(
                        "Mapping conflicts with existing mapping: " + noteMappings.get(i));
            }
        }

        noteMappings.set(index, mapping);
        markUpdated();
    }

    /**
     * Clears all mappings from the profile.
     */
    public void clearMappings() {
        noteMappings.clear();
        markUpdated();
    }

    /**
     * Gets the mapping for a specific MIDI note.
     *
     * @param noteNumber MIDI note number (0-127)
     * @param channel    MIDI channel (0-15, or -1 for any)
     * @return The mapping if found, null otherwise
     */
    public NoteMapping getMappingForNote(int noteNumber, int channel) {
        for (NoteMapping mapping : noteMappings) {
            if (mapping.matches(noteNumber, channel)) {
                return mapping;
            }
        }
        return null;
    }

    /**
     * Gets the mapping for a specific MIDI note with note shift applied.
     *
     * @param noteNumber MIDI note number (0-127)
     * @param channel    MIDI channel (0-15, or -1 for any)
     * @param noteShift  Note shift to apply (-4 to +4)
     * @return The mapping if found, null otherwise
     */
    public NoteMapping getMappingForNoteWithShift(int noteNumber, int channel, int noteShift) {
        int shiftedNote = noteNumber - noteShift; // Reverse shift to find original mapping
        return getMappingForNote(shiftedNote, channel);
    }

    /**
     * Checks if the profile has any mappings.
     *
     * @return true if mappings exist
     */
    public boolean hasMappings() {
        return !noteMappings.isEmpty();
    }

    /**
     * Gets the number of mappings in the profile.
     *
     * @return Mapping count
     */
    public int getMappingCount() {
        return noteMappings.size();
    }

    /**
     * Validates the profile configuration.
     *
     * @return List of validation errors (empty if valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add("Profile name is required");
        }

        if (noteMappings.isEmpty()) {
            errors.add("Profile has no mappings configured");
        }

        // Check for duplicate mappings
        for (int i = 0; i < noteMappings.size(); i++) {
            for (int j = i + 1; j < noteMappings.size(); j++) {
                if (noteMappings.get(i).conflictsWith(noteMappings.get(j))) {
                    errors.add(String.format("Conflicting mappings at positions %d and %d", i, j));
                }
            }
        }

        // Validate playback options
        errors.addAll(playbackOptions.validate());

        return errors;
    }

    /**
     * Creates a copy of this profile with a new ID and name.
     *
     * @param newName Name for the copy
     * @return New profile instance
     */
    public Profile copy(String newName) {
        List<NoteMapping> copiedMappings = new ArrayList<>();
        for (NoteMapping mapping : noteMappings) {
            copiedMappings.add(mapping.copy());
        }

        return new Profile(
                UUID.randomUUID().toString(),
                newName,
                description,
                copiedMappings,
                playbackOptions.copy(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );
    }

    private void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

}

