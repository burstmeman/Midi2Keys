package com.burstmeman.midi2keys.application.services;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Service for managing global hotkeys within the application.
 * Provides registration and handling of keyboard shortcuts that work
 * regardless of which component has focus.
 */
public class GlobalHotkeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalHotkeyService.class);
    
    // Default panic stop hotkey: Escape or Ctrl+Shift+P
    public static final KeyCodeCombination DEFAULT_PANIC_HOTKEY = 
        new KeyCodeCombination(KeyCode.ESCAPE);
    public static final KeyCodeCombination ALTERNATIVE_PANIC_HOTKEY = 
        new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    
    private final Map<String, HotkeyBinding> hotkeyBindings = new ConcurrentHashMap<>();
    private final PanicStopService panicStopService;
    
    private KeyCodeCombination panicHotkey = DEFAULT_PANIC_HOTKEY;
    private boolean enabled = true;
    
    public GlobalHotkeyService(PanicStopService panicStopService) {
        this.panicStopService = panicStopService;
        
        // Register default panic stop hotkey
        registerPanicStopHotkey();
    }
    
    /**
     * Register the global hotkey handler with a scene.
     * Must be called after the scene is created.
     * 
     * @param scene the JavaFX scene to attach handlers to
     */
    public void registerWithScene(Scene scene) {
        if (scene == null) {
            logger.warn("Cannot register hotkeys with null scene");
            return;
        }
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        logger.info("Global hotkey service registered with scene");
    }
    
    /**
     * Handle key press events.
     */
    private void handleKeyPress(KeyEvent event) {
        if (!enabled) {
            return;
        }
        
        // Check for panic stop hotkeys first (highest priority)
        if (isPanicHotkey(event)) {
            logger.info("Panic hotkey detected: {}", event.getCode());
            event.consume();
            panicStopService.triggerPanicStop();
            return;
        }
        
        // Check other registered hotkeys
        for (HotkeyBinding binding : hotkeyBindings.values()) {
            if (binding.matches(event)) {
                logger.debug("Hotkey triggered: {}", binding.getName());
                event.consume();
                try {
                    binding.getAction().accept(event);
                } catch (Exception e) {
                    logger.error("Error executing hotkey action: {}", binding.getName(), e);
                }
                return;
            }
        }
    }
    
    /**
     * Check if the event matches the panic stop hotkey.
     */
    private boolean isPanicHotkey(KeyEvent event) {
        return panicHotkey.match(event) || ALTERNATIVE_PANIC_HOTKEY.match(event);
    }
    
    /**
     * Register default panic stop hotkey binding.
     */
    private void registerPanicStopHotkey() {
        // Panic stop is handled specially in handleKeyPress, but log it here
        logger.info("Panic stop hotkey registered: {} or {}", 
            formatKeyCombination(panicHotkey), 
            formatKeyCombination(ALTERNATIVE_PANIC_HOTKEY));
    }
    
    /**
     * Register a new hotkey binding.
     * 
     * @param name unique name for the hotkey
     * @param combination key combination to trigger
     * @param action action to execute when triggered
     */
    public void registerHotkey(String name, KeyCodeCombination combination, Consumer<KeyEvent> action) {
        if (name == null || combination == null || action == null) {
            logger.warn("Invalid hotkey registration attempt: name={}, combo={}", name, combination);
            return;
        }
        
        HotkeyBinding binding = new HotkeyBinding(name, combination, action);
        hotkeyBindings.put(name, binding);
        logger.info("Registered hotkey '{}': {}", name, formatKeyCombination(combination));
    }
    
    /**
     * Unregister a hotkey binding.
     * 
     * @param name the name of the hotkey to remove
     */
    public void unregisterHotkey(String name) {
        HotkeyBinding removed = hotkeyBindings.remove(name);
        if (removed != null) {
            logger.info("Unregistered hotkey: {}", name);
        }
    }
    
    /**
     * Set a custom panic stop hotkey.
     * 
     * @param combination the new key combination for panic stop
     */
    public void setPanicHotkey(KeyCodeCombination combination) {
        if (combination != null) {
            this.panicHotkey = combination;
            logger.info("Panic hotkey updated to: {}", formatKeyCombination(combination));
        }
    }
    
    /**
     * Get the current panic stop hotkey.
     * 
     * @return the current panic hotkey combination
     */
    public KeyCodeCombination getPanicHotkey() {
        return panicHotkey;
    }
    
    /**
     * Enable or disable all hotkey handling.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Global hotkey service {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Check if the service is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Format a key combination for display.
     */
    private String formatKeyCombination(KeyCodeCombination combination) {
        return combination.getDisplayText();
    }
    
    /**
     * Internal class representing a hotkey binding.
     */
    private static class HotkeyBinding {
        private final String name;
        private final KeyCodeCombination combination;
        private final Consumer<KeyEvent> action;
        
        HotkeyBinding(String name, KeyCodeCombination combination, Consumer<KeyEvent> action) {
            this.name = name;
            this.combination = combination;
            this.action = action;
        }
        
        String getName() {
            return name;
        }
        
        Consumer<KeyEvent> getAction() {
            return action;
        }
        
        boolean matches(KeyEvent event) {
            return combination.match(event);
        }
    }
}

