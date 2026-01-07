package com.burstmeman.midi2keys.infrastructure.persistence.database;

/**
 * Exception thrown for database-related errors.
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

