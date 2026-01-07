package com.burstmeman.midi2keys.application.usecases;

import com.burstmeman.midi2keys.application.services.MidiFileService;
import com.burstmeman.midi2keys.application.services.RootDirectoryService;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Use case for browsing MIDI files within root directories.
 * Handles navigation, listing, and search operations.
 */
@Slf4j
public class BrowseMidiFilesUseCase {

    private final RootDirectoryService rootDirectoryService;
    private final MidiFileService midiFileService;

    public BrowseMidiFilesUseCase(RootDirectoryService rootDirectoryService,
                                  MidiFileService midiFileService) {
        this.rootDirectoryService = rootDirectoryService;
        this.midiFileService = midiFileService;
    }

    /**
     * Gets the contents of a folder within a root directory.
     *
     * @param rootDirectoryId The root directory ID
     * @param folderPath      Relative path to folder (empty for root)
     * @return Folder contents including subdirectories and MIDI files
     */
    public FolderContents getFolderContents(Long rootDirectoryId, String folderPath) {
        RootDirectory rootDirectory = getRootDirectory(rootDirectoryId);

        String normalizedPath = normalizePath(folderPath);

        log.debug("Getting folder contents: {} in root {}", normalizedPath, rootDirectoryId);

        // Get subdirectories
        List<String> subdirectories = midiFileService.getSubdirectories(rootDirectory, normalizedPath);

        // Get MIDI files
        List<MidiFile> midiFiles = midiFileService.getMidiFilesInFolder(rootDirectory, normalizedPath);

        // Check if can navigate to parent
        boolean canNavigateUp = midiFileService.canNavigateToParent(rootDirectory, normalizedPath);
        String parentPath = canNavigateUp ? midiFileService.getParentFolder(normalizedPath) : null;

        return new FolderContents(
                rootDirectory,
                normalizedPath,
                subdirectories,
                midiFiles,
                canNavigateUp,
                parentPath
        );
    }

    /**
     * Navigates to a subfolder.
     *
     * @param rootDirectoryId The root directory ID
     * @param currentPath     Current folder path
     * @param subfolderName   Name of the subfolder to navigate to
     * @return Folder contents of the subfolder
     */
    public FolderContents navigateToSubfolder(Long rootDirectoryId, String currentPath, String subfolderName) {
        String newPath = currentPath.isEmpty() ? subfolderName : currentPath + "/" + subfolderName;
        return getFolderContents(rootDirectoryId, newPath);
    }

    /**
     * Navigates to the parent folder.
     *
     * @param rootDirectoryId The root directory ID
     * @param currentPath     Current folder path
     * @return Folder contents of the parent folder
     * @throws ApplicationException if already at root
     */
    public FolderContents navigateToParent(Long rootDirectoryId, String currentPath) {
        RootDirectory rootDirectory = getRootDirectory(rootDirectoryId);

        if (!midiFileService.canNavigateToParent(rootDirectory, currentPath)) {
            throw new ApplicationException(ErrorCode.PATH_TRAVERSAL,
                    "Already at root directory. Cannot navigate further up.");
        }

        String parentPath = midiFileService.getParentFolder(currentPath);
        return getFolderContents(rootDirectoryId, parentPath);
    }

    /**
     * Searches for MIDI files by filename.
     *
     * @param rootDirectoryId The root directory ID
     * @param searchPattern   Search pattern (case-insensitive)
     * @return List of matching MIDI files
     */
    public List<MidiFile> searchMidiFiles(Long rootDirectoryId, String searchPattern) {
        RootDirectory rootDirectory = getRootDirectory(rootDirectoryId);

        if (searchPattern == null || searchPattern.isBlank()) {
            return List.of();
        }

        log.debug("Searching for '{}' in root {}", searchPattern, rootDirectoryId);
        return midiFileService.searchMidiFiles(rootDirectory, searchPattern);
    }

    /**
     * Gets a specific MIDI file by ID.
     *
     * @param midiFileId The MIDI file ID
     * @return Optional containing the MIDI file
     */
    public Optional<MidiFile> getMidiFile(Long midiFileId) {
        return midiFileService.findById(midiFileId);
    }

    /**
     * Gets a MIDI file with its associated root directory.
     *
     * @param midiFileId The MIDI file ID
     * @return Optional containing the file and directory pair
     */
    public Optional<MidiFileWithRoot> getMidiFileWithRoot(Long midiFileId) {
        return midiFileService.findById(midiFileId)
                .flatMap(file -> rootDirectoryService.findById(file.getRootDirectoryId())
                        .map(root -> new MidiFileWithRoot(file, root)));
    }

    /**
     * Updates the note shift configuration for a MIDI file.
     *
     * @param midiFileId The MIDI file ID
     * @param noteShift  Note shift value (-4 to +4)
     */
    public void updateNoteShift(Long midiFileId, int noteShift) {
        midiFileService.updateNoteShift(midiFileId, noteShift);
    }

    /**
     * Gets the count of MIDI files in a root directory.
     *
     * @param rootDirectoryId The root directory ID
     * @return File count
     */
    public int getMidiFileCount(Long rootDirectoryId) {
        return midiFileService.countByRootDirectory(rootDirectoryId);
    }

    /**
     * Switches to a different root directory.
     *
     * @param rootDirectoryId The root directory ID to switch to
     * @return Folder contents of the new root's top level
     */
    public FolderContents switchRootDirectory(Long rootDirectoryId) {
        rootDirectoryService.setLastSelectedRootDirectory(rootDirectoryId);
        return getFolderContents(rootDirectoryId, "");
    }

    /**
     * Gets the breadcrumb path for navigation display.
     *
     * @param rootDirectory The root directory
     * @param currentPath   Current folder path
     * @return List of breadcrumb items
     */
    public List<BreadcrumbItem> getBreadcrumbs(RootDirectory rootDirectory, String currentPath) {
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();

        // Add root as first item
        breadcrumbs.add(new BreadcrumbItem(rootDirectory.getName(), "", true));

        if (currentPath == null || currentPath.isEmpty()) {
            return breadcrumbs;
        }

        // Add each path segment
        String[] segments = currentPath.split("[/\\\\]");
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            if (!segments[i].isEmpty()) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(segments[i]);

                boolean isLast = (i == segments.length - 1);
                breadcrumbs.add(new BreadcrumbItem(segments[i], pathBuilder.toString(), !isLast));
            }
        }

        return breadcrumbs;
    }

    // ===== Private Helper Methods =====

    private RootDirectory getRootDirectory(Long rootDirectoryId) {
        return rootDirectoryService.findById(rootDirectoryId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found: " + rootDirectoryId));
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        // Replace backslashes with forward slashes and remove leading/trailing slashes
        return path.replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    // ===== Result Types =====

    /**
     * Contents of a folder including files and subdirectories.
     */
    public record FolderContents(
            RootDirectory rootDirectory,
            String currentPath,
            List<String> subdirectories,
            List<MidiFile> midiFiles,
            boolean canNavigateUp,
            String parentPath
    ) {
        public int getTotalItems() {
            return subdirectories.size() + midiFiles.size();
        }

        public boolean isEmpty() {
            return subdirectories.isEmpty() && midiFiles.isEmpty();
        }
    }

    /**
     * MIDI file with its associated root directory.
     */
    public record MidiFileWithRoot(MidiFile midiFile, RootDirectory rootDirectory) {
    }

    /**
     * Breadcrumb navigation item.
     */
    public record BreadcrumbItem(String name, String path, boolean isNavigable) {
    }
}

