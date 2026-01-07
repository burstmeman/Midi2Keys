package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.NoteMapping;
import com.burstmeman.midi2keys.domain.entities.PlaybackOptions;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.repositories.ProfileRepository;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing MIDI-to-keyboard mapping profiles.
 */
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final SettingsRepository settingsRepository;

    // Currently selected profile (application-wide)
    private Profile currentProfile;

    public ProfileService(ProfileRepository profileRepository, SettingsRepository settingsRepository) {
        this.profileRepository = profileRepository;
        this.settingsRepository = settingsRepository;
    }

    /**
     * Creates a new profile.
     *
     * @param name Profile name
     * @return Created profile
     * @throws ApplicationException if name already exists
     */
    public Profile createProfile(String name) {
        validateProfileName(name);

        if (profileRepository.existsByName(name)) {
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "A profile with this name already exists: " + name);
        }

        Profile profile = new Profile(name);
        Profile saved = profileRepository.save(profile);

        log.info("Created profile: {}", saved.getName());
        return saved;
    }

    /**
     * Creates a profile with initial mappings.
     *
     * @param name     Profile name
     * @param mappings Initial note mappings
     * @param options  Playback options
     * @return Created profile
     */
    public Profile createProfile(String name, List<NoteMapping> mappings, PlaybackOptions options) {
        Profile profile = createProfile(name);

        if (mappings != null) {
            for (NoteMapping mapping : mappings) {
                profile.addMapping(mapping);
            }
        }

        if (options != null) {
            profile.setPlaybackOptions(options);
        }

        return profileRepository.save(profile);
    }

    /**
     * Updates a profile.
     *
     * @param profile Profile to update
     * @return Updated profile
     */
    public Profile updateProfile(Profile profile) {
        if (!profileRepository.existsById(profile.getId())) {
            throw new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                    "Profile not found: " + profile.getId());
        }

        Profile saved = profileRepository.save(profile);

        // Update current profile if it's the same one
        if (currentProfile != null && currentProfile.getId().equals(profile.getId())) {
            currentProfile = saved;
        }

        log.info("Updated profile: {}", saved.getName());
        return saved;
    }

    /**
     * Renames a profile.
     *
     * @param id      Profile ID
     * @param newName New name
     * @return Updated profile
     */
    public Profile renameProfile(String id, String newName) {
        validateProfileName(newName);

        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                        "Profile not found: " + id));

        // Check if new name conflicts with another profile
        profileRepository.findByName(newName).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                        "A profile with this name already exists: " + newName);
            }
        });

        profile.setName(newName);
        return updateProfile(profile);
    }

    /**
     * Deletes a profile.
     *
     * @param id Profile ID
     * @return true if deleted
     */
    public boolean deleteProfile(String id) {
        if (!profileRepository.existsById(id)) {
            throw new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                    "Profile not found: " + id);
        }

        boolean deleted = profileRepository.delete(id);

        if (deleted) {
            log.info("Deleted profile: {}", id);

            // Clear current profile if it was deleted
            if (currentProfile != null && currentProfile.getId().equals(id)) {
                currentProfile = null;
                settingsRepository.saveLastUsedProfile(null);
            }
        }

        return deleted;
    }

    /**
     * Duplicates a profile.
     *
     * @param id      Profile ID to duplicate
     * @param newName Name for the copy
     * @return New profile
     */
    public Profile duplicateProfile(String id, String newName) {
        Profile original = profileRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                        "Profile not found: " + id));

        validateProfileName(newName);

        if (profileRepository.existsByName(newName)) {
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "A profile with this name already exists: " + newName);
        }

        Profile copy = original.copy(newName);
        Profile saved = profileRepository.save(copy);

        log.info("Duplicated profile '{}' as '{}'", original.getName(), newName);
        return saved;
    }

    /**
     * Finds a profile by ID.
     *
     * @param id Profile ID
     * @return Optional containing profile if found
     */
    public Optional<Profile> findById(String id) {
        return profileRepository.findById(id);
    }

    /**
     * Finds a profile by name.
     *
     * @param name Profile name
     * @return Optional containing profile if found
     */
    public Optional<Profile> findByName(String name) {
        return profileRepository.findByName(name);
    }

    /**
     * Gets all profiles.
     *
     * @return List of all profiles
     */
    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    /**
     * Gets the default profile.
     *
     * @return Optional containing default profile
     */
    public Optional<Profile> getDefaultProfile() {
        return profileRepository.findDefault();
    }

    /**
     * Sets a profile as the default.
     *
     * @param id Profile ID
     */
    public void setDefaultProfile(String id) {
        profileRepository.setDefault(id);
        log.info("Set default profile: {}", id);
    }

    /**
     * Gets the currently selected profile.
     *
     * @return Current profile, or null if none selected
     */
    public Profile getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Sets the currently selected profile.
     *
     * @param profile Profile to select (null to clear)
     */
    public void setCurrentProfile(Profile profile) {
        this.currentProfile = profile;

        if (profile != null) {
            settingsRepository.saveLastUsedProfile(profile.getId());
            log.info("Selected profile: {}", profile.getName());
        } else {
            settingsRepository.saveLastUsedProfile(null);
            log.info("Cleared profile selection");
        }
    }

    /**
     * Sets the current profile by ID.
     *
     * @param id Profile ID
     */
    public void setCurrentProfileById(String id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                        "Profile not found: " + id));
        setCurrentProfile(profile);
    }

    /**
     * Restores the last used profile from settings.
     *
     * @return true if a profile was restored
     */
    public boolean restoreLastUsedProfile() {
        Optional<String> lastUsedId = settingsRepository.getLastUsedProfile();

        if (lastUsedId.isPresent()) {
            Optional<Profile> profile = profileRepository.findById(lastUsedId.get());
            if (profile.isPresent()) {
                this.currentProfile = profile.get();
                log.info("Restored last used profile: {}", currentProfile.getName());
                return true;
            }
        }

        // Try default profile
        Optional<Profile> defaultProfile = profileRepository.findDefault();
        if (defaultProfile.isPresent()) {
            this.currentProfile = defaultProfile.get();
            log.info("Using default profile: {}", currentProfile.getName());
            return true;
        }

        return false;
    }

    /**
     * Adds a mapping to a profile with conflict detection.
     *
     * @param profileId Profile ID
     * @param mapping   Mapping to add
     * @return Updated profile
     * @throws ApplicationException if mapping conflicts
     */
    public Profile addMapping(String profileId, NoteMapping mapping) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                        "Profile not found: " + profileId));

        // Check for conflicts
        List<String> conflicts = findConflicts(profile, mapping);
        if (!conflicts.isEmpty()) {
            throw new ApplicationException(ErrorCode.MAPPING_CONFLICT,
                    "Mapping conflicts with existing mappings: " + String.join(", ", conflicts));
        }

        profile.addMapping(mapping);
        return updateProfile(profile);
    }

    /**
     * Removes a mapping from a profile.
     *
     * @param profileId Profile ID
     * @param mapping   Mapping to remove
     * @return Updated profile
     */
    public Profile removeMapping(String profileId, NoteMapping mapping) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PROFILE_NOT_FOUND,
                        "Profile not found: " + profileId));

        profile.removeMapping(mapping);
        return updateProfile(profile);
    }

    /**
     * Validates a profile configuration.
     *
     * @param profile Profile to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateProfile(Profile profile) {
        return profile.validate();
    }

    /**
     * Finds conflicts for a potential new mapping.
     *
     * @param profile    Profile to check
     * @param newMapping New mapping to check
     * @return List of conflicting mapping descriptions
     */
    public List<String> findConflicts(Profile profile, NoteMapping newMapping) {
        List<String> conflicts = new ArrayList<>();

        for (NoteMapping existing : profile.getNoteMappings()) {
            if (existing.conflictsWith(newMapping)) {
                conflicts.add(existing.getDescription());
            }
        }

        return conflicts;
    }

    /**
     * Checks if any profiles exist.
     *
     * @return true if at least one profile exists
     */
    public boolean hasProfiles() {
        return profileRepository.count() > 0;
    }

    private void validateProfileName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "Profile name cannot be empty");
        }

        if (name.length() > 100) {
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "Profile name cannot exceed 100 characters");
        }
    }
}

