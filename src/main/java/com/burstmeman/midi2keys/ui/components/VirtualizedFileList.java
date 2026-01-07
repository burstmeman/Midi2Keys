package com.burstmeman.midi2keys.ui.components;

import com.burstmeman.midi2keys.domain.entities.MidiFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Virtualized list component for efficiently displaying large numbers of MIDI files.
 */
@Slf4j
public class VirtualizedFileList extends VBox {

    private final ObservableList<MidiFile> allFiles = FXCollections.observableArrayList();
    private final FilteredList<MidiFile> filteredFiles;
    private final SortedList<MidiFile> sortedFiles;
    private final ListView<MidiFile> listView;
    private final ObjectProperty<MidiFile> selectedFile = new SimpleObjectProperty<>();

    private Consumer<MidiFile> onFileSelected;
    private Consumer<MidiFile> onFileDoubleClicked;
    private Label emptyLabel;

    public VirtualizedFileList() {
        filteredFiles = new FilteredList<>(allFiles, f -> true);
        sortedFiles = new SortedList<>(filteredFiles);

        listView = new ListView<>(sortedFiles);
        listView.setCellFactory(lv -> new MidiFileListCell());
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setFixedCellSize(48);

        initializeComponents();
        setupLayout();
        setupEventHandlers();
        applyStyles();
    }

    private void initializeComponents() {
        emptyLabel = new Label("No MIDI files found");
        emptyLabel.getStyleClass().add("empty-list-label");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);
    }

    private void setupLayout() {
        VBox.setVgrow(listView, Priority.ALWAYS);
        getChildren().addAll(listView, emptyLabel);
    }

    private void setupEventHandlers() {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedFile.set(newVal);
            if (onFileSelected != null && newVal != null) {
                onFileSelected.accept(newVal);
            }
        });

        listView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                MidiFile selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && onFileDoubleClicked != null) {
                    onFileDoubleClicked.accept(selected);
                }
            }
        });

        filteredFiles.addListener((javafx.collections.ListChangeListener<MidiFile>) change -> {
            updateEmptyState();
        });
    }

    private void applyStyles() {
        getStyleClass().add("virtualized-file-list");
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredFiles.isEmpty();
        listView.setVisible(!isEmpty);
        listView.setManaged(!isEmpty);
        emptyLabel.setVisible(isEmpty);
        emptyLabel.setManaged(isEmpty);
    }

    public void addFiles(List<MidiFile> files) {
        if (files != null) {
            allFiles.addAll(files);
        }
        updateEmptyState();
    }

    public void clear() {
        allFiles.clear();
        updateEmptyState();
    }

    public void setFilter(Predicate<MidiFile> predicate) {
        filteredFiles.setPredicate(predicate);
        updateEmptyState();
    }

    public void clearFilter() {
        filteredFiles.setPredicate(f -> true);
        updateEmptyState();
    }

    public void filterByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            clearFilter();
            return;
        }

        String lowerQuery = query.toLowerCase();
        setFilter(file -> file.getFileName().toLowerCase().contains(lowerQuery));
    }

    public void setSort(Comparator<MidiFile> comparator) {
        sortedFiles.setComparator(comparator);
    }

    public void sortByName() {
        setSort(Comparator.comparing(MidiFile::getFileName, String.CASE_INSENSITIVE_ORDER));
    }

    public void sortBySize() {
        setSort(Comparator.comparingLong(MidiFile::getFileSize));
    }

    public void reverseSortOrder() {
        Comparator<? super MidiFile> current = sortedFiles.getComparator();
        if (current != null) {
            // Create a new reversed comparator
            Comparator<MidiFile> reversed = (a, b) -> current.compare(b, a);
            sortedFiles.setComparator(reversed);
        }
    }

    public MidiFile getSelectedFile() {
        return selectedFile.get();
    }

    public ObjectProperty<MidiFile> selectedFileProperty() {
        return selectedFile;
    }

    public void select(MidiFile file) {
        if (file != null && allFiles.contains(file)) {
            listView.getSelectionModel().select(file);
            listView.scrollTo(file);
        }
    }

    public void clearSelection() {
        listView.getSelectionModel().clearSelection();
    }

    public void setOnFileSelected(Consumer<MidiFile> callback) {
        this.onFileSelected = callback;
    }

    public void setOnFileDoubleClicked(Consumer<MidiFile> callback) {
        this.onFileDoubleClicked = callback;
    }

    public void setEmptyMessage(String message) {
        emptyLabel.setText(message);
    }

    public int getTotalCount() {
        return allFiles.size();
    }

    public int getVisibleCount() {
        return filteredFiles.size();
    }

    public ObservableList<MidiFile> getFiles() {
        return allFiles;
    }

    public void setFiles(List<MidiFile> files) {
        allFiles.clear();
        if (files != null) {
            allFiles.addAll(files);
        }
        updateEmptyState();
        log.debug("File list updated with {} files", allFiles.size());
    }

    private static class MidiFileListCell extends ListCell<MidiFile> {
        private final HBox container;
        private final Label nameLabel;
        private final Label infoLabel;

        MidiFileListCell() {
            container = new HBox(12);
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            container.setPadding(new Insets(4, 8, 4, 8));

            nameLabel = new Label();
            nameLabel.getStyleClass().add("file-name");

            infoLabel = new Label();
            infoLabel.getStyleClass().add("file-info");

            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            container.getChildren().addAll(nameLabel, infoLabel);
        }

        @Override
        protected void updateItem(MidiFile file, boolean empty) {
            super.updateItem(file, empty);

            if (empty || file == null) {
                setText(null);
                setGraphic(null);
            } else {
                nameLabel.setText(file.getFileName());
                Long fileSize = file.getFileSize();
                infoLabel.setText(fileSize != null ? formatFileSize(fileSize) : "");
                setGraphic(container);
            }
        }

        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
