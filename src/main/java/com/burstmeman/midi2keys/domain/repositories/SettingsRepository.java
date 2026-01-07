package com.burstmeman.midi2keys.domain.repositories;

import com.burstmeman.midi2keys.domain.entities.RootDirectory;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for application settings and root directory management.
 */
public interface SettingsRepository {
    
    // ===== Root Directory Operations =====
    
    /**
     * Saves a new root directory.
     * 
     * @param rootDirectory The root directory to save
     * @return The saved root directory with generated ID
     */
    RootDirectory saveRootDirectory(RootDirectory rootDirectory);
    
    /**
     * Updates an existing root directory.
     * 
     * @param rootDirectory The root directory to update
     * @return The updated root directory
     */
    RootDirectory updateRootDirectory(RootDirectory rootDirectory);
    
    /**
     * Deletes a root directory by ID.
     * 
     * @param id The ID of the root directory to delete
     * @return true if deleted, false if not found
     */
    boolean deleteRootDirectory(Long id);
    
    /**
     * Finds a root directory by ID.
     * 
     * @param id The ID to search for
     * @return Optional containing the root directory if found
     */
    Optional<RootDirectory> findRootDirectoryById(Long id);
    
    /**
     * Finds a root directory by path.
     * 
     * @param path The path to search for
     * @return Optional containing the root directory if found
     */
    Optional<RootDirectory> findRootDirectoryByPath(String path);
    
    /**
     * Gets all root directories.
     * 
     * @return List of all root directories
     */
    List<RootDirectory> findAllRootDirectories();
    
    /**
     * Gets all active root directories.
     * 
     * @return List of active root directories
     */
    List<RootDirectory> findActiveRootDirectories();
    
    /**
     * Checks if a root directory exists by path.
     * 
     * @param path The path to check
     * @return true if exists
     */
    boolean rootDirectoryExistsByPath(String path);
    
    // ===== Application Settings Operations =====
    
    /**
     * Gets a setting value by key.
     * 
     * @param key Setting key
     * @return Optional containing the value if found
     */
    Optional<String> getSetting(String key);
    
    /**
     * Gets a setting value with a default.
     * 
     * @param key          Setting key
     * @param defaultValue Default value if not found
     * @return Setting value or default
     */
    String getSettingOrDefault(String key, String defaultValue);
    
    /**
     * Saves a setting.
     * 
     * @param key   Setting key
     * @param value Setting value
     */
    void saveSetting(String key, String value);
    
    /**
     * Deletes a setting.
     * 
     * @param key Setting key
     * @return true if deleted
     */
    boolean deleteSetting(String key);
    
    // ===== Convenience Methods for Common Settings =====
    
    /**
     * Gets the last used profile name.
     * 
     * @return Optional containing the profile name
     */
    Optional<String> getLastUsedProfile();
    
    /**
     * Saves the last used profile name.
     * 
     * @param profileName Profile name
     */
    void saveLastUsedProfile(String profileName);
    
    /**
     * Gets the last selected root directory ID.
     * 
     * @return Optional containing the root directory ID
     */
    Optional<Long> getLastRootDirectoryId();
    
    /**
     * Saves the last selected root directory ID.
     * 
     * @param rootDirectoryId Root directory ID
     */
    void saveLastRootDirectoryId(Long rootDirectoryId);
    
    /**
     * Gets the countdown duration in seconds.
     * 
     * @return Countdown seconds (default 3)
     */
    int getCountdownSeconds();
    
    /**
     * Saves the countdown duration.
     * 
     * @param seconds Countdown seconds (1-10)
     */
    void saveCountdownSeconds(int seconds);
    
    /**
     * Gets the panic stop hotkey configuration.
     * 
     * @return Hotkey string (e.g., "Ctrl+Shift+Escape")
     */
    String getPanicStopHotkey();
    
    /**
     * Saves the panic stop hotkey configuration.
     * 
     * @param hotkey Hotkey string
     */
    void savePanicStopHotkey(String hotkey);
    
    /**
     * Checks if test mode is enabled.
     * 
     * @return true if test mode is enabled
     */
    boolean isTestModeEnabled();
    
    /**
     * Sets the test mode state.
     * 
     * @param enabled true to enable test mode
     */
    void setTestModeEnabled(boolean enabled);
}

