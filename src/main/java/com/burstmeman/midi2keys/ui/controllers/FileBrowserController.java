package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.usecases.BrowseMidiFilesUseCase;
import com.burstmeman.midi2keys.application.usecases.BrowseMidiFilesUseCase.BreadcrumbItem;
import com.burstmeman.midi2keys.application.usecases.BrowseMidiFilesUseCase.FolderContents;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the File Browser panel.
 * Handles navigation, file selection, and search.
 */
public class FileBrowserController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(FileBrowserController.class);
    
    @FXML private ComboBox<RootDirectory> rootDirectoryCombo;
    @FXML private TextField searchField;
    @FXML private HBox breadcrumbContainer;
    @FXML private Button navigateUpButton;
    @FXML private ListView<Object> fileListView; // Contains both folders and files
    @FXML private Label statusLabel;
    @FXML private VBox emptyStateContainer;
    
    private BrowseMidiFilesUseCase browseMidiFilesUseCase;
    private List<RootDirectory> rootDirectories;
    private RootDirectory currentRootDirectory;
    private String currentPath = "";
    private FolderContents currentContents;
    
    private Consumer<MidiFile> onFileSelected;
    private Consumer<MidiFile> onFileDoubleClicked;
    
    private final ObservableList<Object> listItems = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing FileBrowserController");
        
        // Setup list view
        if (fileListView != null) {
            fileListView.setItems(listItems);
            fileListView.setCellFactory(lv -> new FileListCell());
            
            // Handle selection
            fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal instanceof MidiFile midiFile) {
                    onFileItemSelected(midiFile);
                }
            });
            
            // Handle double-click
            fileListView.setOnMouseClicked(this::handleListClick);
        }
        
        // Setup search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    performSearch(newVal);
                }
            });
        }
        
        // Setup root directory combo
        if (rootDirectoryCombo != null) {
            rootDirectoryCombo.setOnAction(e -> {
                RootDirectory selected = rootDirectoryCombo.getValue();
                if (selected != null && !selected.equals(currentRootDirectory)) {
                    switchRootDirectory(selected);
                }
            });
        }
    }
    
    /**
     * Injects dependencies.
     */
    public void setDependencies(BrowseMidiFilesUseCase browseMidiFilesUseCase) {
        this.browseMidiFilesUseCase = browseMidiFilesUseCase;
    }
    
    /**
     * Sets callback for file selection (single click).
     */
    public void setOnFileSelected(Consumer<MidiFile> callback) {
        this.onFileSelected = callback;
    }
    
    /**
     * Sets callback for file double-click (play).
     */
    public void setOnFileDoubleClicked(Consumer<MidiFile> callback) {
        this.onFileDoubleClicked = callback;
    }
    
    /**
     * Refreshes the root directories list.
     */
    public void refreshRootDirectories(List<RootDirectory> directories) {
        this.rootDirectories = directories;
        
        if (rootDirectoryCombo != null) {
            rootDirectoryCombo.getItems().clear();
            rootDirectoryCombo.getItems().addAll(directories);
            
            if (currentRootDirectory != null) {
                // Keep current selection if still valid
                directories.stream()
                        .filter(d -> d.getId().equals(currentRootDirectory.getId()))
                        .findFirst()
                        .ifPresent(d -> rootDirectoryCombo.setValue(d));
            } else if (!directories.isEmpty()) {
                rootDirectoryCombo.getSelectionModel().selectFirst();
                switchRootDirectory(directories.get(0));
            }
        }
        
        updateEmptyState();
    }
    
    /**
     * Navigates to a specific root directory.
     */
    public void navigateToRoot(RootDirectory rootDirectory) {
        this.currentRootDirectory = rootDirectory;
        this.currentPath = "";
        
        if (rootDirectoryCombo != null) {
            rootDirectoryCombo.setValue(rootDirectory);
        }
        
        loadCurrentFolder();
    }
    
    /**
     * Refreshes the current folder contents.
     */
    public void refresh() {
        if (currentRootDirectory != null) {
            loadCurrentFolder();
        }
    }
    
    @FXML
    private void onNavigateUp() {
        if (currentRootDirectory == null || !canNavigateUp()) {
            return;
        }
        
        try {
            FolderContents contents = browseMidiFilesUseCase.navigateToParent(
                    currentRootDirectory.getId(), currentPath);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }
    
    private void switchRootDirectory(RootDirectory rootDirectory) {
        try {
            FolderContents contents = browseMidiFilesUseCase.switchRootDirectory(rootDirectory.getId());
            this.currentRootDirectory = rootDirectory;
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }
    
    private void loadCurrentFolder() {
        if (browseMidiFilesUseCase == null || currentRootDirectory == null) {
            return;
        }
        
        try {
            FolderContents contents = browseMidiFilesUseCase.getFolderContents(
                    currentRootDirectory.getId(), currentPath);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
            updateEmptyState();
        }
    }
    
    private void displayFolderContents(FolderContents contents) {
        this.currentContents = contents;
        this.currentPath = contents.currentPath();
        this.currentRootDirectory = contents.rootDirectory();
        
        // Update breadcrumbs
        updateBreadcrumbs();
        
        // Update navigate up button
        if (navigateUpButton != null) {
            navigateUpButton.setDisable(!contents.canNavigateUp());
        }
        
        // Update list
        listItems.clear();
        
        // Add folders first
        for (String subdir : contents.subdirectories()) {
            listItems.add(new FolderItem(subdir));
        }
        
        // Add files
        listItems.addAll(contents.midiFiles());
        
        // Update status
        updateStatus(contents);
        
        // Update empty state
        updateEmptyState();
    }
    
    private void updateBreadcrumbs() {
        if (breadcrumbContainer == null || currentRootDirectory == null) {
            return;
        }
        
        breadcrumbContainer.getChildren().clear();
        
        List<BreadcrumbItem> breadcrumbs = browseMidiFilesUseCase.getBreadcrumbs(
                currentRootDirectory, currentPath);
        
        for (int i = 0; i < breadcrumbs.size(); i++) {
            BreadcrumbItem item = breadcrumbs.get(i);
            
            if (i > 0) {
                Label separator = new Label(" / ");
                separator.setStyle("-fx-text-fill: gray;");
                breadcrumbContainer.getChildren().add(separator);
            }
            
            if (item.isNavigable()) {
                Button button = new Button(item.name());
                final String path = item.path();
                button.setOnAction(e -> navigateToPath(path));
                breadcrumbContainer.getChildren().add(button);
            } else {
                Label label = new Label(item.name());
                label.setStyle("-fx-font-weight: bold;");
                breadcrumbContainer.getChildren().add(label);
            }
        }
    }
    
    private void navigateToPath(String path) {
        if (currentRootDirectory == null) {
            return;
        }
        
        try {
            FolderContents contents = browseMidiFilesUseCase.getFolderContents(
                    currentRootDirectory.getId(), path);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }
    
    private void handleListClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Object selected = fileListView.getSelectionModel().getSelectedItem();
            
            if (selected instanceof FolderItem folder) {
                navigateToSubfolder(folder.name());
            } else if (selected instanceof MidiFile midiFile) {
                if (onFileDoubleClicked != null) {
                    onFileDoubleClicked.accept(midiFile);
                }
            }
        }
    }
    
    private void navigateToSubfolder(String folderName) {
        if (currentRootDirectory == null) {
            return;
        }
        
        try {
            FolderContents contents = browseMidiFilesUseCase.navigateToSubfolder(
                    currentRootDirectory.getId(), currentPath, folderName);
            displayFolderContents(contents);
        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }
    
    private void onFileItemSelected(MidiFile midiFile) {
        if (onFileSelected != null) {
            onFileSelected.accept(midiFile);
        }
    }
    
    private void performSearch(String query) {
        if (currentRootDirectory == null || browseMidiFilesUseCase == null) {
            return;
        }
        
        if (query == null || query.isBlank()) {
            // Clear search, show current folder
            loadCurrentFolder();
            return;
        }
        
        // Search files
        List<MidiFile> results = browseMidiFilesUseCase.searchMidiFiles(
                currentRootDirectory.getId(), query);
        
        listItems.clear();
        listItems.addAll(results);
        
        updateStatus(String.format("Found %d files matching '%s'", results.size(), query));
    }
    
    private void updateStatus(FolderContents contents) {
        String status = String.format("%d folders, %d files",
                contents.subdirectories().size(), contents.midiFiles().size());
        updateStatus(status);
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }
    
    private void updateEmptyState() {
        boolean showEmpty = rootDirectories == null || rootDirectories.isEmpty();
        
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(showEmpty);
            emptyStateContainer.setManaged(showEmpty);
        }
        
        if (fileListView != null) {
            fileListView.setVisible(!showEmpty);
            fileListView.setManaged(!showEmpty);
        }
    }
    
    private boolean canNavigateUp() {
        return currentContents != null && currentContents.canNavigateUp();
    }
    
    /**
     * Represents a folder item in the list.
     */
    public record FolderItem(String name) {
        public String getDisplayName() {
            // Extract just the folder name from path
            int lastSep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            return lastSep >= 0 ? name.substring(lastSep + 1) : name;
        }
    }
    
    /**
     * Custom cell for rendering files and folders.
     */
    private static class FileListCell extends javafx.scene.control.ListCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("folder-item", "file-item");
                return;
            }
            
            if (item instanceof FolderItem folder) {
                setText("📁 " + folder.getDisplayName());
                getStyleClass().add("folder-item");
                getStyleClass().remove("file-item");
            } else if (item instanceof MidiFile file) {
                String display = "🎵 " + file.getFileName();
                if (file.hasNoteShift()) {
                    display += String.format(" [%+d]", file.getNoteShift());
                }
                setText(display);
                getStyleClass().add("file-item");
                getStyleClass().remove("folder-item");
            }
        }
    }
}
