package com.burstmeman.midi2keys.infrastructure.adapters.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Java NIO-based implementation of FileSystemAdapter.
 */
public class JavaFileSystemAdapter implements FileSystemAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaFileSystemAdapter.class);
    
    // Supported MIDI file extensions (case-insensitive)
    private static final Set<String> MIDI_EXTENSIONS = Set.of(".mid", ".midi", ".smf");
    
    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }
    
    @Override
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }
    
    @Override
    public boolean isFile(Path path) {
        return Files.isRegularFile(path);
    }
    
    @Override
    public boolean isReadable(Path path) {
        return Files.isReadable(path);
    }
    
    @Override
    public List<Path> listDirectory(Path directory) throws IOException {
        List<Path> entries = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                entries.add(entry);
            }
        }
        
        return entries;
    }
    
    @Override
    public List<Path> listMidiFiles(Path directory) throws IOException {
        List<Path> midiFiles = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, this::isMidiFile)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    midiFiles.add(entry);
                }
            }
        }
        
        // Sort by filename for consistent ordering
        midiFiles.sort((a, b) -> getFileName(a).compareToIgnoreCase(getFileName(b)));
        
        return midiFiles;
    }
    
    @Override
    public List<Path> listSubdirectories(Path directory) throws IOException {
        List<Path> subdirs = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, Files::isDirectory)) {
            for (Path entry : stream) {
                subdirs.add(entry);
            }
        }
        
        // Sort by name for consistent ordering
        subdirs.sort((a, b) -> getFileName(a).compareToIgnoreCase(getFileName(b)));
        
        return subdirs;
    }
    
    @Override
    public long getFileSize(Path path) throws IOException {
        return Files.size(path);
    }
    
    @Override
    public long getLastModified(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }
    
    @Override
    public byte[] readFile(Path path) throws IOException {
        return Files.readAllBytes(path);
    }
    
    @Override
    public boolean isWithinBounds(Path path, Path root) {
        try {
            Path normalizedPath = path.toAbsolutePath().normalize();
            Path normalizedRoot = root.toAbsolutePath().normalize();
            
            return normalizedPath.startsWith(normalizedRoot);
        } catch (Exception e) {
            logger.warn("Error checking path bounds: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Path resolveSafely(Path base, String relative) {
        if (relative == null || relative.isEmpty()) {
            return base;
        }
        
        // Check for obvious traversal patterns
        if (relative.contains("..") || relative.startsWith("/") || relative.startsWith("\\")) {
            logger.warn("Potential path traversal detected: {}", relative);
            return null;
        }
        
        try {
            Path resolved = base.resolve(relative).normalize();
            Path normalizedBase = base.toAbsolutePath().normalize();
            
            // Verify the resolved path is still within base
            if (!resolved.toAbsolutePath().normalize().startsWith(normalizedBase)) {
                logger.warn("Path resolution escaped base directory: {} + {} = {}", base, relative, resolved);
                return null;
            }
            
            return resolved;
        } catch (Exception e) {
            logger.warn("Error resolving path: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getFileName(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "";
    }
    
    @Override
    public void createDirectories(Path path) throws IOException {
        Files.createDirectories(path);
    }
    
    /**
     * Checks if a path is a MIDI file based on extension.
     * 
     * @param path Path to check
     * @return true if path has a MIDI file extension
     */
    private boolean isMidiFile(Path path) {
        String fileName = getFileName(path).toLowerCase(Locale.ROOT);
        return MIDI_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}

