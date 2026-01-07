package com.burstmeman.midi2keys.infrastructure.persistence.json;

import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.repositories.ProfileRepository;
import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JSON file-based implementation of ProfileRepository.
 * Each profile is stored as a separate JSON file in the profiles directory.
 */
@Slf4j
public class ProfileJsonRepository implements ProfileRepository {
    private static final String PROFILE_EXTENSION = ".json";

    private final Path profilesDirectory;
    private final ProfileJsonSerializer serializer;

    // Cache for loaded profiles
    private final ConcurrentMap<String, Profile> profileCache;
    private boolean cacheLoaded = false;

    public ProfileJsonRepository() {
        this(ApplicationConfig.getProfilesDirectory());
    }

    public ProfileJsonRepository(Path profilesDirectory) {
        this.profilesDirectory = profilesDirectory;
        this.serializer = new ProfileJsonSerializer();
        this.profileCache = new ConcurrentHashMap<>();

        ensureDirectoryExists();
    }

    @Override
    public Profile save(Profile profile) {
        try {
            String json = serializer.serialize(profile);
            Path filePath = getProfilePath(profile.getId());

            Files.writeString(filePath, json, StandardCharsets.UTF_8);
            profileCache.put(profile.getId(), profile);

            log.info("Saved profile: {} to {}", profile.getName(), filePath);
            return profile;

        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.PROFILE_SAVE_ERROR,
                    "Failed to save profile: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            Path filePath = getProfilePath(id);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                profileCache.remove(id);
                log.info("Deleted profile: {}", id);
                return true;
            }

            return false;

        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.PROFILE_SAVE_ERROR,
                    "Failed to delete profile: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Profile> findById(String id) {
        // Check cache first
        if (profileCache.containsKey(id)) {
            return Optional.of(profileCache.get(id));
        }

        // Load from file
        try {
            Path filePath = getProfilePath(id);

            if (Files.exists(filePath)) {
                Profile profile = loadProfile(filePath);
                profileCache.put(id, profile);
                return Optional.of(profile);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to load profile: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Profile> findByName(String name) {
        ensureCacheLoaded();

        return profileCache.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Profile> findAll() {
        ensureCacheLoaded();
        return new ArrayList<>(profileCache.values());
    }

    @Override
    public Optional<Profile> findDefault() {
        ensureCacheLoaded();

        return profileCache.values().stream()
                .filter(Profile::isDefault)
                .findFirst();
    }

    @Override
    public void setDefault(String id) {
        ensureCacheLoaded();

        // Remove default from all profiles
        for (Profile profile : profileCache.values()) {
            if (profile.isDefault() && !profile.getId().equals(id)) {
                profile.setDefault(false);
                save(profile);
            }
        }

        // Set new default
        findById(id).ifPresent(profile -> {
            profile.setDefault(true);
            save(profile);
        });
    }

    @Override
    public boolean existsById(String id) {
        if (profileCache.containsKey(id)) {
            return true;
        }
        return Files.exists(getProfilePath(id));
    }

    @Override
    public boolean existsByName(String name) {
        ensureCacheLoaded();

        return profileCache.values().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }

    @Override
    public int count() {
        ensureCacheLoaded();
        return profileCache.size();
    }

    /**
     * Reloads all profiles from disk.
     */
    public void refresh() {
        profileCache.clear();
        cacheLoaded = false;
        ensureCacheLoaded();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(profilesDirectory)) {
                Files.createDirectories(profilesDirectory);
                log.info("Created profiles directory: {}", profilesDirectory);
            }
        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.PROFILE_SAVE_ERROR,
                    "Failed to create profiles directory", e);
        }
    }

    private void ensureCacheLoaded() {
        if (cacheLoaded) {
            return;
        }

        synchronized (profileCache) {
            if (cacheLoaded) {
                return;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDirectory,
                    "*" + PROFILE_EXTENSION)) {

                for (Path filePath : stream) {
                    try {
                        Profile profile = loadProfile(filePath);
                        profileCache.put(profile.getId(), profile);
                    } catch (Exception e) {
                        log.error("Failed to load profile from {}: {}", filePath, e.getMessage());
                    }
                }

                cacheLoaded = true;
                log.info("Loaded {} profiles from disk", profileCache.size());

            } catch (IOException e) {
                log.error("Failed to scan profiles directory", e);
            }
        }
    }

    private Profile loadProfile(Path filePath) throws Exception {
        String json = Files.readString(filePath, StandardCharsets.UTF_8);
        return serializer.deserialize(json);
    }

    private Path getProfilePath(String id) {
        // Sanitize ID for use as filename
        String safeId = id.replaceAll("[^a-zA-Z0-9-_]", "_");
        return profilesDirectory.resolve(safeId + PROFILE_EXTENSION);
    }
}

