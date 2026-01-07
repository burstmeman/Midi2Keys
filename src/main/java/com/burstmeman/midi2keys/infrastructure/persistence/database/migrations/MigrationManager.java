package com.burstmeman.midi2keys.infrastructure.persistence.database.migrations;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages database schema migrations.
 * Tracks applied migrations and executes pending ones in order.
 */
@Slf4j
public class MigrationManager {

    private final Connection connection;
    private final List<Migration> migrations;

    public MigrationManager(Connection connection) {
        this.connection = connection;
        this.migrations = initializeMigrations();
    }

    /**
     * Runs all pending migrations.
     */
    public void runMigrations() {
        try {
            ensureMigrationTableExists();

            List<String> appliedMigrations = getAppliedMigrations();

            for (Migration migration : migrations) {
                if (!appliedMigrations.contains(migration.version())) {
                    applyMigration(migration);
                }
            }

            log.info("All migrations completed successfully");

        } catch (SQLException e) {
            log.error("Migration failed", e);
            throw new RuntimeException("Database migration failed", e);
        }
    }

    /**
     * Gets the current schema version.
     *
     * @return Current version or "0" if no migrations applied
     */
    public String getCurrentVersion() {
        try {
            List<String> applied = getAppliedMigrations();
            return applied.isEmpty() ? "0" : applied.get(applied.size() - 1);
        } catch (SQLException e) {
            return "0";
        }
    }

    private void ensureMigrationTableExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version TEXT PRIMARY KEY,
                    applied_at TEXT NOT NULL,
                    description TEXT
                )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private List<String> getAppliedMigrations() throws SQLException {
        List<String> versions = new ArrayList<>();
        String sql = "SELECT version FROM schema_migrations ORDER BY version";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                versions.add(rs.getString("version"));
            }
        }

        return versions;
    }

    private void applyMigration(Migration migration) throws SQLException {
        log.info("Applying migration {}: {}", migration.version(), migration.description());

        connection.setAutoCommit(false);
        try {
            // Execute migration SQL
            try (Statement stmt = connection.createStatement()) {
                for (String sql : migration.sql().split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }

            // Record migration
            String recordSql = "INSERT INTO schema_migrations (version, applied_at, description) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(recordSql)) {
                pstmt.setString(1, migration.version());
                pstmt.setString(2, Instant.now().toString());
                pstmt.setString(3, migration.description());
                pstmt.executeUpdate();
            }

            connection.commit();
            log.info("Migration {} applied successfully", migration.version());

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private List<Migration> initializeMigrations() {
        List<Migration> list = new ArrayList<>();

        // V001: Initial schema
        list.add(new Migration("V001", "Initial schema - root directories, midi files, analyses, settings", """
                -- Root directories table
                CREATE TABLE root_directories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    path TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    is_active INTEGER NOT NULL DEFAULT 1
                );
                
                -- MIDI files metadata table
                CREATE TABLE midi_files (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    root_directory_id INTEGER NOT NULL,
                    relative_path TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    file_size INTEGER,
                    last_modified TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (root_directory_id) REFERENCES root_directories(id) ON DELETE CASCADE,
                    UNIQUE(root_directory_id, relative_path)
                );
                
                -- MIDI analysis cache table
                CREATE TABLE midi_analyses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    midi_file_id INTEGER NOT NULL UNIQUE,
                    format_type INTEGER,
                    track_count INTEGER,
                    duration_ms INTEGER,
                    tempo_bpm REAL,
                    tempo_changes_count INTEGER,
                    time_signature TEXT,
                    total_notes INTEGER,
                    channel_note_counts TEXT,
                    track_note_counts TEXT,
                    min_velocity INTEGER,
                    max_velocity INTEGER,
                    avg_velocity REAL,
                    note_histogram TEXT,
                    estimated_melody_length_ms INTEGER,
                    analyzed_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (midi_file_id) REFERENCES midi_files(id) ON DELETE CASCADE
                );
                
                -- Per-file note shift settings
                CREATE TABLE file_note_shifts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    midi_file_id INTEGER NOT NULL UNIQUE,
                    note_shift INTEGER NOT NULL DEFAULT 0 CHECK(note_shift >= -4 AND note_shift <= 4),
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (midi_file_id) REFERENCES midi_files(id) ON DELETE CASCADE
                );
                
                -- Application settings table
                CREATE TABLE application_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT,
                    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
                );
                
                -- Create indexes for common queries
                CREATE INDEX idx_midi_files_root_dir ON midi_files(root_directory_id);
                CREATE INDEX idx_midi_files_filename ON midi_files(file_name);
                CREATE INDEX idx_midi_analyses_file ON midi_analyses(midi_file_id);
                CREATE INDEX idx_file_note_shifts_file ON file_note_shifts(midi_file_id);
                
                -- Insert default settings
                INSERT INTO application_settings (key, value) VALUES 
                    ('last_used_profile', NULL),
                    ('countdown_seconds', '3'),
                    ('panic_stop_hotkey', 'Ctrl+Shift+Escape'),
                    ('test_mode_enabled', 'false'),
                    ('last_root_directory_id', NULL);
                """));

        return list;
    }

    /**
         * Represents a database migration.
         */
        private record Migration(String version, String description, String sql) {
    }
}

