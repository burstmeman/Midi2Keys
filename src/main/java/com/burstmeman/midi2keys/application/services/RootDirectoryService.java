package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.adapters.filesystem.FileSystemAdapter;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing root directories.
 * Handles CRUD operations and validation for MIDI file storage root directories.
 */
@Slf4j
public class RootDirectoryService {

    private final SettingsRepository settingsRepository;
    private final FileSystemAdapter fileSystemAdapter;

    public RootDirectoryService(SettingsRepository settingsRepository, FileSystemAdapter fileSystemAdapter) {
        this.settingsRepository = settingsRepository;
        this.fileSystemAdapter = fileSystemAdapter;
    }

    /**
     * Adds a new root directory.
     *
     * @param path Absolute path to the directory
     * @param name Optional display name (will use folder name if null)
     * @return The created root directory
     * @throws ApplicationException if path is invalid or already exists
     */
    public RootDirectory addRootDirectory(String path, String name) {
        Path dirPath = Paths.get(path).toAbsolutePath().normalize();
        String normalizedPath = dirPath.toString();

        // Validate directory exists
        if (!fileSystemAdapter.exists(dirPath)) {
            throw new ApplicationException(ErrorCode.DIRECTORY_NOT_FOUND,
                    "Directory does not exist: " + normalizedPath);
        }

        // Validate it's a directory
        if (!fileSystemAdapter.isDirectory(dirPath)) {
            throw new ApplicationException(ErrorCode.INVALID_CONFIGURATION,
                    "Path is not a directory: " + normalizedPath);
        }

        // Validate it's readable
        if (!fileSystemAdapter.isReadable(dirPath)) {
            throw new ApplicationException(ErrorCode.PERMISSION_DENIED,
                    "Directory is not readable: " + normalizedPath);
        }

        // Check if already exists
        if (settingsRepository.rootDirectoryExistsByPath(normalizedPath)) {
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "Root directory already exists: " + normalizedPath);
        }

        // Check for overlapping paths (nested directories)
        for (RootDirectory existing : settingsRepository.findAllRootDirectories()) {
            Path existingPath = existing.getPathAsPath();
            if (dirPath.startsWith(existingPath) || existingPath.startsWith(dirPath)) {
                throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                        "Root directories cannot be nested. Conflict with: " + existing.getName());
            }
        }

        RootDirectory rootDirectory = new RootDirectory(normalizedPath, name);
        RootDirectory saved = settingsRepository.saveRootDirectory(rootDirectory);

        log.info("Added root directory: {}", saved);
        return saved;
    }

    /**
     * Updates an existing root directory's name.
     *
     * @param id   The root directory ID
     * @param name New display name
     * @return Updated root directory
     * @throws ApplicationException if not found
     */
    public RootDirectory updateRootDirectoryName(Long id, String name) {
        RootDirectory rootDirectory = findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found: " + id));

        rootDirectory.setName(name);
        RootDirectory updated = settingsRepository.updateRootDirectory(rootDirectory);

        log.info("Updated root directory name: {}", updated);
        return updated;
    }

    /**
     * Removes a root directory.
     *
     * @param id The root directory ID
     * @return true if removed
     * @throws ApplicationException if not found
     */
    public boolean removeRootDirectory(Long id) {
        if (findById(id).isEmpty()) {
            throw new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                    "Root directory not found: " + id);
        }

        boolean deleted = settingsRepository.deleteRootDirectory(id);

        if (deleted) {
            log.info("Removed root directory: {}", id);

            // Clear last selected if it was this directory
            settingsRepository.getLastRootDirectoryId().ifPresent(lastId -> {
                if (lastId.equals(id)) {
                    settingsRepository.saveLastRootDirectoryId(null);
                }
            });
        }

        return deleted;
    }

    /**
     * Deactivates a root directory without deleting it.
     *
     * @param id The root directory ID
     * @return Updated root directory
     */
    public RootDirectory deactivateRootDirectory(Long id) {
        RootDirectory rootDirectory = findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found: " + id));

        rootDirectory.setActive(false);
        return settingsRepository.updateRootDirectory(rootDirectory);
    }

    /**
     * Reactivates a previously deactivated root directory.
     *
     * @param id The root directory ID
     * @return Updated root directory
     */
    public RootDirectory reactivateRootDirectory(Long id) {
        RootDirectory rootDirectory = findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found: " + id));

        // Verify the path still exists
        if (!fileSystemAdapter.exists(rootDirectory.getPathAsPath())) {
            throw new ApplicationException(ErrorCode.DIRECTORY_NOT_FOUND,
                    "Directory no longer exists: " + rootDirectory.getPath());
        }

        rootDirectory.setActive(true);
        return settingsRepository.updateRootDirectory(rootDirectory);
    }

    /**
     * Finds a root directory by ID.
     *
     * @param id The ID to find
     * @return Optional containing root directory if found
     */
    public Optional<RootDirectory> findById(Long id) {
        return settingsRepository.findRootDirectoryById(id);
    }

    /**
     * Finds a root directory by path.
     *
     * @param path The path to find
     * @return Optional containing root directory if found
     */
    public Optional<RootDirectory> findByPath(String path) {
        return settingsRepository.findRootDirectoryByPath(path);
    }

    /**
     * Gets all root directories.
     *
     * @return List of all root directories
     */
    public List<RootDirectory> getAllRootDirectories() {
        return settingsRepository.findAllRootDirectories();
    }

    /**
     * Gets all active root directories.
     *
     * @return List of active root directories
     */
    public List<RootDirectory> getActiveRootDirectories() {
        return settingsRepository.findActiveRootDirectories();
    }

    /**
     * Validates all root directories and marks invalid ones as inactive.
     *
     * @return List of directories that were marked invalid
     */
    public List<RootDirectory> validateAllDirectories() {
        List<RootDirectory> allDirs = settingsRepository.findAllRootDirectories();
        List<RootDirectory> invalidDirs = allDirs.stream()
                .filter(dir -> {
                    Path path = dir.getPathAsPath();
                    return !fileSystemAdapter.exists(path) ||
                            !fileSystemAdapter.isDirectory(path) ||
                            !fileSystemAdapter.isReadable(path);
                })
                .peek(dir -> {
                    if (dir.isActive()) {
                        dir.setActive(false);
                        settingsRepository.updateRootDirectory(dir);
                        log.warn("Deactivated invalid root directory: {}", dir.getPath());
                    }
                })
                .toList();

        return invalidDirs;
    }

    /**
     * Checks if a root directory path is still valid.
     *
     * @param id The root directory ID to check
     * @return true if the directory exists and is accessible
     */
    public boolean isDirectoryValid(Long id) {
        return findById(id)
                .map(dir -> {
                    Path path = dir.getPathAsPath();
                    return fileSystemAdapter.exists(path) &&
                            fileSystemAdapter.isDirectory(path) &&
                            fileSystemAdapter.isReadable(path);
                })
                .orElse(false);
    }

    /**
     * Gets the last selected root directory.
     *
     * @return Optional containing the last selected directory
     */
    public Optional<RootDirectory> getLastSelectedRootDirectory() {
        return settingsRepository.getLastRootDirectoryId()
                .flatMap(this::findById)
                .filter(dir -> dir.isActive() && isDirectoryValid(dir.getId()));
    }

    /**
     * Sets the last selected root directory.
     *
     * @param id Root directory ID
     */
    public void setLastSelectedRootDirectory(Long id) {
        settingsRepository.saveLastRootDirectoryId(id);
    }

    /**
     * Checks if there are any configured root directories.
     *
     * @return true if at least one root directory is configured
     */
    public boolean hasRootDirectories() {
        return !getAllRootDirectories().isEmpty();
    }

    /**
     * Checks if this is the first launch (no root directories configured).
     *
     * @return true if no root directories are configured
     */
    public boolean isFirstLaunch() {
        return !hasRootDirectories();
    }
}

