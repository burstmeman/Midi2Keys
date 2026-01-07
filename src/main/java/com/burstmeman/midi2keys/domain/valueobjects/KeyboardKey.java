package com.burstmeman.midi2keys.domain.valueobjects;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single keyboard key with its virtual key code.
 */
public final class KeyboardKey {
    
    // Common key code mappings
    private static final Map<String, Integer> NAME_TO_CODE = new HashMap<>();
    private static final Map<Integer, String> CODE_TO_NAME = new HashMap<>();
    
    static {
        // Letters
        for (char c = 'A'; c <= 'Z'; c++) {
            int code = KeyEvent.VK_A + (c - 'A');
            NAME_TO_CODE.put(String.valueOf(c), code);
            CODE_TO_NAME.put(code, String.valueOf(c));
        }
        
        // Numbers
        for (char c = '0'; c <= '9'; c++) {
            int code = KeyEvent.VK_0 + (c - '0');
            NAME_TO_CODE.put(String.valueOf(c), code);
            CODE_TO_NAME.put(code, String.valueOf(c));
        }
        
        // Function keys
        for (int i = 1; i <= 12; i++) {
            int code = KeyEvent.VK_F1 + (i - 1);
            String name = "F" + i;
            NAME_TO_CODE.put(name, code);
            CODE_TO_NAME.put(code, name);
        }
        
        // Special keys
        addKey("SPACE", KeyEvent.VK_SPACE, "Space");
        addKey("ENTER", KeyEvent.VK_ENTER, "Enter");
        addKey("TAB", KeyEvent.VK_TAB, "Tab");
        addKey("ESCAPE", KeyEvent.VK_ESCAPE, "Esc");
        addKey("ESC", KeyEvent.VK_ESCAPE, "Esc");
        addKey("BACKSPACE", KeyEvent.VK_BACK_SPACE, "Backspace");
        addKey("DELETE", KeyEvent.VK_DELETE, "Del");
        addKey("DEL", KeyEvent.VK_DELETE, "Del");
        addKey("INSERT", KeyEvent.VK_INSERT, "Ins");
        addKey("INS", KeyEvent.VK_INSERT, "Ins");
        addKey("HOME", KeyEvent.VK_HOME, "Home");
        addKey("END", KeyEvent.VK_END, "End");
        addKey("PAGEUP", KeyEvent.VK_PAGE_UP, "PgUp");
        addKey("PGUP", KeyEvent.VK_PAGE_UP, "PgUp");
        addKey("PAGEDOWN", KeyEvent.VK_PAGE_DOWN, "PgDn");
        addKey("PGDN", KeyEvent.VK_PAGE_DOWN, "PgDn");
        
        // Arrow keys
        addKey("UP", KeyEvent.VK_UP, "↑");
        addKey("DOWN", KeyEvent.VK_DOWN, "↓");
        addKey("LEFT", KeyEvent.VK_LEFT, "←");
        addKey("RIGHT", KeyEvent.VK_RIGHT, "→");
        
        // Modifiers
        addKey("CTRL", KeyEvent.VK_CONTROL, "Ctrl");
        addKey("CONTROL", KeyEvent.VK_CONTROL, "Ctrl");
        addKey("ALT", KeyEvent.VK_ALT, "Alt");
        addKey("SHIFT", KeyEvent.VK_SHIFT, "Shift");
        addKey("WIN", KeyEvent.VK_WINDOWS, "Win");
        addKey("WINDOWS", KeyEvent.VK_WINDOWS, "Win");
        
        // Punctuation and symbols
        addKey("COMMA", KeyEvent.VK_COMMA, ",");
        addKey(",", KeyEvent.VK_COMMA, ",");
        addKey("PERIOD", KeyEvent.VK_PERIOD, ".");
        addKey(".", KeyEvent.VK_PERIOD, ".");
        addKey("SLASH", KeyEvent.VK_SLASH, "/");
        addKey("/", KeyEvent.VK_SLASH, "/");
        addKey("SEMICOLON", KeyEvent.VK_SEMICOLON, ";");
        addKey(";", KeyEvent.VK_SEMICOLON, ";");
        addKey("QUOTE", KeyEvent.VK_QUOTE, "'");
        addKey("'", KeyEvent.VK_QUOTE, "'");
        addKey("BACKSLASH", KeyEvent.VK_BACK_SLASH, "\\");
        addKey("\\", KeyEvent.VK_BACK_SLASH, "\\");
        addKey("OPENBRACKET", KeyEvent.VK_OPEN_BRACKET, "[");
        addKey("[", KeyEvent.VK_OPEN_BRACKET, "[");
        addKey("CLOSEBRACKET", KeyEvent.VK_CLOSE_BRACKET, "]");
        addKey("]", KeyEvent.VK_CLOSE_BRACKET, "]");
        addKey("MINUS", KeyEvent.VK_MINUS, "-");
        addKey("-", KeyEvent.VK_MINUS, "-");
        addKey("EQUALS", KeyEvent.VK_EQUALS, "=");
        addKey("=", KeyEvent.VK_EQUALS, "=");
        addKey("BACKQUOTE", KeyEvent.VK_BACK_QUOTE, "`");
        addKey("`", KeyEvent.VK_BACK_QUOTE, "`");
        
        // Numpad
        for (int i = 0; i <= 9; i++) {
            int code = KeyEvent.VK_NUMPAD0 + i;
            String name = "NUMPAD" + i;
            NAME_TO_CODE.put(name, code);
            CODE_TO_NAME.put(code, "Num" + i);
        }
        addKey("NUMPADPLUS", KeyEvent.VK_ADD, "Num+");
        addKey("NUMPADMINUS", KeyEvent.VK_SUBTRACT, "Num-");
        addKey("NUMPADMULTIPLY", KeyEvent.VK_MULTIPLY, "Num*");
        addKey("NUMPADDIVIDE", KeyEvent.VK_DIVIDE, "Num/");
        addKey("NUMPADENTER", KeyEvent.VK_ENTER, "NumEnter");
    }
    
    private static void addKey(String name, int code, String displayName) {
        NAME_TO_CODE.put(name.toUpperCase(), code);
        if (!CODE_TO_NAME.containsKey(code)) {
            CODE_TO_NAME.put(code, displayName);
        }
    }
    
    private final int keyCode;
    private final String displayName;
    
    /**
     * Creates a keyboard key from a key code.
     * 
     * @param keyCode Virtual key code (from java.awt.event.KeyEvent)
     */
    public KeyboardKey(int keyCode) {
        this.keyCode = keyCode;
        this.displayName = CODE_TO_NAME.getOrDefault(keyCode, "Key" + keyCode);
    }
    
    /**
     * Creates a keyboard key from a key name.
     * 
     * @param keyName Key name (case-insensitive)
     * @return KeyboardKey instance
     * @throws IllegalArgumentException if key name is unknown
     */
    public static KeyboardKey fromName(String keyName) {
        String normalized = keyName.toUpperCase().trim();
        Integer code = NAME_TO_CODE.get(normalized);
        
        if (code == null) {
            throw new IllegalArgumentException("Unknown key name: " + keyName);
        }
        
        return new KeyboardKey(code);
    }
    
    /**
     * Tries to create a keyboard key from a name, returning null if invalid.
     * 
     * @param keyName Key name
     * @return KeyboardKey or null
     */
    public static KeyboardKey tryFromName(String keyName) {
        try {
            return fromName(keyName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Checks if a key name is valid.
     * 
     * @param keyName Key name to check
     * @return true if valid
     */
    public static boolean isValidKeyName(String keyName) {
        return NAME_TO_CODE.containsKey(keyName.toUpperCase().trim());
    }
    
    /**
     * Gets the virtual key code.
     * 
     * @return Key code
     */
    public int getKeyCode() {
        return keyCode;
    }
    
    /**
     * Gets the display name for the key.
     * 
     * @return Human-readable key name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if this is a modifier key (Ctrl, Alt, Shift, Win).
     * 
     * @return true if modifier
     */
    public boolean isModifier() {
        return keyCode == KeyEvent.VK_CONTROL ||
               keyCode == KeyEvent.VK_ALT ||
               keyCode == KeyEvent.VK_SHIFT ||
               keyCode == KeyEvent.VK_WINDOWS ||
               keyCode == KeyEvent.VK_META;
    }
    
    /**
     * Checks if this is a function key (F1-F12).
     * 
     * @return true if function key
     */
    public boolean isFunctionKey() {
        return keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12;
    }
    
    /**
     * Checks if this is a letter key (A-Z).
     * 
     * @return true if letter
     */
    public boolean isLetter() {
        return keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z;
    }
    
    /**
     * Checks if this is a number key (0-9, not numpad).
     * 
     * @return true if number
     */
    public boolean isNumber() {
        return keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyboardKey that = (KeyboardKey) o;
        return keyCode == that.keyCode;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(keyCode);
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}

