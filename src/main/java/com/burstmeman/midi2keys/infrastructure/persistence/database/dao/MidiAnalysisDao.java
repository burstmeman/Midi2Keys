package com.burstmeman.midi2keys.infrastructure.persistence.database.dao;

import com.burstmeman.midi2keys.domain.entities.MidiAnalysis;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseException;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for MIDI analysis data.
 */
public class MidiAnalysisDao {
    
    private static final Logger logger = LoggerFactory.getLogger(MidiAnalysisDao.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final DatabaseManager databaseManager;
    private final ObjectMapper objectMapper;
    
    public MidiAnalysisDao() {
        this(DatabaseManager.getInstance());
    }
    
    public MidiAnalysisDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Saves or updates a MIDI analysis.
     */
    public MidiAnalysis save(MidiAnalysis analysis) {
        if (analysis.getId() != null) {
            return update(analysis);
        }
        
        String sql = """
            INSERT INTO midi_analyses (
                midi_file_id, format_type, track_count, duration_ms, tempo_bpm,
                tempo_changes_count, time_signature, total_notes, channel_note_counts,
                track_note_counts, min_velocity, max_velocity, avg_velocity,
                note_histogram, estimated_melody_length_ms, analyzed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setStatementParameters(pstmt, analysis);
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    analysis.setId(generatedKeys.getLong(1));
                }
            }
            
            logger.debug("Saved MIDI analysis for file: {}", analysis.getMidiFileId());
            return analysis;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save MIDI analysis", e);
        }
    }
    
    private MidiAnalysis update(MidiAnalysis analysis) {
        String sql = """
            UPDATE midi_analyses SET
                format_type = ?, track_count = ?, duration_ms = ?, tempo_bpm = ?,
                tempo_changes_count = ?, time_signature = ?, total_notes = ?,
                channel_note_counts = ?, track_note_counts = ?, min_velocity = ?,
                max_velocity = ?, avg_velocity = ?, note_histogram = ?,
                estimated_melody_length_ms = ?, analyzed_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, analysis.getFormatType());
            pstmt.setInt(2, analysis.getTrackCount());
            pstmt.setLong(3, analysis.getDurationMs());
            pstmt.setFloat(4, analysis.getTempoBpm());
            pstmt.setInt(5, analysis.getTempoChangesCount());
            pstmt.setString(6, analysis.getTimeSignature());
            pstmt.setInt(7, analysis.getTotalNotes());
            pstmt.setString(8, serializeMap(analysis.getChannelNoteCounts()));
            pstmt.setString(9, serializeMap(analysis.getTrackNoteCounts()));
            pstmt.setInt(10, analysis.getMinVelocity());
            pstmt.setInt(11, analysis.getMaxVelocity());
            pstmt.setFloat(12, analysis.getAvgVelocity());
            pstmt.setString(13, serializeMap(analysis.getNoteHistogram()));
            pstmt.setLong(14, analysis.getEstimatedMelodyLengthMs());
            pstmt.setString(15, LocalDateTime.now().format(DATE_FORMATTER));
            pstmt.setLong(16, analysis.getId());
            
            pstmt.executeUpdate();
            
            logger.debug("Updated MIDI analysis: {}", analysis.getId());
            return analysis;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update MIDI analysis", e);
        }
    }
    
    /**
     * Finds analysis by MIDI file ID.
     */
    public Optional<MidiAnalysis> findByMidiFileId(Long midiFileId) {
        String sql = "SELECT * FROM midi_analyses WHERE midi_file_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, midiFileId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find MIDI analysis", e);
        }
    }
    
    /**
     * Deletes analysis for a MIDI file.
     */
    public boolean deleteByMidiFileId(Long midiFileId) {
        String sql = "DELETE FROM midi_analyses WHERE midi_file_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, midiFileId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete MIDI analysis", e);
        }
    }
    
    /**
     * Checks if analysis exists for a MIDI file.
     */
    public boolean existsByMidiFileId(Long midiFileId) {
        String sql = "SELECT COUNT(*) FROM midi_analyses WHERE midi_file_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, midiFileId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check MIDI analysis existence", e);
        }
    }
    
    private void setStatementParameters(PreparedStatement pstmt, MidiAnalysis analysis) throws SQLException {
        pstmt.setLong(1, analysis.getMidiFileId());
        pstmt.setInt(2, analysis.getFormatType());
        pstmt.setInt(3, analysis.getTrackCount());
        pstmt.setLong(4, analysis.getDurationMs());
        pstmt.setFloat(5, analysis.getTempoBpm());
        pstmt.setInt(6, analysis.getTempoChangesCount());
        pstmt.setString(7, analysis.getTimeSignature());
        pstmt.setInt(8, analysis.getTotalNotes());
        pstmt.setString(9, serializeMap(analysis.getChannelNoteCounts()));
        pstmt.setString(10, serializeMap(analysis.getTrackNoteCounts()));
        pstmt.setInt(11, analysis.getMinVelocity());
        pstmt.setInt(12, analysis.getMaxVelocity());
        pstmt.setFloat(13, analysis.getAvgVelocity());
        pstmt.setString(14, serializeMap(analysis.getNoteHistogram()));
        pstmt.setLong(15, analysis.getEstimatedMelodyLengthMs());
        pstmt.setString(16, analysis.getAnalyzedAt().format(DATE_FORMATTER));
    }
    
    private MidiAnalysis mapResultSet(ResultSet rs) throws SQLException {
        return new MidiAnalysis(
                rs.getLong("id"),
                rs.getLong("midi_file_id"),
                rs.getInt("format_type"),
                rs.getInt("track_count"),
                rs.getLong("duration_ms"),
                rs.getFloat("tempo_bpm"),
                rs.getInt("tempo_changes_count"),
                rs.getString("time_signature"),
                rs.getInt("total_notes"),
                deserializeMap(rs.getString("channel_note_counts")),
                deserializeMap(rs.getString("track_note_counts")),
                deserializeMap(rs.getString("note_histogram")),
                rs.getInt("min_velocity"),
                rs.getInt("max_velocity"),
                rs.getFloat("avg_velocity"),
                rs.getLong("estimated_melody_length_ms"),
                parseDateTime(rs.getString("analyzed_at"))
        );
    }
    
    private String serializeMap(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize map", e);
            return "{}";
        }
    }
    
    private Map<Integer, Integer> deserializeMap(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<Integer, Integer>>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize map: {}", json, e);
            return new HashMap<>();
        }
    }
    
    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(str, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(str.replace(" ", "T"), DATE_FORMATTER);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }
}

