package com.burstmeman.midi2keys.domain.entities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a root directory for MIDI file storage.
 * Users can configure multiple root directories to organize their MIDI collections.
 */
public class RootDirectory {
    
    private Long id;
    private String path;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    
    /**
     * Creates a new root directory.
     * 
     * @param path Absolute path to the directory
     * @param name Display name for the directory
     */
    public RootDirectory(String path, String name) {
        this.path = Objects.requireNonNull(path, "Path cannot be null");
        this.name = name != null && !name.isBlank() ? name : extractDefaultName(path);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }
    
    /**
     * Creates a root directory from database record.
     */
    public RootDirectory(Long id, String path, String name, LocalDateTime createdAt, 
                         LocalDateTime updatedAt, boolean active) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
    }
    
    // Getters
    
    public Long getId() {
        return id;
    }
    
    public String getPath() {
        return path;
    }
    
    public Path getPathAsPath() {
        return Paths.get(path);
    }
    
    public String getName() {
        return name;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    // Setters
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name != null && !name.isBlank() ? name : this.name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Marks this directory as updated.
     */
    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Checks if the given path is within this root directory.
     * 
     * @param childPath Path to check
     * @return true if childPath is within or equal to this root
     */
    public boolean contains(Path childPath) {
        Path normalizedRoot = getPathAsPath().toAbsolutePath().normalize();
        Path normalizedChild = childPath.toAbsolutePath().normalize();
        return normalizedChild.startsWith(normalizedRoot);
    }
    
    /**
     * Gets the relative path from this root to the given child path.
     * 
     * @param childPath Path within this root
     * @return Relative path, or null if child is not within root
     */
    public Path relativize(Path childPath) {
        if (!contains(childPath)) {
            return null;
        }
        Path normalizedRoot = getPathAsPath().toAbsolutePath().normalize();
        Path normalizedChild = childPath.toAbsolutePath().normalize();
        return normalizedRoot.relativize(normalizedChild);
    }
    
    private String extractDefaultName(String path) {
        Path p = Paths.get(path);
        Path fileName = p.getFileName();
        return fileName != null ? fileName.toString() : path;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RootDirectory that = (RootDirectory) o;
        return Objects.equals(path, that.path);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
    
    @Override
    public String toString() {
        return "RootDirectory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", active=" + active +
                '}';
    }
}

