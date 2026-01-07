package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.domain.repositories.MidiFileRepository;
import com.burstmeman.midi2keys.infrastructure.adapters.filesystem.FileSystemAdapter;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing MIDI file metadata and browsing.
 */
@Slf4j
public class MidiFileService {

    private final MidiFileRepository midiFileRepository;
    private final FileSystemAdapter fileSystemAdapter;

    public MidiFileService(MidiFileRepository midiFileRepository, FileSystemAdapter fileSystemAdapter) {
        this.midiFileRepository = midiFileRepository;
        this.fileSystemAdapter = fileSystemAdapter;
    }

    /**
     * Scans a root directory for MIDI files and updates the database.
     *
     * @param rootDirectory The root directory to scan
     * @return Number of MIDI files found
     */
    public int scanRootDirectory(RootDirectory rootDirectory) {
        log.info("Scanning root directory: {}", rootDirectory.getPath());

        Path rootPath = rootDirectory.getPathAsPath();
        int count = scanDirectory(rootDirectory.getId(), rootPath, rootPath);

        log.info("Scan complete. Found {} MIDI files in {}", count, rootDirectory.getPath());
        return count;
    }

    /**
     * Gets MIDI files in a specific folder within a root directory.
     *
     * @param rootDirectory The root directory
     * @param folderPath    Relative path to the folder (empty string for root)
     * @return List of MIDI files in the folder
     */
    public List<MidiFile> getMidiFilesInFolder(RootDirectory rootDirectory, String folderPath) {
        Path absolutePath = rootDirectory.getPathAsPath().resolve(folderPath);

        // Validate path is within root
        if (!fileSystemAdapter.isWithinBounds(absolutePath, rootDirectory.getPathAsPath())) {
            throw new ApplicationException(ErrorCode.PATH_TRAVERSAL,
                    "Path is outside root directory bounds");
        }

        // Ensure folder exists
        if (!fileSystemAdapter.exists(absolutePath) || !fileSystemAdapter.isDirectory(absolutePath)) {
            throw new ApplicationException(ErrorCode.DIRECTORY_NOT_FOUND,
                    "Folder not found: " + folderPath);
        }

        try {
            // Get files from filesystem and sync with database
            List<Path> filesOnDisk = fileSystemAdapter.listMidiFiles(absolutePath);
            List<MidiFile> result = new ArrayList<>();

            for (Path filePath : filesOnDisk) {
                String relativePath = rootDirectory.relativize(filePath).toString();
                MidiFile midiFile = getOrCreateMidiFile(rootDirectory.getId(), relativePath, filePath);
                result.add(midiFile);
            }

            return result;

        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.FILE_NOT_READABLE,
                    "Failed to read folder: " + e.getMessage(), e);
        }
    }

    /**
     * Gets subdirectories in a specific folder within a root directory.
     *
     * @param rootDirectory The root directory
     * @param folderPath    Relative path to the folder (empty string for root)
     * @return List of subdirectory paths (relative to root)
     */
    public List<String> getSubdirectories(RootDirectory rootDirectory, String folderPath) {
        Path absolutePath = rootDirectory.getPathAsPath().resolve(folderPath);

        // Validate path is within root
        if (!fileSystemAdapter.isWithinBounds(absolutePath, rootDirectory.getPathAsPath())) {
            throw new ApplicationException(ErrorCode.PATH_TRAVERSAL,
                    "Path is outside root directory bounds");
        }

        try {
            List<Path> subdirs = fileSystemAdapter.listSubdirectories(absolutePath);
            return subdirs.stream()
                    .map(subdir -> {
                        Path relative = rootDirectory.relativize(subdir);
                        return relative != null ? relative.toString() : "";
                    })
                    .filter(s -> !s.isEmpty())
                    .toList();

        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.FILE_NOT_READABLE,
                    "Failed to read subdirectories: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the parent folder path.
     *
     * @param folderPath Current folder path
     * @return Parent folder path, or empty string if at root
     */
    public String getParentFolder(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return "";
        }

        int lastSeparator = Math.max(folderPath.lastIndexOf('/'), folderPath.lastIndexOf('\\'));
        if (lastSeparator <= 0) {
            return "";
        }

        return folderPath.substring(0, lastSeparator);
    }

    /**
     * Checks if a path can navigate to its parent within the root directory bounds.
     *
     * @param rootDirectory The root directory
     * @param currentPath   Current folder path (relative to root)
     * @return true if navigation to parent is allowed
     */
    public boolean canNavigateToParent(RootDirectory rootDirectory, String currentPath) {
        return currentPath != null && !currentPath.isEmpty();
    }

    /**
     * Searches for MIDI files by filename.
     *
     * @param rootDirectory The root directory to search
     * @param searchPattern The search pattern (case-insensitive)
     * @return List of matching MIDI files
     */
    public List<MidiFile> searchMidiFiles(RootDirectory rootDirectory, String searchPattern) {
        if (searchPattern == null || searchPattern.isBlank()) {
            return List.of();
        }

        return midiFileRepository.searchByFilename(rootDirectory.getId(), searchPattern.trim());
    }

    /**
     * Gets a MIDI file by ID.
     *
     * @param id The MIDI file ID
     * @return Optional containing the MIDI file
     */
    public Optional<MidiFile> findById(Long id) {
        return midiFileRepository.findById(id);
    }

    /**
     * Updates the note shift for a MIDI file.
     *
     * @param midiFileId The MIDI file ID
     * @param noteShift  The note shift value (-4 to 4)
     */
    public void updateNoteShift(Long midiFileId, int noteShift) {
        if (noteShift < -4 || noteShift > 4) {
            throw new ApplicationException(ErrorCode.VALIDATION_ERROR,
                    "Note shift must be between -4 and 4");
        }

        MidiFile midiFile = midiFileRepository.findById(midiFileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FILE_NOT_FOUND,
                        "MIDI file not found: " + midiFileId));

        midiFile.setNoteShift(noteShift);
        midiFileRepository.update(midiFile);

        log.debug("Updated note shift for file {}: {}", midiFileId, noteShift);
    }

    /**
     * Deletes all MIDI file metadata for a root directory.
     *
     * @param rootDirectoryId The root directory ID
     * @return Number of deleted records
     */
    public int deleteByRootDirectory(Long rootDirectoryId) {
        return midiFileRepository.deleteByRootDirectory(rootDirectoryId);
    }

    /**
     * Gets the count of MIDI files in a root directory.
     *
     * @param rootDirectoryId The root directory ID
     * @return File count
     */
    public int countByRootDirectory(Long rootDirectoryId) {
        return midiFileRepository.countByRootDirectory(rootDirectoryId);
    }

    /**
     * Validates that a MIDI file still exists on disk.
     *
     * @param midiFile      The MIDI file to validate
     * @param rootDirectory The root directory
     * @return true if file exists and is readable
     */
    public boolean validateMidiFile(MidiFile midiFile, RootDirectory rootDirectory) {
        Path absolutePath = midiFile.getAbsolutePath(rootDirectory.getPathAsPath());
        return fileSystemAdapter.exists(absolutePath) &&
                fileSystemAdapter.isFile(absolutePath) &&
                fileSystemAdapter.isReadable(absolutePath);
    }

    // ===== Private Helper Methods =====

    private int scanDirectory(Long rootDirectoryId, Path rootPath, Path currentPath) {
        int count = 0;

        try {
            // Scan MIDI files in current directory
            List<Path> midiFiles = fileSystemAdapter.listMidiFiles(currentPath);
            for (Path midiFile : midiFiles) {
                String relativePath = rootPath.relativize(midiFile).toString();
                getOrCreateMidiFile(rootDirectoryId, relativePath, midiFile);
                count++;
            }

            // Recursively scan subdirectories
            List<Path> subdirs = fileSystemAdapter.listSubdirectories(currentPath);
            for (Path subdir : subdirs) {
                count += scanDirectory(rootDirectoryId, rootPath, subdir);
            }

        } catch (IOException e) {
            log.warn("Failed to scan directory: {} - {}", currentPath, e.getMessage());
        }

        return count;
    }

    private MidiFile getOrCreateMidiFile(Long rootDirectoryId, String relativePath, Path absolutePath) {
        Optional<MidiFile> existing = midiFileRepository.findByRootAndPath(rootDirectoryId, relativePath);

        if (existing.isPresent()) {
            MidiFile midiFile = existing.get();
            // Update file metadata if changed
            try {
                long currentSize = fileSystemAdapter.getFileSize(absolutePath);
                long currentModified = fileSystemAdapter.getLastModified(absolutePath);

                if (midiFile.getFileSize() == null || !midiFile.getFileSize().equals(currentSize) ||
                        midiFile.getLastModified() == null || midiFile.getLastModified().toEpochMilli() != currentModified) {

                    midiFile.setFileSize(currentSize);
                    midiFile.setLastModified(Instant.ofEpochMilli(currentModified));
                    midiFileRepository.update(midiFile);
                }
            } catch (IOException e) {
                log.warn("Failed to update file metadata: {}", relativePath);
            }

            return midiFile;
        }

        // Create new entry
        String fileName = fileSystemAdapter.getFileName(absolutePath);
        MidiFile midiFile = new MidiFile(rootDirectoryId, relativePath, fileName);

        try {
            midiFile.setFileSize(fileSystemAdapter.getFileSize(absolutePath));
            midiFile.setLastModified(Instant.ofEpochMilli(fileSystemAdapter.getLastModified(absolutePath)));
        } catch (IOException e) {
            log.warn("Failed to get file metadata: {}", relativePath);
        }

        return midiFileRepository.save(midiFile);
    }
}

