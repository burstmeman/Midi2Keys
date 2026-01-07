package com.burstmeman.midi2keys.domain.entities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a MIDI file within a root directory.
 * Stores metadata and reference to the file's location.
 */
public class MidiFile {
    
    private Long id;
    private Long rootDirectoryId;
    private String relativePath;
    private String fileName;
    private Long fileSize;
    private Instant lastModified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Per-file note shift configuration (1-4 notes up or down)
    private Integer noteShift;
    
    /**
     * Creates a new MIDI file reference.
     * 
     * @param rootDirectoryId ID of the parent root directory
     * @param relativePath    Path relative to the root directory
     * @param fileName        Name of the file
     */
    public MidiFile(Long rootDirectoryId, String relativePath, String fileName) {
        this.rootDirectoryId = Objects.requireNonNull(rootDirectoryId, "Root directory ID cannot be null");
        this.relativePath = Objects.requireNonNull(relativePath, "Relative path cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.noteShift = 0;
    }
    
    /**
     * Creates a MIDI file from database record.
     */
    public MidiFile(Long id, Long rootDirectoryId, String relativePath, String fileName,
                    Long fileSize, Instant lastModified, LocalDateTime createdAt, 
                    LocalDateTime updatedAt, Integer noteShift) {
        this.id = id;
        this.rootDirectoryId = rootDirectoryId;
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.noteShift = noteShift != null ? noteShift : 0;
    }
    
    // Getters
    
    public Long getId() {
        return id;
    }
    
    public Long getRootDirectoryId() {
        return rootDirectoryId;
    }
    
    public String getRelativePath() {
        return relativePath;
    }
    
    public Path getRelativePathAsPath() {
        return Paths.get(relativePath);
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public Instant getLastModified() {
        return lastModified;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public Integer getNoteShift() {
        return noteShift;
    }
    
    // Setters
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
        markUpdated();
    }
    
    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
        markUpdated();
    }
    
    /**
     * Sets the per-file note shift configuration.
     * Valid range is -4 to +4.
     * 
     * @param noteShift Note shift value
     * @throws IllegalArgumentException if shift is out of range
     */
    public void setNoteShift(Integer noteShift) {
        if (noteShift != null && (noteShift < -4 || noteShift > 4)) {
            throw new IllegalArgumentException("Note shift must be between -4 and 4, got: " + noteShift);
        }
        this.noteShift = noteShift != null ? noteShift : 0;
        markUpdated();
    }
    
    /**
     * Marks the file as updated.
     */
    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Gets the absolute path to this file given a root directory path.
     * 
     * @param rootPath Path of the root directory
     * @return Absolute path to the MIDI file
     */
    public Path getAbsolutePath(Path rootPath) {
        return rootPath.resolve(relativePath);
    }
    
    /**
     * Gets a user-friendly display name for the file.
     * 
     * @return File name without extension
     */
    public String getDisplayName() {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
    
    /**
     * Gets a formatted file size string.
     * 
     * @return Human-readable file size (e.g., "1.5 KB")
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "Unknown";
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }
    }
    
    /**
     * Checks if the file has a custom note shift configured.
     * 
     * @return true if note shift is non-zero
     */
    public boolean hasNoteShift() {
        return noteShift != null && noteShift != 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MidiFile midiFile = (MidiFile) o;
        return Objects.equals(rootDirectoryId, midiFile.rootDirectoryId) &&
               Objects.equals(relativePath, midiFile.relativePath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(rootDirectoryId, relativePath);
    }
    
    @Override
    public String toString() {
        return "MidiFile{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", relativePath='" + relativePath + '\'' +
                ", noteShift=" + noteShift +
                '}';
    }
}

