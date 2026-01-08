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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring configuration class that replaces ServiceLocator.
 * Defines all application beans and their dependencies.
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = {
    "com.burstmeman.midi2keys.ui.controllers",
    "com.burstmeman.midi2keys.infrastructure.config"
})
public class SpringConfig {

    @Value("${app.test-mode:false}")
    private boolean testMode;

    // ========== Infrastructure Beans ==========

    @Bean(initMethod = "initialize", destroyMethod = "close")
    public DatabaseManager databaseManager() {
        log.debug("Creating DatabaseManager bean");
        return DatabaseManager.getInstance();
    }

    @Bean
    public FileSystemAdapter fileSystemAdapter() {
        log.debug("Creating FileSystemAdapter bean");
        return new JavaFileSystemAdapter();
    }

    @Bean
    @Primary
    public KeyboardSimulator keyboardSimulator() {
        log.debug("Creating RobotKeyboardSimulator bean");
        return new RobotKeyboardSimulator();
    }

    @Bean
    public KeyboardSimulator testKeyboardSimulator() {
        log.debug("Creating TestKeyboardSimulator bean");
        return new TestKeyboardSimulator();
    }

    @Bean
    public MidiParser midiParser() {
        log.debug("Creating MidiParser bean");
        return new JavaSoundMidiParser();
    }

    // ========== DAO Beans ==========

    @Bean
    @DependsOn("databaseManager")
    public RootDirectoryDao rootDirectoryDao(DatabaseManager databaseManager) {
        log.debug("Creating RootDirectoryDao bean");
        return new RootDirectoryDao(databaseManager);
    }

    @Bean
    @DependsOn("databaseManager")
    public MidiFileDao midiFileDao(DatabaseManager databaseManager) {
        log.debug("Creating MidiFileDao bean");
        return new MidiFileDao(databaseManager);
    }

    @Bean
    @DependsOn("databaseManager")
    public MidiAnalysisDao midiAnalysisDao(DatabaseManager databaseManager) {
        log.debug("Creating MidiAnalysisDao bean");
        return new MidiAnalysisDao(databaseManager);
    }

    // ========== Repository Beans ==========

    @Bean
    @Primary
    public SettingsRepository settingsRepository(RootDirectoryDao rootDirectoryDao) {
        log.debug("Creating SettingsRepository bean (using RootDirectoryDao)");
        // RootDirectoryDao implements SettingsRepository
        // Marked as @Primary to avoid ambiguity since rootDirectoryDao also implements SettingsRepository
        return rootDirectoryDao;
    }

    @Bean
    public ProfileRepository profileRepository() {
        log.debug("Creating ProfileRepository bean");
        Path profilesPath = getAppDataPath().resolve("profiles");
        return new ProfileJsonRepository(profilesPath);
    }

    // ========== Service Beans ==========

    @Bean
    public RootDirectoryService rootDirectoryService(
            SettingsRepository settingsRepository,
            FileSystemAdapter fileSystemAdapter) {
        log.debug("Creating RootDirectoryService bean");
        return new RootDirectoryService(settingsRepository, fileSystemAdapter);
    }

    @Bean
    public MidiFileService midiFileService(
            MidiFileDao midiFileDao,
            FileSystemAdapter fileSystemAdapter) {
        log.debug("Creating MidiFileService bean");
        return new MidiFileService(midiFileDao, fileSystemAdapter);
    }

    @Bean
    public ProfileService profileService(
            ProfileRepository profileRepository,
            SettingsRepository settingsRepository) {
        log.debug("Creating ProfileService bean");
        return new ProfileService(profileRepository, settingsRepository);
    }

    @Bean
    public MidiAnalysisService midiAnalysisService(
            MidiAnalysisDao midiAnalysisDao,
            MidiParser midiParser) {
        log.debug("Creating MidiAnalysisService bean");
        return new MidiAnalysisService(midiAnalysisDao, midiParser);
    }

    @Bean
    public PlaybackService playbackService(
            MidiParser midiParser,
            KeyboardSimulator keyboardSimulator,
            KeyboardSimulator testKeyboardSimulator) {
        log.debug("Creating PlaybackService bean");
        // Use test or real simulator based on test mode
        KeyboardSimulator activeSimulator = testMode ? testKeyboardSimulator : keyboardSimulator;
        return new PlaybackService(midiParser, activeSimulator);
    }

    @Bean
    public SearchService searchService() {
        log.debug("Creating SearchService bean");
        return new SearchService();
    }

    @Bean
    @DependsOn("playbackService")
    public PanicStopService panicStopService(
            KeyboardSimulator keyboardSimulator,
            PlaybackService playbackService) {
        log.debug("Creating PanicStopService bean");
        PanicStopService service = new PanicStopService(keyboardSimulator);
        service.setPlaybackService(playbackService);
        return service;
    }

    @Bean
    @DependsOn("panicStopService")
    public GlobalHotkeyService globalHotkeyService(PanicStopService panicStopService) {
        log.debug("Creating GlobalHotkeyService bean");
        return new GlobalHotkeyService(panicStopService);
    }

    @Bean
    public TestModeService testModeService() {
        log.debug("Creating TestModeService bean");
        TestModeService service = new TestModeService();
        service.setTestModeEnabled(testMode);
        return service;
    }

    // ========== Use Case Beans ==========

    @Bean
    public ConfigureRootDirectoryUseCase configureRootDirectoryUseCase(
            RootDirectoryService rootDirectoryService,
            MidiFileService midiFileService) {
        log.debug("Creating ConfigureRootDirectoryUseCase bean");
        return new ConfigureRootDirectoryUseCase(rootDirectoryService, midiFileService);
    }

    @Bean
    public BrowseMidiFilesUseCase browseMidiFilesUseCase(
            RootDirectoryService rootDirectoryService,
            MidiFileService midiFileService) {
        log.debug("Creating BrowseMidiFilesUseCase bean");
        return new BrowseMidiFilesUseCase(rootDirectoryService, midiFileService);
    }

    @Bean
    public CreateProfileUseCase createProfileUseCase(ProfileService profileService) {
        log.debug("Creating CreateProfileUseCase bean");
        return new CreateProfileUseCase(profileService);
    }

    @Bean
    public AnalyzeMidiFileUseCase analyzeMidiFileUseCase(
            MidiAnalysisService midiAnalysisService,
            MidiFileService midiFileService,
            RootDirectoryService rootDirectoryService) {
        log.debug("Creating AnalyzeMidiFileUseCase bean");
        return new AnalyzeMidiFileUseCase(midiAnalysisService, midiFileService, rootDirectoryService);
    }

    @Bean
    public PlayMidiFileUseCase playMidiFileUseCase(
            PlaybackService playbackService,
            ProfileService profileService,
            MidiFileService midiFileService,
            RootDirectoryService rootDirectoryService,
            SettingsRepository settingsRepository) {
        log.debug("Creating PlayMidiFileUseCase bean");
        return new PlayMidiFileUseCase(playbackService, profileService, midiFileService,
                rootDirectoryService, settingsRepository);
    }

    // ========== Shutdown Handler Bean ==========
    // Note: Controllers are defined via @ComponentScan above

    @Bean
    @DependsOn({"playbackService", "panicStopService", "searchService", "databaseManager"})
    public ShutdownHandler shutdownHandler(
            PlaybackService playbackService,
            PanicStopService panicStopService,
            SearchService searchService,
            DatabaseManager databaseManager) {
        log.debug("Creating ShutdownHandler bean");
        return new ShutdownHandler(playbackService, panicStopService, searchService, databaseManager);
    }

    // ========== Helper Methods ==========

    /**
     * Get the application data path.
     */
    private Path getAppDataPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home");
        }
        Path path = Paths.get(appData, "Midi2Keys");

        try {
            java.nio.file.Files.createDirectories(path);
        } catch (java.io.IOException e) {
            log.error("Failed to create app data directory", e);
        }

        return path;
    }
}

