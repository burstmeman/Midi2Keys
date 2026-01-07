package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.services.MidiFileService;
import com.burstmeman.midi2keys.application.usecases.AnalyzeMidiFileUseCase;
import com.burstmeman.midi2keys.application.usecases.AnalyzeMidiFileUseCase.AnalysisResult;
import com.burstmeman.midi2keys.domain.entities.MidiAnalysis;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the MIDI file preview panel.
 * Displays file metadata, analysis statistics, and note histogram.
 */
public class PreviewController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(PreviewController.class);
    
    // File info
    @FXML private Label fileNameLabel;
    @FXML private Label filePathLabel;
    @FXML private Label fileSizeLabel;
    
    // Analysis info
    @FXML private Label formatLabel;
    @FXML private Label durationLabel;
    @FXML private Label trackCountLabel;
    @FXML private Label tempoLabel;
    @FXML private Label timeSignatureLabel;
    @FXML private Label totalNotesLabel;
    @FXML private Label noteRangeLabel;
    @FXML private Label channelCountLabel;
    @FXML private Label velocityStatsLabel;
    
    // Note shift
    @FXML private ComboBox<String> noteShiftCombo;
    
    // Visualization
    @FXML private Canvas histogramCanvas;
    
    // Loading state
    @FXML private VBox loadingPane;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private VBox contentPane;
    @FXML private VBox emptyPane;
    
    // Actions
    @FXML private Button playButton;
    @FXML private Button reanalyzeButton;
    
    private AnalyzeMidiFileUseCase analyzeMidiFileUseCase;
    private MidiFileService midiFileService;
    private Consumer<MidiFile> onPlayRequested;
    
    private MidiFile currentFile;
    private MidiAnalysis currentAnalysis;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing PreviewController");
        
        // Setup note shift combo
        if (noteShiftCombo != null) {
            noteShiftCombo.getItems().addAll(
                    "-4 semitones", "-3 semitones", "-2 semitones", "-1 semitone",
                    "No shift",
                    "+1 semitone", "+2 semitones", "+3 semitones", "+4 semitones"
            );
            noteShiftCombo.getSelectionModel().select(4); // Default to "No shift"
            
            noteShiftCombo.setOnAction(e -> onNoteShiftChanged());
        }
        
        showEmptyState();
    }
    
    /**
     * Sets dependencies.
     */
    public void setDependencies(AnalyzeMidiFileUseCase analyzeMidiFileUseCase,
                                MidiFileService midiFileService) {
        this.analyzeMidiFileUseCase = analyzeMidiFileUseCase;
        this.midiFileService = midiFileService;
    }
    
    /**
     * Sets callback for play request.
     */
    public void setOnPlayRequested(Consumer<MidiFile> callback) {
        this.onPlayRequested = callback;
    }
    
    /**
     * Shows preview for a MIDI file.
     */
    public void showPreview(MidiFile midiFile) {
        if (midiFile == null) {
            showEmptyState();
            return;
        }
        
        this.currentFile = midiFile;
        showLoadingState();
        
        // Load analysis in background
        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                return analyzeMidiFileUseCase.getAnalysis(midiFile.getId());
            }
        };
        
        task.setOnSucceeded(e -> {
            AnalysisResult result = task.getValue();
            Platform.runLater(() -> displayAnalysis(result));
        });
        
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Failed to analyze file", ex);
            Platform.runLater(() -> {
                if (ex instanceof ApplicationException appEx) {
                    ErrorHandler.handle(appEx);
                } else {
                    ErrorHandler.handle("Analysis", ex);
                }
                showEmptyState();
            });
        });
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Clears the preview.
     */
    public void clear() {
        this.currentFile = null;
        this.currentAnalysis = null;
        showEmptyState();
    }
    
    @FXML
    private void onPlay() {
        if (currentFile != null && onPlayRequested != null) {
            onPlayRequested.accept(currentFile);
        }
    }
    
    @FXML
    private void onReanalyze() {
        if (currentFile == null || analyzeMidiFileUseCase == null) return;
        
        showLoadingState();
        
        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                return analyzeMidiFileUseCase.reanalyze(currentFile.getId());
            }
        };
        
        task.setOnSucceeded(e -> {
            AnalysisResult result = task.getValue();
            Platform.runLater(() -> displayAnalysis(result));
        });
        
        task.setOnFailed(e -> {
            logger.error("Failed to re-analyze file", task.getException());
            Platform.runLater(() -> {
                ErrorHandler.handle("Re-analyze", task.getException());
                showContentState();
            });
        });
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void onNoteShiftChanged() {
        if (currentFile == null || noteShiftCombo == null || midiFileService == null) return;
        
        int selectedIndex = noteShiftCombo.getSelectionModel().getSelectedIndex();
        int noteShift = selectedIndex - 4; // Index 4 is "No shift" (0)
        
        try {
            midiFileService.updateNoteShift(currentFile.getId(), noteShift);
            currentFile.setNoteShift(noteShift);
            logger.debug("Updated note shift to {} for file {}", noteShift, currentFile.getId());
        } catch (Exception e) {
            ErrorHandler.handle("Update Note Shift", e);
        }
    }
    
    private void displayAnalysis(AnalysisResult result) {
        this.currentAnalysis = result.analysis();
        MidiFile file = result.midiFile();
        MidiAnalysis analysis = result.analysis();
        
        // File info
        if (fileNameLabel != null) {
            fileNameLabel.setText(file.getFileName());
        }
        if (filePathLabel != null) {
            filePathLabel.setText(result.getFullPath());
        }
        if (fileSizeLabel != null) {
            fileSizeLabel.setText(file.getFormattedFileSize());
        }
        
        // Analysis info
        if (formatLabel != null) {
            formatLabel.setText(analysis.getFormatDescription());
        }
        if (durationLabel != null) {
            durationLabel.setText(analysis.getFormattedDuration());
        }
        if (trackCountLabel != null) {
            trackCountLabel.setText(String.valueOf(analysis.getTrackCount()));
        }
        if (tempoLabel != null) {
            String tempoText = String.format("%.1f BPM", analysis.getTempoBpm());
            if (analysis.getTempoChangesCount() > 0) {
                tempoText += String.format(" (%d changes)", analysis.getTempoChangesCount());
            }
            tempoLabel.setText(tempoText);
        }
        if (timeSignatureLabel != null) {
            timeSignatureLabel.setText(analysis.getTimeSignature());
        }
        if (totalNotesLabel != null) {
            totalNotesLabel.setText(String.valueOf(analysis.getTotalNotes()));
        }
        if (noteRangeLabel != null) {
            int lowest = analysis.getLowestNote();
            int highest = analysis.getHighestNote();
            String lowestName = new MidiNote(lowest).getNoteName();
            String highestName = new MidiNote(highest).getNoteName();
            noteRangeLabel.setText(String.format("%s - %s (%d notes)", lowestName, highestName, analysis.getNoteRange()));
        }
        if (channelCountLabel != null) {
            channelCountLabel.setText(String.valueOf(analysis.getActiveChannelCount()));
        }
        if (velocityStatsLabel != null) {
            velocityStatsLabel.setText(String.format("Min: %d, Max: %d, Avg: %.0f",
                    analysis.getMinVelocity(), analysis.getMaxVelocity(), analysis.getAvgVelocity()));
        }
        
        // Note shift
        if (noteShiftCombo != null && file.getNoteShift() != null) {
            noteShiftCombo.getSelectionModel().select(file.getNoteShift() + 4);
        }
        
        // Draw histogram
        drawHistogram(analysis);
        
        showContentState();
    }
    
    private void drawHistogram(MidiAnalysis analysis) {
        if (histogramCanvas == null) return;
        
        GraphicsContext gc = histogramCanvas.getGraphicsContext2D();
        double width = histogramCanvas.getWidth();
        double height = histogramCanvas.getHeight();
        
        // Clear canvas
        gc.setFill(Color.web("#1e1e1e"));
        gc.fillRect(0, 0, width, height);
        
        Map<Integer, Integer> histogram = analysis.getNoteHistogram();
        if (histogram.isEmpty()) return;
        
        // Find max count for scaling
        int maxCount = histogram.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int lowestNote = analysis.getLowestNote();
        int highestNote = analysis.getHighestNote();
        int noteRange = highestNote - lowestNote + 1;
        
        if (noteRange <= 0) return;
        
        double barWidth = Math.max(2, width / noteRange);
        double padding = 10;
        double graphHeight = height - padding * 2;
        
        // Draw bars
        gc.setFill(Color.web("#00bcd4"));
        
        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            int note = entry.getKey();
            int count = entry.getValue();
            
            double x = (note - lowestNote) * barWidth;
            double barHeight = (count / (double) maxCount) * graphHeight;
            double y = height - padding - barHeight;
            
            gc.fillRect(x, y, Math.max(1, barWidth - 1), barHeight);
        }
        
        // Draw note labels at octave boundaries
        gc.setFill(Color.web("rgba(255,255,255,0.6)"));
        gc.setFont(javafx.scene.text.Font.font(10));
        
        for (int note = lowestNote; note <= highestNote; note++) {
            if (note % 12 == 0) { // C notes
                double x = (note - lowestNote) * barWidth;
                String label = new MidiNote(note).getNoteName();
                gc.fillText(label, x, height - 2);
            }
        }
    }
    
    private void showEmptyState() {
        setVisibility(false, false, true);
    }
    
    private void showLoadingState() {
        setVisibility(true, false, false);
    }
    
    private void showContentState() {
        setVisibility(false, true, false);
    }
    
    private void setVisibility(boolean loading, boolean content, boolean empty) {
        if (loadingPane != null) {
            loadingPane.setVisible(loading);
            loadingPane.setManaged(loading);
        }
        if (contentPane != null) {
            contentPane.setVisible(content);
            contentPane.setManaged(content);
        }
        if (emptyPane != null) {
            emptyPane.setVisible(empty);
            emptyPane.setManaged(empty);
        }
    }
}
