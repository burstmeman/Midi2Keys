package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.*;
import com.burstmeman.midi2keys.domain.valueobjects.KeyCombination;
import com.burstmeman.midi2keys.infrastructure.adapters.keyboard.KeyboardSimulator;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser.MidiFileInfo;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser.MidiNoteEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Service for playing MIDI files with keyboard simulation.
 */
@Slf4j
public class PlaybackService {

    private final MidiParser midiParser;
    private final KeyboardSimulator keyboardSimulator;

    // Playback state
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicLong currentPositionMs = new AtomicLong(0);

    private Thread playbackThread;
    private MidiFileInfo currentFileInfo;
    private Profile currentProfile;
    private int currentNoteShift;

    // Callbacks
    @Setter
    private Consumer<PlaybackState> onStateChanged;
    @Setter
    private Consumer<Long> onProgressUpdated;
    @Setter
    private Consumer<String> onNotePressed;

    public PlaybackService(MidiParser midiParser, KeyboardSimulator keyboardSimulator) {
        this.midiParser = midiParser;
        this.keyboardSimulator = keyboardSimulator;
    }

    /**
     * Starts playback of a MIDI file.
     *
     * @param midiFile      The MIDI file to play
     * @param rootDirectory Root directory containing the file
     * @param profile       Profile with key mappings
     */
    public void play(MidiFile midiFile, RootDirectory rootDirectory, Profile profile) {
        if (isPlaying.get()) {
            log.warn("Playback already in progress");
            return;
        }

        Path filePath = midiFile.getAbsolutePath(rootDirectory.getPathAsPath());
        int noteShift = midiFile.getNoteShift() != null ? midiFile.getNoteShift() : 0;

        try {
            MidiFileInfo fileInfo = midiParser.parse(filePath);
            startPlayback(fileInfo, profile, noteShift);
        } catch (Exception e) {
            log.error("Failed to start playback", e);
            throw new RuntimeException("Failed to start playback: " + e.getMessage(), e);
        }
    }

    /**
     * Pauses playback.
     */
    public void pause() {
        if (isPlaying.get() && !isPaused.get()) {
            isPaused.set(true);
            notifyStateChanged(PlaybackState.PAUSED);
            log.info("Playback paused");
        }
    }

    /**
     * Resumes paused playback.
     */
    public void resume() {
        if (isPlaying.get() && isPaused.get()) {
            isPaused.set(false);
            notifyStateChanged(PlaybackState.PLAYING);
            log.info("Playback resumed");
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        if (isPlaying.get()) {
            stopRequested.set(true);
            log.info("Stop requested");
        }
    }

    /**
     * Emergency stop - immediately stops playback and releases all keys.
     * Used for panic stop functionality.
     */
    public void panicStop() {
        log.warn("PANIC STOP activated");

        stopRequested.set(true);

        // Immediately release all keys
        keyboardSimulator.releaseAllKeys();

        // Reset state
        isPlaying.set(false);
        isPaused.set(false);
        currentPositionMs.set(0);

        notifyStateChanged(PlaybackState.STOPPED);
    }

    /**
     * Gets current playback state.
     */
    public PlaybackState getState() {
        if (!isPlaying.get()) {
            return PlaybackState.STOPPED;
        }
        return isPaused.get() ? PlaybackState.PAUSED : PlaybackState.PLAYING;
    }

    /**
     * Gets current playback position in milliseconds.
     */
    public long getCurrentPositionMs() {
        return currentPositionMs.get();
    }

    /**
     * Gets total duration of current file in milliseconds.
     */
    public long getTotalDurationMs() {
        return currentFileInfo != null ? currentFileInfo.durationMs() : 0;
    }

    /**
     * Checks if playback is in progress.
     */
    public boolean isPlaying() {
        return isPlaying.get();
    }

    /**
     * Checks if playback is paused.
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    private void startPlayback(MidiFileInfo fileInfo, Profile profile, int noteShift) {
        this.currentFileInfo = fileInfo;
        this.currentProfile = profile;
        this.currentNoteShift = noteShift;

        isPlaying.set(true);
        isPaused.set(false);
        stopRequested.set(false);
        currentPositionMs.set(0);

        playbackThread = new Thread(this::playbackLoop, "MIDI-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        notifyStateChanged(PlaybackState.PLAYING);
        log.info("Playback started: {} notes, {}ms",
                fileInfo.noteEvents().size(), fileInfo.durationMs());
    }

    private void playbackLoop() {
        try {
            PlaybackOptions options = currentProfile.getPlaybackOptions();
            List<MidiNoteEvent> events = prepareEvents(currentFileInfo.noteEvents(), options);

            long startTime = System.currentTimeMillis();
            long pauseOffset = 0;
            int eventIndex = 0;

            while (eventIndex < events.size() && !stopRequested.get()) {
                // Handle pause
                if (isPaused.get()) {
                    long pauseStart = System.currentTimeMillis();
                    while (isPaused.get() && !stopRequested.get()) {
                        Thread.sleep(50);
                    }
                    pauseOffset += System.currentTimeMillis() - pauseStart;
                }

                if (stopRequested.get()) break;

                MidiNoteEvent event = events.get(eventIndex);
                long eventTime = options.adjustTimeForTempo(event.timeMs());

                // Wait until event time
                long currentTime = System.currentTimeMillis() - startTime - pauseOffset;
                long waitTime = eventTime - currentTime;

                if (waitTime > 0) {
                    Thread.sleep(Math.min(waitTime, 10));
                    continue; // Re-check time after sleep
                }

                // Process event
                processNoteEvent(event, options);
                currentPositionMs.set(eventTime);
                notifyProgress(eventTime);

                eventIndex++;
            }

        } catch (InterruptedException e) {
            log.info("Playback interrupted");
        } catch (Exception e) {
            log.error("Playback error", e);
        } finally {
            // Cleanup
            keyboardSimulator.releaseAllKeys();
            isPlaying.set(false);
            isPaused.set(false);
            stopRequested.set(false);

            notifyStateChanged(PlaybackState.STOPPED);
            log.info("Playback finished");
        }
    }

    private List<MidiNoteEvent> prepareEvents(List<MidiNoteEvent> events, PlaybackOptions options) {
        List<MidiNoteEvent> filtered = new ArrayList<>();

        for (MidiNoteEvent event : events) {
            // Filter by channel
            if (options.isChannelIgnored(event.channel())) {
                continue;
            }

            // Filter by velocity threshold (for note on events)
            if (event.isNoteOn() && !options.velocityPassesThreshold(event.velocity())) {
                continue;
            }

            filtered.add(event);
        }

        // Sort by time
        filtered.sort(Comparator.comparingLong(MidiNoteEvent::timeMs));

        return filtered;
    }

    private void processNoteEvent(MidiNoteEvent event, PlaybackOptions options) {
        // Apply transpose and note shift
        int noteNumber = options.applyTranspose(event.noteNumber());
        noteNumber += currentNoteShift;
        noteNumber = Math.max(0, Math.min(127, noteNumber));

        // Find mapping (account for note shift in profile lookup)
        NoteMapping mapping = currentProfile.getMappingForNoteWithShift(
                noteNumber, event.channel(), currentNoteShift);

        if (mapping == null) {
            // Try without channel filter
            mapping = currentProfile.getMappingForNoteWithShift(
                    noteNumber, -1, currentNoteShift);
        }

        if (mapping == null) {
            return; // No mapping for this note
        }

        // Check velocity range
        if (!mapping.velocityInRange(event.velocity())) {
            return;
        }

        KeyCombination keyComb = mapping.keyCombination();

        if (event.isNoteOn() && event.velocity() > 0) {
            // Note On - press keys
            if (keyComb.hasModifiers()) {
                keyboardSimulator.pressKeyCombination(
                        keyComb.getModifierKeyCodes(), keyComb.getMainKeyCode());
            } else {
                keyboardSimulator.keyPress(keyComb.getMainKeyCode());
            }

            notifyNotePressed(keyComb.getDisplayString());

            // Schedule key release if using fixed duration
            int duration = options.getKeyPressDurationMs();
            if (duration > 0) {
                scheduleKeyRelease(keyComb, duration);
            }

        } else {
            // Note Off - release keys (if not using fixed duration)
            if (options.getKeyPressDurationMs() == 0) {
                if (keyComb.hasModifiers()) {
                    keyboardSimulator.releaseKeyCombination(
                            keyComb.getModifierKeyCodes(), keyComb.getMainKeyCode());
                } else {
                    keyboardSimulator.keyRelease(keyComb.getMainKeyCode());
                }
            }
        }
    }

    private void scheduleKeyRelease(KeyCombination keyComb, int delayMs) {
        Thread releaseThread = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                if (keyComb.hasModifiers()) {
                    keyboardSimulator.releaseKeyCombination(
                            keyComb.getModifierKeyCodes(), keyComb.getMainKeyCode());
                } else {
                    keyboardSimulator.keyRelease(keyComb.getMainKeyCode());
                }
            } catch (InterruptedException e) {
                // Thread interrupted, key will be released in cleanup
            }
        }, "Key-Release-" + keyComb.getMainKeyCode());
        releaseThread.setDaemon(true);
        releaseThread.start();
    }

    private void notifyStateChanged(PlaybackState state) {
        if (onStateChanged != null) {
            try {
                onStateChanged.accept(state);
            } catch (Exception e) {
                log.error("Error in state change callback", e);
            }
        }
    }

    private void notifyProgress(long positionMs) {
        if (onProgressUpdated != null) {
            try {
                onProgressUpdated.accept(positionMs);
            } catch (Exception e) {
                // Ignore progress callback errors
            }
        }
    }

    private void notifyNotePressed(String keyDescription) {
        if (onNotePressed != null) {
            try {
                onNotePressed.accept(keyDescription);
            } catch (Exception e) {
                // Ignore note callback errors
            }
        }
    }

    /**
     * Playback state enum.
     */
    public enum PlaybackState {
        STOPPED,
        PLAYING,
        PAUSED
    }
}

