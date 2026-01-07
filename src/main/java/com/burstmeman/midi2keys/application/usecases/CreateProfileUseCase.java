package com.burstmeman.midi2keys.application.usecases;

import com.burstmeman.midi2keys.application.services.ProfileService;
import com.burstmeman.midi2keys.domain.entities.NoteMapping;
import com.burstmeman.midi2keys.domain.entities.PlaybackOptions;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.valueobjects.KeyCombination;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Use case for creating and managing MIDI-to-keyboard mapping profiles.
 */
@Slf4j
public class CreateProfileUseCase {

    private final ProfileService profileService;

    public CreateProfileUseCase(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Creates a new empty profile.
     *
     * @param name Profile name
     * @return Created profile
     */
    public Profile createProfile(String name) {
        log.info("Creating new profile: {}", name);
        Profile profile = profileService.createProfile(name);

        // If this is the first profile, set it as current
        if (profileService.getAllProfiles().size() == 1) {
            profileService.setCurrentProfile(profile);
        }

        return profile;
    }

    /**
     * Creates a profile with a simple octave mapping.
     * Maps a range of MIDI notes to keyboard keys.
     *
     * @param name        Profile name
     * @param startNote   Starting MIDI note number
     * @param keyMappings Array of key names to map (e.g., ["A", "S", "D", "F", ...])
     * @return Created profile
     */
    public Profile createProfileWithSimpleMapping(String name, int startNote, String[] keyMappings) {
        List<NoteMapping> mappings = new ArrayList<>();

        for (int i = 0; i < keyMappings.length; i++) {
            int noteNumber = startNote + i;
            if (noteNumber > 127) break;

            try {
                MidiNote midiNote = new MidiNote(noteNumber);
                KeyCombination keyComb = KeyCombination.parse(keyMappings[i]);
                mappings.add(new NoteMapping(midiNote, keyComb));
            } catch (Exception e) {
                log.warn("Failed to create mapping for note {} to key {}: {}",
                        noteNumber, keyMappings[i], e.getMessage());
            }
        }

        return profileService.createProfile(name, mappings, new PlaybackOptions());
    }

    /**
     * Creates a default piano-style profile mapping middle C range to QWERTY keys.
     *
     * @param name Profile name
     * @return Created profile
     */
    public Profile createDefaultPianoProfile(String name) {
        // Map C4 (60) through B5 (83) to two rows of keys
        String[] bottomRow = {"Z", "S", "X", "D", "C", "V", "G", "B", "H", "N", "J", "M"};
        String[] topRow = {"Q", "2", "W", "3", "E", "R", "5", "T", "6", "Y", "7", "U"};

        List<NoteMapping> mappings = new ArrayList<>();

        // C4-B4 (60-71) → bottom row (white and black keys)
        for (int i = 0; i < bottomRow.length && i < 12; i++) {
            try {
                MidiNote midiNote = new MidiNote(60 + i);
                KeyCombination keyComb = KeyCombination.parse(bottomRow[i]);
                mappings.add(new NoteMapping(midiNote, keyComb));
            } catch (Exception e) {
                log.warn("Failed to create mapping: {}", e.getMessage());
            }
        }

        // C5-B5 (72-83) → top row
        for (int i = 0; i < topRow.length && i < 12; i++) {
            try {
                MidiNote midiNote = new MidiNote(72 + i);
                KeyCombination keyComb = KeyCombination.parse(topRow[i]);
                mappings.add(new NoteMapping(midiNote, keyComb));
            } catch (Exception e) {
                log.warn("Failed to create mapping: {}", e.getMessage());
            }
        }

        PlaybackOptions options = new PlaybackOptions();
        options.setKeyPressDurationMs(100);

        Profile profile = profileService.createProfile(name, mappings, options);
        profileService.setDefaultProfile(profile.getId());

        log.info("Created default piano profile with {} mappings", mappings.size());
        return profile;
    }

    /**
     * Duplicates an existing profile.
     *
     * @param profileId Profile ID to duplicate
     * @param newName   Name for the copy
     * @return New profile
     */
    public Profile duplicateProfile(String profileId, String newName) {
        return profileService.duplicateProfile(profileId, newName);
    }

    /**
     * Deletes a profile.
     *
     * @param profileId Profile ID to delete
     * @return true if deleted
     */
    public boolean deleteProfile(String profileId) {
        return profileService.deleteProfile(profileId);
    }

    /**
     * Gets all profiles.
     *
     * @return List of all profiles
     */
    public List<Profile> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    /**
     * Gets the currently selected profile.
     *
     * @return Current profile or empty
     */
    public Optional<Profile> getCurrentProfile() {
        return Optional.ofNullable(profileService.getCurrentProfile());
    }

    /**
     * Selects a profile as the current one.
     *
     * @param profileId Profile ID to select
     */
    public void selectProfile(String profileId) {
        profileService.setCurrentProfileById(profileId);
    }

    /**
     * Validates a profile.
     *
     * @param profileId Profile ID to validate
     * @return List of validation errors
     */
    public List<String> validateProfile(String profileId) {
        return profileService.findById(profileId)
                .map(profileService::validateProfile)
                .orElse(List.of("Profile not found"));
    }

    /**
     * Adds a mapping to a profile.
     *
     * @param profileId  Profile ID
     * @param noteNumber MIDI note number
     * @param keyString  Key combination string (e.g., "A", "Ctrl+B")
     * @return Updated profile
     */
    public Profile addMapping(String profileId, int noteNumber, String keyString) {
        MidiNote midiNote = new MidiNote(noteNumber);
        KeyCombination keyComb = KeyCombination.parse(keyString);
        NoteMapping mapping = new NoteMapping(midiNote, keyComb);

        return profileService.addMapping(profileId, mapping);
    }

    /**
     * Adds a mapping with channel and velocity filters.
     *
     * @param profileId   Profile ID
     * @param noteNumber  MIDI note number
     * @param keyString   Key combination string
     * @param channel     MIDI channel (-1 for any)
     * @param minVelocity Minimum velocity
     * @param maxVelocity Maximum velocity
     * @return Updated profile
     */
    public Profile addMapping(String profileId, int noteNumber, String keyString,
                              int channel, int minVelocity, int maxVelocity) {
        MidiNote midiNote = new MidiNote(noteNumber);
        KeyCombination keyComb = KeyCombination.parse(keyString);
        NoteMapping mapping = new NoteMapping(midiNote, keyComb, channel, minVelocity, maxVelocity);

        return profileService.addMapping(profileId, mapping);
    }

    /**
     * Updates playback options for a profile.
     *
     * @param profileId Profile ID
     * @param options   New playback options
     * @return Updated profile
     */
    public Profile updatePlaybackOptions(String profileId, PlaybackOptions options) {
        Profile profile = profileService.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));

        profile.setPlaybackOptions(options);
        return profileService.updateProfile(profile);
    }

    /**
     * Initializes profile system with a default profile if none exist.
     */
    public void initializeDefaults() {
        if (!profileService.hasProfiles()) {
            log.info("No profiles found, creating default profile");
            createDefaultPianoProfile("Default Piano");
        }

        profileService.restoreLastUsedProfile();
    }
}

