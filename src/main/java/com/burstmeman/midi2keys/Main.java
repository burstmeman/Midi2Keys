package com.burstmeman.midi2keys;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Midi2Keys application.
 * This class serves as the launcher to avoid JavaFX module issues.
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        log.info("Starting Midi2Keys application...");

        try {
            // Launch the JavaFX application
            Application.launch(args);
        } catch (Exception e) {
            log.error("Failed to start application", e);
            System.exit(1);
        }
    }
}

