package com.burstmeman.midi2keys.application.usecases;

import com.burstmeman.midi2keys.application.services.MidiFileService;
import com.burstmeman.midi2keys.application.services.RootDirectoryService;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Use case for configuring root directories.
 * Coordinates root directory management and initial file scanning.
 */
public class ConfigureRootDirectoryUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigureRootDirectoryUseCase.class);
    
    private final RootDirectoryService rootDirectoryService;
    private final MidiFileService midiFileService;
    
    public ConfigureRootDirectoryUseCase(RootDirectoryService rootDirectoryService, 
                                         MidiFileService midiFileService) {
        this.rootDirectoryService = rootDirectoryService;
        this.midiFileService = midiFileService;
    }
    
    /**
     * Adds a new root directory and scans for MIDI files.
     * 
     * @param path Directory path
     * @param name Optional display name
     * @return Result containing the created directory and scan results
     */
    public AddRootDirectoryResult addRootDirectory(String path, String name) {
        logger.info("Adding root directory: {}", path);
        
        // Create root directory
        RootDirectory rootDirectory = rootDirectoryService.addRootDirectory(path, name);
        
        // Scan for MIDI files
        int fileCount = midiFileService.scanRootDirectory(rootDirectory);
        
        // Set as last selected
        rootDirectoryService.setLastSelectedRootDirectory(rootDirectory.getId());
        
        logger.info("Root directory added successfully. Found {} MIDI files.", fileCount);
        
        return new AddRootDirectoryResult(rootDirectory, fileCount);
    }
    
    /**
     * Removes a root directory and its associated file metadata.
     * 
     * @param rootDirectoryId The root directory ID to remove
     * @return true if successfully removed
     */
    public boolean removeRootDirectory(Long rootDirectoryId) {
        logger.info("Removing root directory: {}", rootDirectoryId);
        
        // Delete associated MIDI file metadata first
        int deletedFiles = midiFileService.deleteByRootDirectory(rootDirectoryId);
        logger.debug("Deleted {} MIDI file records", deletedFiles);
        
        // Delete root directory
        boolean removed = rootDirectoryService.removeRootDirectory(rootDirectoryId);
        
        if (removed) {
            logger.info("Root directory removed successfully");
        }
        
        return removed;
    }
    
    /**
     * Updates a root directory's display name.
     * 
     * @param rootDirectoryId The root directory ID
     * @param newName         The new display name
     * @return Updated root directory
     */
    public RootDirectory updateRootDirectoryName(Long rootDirectoryId, String newName) {
        return rootDirectoryService.updateRootDirectoryName(rootDirectoryId, newName);
    }
    
    /**
     * Rescans a root directory for new or changed MIDI files.
     * 
     * @param rootDirectoryId The root directory ID to rescan
     * @return Number of MIDI files found
     */
    public int rescanRootDirectory(Long rootDirectoryId) {
        logger.info("Rescanning root directory: {}", rootDirectoryId);
        
        RootDirectory rootDirectory = rootDirectoryService.findById(rootDirectoryId)
                .orElseThrow(() -> new IllegalArgumentException("Root directory not found: " + rootDirectoryId));
        
        return midiFileService.scanRootDirectory(rootDirectory);
    }
    
    /**
     * Gets all configured root directories.
     * 
     * @return List of all root directories
     */
    public List<RootDirectory> getAllRootDirectories() {
        return rootDirectoryService.getAllRootDirectories();
    }
    
    /**
     * Gets all active root directories.
     * 
     * @return List of active root directories
     */
    public List<RootDirectory> getActiveRootDirectories() {
        return rootDirectoryService.getActiveRootDirectories();
    }
    
    /**
     * Gets the last selected root directory.
     * 
     * @return Optional containing the last selected directory
     */
    public Optional<RootDirectory> getLastSelectedRootDirectory() {
        return rootDirectoryService.getLastSelectedRootDirectory();
    }
    
    /**
     * Sets the last selected root directory.
     * 
     * @param rootDirectoryId The root directory ID
     */
    public void setLastSelectedRootDirectory(Long rootDirectoryId) {
        rootDirectoryService.setLastSelectedRootDirectory(rootDirectoryId);
    }
    
    /**
     * Validates all root directories and returns invalid ones.
     * 
     * @return List of directories that are no longer valid
     */
    public List<RootDirectory> validateAllDirectories() {
        return rootDirectoryService.validateAllDirectories();
    }
    
    /**
     * Checks if this is the first launch (no root directories configured).
     * 
     * @return true if first launch
     */
    public boolean isFirstLaunch() {
        return rootDirectoryService.isFirstLaunch();
    }
    
    /**
     * Result of adding a root directory.
     */
    public record AddRootDirectoryResult(RootDirectory rootDirectory, int midiFileCount) {}
}

