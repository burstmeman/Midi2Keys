package com.burstmeman.midi2keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Midi2Keys application.
 * This class serves as the launcher to avoid JavaFX module issues.
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("Starting Midi2Keys application...");
        
        try {
            // Launch the JavaFX application
            Application.launch(args);
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
}

