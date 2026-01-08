package com.burstmeman.midi2keys.infrastructure.di;

import com.burstmeman.midi2keys.application.services.PanicStopService;
import com.burstmeman.midi2keys.application.services.PlaybackService;
import com.burstmeman.midi2keys.application.services.SearchService;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles application shutdown logic.
 * Ensures all services are properly shut down when the application closes.
 */
@Slf4j
@Component
public class ShutdownHandler {

    private final PlaybackService playbackService;
    private final PanicStopService panicStopService;
    private final SearchService searchService;
    private final DatabaseManager databaseManager;

    @Autowired
    public ShutdownHandler(
            PlaybackService playbackService,
            PanicStopService panicStopService,
            SearchService searchService,
            DatabaseManager databaseManager) {
        this.playbackService = playbackService;
        this.panicStopService = panicStopService;
        this.searchService = searchService;
        this.databaseManager = databaseManager;
    }

    /**
     * Shuts down all application services.
     * Should be called explicitly before closing the Spring context.
     */
    public void shutdown() {
        log.info("Shutting down application services...");

        try {
            if (playbackService != null) {
                playbackService.panicStop();
            }

            if (panicStopService != null) {
                panicStopService.releaseAllKeys();
            }

            if (searchService != null) {
                searchService.shutdown();
            }

            if (databaseManager != null) {
                databaseManager.close();
            }
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }

        log.info("Application shutdown complete");
    }
}

