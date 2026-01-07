package com.burstmeman.midi2keys.infrastructure.adapters.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test mode implementation of KeyboardSimulator.
 * Records all key events without actually pressing keys.
 * Used for testing and preview mode.
 */
public class TestKeyboardSimulator implements KeyboardSimulator {
    
    private static final Logger logger = LoggerFactory.getLogger(TestKeyboardSimulator.class);
    
    private final Set<Integer> pressedKeys;
    private final List<KeyEvent> eventHistory;
    private int autoDelay;
    
    public TestKeyboardSimulator() {
        this.pressedKeys = new HashSet<>();
        this.eventHistory = new ArrayList<>();
        this.autoDelay = 0;
        logger.info("TestKeyboardSimulator initialized - no actual key presses will occur");
    }
    
    @Override
    public void keyPress(int keyCode) {
        pressedKeys.add(keyCode);
        eventHistory.add(new KeyEvent(KeyEventType.PRESS, keyCode, System.currentTimeMillis()));
        logger.debug("Test key pressed: {} (total pressed: {})", keyCode, pressedKeys.size());
    }
    
    @Override
    public void keyRelease(int keyCode) {
        pressedKeys.remove(keyCode);
        eventHistory.add(new KeyEvent(KeyEventType.RELEASE, keyCode, System.currentTimeMillis()));
        logger.debug("Test key released: {} (total pressed: {})", keyCode, pressedKeys.size());
    }
    
    @Override
    public void typeKey(int keyCode) {
        keyPress(keyCode);
        keyRelease(keyCode);
    }
    
    @Override
    public void pressKeyCombination(Set<Integer> modifiers, int keyCode) {
        for (int modifier : modifiers) {
            keyPress(modifier);
        }
        keyPress(keyCode);
    }
    
    @Override
    public void releaseKeyCombination(Set<Integer> modifiers, int keyCode) {
        keyRelease(keyCode);
        for (int modifier : modifiers) {
            keyRelease(modifier);
        }
    }
    
    @Override
    public void releaseAllKeys() {
        logger.info("Test mode: releasing all {} pressed keys", pressedKeys.size());
        
        for (int keyCode : new HashSet<>(pressedKeys)) {
            keyRelease(keyCode);
        }
        
        pressedKeys.clear();
    }
    
    @Override
    public Set<Integer> getPressedKeys() {
        return Collections.unmodifiableSet(new HashSet<>(pressedKeys));
    }
    
    @Override
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    @Override
    public void setAutoDelay(int delayMs) {
        this.autoDelay = Math.max(0, delayMs);
    }
    
    @Override
    public int getAutoDelay() {
        return autoDelay;
    }
    
    @Override
    public boolean isOperational() {
        return true; // Always operational for testing
    }
    
    // ===== Test-specific methods =====
    
    /**
     * Gets the complete event history.
     * 
     * @return List of all key events in chronological order
     */
    public List<KeyEvent> getEventHistory() {
        return Collections.unmodifiableList(new ArrayList<>(eventHistory));
    }
    
    /**
     * Clears the event history.
     */
    public void clearHistory() {
        eventHistory.clear();
        logger.debug("Event history cleared");
    }
    
    /**
     * Gets the count of events by type.
     * 
     * @param type Event type to count
     * @return Number of events of that type
     */
    public int getEventCount(KeyEventType type) {
        return (int) eventHistory.stream()
                .filter(e -> e.type() == type)
                .count();
    }
    
    /**
     * Gets all events for a specific key code.
     * 
     * @param keyCode Key code to filter by
     * @return List of events for that key
     */
    public List<KeyEvent> getEventsForKey(int keyCode) {
        return eventHistory.stream()
                .filter(e -> e.keyCode() == keyCode)
                .toList();
    }
    
    /**
     * Represents a recorded key event.
     */
    public record KeyEvent(KeyEventType type, int keyCode, long timestamp) {}
    
    /**
     * Type of key event.
     */
    public enum KeyEventType {
        PRESS,
        RELEASE
    }
}

