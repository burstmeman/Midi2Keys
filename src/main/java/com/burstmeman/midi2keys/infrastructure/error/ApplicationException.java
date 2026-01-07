package com.burstmeman.midi2keys.infrastructure.error;

/**
 * Base exception for all application-specific exceptions.
 * Provides user-friendly error messages and error codes.
 */
public class ApplicationException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String userMessage;
    
    public ApplicationException(ErrorCode errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public ApplicationException(ErrorCode errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    /**
     * Gets the error code for categorization.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the user-friendly error message.
     */
    public String getUserMessage() {
        return userMessage;
    }
    
    /**
     * Error codes for categorizing exceptions.
     */
    public enum ErrorCode {
        // File system errors
        FILE_NOT_FOUND("FILE_001", "File Not Found"),
        DIRECTORY_NOT_FOUND("FILE_002", "Directory Not Found"),
        FILE_NOT_READABLE("FILE_003", "File Not Readable"),
        PERMISSION_DENIED("FILE_004", "Permission Denied"),
        PATH_TRAVERSAL("FILE_005", "Invalid Path"),
        
        // MIDI errors
        INVALID_MIDI_FILE("MIDI_001", "Invalid MIDI File"),
        UNSUPPORTED_MIDI_FORMAT("MIDI_002", "Unsupported MIDI Format"),
        MIDI_PARSE_ERROR("MIDI_003", "MIDI Parse Error"),
        
        // Profile errors
        PROFILE_NOT_FOUND("PROF_001", "Profile Not Found"),
        PROFILE_SAVE_ERROR("PROF_002", "Profile Save Error"),
        PROFILE_LOAD_ERROR("PROF_003", "Profile Load Error"),
        PROFILE_VALIDATION_ERROR("PROF_004", "Profile Validation Error"),
        MAPPING_CONFLICT("PROF_005", "Mapping Conflict"),
        
        // Playback errors
        PLAYBACK_ERROR("PLAY_001", "Playback Error"),
        KEYBOARD_SIMULATION_ERROR("PLAY_002", "Keyboard Simulation Error"),
        NO_PROFILE_SELECTED("PLAY_003", "No Profile Selected"),
        
        // Database errors
        DATABASE_ERROR("DB_001", "Database Error"),
        
        // Configuration errors
        INVALID_CONFIGURATION("CFG_001", "Invalid Configuration"),
        ROOT_DIRECTORY_INVALID("CFG_002", "Root Directory Invalid"),
        
        // General errors
        UNKNOWN_ERROR("GEN_001", "Unknown Error"),
        VALIDATION_ERROR("GEN_002", "Validation Error");
        
        private final String code;
        private final String category;
        
        ErrorCode(String code, String category) {
            this.code = code;
            this.category = category;
        }
        
        public String getCode() { return code; }
        public String getCategory() { return category; }
    }
}

