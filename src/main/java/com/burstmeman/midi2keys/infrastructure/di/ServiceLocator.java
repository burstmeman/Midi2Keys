package com.burstmeman.midi2keys.infrastructure.di;

import com.burstmeman.midi2keys.application.services.*;
import com.burstmeman.midi2keys.application.usecases.*;
import com.burstmeman.midi2keys.domain.repositories.ProfileRepository;
import com.burstmeman.midi2keys.domain.repositories.SettingsRepository;
import com.burstmeman.midi2keys.infrastructure.adapters.filesystem.FileSystemAdapter;
import com.burstmeman.midi2keys.infrastructure.adapters.filesystem.JavaFileSystemAdapter;
import com.burstmeman.midi2keys.infrastructure.adapters.keyboard.KeyboardSimulator;
import com.burstmeman.midi2keys.infrastructure.adapters.keyboard.RobotKeyboardSimulator;
import com.burstmeman.midi2keys.infrastructure.adapters.keyboard.TestKeyboardSimulator;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.JavaSoundMidiParser;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser;
import com.burstmeman.midi2keys.infrastructure.persistence.database.DatabaseManager;
import com.burstmeman.midi2keys.infrastructure.persistence.database.dao.MidiAnalysisDao;
import com.burstmeman.midi2keys.infrastructure.persistence.database.dao.MidiFileDao;
import com.burstmeman.midi2keys.infrastructure.persistence.database.dao.RootDirectoryDao;
import com.burstmeman.midi2keys.infrastructure.persistence.json.ProfileJsonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service locator for dependency injection.
 * Provides a centralized registry for all application services and manages their lifecycle.
 */
public class ServiceLocator {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceLocator.class);
    
    private static ServiceLocator instance;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Infrastructure
    private DatabaseManager databaseManager;
    private FileSystemAdapter fileSystemAdapter;
    private KeyboardSimulator keyboardSimulator;
    private KeyboardSimulator testKeyboardSimulator;
    private MidiParser midiParser;
    
    // DAOs (RootDirectoryDao implements SettingsRepository)
    private RootDirectoryDao rootDirectoryDao;
    private MidiFileDao midiFileDao;
    private MidiAnalysisDao midiAnalysisDao;
    
    // Repositories
    private SettingsRepository settingsRepository;
    private ProfileRepository profileRepository;
    
    // Application Services
    private RootDirectoryService rootDirectoryService;
    private MidiFileService midiFileService;
    private ProfileService profileService;
    private MidiAnalysisService midiAnalysisService;
    private PlaybackService playbackService;
    private SearchService searchService;
    private PanicStopService panicStopService;
    private GlobalHotkeyService globalHotkeyService;
    private TestModeService testModeService;
    
    // Use Cases
    private ConfigureRootDirectoryUseCase configureRootDirectoryUseCase;
    private BrowseMidiFilesUseCase browseMidiFilesUseCase;
    private CreateProfileUseCase createProfileUseCase;
    private AnalyzeMidiFileUseCase analyzeMidiFileUseCase;
    private PlayMidiFileUseCase playMidiFileUseCase;
    
    // Configuration
    private boolean testMode = false;
    
    private ServiceLocator() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance.
     */
    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }
    
    /**
     * Initialize all services.
     */
    public void initialize() {
        if (initialized.getAndSet(true)) {
            logger.warn("ServiceLocator already initialized");
            return;
        }
        
        logger.info("Initializing ServiceLocator...");
        
        try {
            initializeInfrastructure();
            initializeDaos();
            initializeRepositories();
            initializeServices();
            initializeUseCases();
            wireServices();
            
            logger.info("ServiceLocator initialization complete");
        } catch (Exception e) {
            initialized.set(false);
            logger.error("Failed to initialize ServiceLocator", e);
            throw new RuntimeException("Service initialization failed", e);
        }
    }
    
    private void initializeInfrastructure() {
        logger.debug("Initializing infrastructure components...");
        
        // Database - uses singleton pattern
        databaseManager = DatabaseManager.getInstance();
        databaseManager.initialize();
        
        // File system
        fileSystemAdapter = new JavaFileSystemAdapter();
        
        // Keyboard simulators
        keyboardSimulator = new RobotKeyboardSimulator();
        testKeyboardSimulator = new TestKeyboardSimulator();
        
        // MIDI parser
        midiParser = new JavaSoundMidiParser();
    }
    
    private void initializeDaos() {
        logger.debug("Initializing DAOs...");
        rootDirectoryDao = new RootDirectoryDao(databaseManager);
        midiFileDao = new MidiFileDao(databaseManager);
        midiAnalysisDao = new MidiAnalysisDao(databaseManager);
    }
    
    private void initializeRepositories() {
        logger.debug("Initializing repositories...");
        
        // RootDirectoryDao implements SettingsRepository
        settingsRepository = rootDirectoryDao;
        
        // Profile repository with JSON storage
        Path profilesPath = getAppDataPath().resolve("profiles");
        profileRepository = new ProfileJsonRepository(profilesPath);
    }
    
    private void initializeServices() {
        logger.debug("Initializing services...");
        
        rootDirectoryService = new RootDirectoryService(settingsRepository, fileSystemAdapter);
        midiFileService = new MidiFileService(midiFileDao, fileSystemAdapter);
        profileService = new ProfileService(profileRepository, settingsRepository);
        midiAnalysisService = new MidiAnalysisService(midiAnalysisDao, midiParser);
        
        KeyboardSimulator activeSimulator = testMode ? testKeyboardSimulator : keyboardSimulator;
        playbackService = new PlaybackService(midiParser, activeSimulator);
        
        searchService = new SearchService();
        panicStopService = new PanicStopService(keyboardSimulator);
        globalHotkeyService = new GlobalHotkeyService(panicStopService);
        testModeService = new TestModeService();
    }
    
    private void initializeUseCases() {
        logger.debug("Initializing use cases...");
        
        configureRootDirectoryUseCase = new ConfigureRootDirectoryUseCase(rootDirectoryService, midiFileService);
        browseMidiFilesUseCase = new BrowseMidiFilesUseCase(rootDirectoryService, midiFileService);
        createProfileUseCase = new CreateProfileUseCase(profileService);
        analyzeMidiFileUseCase = new AnalyzeMidiFileUseCase(midiAnalysisService, midiFileService, rootDirectoryService);
        playMidiFileUseCase = new PlayMidiFileUseCase(playbackService, profileService, midiFileService, rootDirectoryService, settingsRepository);
    }
    
    private void wireServices() {
        logger.debug("Wiring service dependencies...");
        panicStopService.setPlaybackService(playbackService);
    }
    
    /**
     * Get the application data path.
     */
    public Path getAppDataPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home");
        }
        Path path = Paths.get(appData, "Midi2Keys");
        
        try {
            java.nio.file.Files.createDirectories(path);
        } catch (java.io.IOException e) {
            logger.error("Failed to create app data directory", e);
        }
        
        return path;
    }
    
    /**
     * Enable or disable test mode.
     */
    public void setTestMode(boolean testMode) {
        if (this.testMode != testMode) {
            this.testMode = testMode;
            logger.info("Test mode {}", testMode ? "enabled" : "disabled");
            
            if (playbackService != null) {
                KeyboardSimulator activeSimulator = testMode ? testKeyboardSimulator : keyboardSimulator;
                playbackService = new PlaybackService(midiParser, activeSimulator);
                playMidiFileUseCase = new PlayMidiFileUseCase(playbackService, profileService, midiFileService, rootDirectoryService, settingsRepository);
            }
            
            testModeService.setTestModeEnabled(testMode);
        }
    }
    
    /**
     * Check if test mode is enabled.
     */
    public boolean isTestMode() {
        return testMode;
    }
    
    /**
     * Shutdown all services and release resources.
     */
    public void shutdown() {
        logger.info("Shutting down ServiceLocator...");
        
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
            logger.error("Error during shutdown", e);
        }
        
        initialized.set(false);
        logger.info("ServiceLocator shutdown complete");
    }
    
    // Getters for services
    
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public FileSystemAdapter getFileSystemAdapter() { return fileSystemAdapter; }
    public KeyboardSimulator getKeyboardSimulator() { return testMode ? testKeyboardSimulator : keyboardSimulator; }
    public MidiParser getMidiParser() { return midiParser; }
    public RootDirectoryService getRootDirectoryService() { return rootDirectoryService; }
    public MidiFileService getMidiFileService() { return midiFileService; }
    public ProfileService getProfileService() { return profileService; }
    public MidiAnalysisService getMidiAnalysisService() { return midiAnalysisService; }
    public PlaybackService getPlaybackService() { return playbackService; }
    public SearchService getSearchService() { return searchService; }
    public PanicStopService getPanicStopService() { return panicStopService; }
    public GlobalHotkeyService getGlobalHotkeyService() { return globalHotkeyService; }
    public TestModeService getTestModeService() { return testModeService; }
    public ConfigureRootDirectoryUseCase getConfigureRootDirectoryUseCase() { return configureRootDirectoryUseCase; }
    public BrowseMidiFilesUseCase getBrowseMidiFilesUseCase() { return browseMidiFilesUseCase; }
    public CreateProfileUseCase getCreateProfileUseCase() { return createProfileUseCase; }
    public AnalyzeMidiFileUseCase getAnalyzeMidiFileUseCase() { return analyzeMidiFileUseCase; }
    public PlayMidiFileUseCase getPlayMidiFileUseCase() { return playMidiFileUseCase; }
    public ProfileRepository getProfileRepository() { return profileRepository; }
    public SettingsRepository getSettingsRepository() { return settingsRepository; }
}
