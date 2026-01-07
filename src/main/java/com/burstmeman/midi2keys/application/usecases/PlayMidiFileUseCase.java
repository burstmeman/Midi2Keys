package com.burstmeman.midi2keys.application.usecases;

import com.burstmeman.midi2keys.application.services.MidiFileService;
import com.burstmeman.midi2keys.application.services.PlaybackService;
import com.burstmeman.midi2keys.application.services.PlaybackService.PlaybackState;
import com.burstmeman.midi2keys.application.services.ProfileService;
import com.burstmeman.midi2keys.application.services.RootDirectoryService;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Use case for playing MIDI files with keyboard simulation.
 */
@Slf4j
public class PlayMidiFileUseCase {

    private final PlaybackService playbackService;
    private final ProfileService profileService;
    private final MidiFileService midiFileService;
    private final RootDirectoryService rootDirectoryService;
    private final SettingsRepository settingsRepository;

    // Countdown state
    private Timer countdownTimer;
    private Consumer<Integer> onCountdownTick;
    private Runnable onCountdownComplete;

    public PlayMidiFileUseCase(PlaybackService playbackService,
                               ProfileService profileService,
                               MidiFileService midiFileService,
                               RootDirectoryService rootDirectoryService,
                               SettingsRepository settingsRepository) {
        this.playbackService = playbackService;
        this.profileService = profileService;
        this.midiFileService = midiFileService;
        this.rootDirectoryService = rootDirectoryService;
        this.settingsRepository = settingsRepository;
    }

    /**
     * Starts playback with countdown.
     *
     * @param midiFileId MIDI file ID to play
     */
    public void play(Long midiFileId) {
        // Validate file
        MidiFile midiFile = midiFileService.findById(midiFileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FILE_NOT_FOUND,
                        "MIDI file not found: " + midiFileId));

        RootDirectory rootDir = rootDirectoryService.findById(midiFile.getRootDirectoryId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found"));

        // Validate profile
        Profile profile = profileService.getCurrentProfile();
        if (profile == null) {
            throw new ApplicationException(ErrorCode.NO_PROFILE_SELECTED,
                    "No profile selected. Please select a profile before playing.");
        }

        if (!profile.hasMappings()) {
            throw new ApplicationException(ErrorCode.PROFILE_VALIDATION_ERROR,
                    "Selected profile has no key mappings configured.");
        }

        // Get countdown duration
        int countdownSeconds = settingsRepository.getCountdownSeconds();

        if (countdownSeconds > 0) {
            startCountdown(countdownSeconds, () -> {
                startPlaybackInternal(midiFile, rootDir, profile);
            });
        } else {
            startPlaybackInternal(midiFile, rootDir, profile);
        }
    }

    /**
     * Starts playback immediately without countdown.
     *
     * @param midiFileId MIDI file ID to play
     */
    public void playImmediately(Long midiFileId) {
        MidiFile midiFile = midiFileService.findById(midiFileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FILE_NOT_FOUND,
                        "MIDI file not found: " + midiFileId));

        RootDirectory rootDir = rootDirectoryService.findById(midiFile.getRootDirectoryId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found"));

        Profile profile = profileService.getCurrentProfile();
        if (profile == null) {
            throw new ApplicationException(ErrorCode.NO_PROFILE_SELECTED,
                    "No profile selected");
        }

        startPlaybackInternal(midiFile, rootDir, profile);
    }

    /**
     * Pauses playback.
     */
    public void pause() {
        playbackService.pause();
    }

    /**
     * Resumes playback.
     */
    public void resume() {
        playbackService.resume();
    }

    /**
     * Stops playback.
     */
    public void stop() {
        cancelCountdown();
        playbackService.stop();
    }

    /**
     * Panic stop - immediately stops and releases all keys.
     */
    public void panicStop() {
        cancelCountdown();
        playbackService.panicStop();
    }

    /**
     * Gets current playback state.
     */
    public PlaybackState getState() {
        return playbackService.getState();
    }

    /**
     * Gets current playback position.
     */
    public long getCurrentPositionMs() {
        return playbackService.getCurrentPositionMs();
    }

    /**
     * Gets total duration.
     */
    public long getTotalDurationMs() {
        return playbackService.getTotalDurationMs();
    }

    /**
     * Checks if currently playing.
     */
    public boolean isPlaying() {
        return playbackService.isPlaying();
    }

    /**
     * Checks if paused.
     */
    public boolean isPaused() {
        return playbackService.isPaused();
    }

    // Callback setters

    public void setOnCountdownTick(Consumer<Integer> callback) {
        this.onCountdownTick = callback;
    }

    public void setOnCountdownComplete(Runnable callback) {
        this.onCountdownComplete = callback;
    }

    public void setOnStateChanged(Consumer<PlaybackState> callback) {
        playbackService.setOnStateChanged(callback);
    }

    public void setOnProgressUpdated(Consumer<Long> callback) {
        playbackService.setOnProgressUpdated(callback);
    }

    public void setOnNotePressed(Consumer<String> callback) {
        playbackService.setOnNotePressed(callback);
    }

    // Private methods

    private void startCountdown(int seconds, Runnable onComplete) {
        cancelCountdown();

        countdownTimer = new Timer("Countdown", true);

        final int[] remaining = {seconds};

        // Notify initial tick
        if (onCountdownTick != null) {
            onCountdownTick.accept(remaining[0]);
        }

        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                remaining[0]--;

                if (remaining[0] <= 0) {
                    cancel();
                    countdownTimer.cancel();
                    countdownTimer = null;

                    if (onCountdownComplete != null) {
                        onCountdownComplete.run();
                    }
                    onComplete.run();
                } else {
                    if (onCountdownTick != null) {
                        onCountdownTick.accept(remaining[0]);
                    }
                }
            }
        }, 1000, 1000);

        log.info("Countdown started: {} seconds", seconds);
    }

    private void cancelCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
            log.debug("Countdown cancelled");
        }
    }

    private void startPlaybackInternal(MidiFile midiFile, RootDirectory rootDir, Profile profile) {
        log.info("Starting playback: {} with profile {}", midiFile.getFileName(), profile.getName());
        playbackService.play(midiFile, rootDir, profile);
    }
}

