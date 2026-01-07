package com.burstmeman.midi2keys.infrastructure.persistence.database.dao;

import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.repositories.MidiFileRepository;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseException;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for MIDI file metadata.
 */
public class MidiFileDao implements MidiFileRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(MidiFileDao.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final DatabaseManager databaseManager;
    
    public MidiFileDao() {
        this.databaseManager = DatabaseManager.getInstance();
    }
    
    public MidiFileDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    @Override
    public MidiFile save(MidiFile midiFile) {
        String sql = """
            INSERT INTO midi_files (root_directory_id, relative_path, file_name, file_size, last_modified, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, midiFile.getRootDirectoryId());
            pstmt.setString(2, midiFile.getRelativePath());
            pstmt.setString(3, midiFile.getFileName());
            pstmt.setObject(4, midiFile.getFileSize());
            pstmt.setString(5, midiFile.getLastModified() != null ? midiFile.getLastModified().toString() : null);
            pstmt.setString(6, midiFile.getCreatedAt().format(DATE_FORMATTER));
            pstmt.setString(7, midiFile.getUpdatedAt().format(DATE_FORMATTER));
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    midiFile.setId(generatedKeys.getLong(1));
                }
            }
            
            // Save note shift if set
            if (midiFile.getNoteShift() != null && midiFile.getNoteShift() != 0) {
                saveNoteShift(midiFile.getId(), midiFile.getNoteShift());
            }
            
            logger.debug("Saved MIDI file: {}", midiFile);
            return midiFile;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save MIDI file", e);
        }
    }
    
    @Override
    public MidiFile update(MidiFile midiFile) {
        String sql = """
            UPDATE midi_files SET file_size = ?, last_modified = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            midiFile.markUpdated();
            
            pstmt.setObject(1, midiFile.getFileSize());
            pstmt.setString(2, midiFile.getLastModified() != null ? midiFile.getLastModified().toString() : null);
            pstmt.setString(3, midiFile.getUpdatedAt().format(DATE_FORMATTER));
            pstmt.setLong(4, midiFile.getId());
            
            pstmt.executeUpdate();
            
            // Update note shift
            saveNoteShift(midiFile.getId(), midiFile.getNoteShift() != null ? midiFile.getNoteShift() : 0);
            
            logger.debug("Updated MIDI file: {}", midiFile);
            return midiFile;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update MIDI file", e);
        }
    }
    
    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM midi_files WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            int affected = pstmt.executeUpdate();
            
            logger.debug("Deleted MIDI file with id: {}, affected: {}", id, affected);
            return affected > 0;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete MIDI file", e);
        }
    }
    
    @Override
    public Optional<MidiFile> findById(Long id) {
        String sql = """
            SELECT mf.*, fns.note_shift
            FROM midi_files mf
            LEFT JOIN file_note_shifts fns ON mf.id = fns.midi_file_id
            WHERE mf.id = ?
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMidiFile(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find MIDI file by id", e);
        }
    }
    
    @Override
    public Optional<MidiFile> findByRootAndPath(Long rootDirectoryId, String relativePath) {
        String sql = """
            SELECT mf.*, fns.note_shift
            FROM midi_files mf
            LEFT JOIN file_note_shifts fns ON mf.id = fns.midi_file_id
            WHERE mf.root_directory_id = ? AND mf.relative_path = ?
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rootDirectoryId);
            pstmt.setString(2, relativePath);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMidiFile(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find MIDI file by root and path", e);
        }
    }
    
    @Override
    public List<MidiFile> findByRootDirectory(Long rootDirectoryId) {
        String sql = """
            SELECT mf.*, fns.note_shift
            FROM midi_files mf
            LEFT JOIN file_note_shifts fns ON mf.id = fns.midi_file_id
            WHERE mf.root_directory_id = ?
            ORDER BY mf.file_name
            """;
        
        return executeQueryForList(sql, pstmt -> pstmt.setLong(1, rootDirectoryId));
    }
    
    @Override
    public List<MidiFile> findByRootDirectoryAndFolder(Long rootDirectoryId, String folderPath) {
        // Match files where relative_path starts with folderPath and doesn't contain additional path separators
        String pattern = folderPath.isEmpty() ? "%" : folderPath + "/%";
        String excludePattern = folderPath.isEmpty() ? "%/%" : folderPath + "/%/%";
        
        String sql = """
            SELECT mf.*, fns.note_shift
            FROM midi_files mf
            LEFT JOIN file_note_shifts fns ON mf.id = fns.midi_file_id
            WHERE mf.root_directory_id = ?
            AND mf.relative_path LIKE ?
            AND mf.relative_path NOT LIKE ?
            ORDER BY mf.file_name
            """;
        
        return executeQueryForList(sql, pstmt -> {
            pstmt.setLong(1, rootDirectoryId);
            pstmt.setString(2, pattern);
            pstmt.setString(3, excludePattern);
        });
    }
    
    @Override
    public List<MidiFile> searchByFilename(Long rootDirectoryId, String searchPattern) {
        String sql = """
            SELECT mf.*, fns.note_shift
            FROM midi_files mf
            LEFT JOIN file_note_shifts fns ON mf.id = fns.midi_file_id
            WHERE mf.root_directory_id = ?
            AND mf.file_name LIKE ?
            ORDER BY mf.file_name
            """;
        
        String likePattern = "%" + searchPattern.replace("%", "\\%").replace("_", "\\_") + "%";
        
        return executeQueryForList(sql, pstmt -> {
            pstmt.setLong(1, rootDirectoryId);
            pstmt.setString(2, likePattern);
        });
    }
    
    @Override
    public int countByRootDirectory(Long rootDirectoryId) {
        String sql = "SELECT COUNT(*) FROM midi_files WHERE root_directory_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rootDirectoryId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
            return 0;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count MIDI files", e);
        }
    }
    
    @Override
    public int deleteByRootDirectory(Long rootDirectoryId) {
        String sql = "DELETE FROM midi_files WHERE root_directory_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rootDirectoryId);
            int affected = pstmt.executeUpdate();
            
            logger.debug("Deleted {} MIDI files for root directory: {}", affected, rootDirectoryId);
            return affected;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete MIDI files by root directory", e);
        }
    }
    
    @Override
    public void saveNoteShift(Long midiFileId, int noteShift) {
        String sql = """
            INSERT OR REPLACE INTO file_note_shifts (midi_file_id, note_shift, created_at, updated_at)
            VALUES (?, ?, datetime('now'), datetime('now'))
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, midiFileId);
            pstmt.setInt(2, noteShift);
            pstmt.executeUpdate();
            
            logger.debug("Saved note shift for MIDI file {}: {}", midiFileId, noteShift);
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save note shift", e);
        }
    }
    
    @Override
    public int getNoteShift(Long midiFileId) {
        String sql = "SELECT note_shift FROM file_note_shifts WHERE midi_file_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, midiFileId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("note_shift");
                }
            }
            
            return 0;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get note shift", e);
        }
    }
    
    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM midi_files WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check MIDI file existence", e);
        }
    }
    
    @Override
    public boolean existsByRootAndPath(Long rootDirectoryId, String relativePath) {
        String sql = "SELECT COUNT(*) FROM midi_files WHERE root_directory_id = ? AND relative_path = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rootDirectoryId);
            pstmt.setString(2, relativePath);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check MIDI file existence", e);
        }
    }
    
    // ===== Helper Methods =====
    
    @FunctionalInterface
    private interface PreparedStatementSetter {
        void set(PreparedStatement pstmt) throws SQLException;
    }
    
    private List<MidiFile> executeQueryForList(String sql, PreparedStatementSetter setter) {
        List<MidiFile> results = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            setter.set(pstmt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToMidiFile(rs));
                }
            }
            
            return results;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to execute MIDI file query", e);
        }
    }
    
    private MidiFile mapResultSetToMidiFile(ResultSet rs) throws SQLException {
        Integer noteShift = rs.getObject("note_shift") != null ? rs.getInt("note_shift") : 0;
        
        return new MidiFile(
                rs.getLong("id"),
                rs.getLong("root_directory_id"),
                rs.getString("relative_path"),
                rs.getString("file_name"),
                rs.getObject("file_size") != null ? rs.getLong("file_size") : null,
                parseInstant(rs.getString("last_modified")),
                parseDateTime(rs.getString("created_at")),
                parseDateTime(rs.getString("updated_at")),
                noteShift
        );
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr.replace(" ", "T"), DATE_FORMATTER);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }
    
    private Instant parseInstant(String instantStr) {
        if (instantStr == null || instantStr.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(instantStr);
        } catch (Exception e) {
            return null;
        }
    }
}

