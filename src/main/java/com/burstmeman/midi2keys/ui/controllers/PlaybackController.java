package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.services.PlaybackService.PlaybackState;
import com.burstmeman.midi2keys.application.usecases.PlayMidiFileUseCase;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for playback controls.
 */
@Slf4j
public class PlaybackController implements Initializable {

    @FXML
    private HBox playbackControls;
    @FXML
    private Button playButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button stopButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label timeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label currentKeyLabel;

    // Countdown overlay
    @FXML
    private StackPane countdownOverlay;
    @FXML
    private Label countdownLabel;

    private PlayMidiFileUseCase playMidiFileUseCase;
    private MidiFile currentFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing PlaybackController");

        // Initial state
        updateControlsForState(PlaybackState.STOPPED);
        hideCountdown();
    }

    /**
     * Sets dependencies.
     */
    public void setDependencies(PlayMidiFileUseCase playMidiFileUseCase) {
        this.playMidiFileUseCase = playMidiFileUseCase;

        // Setup callbacks
        playMidiFileUseCase.setOnStateChanged(this::onStateChanged);
        playMidiFileUseCase.setOnProgressUpdated(this::onProgressUpdated);
        playMidiFileUseCase.setOnCountdownTick(this::onCountdownTick);
        playMidiFileUseCase.setOnCountdownComplete(this::onCountdownComplete);
        playMidiFileUseCase.setOnNotePressed(this::onNotePressed);
    }

    /**
     * Sets the current file for playback.
     */
    public void setCurrentFile(MidiFile file) {
        this.currentFile = file;

        boolean hasFile = file != null;
        if (playButton != null) {
            playButton.setDisable(!hasFile);
        }

        updateStatus(hasFile ? "Ready to play: " + file.getFileName() : "No file selected");
    }

    @FXML
    private void onPlay() {
        if (currentFile == null) {
            ErrorHandler.showWarning("No File", "Please select a MIDI file to play.");
            return;
        }

        if (playMidiFileUseCase.isPaused()) {
            playMidiFileUseCase.resume();
        } else {
            try {
                playMidiFileUseCase.play(currentFile.getId());
            } catch (ApplicationException e) {
                ErrorHandler.handle(e);
            }
        }
    }

    @FXML
    private void onPause() {
        playMidiFileUseCase.pause();
    }

    @FXML
    private void onStop() {
        playMidiFileUseCase.stop();
    }

    /**
     * Panic stop - immediately stops playback.
     */
    public void panicStop() {
        log.warn("Panic stop triggered from UI");
        playMidiFileUseCase.panicStop();
        hideCountdown();
        updateStatus("STOPPED - All keys released");
    }

    private void onStateChanged(PlaybackState state) {
        Platform.runLater(() -> {
            updateControlsForState(state);

            switch (state) {
                case PLAYING -> updateStatus("Playing...");
                case PAUSED -> updateStatus("Paused");
                case STOPPED -> {
                    updateStatus("Stopped");
                    resetProgress();
                }
            }
        });
    }

    private void onProgressUpdated(Long positionMs) {
        Platform.runLater(() -> {
            long totalMs = playMidiFileUseCase.getTotalDurationMs();

            if (totalMs > 0 && progressBar != null) {
                double progress = (double) positionMs / totalMs;
                progressBar.setProgress(progress);
            }

            if (timeLabel != null) {
                String time = formatTime(positionMs) + " / " + formatTime(totalMs);
                timeLabel.setText(time);
            }
        });
    }

    private void onCountdownTick(Integer seconds) {
        Platform.runLater(() -> {
            showCountdown(seconds);
        });
    }

    private void onCountdownComplete() {
        Platform.runLater(this::hideCountdown);
    }

    private void onNotePressed(String keyDescription) {
        Platform.runLater(() -> {
            if (currentKeyLabel != null) {
                currentKeyLabel.setText(keyDescription);
            }
        });
    }

    private void updateControlsForState(PlaybackState state) {
        boolean playing = state == PlaybackState.PLAYING;
        boolean paused = state == PlaybackState.PAUSED;
        boolean stopped = state == PlaybackState.STOPPED;

        if (playButton != null) {
            playButton.setVisible(stopped || paused);
            playButton.setManaged(stopped || paused);
            playButton.setText(paused ? "▶ Resume" : "▶ Play");
        }

        if (pauseButton != null) {
            pauseButton.setVisible(playing);
            pauseButton.setManaged(playing);
        }

        if (stopButton != null) {
            stopButton.setDisable(stopped);
        }
    }

    private void showCountdown(int seconds) {
        if (countdownOverlay != null) {
            countdownOverlay.setVisible(true);
            countdownOverlay.setManaged(true);
        }

        if (countdownLabel != null) {
            countdownLabel.setText(String.valueOf(seconds));
        }

        updateStatus("Starting in " + seconds + "...");
    }

    private void hideCountdown() {
        if (countdownOverlay != null) {
            countdownOverlay.setVisible(false);
            countdownOverlay.setManaged(false);
        }
    }

    private void resetProgress() {
        if (progressBar != null) {
            progressBar.setProgress(0);
        }

        if (timeLabel != null) {
            timeLabel.setText("0:00 / 0:00");
        }

        if (currentKeyLabel != null) {
            currentKeyLabel.setText("-");
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
