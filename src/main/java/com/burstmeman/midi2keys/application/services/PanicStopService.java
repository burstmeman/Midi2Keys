package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.infrastructure.adapters.keyboard.KeyboardSimulator;
import javafx.application.Platform;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Service for handling panic stop functionality.
 * Provides global emergency stop capability to immediately halt playback
 * and release all pressed keys.
 */
@Slf4j
public class PanicStopService {

    private final KeyboardSimulator keyboardSimulator;
    private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean panicTriggered = new AtomicBoolean(false);

    /**
     * -- SETTER --
     * Set callback to be notified when panic stop is triggered.
     */
    @Setter
    private Consumer<Void> onPanicStop;
    /**
     * -- SETTER --
     * Set the playback service to stop on panic.
     */
    @Setter
    private PlaybackService playbackService;

    public PanicStopService(KeyboardSimulator keyboardSimulator) {
        this.keyboardSimulator = keyboardSimulator;
    }

    /**
     * Register a key as currently pressed.
     */
    public void registerKeyPress(int keyCode) {
        pressedKeys.add(keyCode);
    }

    /**
     * Unregister a key that was released.
     */
    public void registerKeyRelease(int keyCode) {
        pressedKeys.remove(keyCode);
    }

    /**
     * Trigger panic stop - immediately stop all playback and release all keys.
     */
    public void triggerPanicStop() {
        if (panicTriggered.getAndSet(true)) {
            return;
        }

        log.warn("PANIC STOP triggered - releasing all keys and stopping playback");

        // Stop playback first
        if (playbackService != null) {
            try {
                playbackService.panicStop();
            } catch (Exception e) {
                log.error("Error stopping playback during panic stop", e);
            }
        }

        // Release all tracked pressed keys
        releaseAllKeys();

        // Notify listeners on JavaFX thread
        if (onPanicStop != null) {
            Platform.runLater(() -> {
                try {
                    onPanicStop.accept(null);
                } catch (Exception e) {
                    log.error("Error in panic stop callback", e);
                }
            });
        }

        panicTriggered.set(false);

        log.info("Panic stop completed - all keys released");
    }

    /**
     * Release all currently pressed keys.
     */
    public void releaseAllKeys() {
        // Use the keyboard simulator's built-in releaseAllKeys
        keyboardSimulator.releaseAllKeys();

        // Clear tracked keys
        pressedKeys.clear();
    }

    /**
     * Check if any keys are currently pressed.
     */
    public boolean hasActiveKeys() {
        return !pressedKeys.isEmpty();
    }

    /**
     * Get the set of currently pressed keys.
     */
    public Set<Integer> getPressedKeys() {
        return Collections.unmodifiableSet(pressedKeys);
    }

    /**
     * Check if panic stop is currently being processed.
     */
    public boolean isPanicInProgress() {
        return panicTriggered.get();
    }

    /**
     * Clear all state.
     */
    public void reset() {
        pressedKeys.clear();
        panicTriggered.set(false);
    }
}
