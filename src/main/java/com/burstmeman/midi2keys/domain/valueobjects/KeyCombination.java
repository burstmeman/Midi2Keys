package com.burstmeman.midi2keys.domain.valueobjects;

import java.awt.event.KeyEvent;
import java.util.*;

/**
 * Represents a keyboard key combination (e.g., Ctrl+C, Shift+A).
 * Contains a main key and optional modifier keys.
 */
public record KeyCombination(KeyboardKey mainKey, Set<KeyboardKey> modifiers) {

    /**
     * Creates a simple key (no modifiers).
     *
     * @param mainKey The main key
     */
    public KeyCombination(KeyboardKey mainKey) {
        this(Objects.requireNonNull(mainKey, "Main key cannot be null"), Collections.emptySet());
    }

    /**
     * Creates a key combination with modifiers.
     *
     * @param mainKey   The main key
     * @param modifiers Set of modifier keys
     */
    public KeyCombination(KeyboardKey mainKey, Set<KeyboardKey> modifiers) {
        this.mainKey = Objects.requireNonNull(mainKey, "Main key cannot be null");

        // Filter to only actual modifier keys
        Set<KeyboardKey> validModifiers = new HashSet<>();
        if (modifiers != null) {
            for (KeyboardKey mod : modifiers) {
                if (mod != null && mod.isModifier()) {
                    validModifiers.add(mod);
                }
            }
        }
        this.modifiers = Collections.unmodifiableSet(validModifiers);
    }

    /**
     * Parses a key combination from a string (e.g., "Ctrl+Shift+A", "F5", "Space").
     *
     * @param input Key combination string
     * @return KeyCombination instance
     * @throws IllegalArgumentException if parsing fails
     */
    public static KeyCombination parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Key combination string cannot be empty");
        }

        String[] parts = input.split("\\+");
        Set<KeyboardKey> modifiers = new HashSet<>();
        KeyboardKey mainKey = null;

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            KeyboardKey key = KeyboardKey.fromName(trimmed);

            if (key.isModifier()) {
                modifiers.add(key);
            } else {
                if (mainKey != null) {
                    throw new IllegalArgumentException(
                            "Multiple non-modifier keys specified: " + mainKey + " and " + key);
                }
                mainKey = key;
            }
        }

        if (mainKey == null) {
            throw new IllegalArgumentException("No main (non-modifier) key specified in: " + input);
        }

        return new KeyCombination(mainKey, modifiers);
    }

    /**
     * Tries to parse a key combination, returning null on failure.
     *
     * @param input Key combination string
     * @return KeyCombination or null
     */
    public static KeyCombination tryParse(String input) {
        try {
            return parse(input);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a key combination with Ctrl modifier.
     *
     * @param key The main key
     * @return KeyCombination with Ctrl
     */
    public static KeyCombination ctrl(KeyboardKey key) {
        return new KeyCombination(key, Set.of(new KeyboardKey(KeyEvent.VK_CONTROL)));
    }

    /**
     * Creates a key combination with Shift modifier.
     *
     * @param key The main key
     * @return KeyCombination with Shift
     */
    public static KeyCombination shift(KeyboardKey key) {
        return new KeyCombination(key, Set.of(new KeyboardKey(KeyEvent.VK_SHIFT)));
    }

    /**
     * Creates a key combination with Alt modifier.
     *
     * @param key The main key
     * @return KeyCombination with Alt
     */
    public static KeyCombination alt(KeyboardKey key) {
        return new KeyCombination(key, Set.of(new KeyboardKey(KeyEvent.VK_ALT)));
    }

    /**
     * Gets the modifier keys.
     *
     * @return Unmodifiable set of modifiers
     */
    @Override
    public Set<KeyboardKey> modifiers() {
        return modifiers;
    }

    /**
     * Gets all key codes (main key + modifiers) as a set.
     *
     * @return Set of key codes
     */
    public Set<Integer> getAllKeyCodes() {
        Set<Integer> codes = new HashSet<>();
        codes.add(mainKey.getKeyCode());
        for (KeyboardKey mod : modifiers) {
            codes.add(mod.getKeyCode());
        }
        return codes;
    }

    /**
     * Gets only the modifier key codes.
     *
     * @return Set of modifier key codes
     */
    public Set<Integer> getModifierKeyCodes() {
        Set<Integer> codes = new HashSet<>();
        for (KeyboardKey mod : modifiers) {
            codes.add(mod.getKeyCode());
        }
        return codes;
    }

    /**
     * Gets the main key code.
     *
     * @return Main key code
     */
    public int getMainKeyCode() {
        return mainKey.getKeyCode();
    }

    /**
     * Checks if this combination has any modifiers.
     *
     * @return true if has modifiers
     */
    public boolean hasModifiers() {
        return !modifiers.isEmpty();
    }

    /**
     * Checks if this combination includes Ctrl.
     *
     * @return true if Ctrl is included
     */
    public boolean hasCtrl() {
        return modifiers.stream().anyMatch(m -> m.getKeyCode() == KeyEvent.VK_CONTROL);
    }

    /**
     * Checks if this combination includes Shift.
     *
     * @return true if Shift is included
     */
    public boolean hasShift() {
        return modifiers.stream().anyMatch(m -> m.getKeyCode() == KeyEvent.VK_SHIFT);
    }

    /**
     * Checks if this combination includes Alt.
     *
     * @return true if Alt is included
     */
    public boolean hasAlt() {
        return modifiers.stream().anyMatch(m -> m.getKeyCode() == KeyEvent.VK_ALT);
    }

    /**
     * Gets a human-readable display string (e.g., "Ctrl+Shift+A").
     *
     * @return Display string
     */
    public String getDisplayString() {
        StringJoiner joiner = new StringJoiner("+");

        // Add modifiers in consistent order: Ctrl, Alt, Shift, Win
        if (hasCtrl()) joiner.add("Ctrl");
        if (hasAlt()) joiner.add("Alt");
        if (hasShift()) joiner.add("Shift");
        for (KeyboardKey mod : modifiers) {
            if (mod.getKeyCode() == KeyEvent.VK_WINDOWS || mod.getKeyCode() == KeyEvent.VK_META) {
                joiner.add("Win");
            }
        }

        joiner.add(mainKey.getDisplayName());

        return joiner.toString();
    }

    /**
     * Creates a copy of this key combination.
     *
     * @return New KeyCombination instance
     */
    public KeyCombination copy() {
        return new KeyCombination(
                new KeyboardKey(mainKey.getKeyCode()),
                new HashSet<>(modifiers)
        );
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}

