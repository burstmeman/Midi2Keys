package com.burstmeman.midi2keys.infrastructure.persistence.database.dao;

import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseException;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for root directories and application settings.
 * Implements SettingsRepository interface.
 */
@Slf4j
public class RootDirectoryDao implements SettingsRepository {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DatabaseManager databaseManager;

    public RootDirectoryDao() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public RootDirectoryDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // ===== Root Directory Operations =====

    @Override
    public RootDirectory saveRootDirectory(RootDirectory rootDirectory) {
        String sql = "INSERT INTO root_directories (path, name, created_at, updated_at, is_active) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, rootDirectory.getPath());
            pstmt.setString(2, rootDirectory.getName());
            pstmt.setString(3, rootDirectory.getCreatedAt().format(DATE_FORMATTER));
            pstmt.setString(4, rootDirectory.getUpdatedAt().format(DATE_FORMATTER));
            pstmt.setInt(5, rootDirectory.isActive() ? 1 : 0);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rootDirectory.setId(generatedKeys.getLong(1));
                }
            }

            log.debug("Saved root directory: {}", rootDirectory);
            return rootDirectory;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save root directory", e);
        }
    }

    @Override
    public RootDirectory updateRootDirectory(RootDirectory rootDirectory) {
        String sql = "UPDATE root_directories SET name = ?, updated_at = ?, is_active = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            rootDirectory.markUpdated();

            pstmt.setString(1, rootDirectory.getName());
            pstmt.setString(2, rootDirectory.getUpdatedAt().format(DATE_FORMATTER));
            pstmt.setInt(3, rootDirectory.isActive() ? 1 : 0);
            pstmt.setLong(4, rootDirectory.getId());

            pstmt.executeUpdate();

            log.debug("Updated root directory: {}", rootDirectory);
            return rootDirectory;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update root directory", e);
        }
    }

    @Override
    public boolean deleteRootDirectory(Long id) {
        String sql = "DELETE FROM root_directories WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affected = pstmt.executeUpdate();

            log.debug("Deleted root directory with id: {}, affected: {}", id, affected);
            return affected > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete root directory", e);
        }
    }

    @Override
    public Optional<RootDirectory> findRootDirectoryById(Long id) {
        String sql = "SELECT * FROM root_directories WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRootDirectory(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to find root directory by id", e);
        }
    }

    @Override
    public Optional<RootDirectory> findRootDirectoryByPath(String path) {
        String sql = "SELECT * FROM root_directories WHERE path = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, path);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRootDirectory(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to find root directory by path", e);
        }
    }

    @Override
    public List<RootDirectory> findAllRootDirectories() {
        String sql = "SELECT * FROM root_directories ORDER BY name";
        List<RootDirectory> directories = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                directories.add(mapResultSetToRootDirectory(rs));
            }

            return directories;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to find all root directories", e);
        }
    }

    @Override
    public List<RootDirectory> findActiveRootDirectories() {
        String sql = "SELECT * FROM root_directories WHERE is_active = 1 ORDER BY name";
        List<RootDirectory> directories = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                directories.add(mapResultSetToRootDirectory(rs));
            }

            return directories;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active root directories", e);
        }
    }

    @Override
    public boolean rootDirectoryExistsByPath(String path) {
        String sql = "SELECT COUNT(*) FROM root_directories WHERE path = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, path);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check root directory existence", e);
        }
    }

    // ===== Application Settings Operations =====

    @Override
    public Optional<String> getSetting(String key) {
        String sql = "SELECT value FROM application_settings WHERE key = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("value"));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to get setting: " + key, e);
        }
    }

    @Override
    public String getSettingOrDefault(String key, String defaultValue) {
        return getSetting(key).orElse(defaultValue);
    }

    @Override
    public void saveSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO application_settings (key, value, updated_at) VALUES (?, ?, datetime('now'))";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();

            log.debug("Saved setting: {} = {}", key, value);

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save setting: " + key, e);
        }
    }

    @Override
    public boolean deleteSetting(String key) {
        String sql = "DELETE FROM application_settings WHERE key = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete setting: " + key, e);
        }
    }

    // ===== Convenience Methods =====

    @Override
    public Optional<String> getLastUsedProfile() {
        return getSetting("last_used_profile");
    }

    @Override
    public void saveLastUsedProfile(String profileName) {
        saveSetting("last_used_profile", profileName);
    }

    @Override
    public Optional<Long> getLastRootDirectoryId() {
        return getSetting("last_root_directory_id")
                .filter(s -> !s.isBlank())
                .map(Long::parseLong);
    }

    @Override
    public void saveLastRootDirectoryId(Long rootDirectoryId) {
        saveSetting("last_root_directory_id", rootDirectoryId != null ? rootDirectoryId.toString() : null);
    }

    @Override
    public int getCountdownSeconds() {
        return getSetting("countdown_seconds")
                .map(Integer::parseInt)
                .orElse(ApplicationConfig.DEFAULT_COUNTDOWN_SECONDS);
    }

    @Override
    public void saveCountdownSeconds(int seconds) {
        int clamped = Math.max(ApplicationConfig.MIN_COUNTDOWN_SECONDS,
                Math.min(ApplicationConfig.MAX_COUNTDOWN_SECONDS, seconds));
        saveSetting("countdown_seconds", String.valueOf(clamped));
    }

    @Override
    public String getPanicStopHotkey() {
        return getSettingOrDefault("panic_stop_hotkey", ApplicationConfig.DEFAULT_PANIC_STOP_HOTKEY);
    }

    @Override
    public void savePanicStopHotkey(String hotkey) {
        saveSetting("panic_stop_hotkey", hotkey);
    }

    @Override
    public boolean isTestModeEnabled() {
        return getSetting("test_mode_enabled")
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public void setTestModeEnabled(boolean enabled) {
        saveSetting("test_mode_enabled", String.valueOf(enabled));
    }

    // ===== Helper Methods =====

    private RootDirectory mapResultSetToRootDirectory(ResultSet rs) throws SQLException {
        return new RootDirectory(
                rs.getLong("id"),
                rs.getString("path"),
                rs.getString("name"),
                parseDateTime(rs.getString("created_at")),
                parseDateTime(rs.getString("updated_at")),
                rs.getInt("is_active") == 1
        );
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_FORMATTER);
        } catch (Exception e) {
            // Try parsing without 'T' separator (SQLite default format)
            try {
                return LocalDateTime.parse(dateTimeStr.replace(" ", "T"), DATE_FORMATTER);
            } catch (Exception e2) {
                log.warn("Failed to parse datetime: {}", dateTimeStr);
                return LocalDateTime.now();
            }
        }
    }
}

