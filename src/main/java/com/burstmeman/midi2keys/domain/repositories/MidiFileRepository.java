package com.burstmeman.midi2keys.domain.repositories;

import com.burstmeman.midi2keys.domain.entities.MidiFile;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MIDI file metadata operations.
 */
public interface MidiFileRepository {
    
    /**
     * Saves a new MIDI file metadata.
     * 
     * @param midiFile The MIDI file to save
     * @return The saved MIDI file with generated ID
     */
    MidiFile save(MidiFile midiFile);
    
    /**
     * Updates an existing MIDI file metadata.
     * 
     * @param midiFile The MIDI file to update
     * @return The updated MIDI file
     */
    MidiFile update(MidiFile midiFile);
    
    /**
     * Deletes a MIDI file by ID.
     * 
     * @param id The ID of the MIDI file to delete
     * @return true if deleted, false if not found
     */
    boolean delete(Long id);
    
    /**
     * Finds a MIDI file by ID.
     * 
     * @param id The ID to search for
     * @return Optional containing the MIDI file if found
     */
    Optional<MidiFile> findById(Long id);
    
    /**
     * Finds a MIDI file by root directory and relative path.
     * 
     * @param rootDirectoryId The root directory ID
     * @param relativePath    The relative path within the root
     * @return Optional containing the MIDI file if found
     */
    Optional<MidiFile> findByRootAndPath(Long rootDirectoryId, String relativePath);
    
    /**
     * Finds all MIDI files in a root directory.
     * 
     * @param rootDirectoryId The root directory ID
     * @return List of MIDI files
     */
    List<MidiFile> findByRootDirectory(Long rootDirectoryId);
    
    /**
     * Finds all MIDI files in a specific folder within a root directory.
     * 
     * @param rootDirectoryId The root directory ID
     * @param folderPath      The folder path relative to root (empty string for root)
     * @return List of MIDI files in the folder (non-recursive)
     */
    List<MidiFile> findByRootDirectoryAndFolder(Long rootDirectoryId, String folderPath);
    
    /**
     * Searches MIDI files by filename pattern.
     * 
     * @param rootDirectoryId The root directory ID
     * @param searchPattern   The filename search pattern (case-insensitive)
     * @return List of matching MIDI files
     */
    List<MidiFile> searchByFilename(Long rootDirectoryId, String searchPattern);
    
    /**
     * Gets the count of MIDI files in a root directory.
     * 
     * @param rootDirectoryId The root directory ID
     * @return Count of MIDI files
     */
    int countByRootDirectory(Long rootDirectoryId);
    
    /**
     * Deletes all MIDI files in a root directory.
     * 
     * @param rootDirectoryId The root directory ID
     * @return Number of deleted records
     */
    int deleteByRootDirectory(Long rootDirectoryId);
    
    /**
     * Saves or updates the note shift for a MIDI file.
     * 
     * @param midiFileId The MIDI file ID
     * @param noteShift  The note shift value (-4 to 4)
     */
    void saveNoteShift(Long midiFileId, int noteShift);
    
    /**
     * Gets the note shift for a MIDI file.
     * 
     * @param midiFileId The MIDI file ID
     * @return Note shift value (0 if not set)
     */
    int getNoteShift(Long midiFileId);
    
    /**
     * Checks if a MIDI file exists by ID.
     * 
     * @param id The ID to check
     * @return true if exists
     */
    boolean existsById(Long id);
    
    /**
     * Checks if a MIDI file exists by root directory and path.
     * 
     * @param rootDirectoryId The root directory ID
     * @param relativePath    The relative path
     * @return true if exists
     */
    boolean existsByRootAndPath(Long rootDirectoryId, String relativePath);
}

