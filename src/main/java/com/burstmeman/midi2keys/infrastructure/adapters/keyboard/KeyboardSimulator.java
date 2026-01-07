package com.burstmeman.midi2keys.infrastructure.adapters.keyboard;

import java.util.Set;

/**
 * Interface for simulating keyboard key presses.
 * Abstracts keyboard simulation for different implementations and testing.
 */
public interface KeyboardSimulator {

    /**
     * Presses a key down (does not release).
     *
     * @param keyCode The virtual key code to press
     */
    void keyPress(int keyCode);

    /**
     * Releases a key.
     *
     * @param keyCode The virtual key code to release
     */
    void keyRelease(int keyCode);

    /**
     * Types a key (press and release).
     *
     * @param keyCode The virtual key code to type
     */
    void typeKey(int keyCode);

    /**
     * Presses a key combination (e.g., Ctrl+C).
     * All modifier keys are pressed first, then the main key.
     *
     * @param modifiers Set of modifier key codes (Ctrl, Shift, Alt)
     * @param keyCode   The main key code
     */
    void pressKeyCombination(Set<Integer> modifiers, int keyCode);

    /**
     * Releases a key combination.
     * Main key is released first, then modifiers.
     *
     * @param modifiers Set of modifier key codes
     * @param keyCode   The main key code
     */
    void releaseKeyCombination(Set<Integer> modifiers, int keyCode);

    /**
     * Releases all currently pressed keys.
     * Used for panic stop functionality.
     */
    void releaseAllKeys();

    /**
     * Gets all currently pressed keys.
     *
     * @return Set of currently pressed key codes
     */
    Set<Integer> getPressedKeys();

    /**
     * Checks if a specific key is currently pressed.
     *
     * @param keyCode The key code to check
     * @return true if the key is pressed
     */
    boolean isKeyPressed(int keyCode);

    /**
     * Gets the current auto-delay setting.
     *
     * @return Current delay in milliseconds
     */
    int getAutoDelay();

    /**
     * Sets the delay between key events in milliseconds.
     * Some applications need small delays between events.
     *
     * @param delayMs Delay in milliseconds (0 for no delay)
     */
    void setAutoDelay(int delayMs);

    /**
     * Checks if the simulator is operational.
     * May return false if system permissions are lacking.
     *
     * @return true if simulator can function
     */
    boolean isOperational();
}

