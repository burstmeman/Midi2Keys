package com.burstmeman.midi2keys.domain.repositories;

import com.burstmeman.midi2keys.domain.entities.Profile;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Profile persistence.
 * Profiles are stored as individual JSON files.
 */
public interface ProfileRepository {

    /**
     * Saves a profile. Creates new or updates existing.
     *
     * @param profile The profile to save
     * @return The saved profile
     */
    Profile save(Profile profile);

    /**
     * Deletes a profile by ID.
     *
     * @param id Profile ID
     * @return true if deleted
     */
    boolean delete(String id);

    /**
     * Finds a profile by ID.
     *
     * @param id Profile ID
     * @return Optional containing profile if found
     */
    Optional<Profile> findById(String id);

    /**
     * Finds a profile by name.
     *
     * @param name Profile name
     * @return Optional containing profile if found
     */
    Optional<Profile> findByName(String name);

    /**
     * Gets all profiles.
     *
     * @return List of all profiles
     */
    List<Profile> findAll();

    /**
     * Gets the default profile if set.
     *
     * @return Optional containing default profile
     */
    Optional<Profile> findDefault();

    /**
     * Sets a profile as the default.
     *
     * @param id Profile ID to set as default
     */
    void setDefault(String id);

    /**
     * Checks if a profile exists by ID.
     *
     * @param id Profile ID
     * @return true if exists
     */
    boolean existsById(String id);

    /**
     * Checks if a profile exists by name.
     *
     * @param name Profile name
     * @return true if exists
     */
    boolean existsByName(String name);

    /**
     * Gets the count of profiles.
     *
     * @return Profile count
     */
    int count();
}

