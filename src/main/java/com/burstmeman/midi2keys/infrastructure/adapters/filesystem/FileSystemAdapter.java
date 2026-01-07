package com.burstmeman.midi2keys.infrastructure.adapters.filesystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Adapter interface for file system operations.
 * Abstracts file system access for testing and flexibility.
 */
public interface FileSystemAdapter {
    
    /**
     * Checks if a path exists.
     * 
     * @param path Path to check
     * @return true if path exists
     */
    boolean exists(Path path);
    
    /**
     * Checks if a path is a directory.
     * 
     * @param path Path to check
     * @return true if path is a directory
     */
    boolean isDirectory(Path path);
    
    /**
     * Checks if a path is a regular file.
     * 
     * @param path Path to check
     * @return true if path is a regular file
     */
    boolean isFile(Path path);
    
    /**
     * Checks if a path is readable.
     * 
     * @param path Path to check
     * @return true if path is readable
     */
    boolean isReadable(Path path);
    
    /**
     * Lists files and directories in a directory.
     * 
     * @param directory Directory to list
     * @return List of paths in the directory
     * @throws IOException if listing fails
     */
    List<Path> listDirectory(Path directory) throws IOException;
    
    /**
     * Lists only MIDI files in a directory (non-recursive).
     * 
     * @param directory Directory to search
     * @return List of MIDI file paths
     * @throws IOException if listing fails
     */
    List<Path> listMidiFiles(Path directory) throws IOException;
    
    /**
     * Lists only subdirectories in a directory.
     * 
     * @param directory Directory to search
     * @return List of subdirectory paths
     * @throws IOException if listing fails
     */
    List<Path> listSubdirectories(Path directory) throws IOException;
    
    /**
     * Gets the file size in bytes.
     * 
     * @param path Path to file
     * @return File size in bytes
     * @throws IOException if operation fails
     */
    long getFileSize(Path path) throws IOException;
    
    /**
     * Gets the last modified time.
     * 
     * @param path Path to file or directory
     * @return Last modified time in milliseconds since epoch
     * @throws IOException if operation fails
     */
    long getLastModified(Path path) throws IOException;
    
    /**
     * Reads file contents as bytes.
     * 
     * @param path Path to file
     * @return File contents
     * @throws IOException if reading fails
     */
    byte[] readFile(Path path) throws IOException;
    
    /**
     * Checks if a path is within or equal to a root path.
     * Prevents directory traversal attacks and navigation above root.
     * 
     * @param path Path to check
     * @param root Root path boundary
     * @return true if path is within root bounds
     */
    boolean isWithinBounds(Path path, Path root);
    
    /**
     * Resolves a relative path against a base path safely.
     * Prevents directory traversal using ".." etc.
     * 
     * @param base Base path
     * @param relative Relative path to resolve
     * @return Resolved absolute path, or null if traversal detected
     */
    Path resolveSafely(Path base, String relative);
    
    /**
     * Gets the file name from a path.
     * 
     * @param path Path
     * @return File name
     */
    String getFileName(Path path);
    
    /**
     * Creates directories including parents.
     * 
     * @param path Directory path to create
     * @throws IOException if creation fails
     */
    void createDirectories(Path path) throws IOException;
}

