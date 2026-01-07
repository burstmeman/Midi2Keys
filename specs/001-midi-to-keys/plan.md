# Implementation Plan: MIDI-to-Keys Converter

**Branch**: `001-midi-to-keys` | **Date**: 2025-01-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-midi-to-keys/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Build a Windows 11 desktop Java application using JavaFX with Material Design UI that converts MIDI files into simulated keyboard keystrokes. The application allows users to configure root directories for MIDI file storage, browse and preview MIDI files, create and manage MIDI-to-keyboard mapping profiles, and play MIDI files with real-time keyboard simulation. Technical approach: JavaFX with MaterialFX library for modern UI, javax.sound.midi for MIDI parsing, Java Robot API or JNativeHook for Windows keyboard simulation, JSON for profile storage, and SQLite for metadata persistence.

## Technical Context

**Language/Version**: Java 17+ (upgrade from Java 8 to support modern libraries and JavaFX 21)  
**Primary Dependencies**: 
- JavaFX 21.0.6 (already in project)
- MaterialFX (io.github.palexdev:materialfx) for Material Design UI components
- javax.sound.midi (built-in JDK) for MIDI file parsing and event handling
- Java Robot API (java.awt.Robot) for keyboard simulation on Windows
- SQLite JDBC Driver (org.xerial:sqlite-jdbc) for local database
- Jackson (com.fasterxml.jackson.core:jackson-databind) for JSON profile serialization
- JUnit 5.12.1 (already in project) for testing

**Storage**: 
- SQLite database for metadata (root directories, MIDI file analysis, per-file note shifts, application settings)
- JSON files for profile storage (one file per profile in user data directory)

**Testing**: 
- JUnit 5 for unit and integration tests
- TestFX for JavaFX UI testing
- Mockito for mocking external dependencies (keyboard simulation, file system)

**Target Platform**: Windows 11 desktop application  
**Project Type**: Single desktop application (JavaFX)  
**Performance Goals**: 
- Application startup < 2 seconds
- UI interactions < 100ms response time
- MIDI file preview analysis < 1 second
- Playback timing accuracy within 10ms of MIDI file timing
- Handle 10,000+ MIDI files in directory without performance degradation

**Constraints**: 
- Windows 11 only (keyboard simulation platform-specific)
- Must not block UI thread during MIDI playback
- Must handle MIDI files with up to 16 tracks and multiple tempo changes
- Memory-efficient handling of large MIDI files
- Keyboard simulation must work even when application loses focus

**Scale/Scope**: 
- Single-user desktop application
- Support for unlimited profiles (stored as JSON files)
- Support for unlimited MIDI file mappings per profile
- Support for multiple root directories
- Estimated codebase: 15,000-20,000 lines of Java code

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Architecture Compliance**:
- [x] UI (JavaFX controllers) separated from business logic (services)
- [x] Dependencies flow inward: UI → Application → Domain
- [x] External dependencies abstracted behind interfaces (keyboard simulation, MIDI parsing, file system, database)
- [x] Business logic is framework-agnostic and independently testable

**Testing Standards**:
- [x] Test strategy follows AAA pattern (Arrange-Act-Assert)
- [x] Unit tests planned for business logic (target: 80% coverage)
- [x] Integration tests planned for component interactions (MIDI parsing, profile management, database operations)
- [x] UI tests planned for user interactions (target: 60% coverage for controllers using TestFX)

**Code Quality**:
- [x] Functions designed for single responsibility
- [x] Naming conventions follow domain language (Profile, NoteMapping, MidiFile, etc.)
- [x] Error handling strategy defined (user-friendly messages, logging, graceful degradation)
- [x] Logging strategy defined (SLF4J with appropriate log levels)

**Performance Requirements**:
- [x] Startup time target defined (< 2 seconds)
- [x] UI responsiveness targets defined (< 100ms for interactions)
- [x] Background operation strategy (non-blocking UI thread - use JavaFX Task/Service for MIDI parsing and playback)
- [x] Memory usage considerations identified (virtualized lists, lazy loading of MIDI analysis)

**User Experience**:
- [x] UI consistency patterns defined (Material Design via MaterialFX)
- [x] Error message strategy defined (clear, actionable messages with guidance)
- [x] User feedback mechanisms planned (loading states, progress indicators, countdown timer)
- [x] Accessibility considerations identified (keyboard navigation, screen reader support where applicable)

## Project Structure

### Documentation (this feature)

```text
specs/001-midi-to-keys/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── burstmeman/
│   │           └── midi2keys/
│   │               ├── ui/                    # JavaFX UI layer (Controllers, Views)
│   │               │   ├── controllers/       # FXML controllers
│   │               │   │   ├── MainController.java
│   │               │   │   ├── SettingsController.java
│   │               │   │   ├── FileBrowserController.java
│   │               │   │   ├── ProfileEditorController.java
│   │               │   │   ├── ProfileManagerController.java
│   │               │   │   ├── PlaybackController.java
│   │               │   │   └── PreviewController.java
│   │               │   ├── views/            # FXML view files
│   │               │   │   ├── main-view.fxml
│   │               │   │   ├── settings-view.fxml
│   │               │   │   ├── file-browser-view.fxml
│   │               │   │   ├── profile-editor-view.fxml
│   │               │   │   ├── profile-manager-view.fxml
│   │               │   │   ├── playback-view.fxml
│   │               │   │   └── preview-view.fxml
│   │               │   └── components/      # Reusable UI components
│   │               │       ├── MidiFileListCell.java
│   │               │       ├── NoteMappingEditor.java
│   │               │       └── ProgressIndicator.java
│   │               ├── application/          # Application layer (Services, Use Cases)
│   │               │   ├── services/
│   │               │   │   ├── RootDirectoryService.java
│   │               │   │   ├── MidiFileService.java
│   │               │   │   ├── ProfileService.java
│   │               │   │   ├── PlaybackService.java
│   │               │   │   ├── MidiAnalysisService.java
│   │               │   │   └── SettingsService.java
│   │               │   └── usecases/
│   │               │       ├── ConfigureRootDirectoryUseCase.java
│   │               │       ├── BrowseMidiFilesUseCase.java
│   │               │       ├── CreateProfileUseCase.java
│   │               │       ├── PlayMidiFileUseCase.java
│   │               │       └── AnalyzeMidiFileUseCase.java
│   │               ├── domain/               # Domain layer (Entities, Value Objects)
│   │               │   ├── entities/
│   │               │   │   ├── RootDirectory.java
│   │               │   │   ├── MidiFile.java
│   │               │   │   ├── Profile.java
│   │               │   │   ├── NoteMapping.java
│   │               │   │   ├── PlaybackOptions.java
│   │               │   │   └── MidiAnalysis.java
│   │               │   ├── valueobjects/
│   │               │   │   ├── MidiNote.java
│   │               │   │   ├── KeyboardKey.java
│   │               │   │   ├── KeyCombination.java
│   │               │   │   └── NoteShift.java
│   │               │   └── repositories/      # Repository interfaces
│   │               │       ├── ProfileRepository.java
│   │               │       ├── MidiFileRepository.java
│   │               │       └── SettingsRepository.java
│   │               ├── infrastructure/        # Infrastructure layer (Adapters, Implementations)
│   │               │   ├── adapters/
│   │               │   │   ├── keyboard/
│   │               │   │   │   ├── KeyboardSimulator.java (interface)
│   │               │   │   │   ├── RobotKeyboardSimulator.java (Java Robot implementation)
│   │               │   │   │   └── TestKeyboardSimulator.java (test mode implementation)
│   │               │   │   ├── midi/
│   │               │   │   │   ├── MidiParser.java (interface)
│   │               │   │   │   └── JavaSoundMidiParser.java (javax.sound.midi implementation)
│   │               │   │   └── filesystem/
│   │               │   │       ├── FileSystemAdapter.java (interface)
│   │               │   │       └── JavaFileSystemAdapter.java (java.nio implementation)
│   │               │   ├── persistence/
│   │               │   │   ├── database/
│   │               │   │   │   ├── DatabaseManager.java
│   │               │   │   │   ├── dao/
│   │               │   │   │   │   ├── RootDirectoryDao.java
│   │               │   │   │   │   ├── MidiFileDao.java
│   │               │   │   │   │   └── MidiAnalysisDao.java
│   │               │   │   │   └── migrations/
│   │               │   │   │       └── MigrationManager.java
│   │               │   │   └── json/
│   │               │   │       ├── ProfileJsonRepository.java
│   │               │   │       └── ProfileJsonSerializer.java
│   │               │   └── config/
│   │               │       └── ApplicationConfig.java
│   │               └── Main.java             # Application entry point
│   │               └── Application.java     # JavaFX Application class
│   └── resources/
│       ├── com/
│       │   └── burstmeman/
│       │       └── midi2keys/
│       │           └── ui/
│       │               └── views/            # FXML files (same structure as java/views)
│       └── styles/
│           └── application.css               # Custom styles (MaterialFX handles most styling)
│
tests/
├── unit/
│   ├── domain/                               # Domain entity tests
│   ├── application/                          # Service and use case tests
│   └── infrastructure/                       # Adapter tests
├── integration/
│   ├── midi/                                 # MIDI parsing integration tests
│   ├── database/                             # Database integration tests
│   ├── profiles/                             # Profile persistence tests
│   └── playback/                             # End-to-end playback tests
└── ui/
    └── controllers/                          # TestFX controller tests
```

**Structure Decision**: Single project structure with clear separation of UI, Application, Domain, and Infrastructure layers following Clean Architecture principles. The structure supports the constitution requirement of UI/backend separation with dependencies flowing inward (UI → Application → Domain). External dependencies (keyboard simulation, MIDI parsing, file system, database) are abstracted behind interfaces in the infrastructure layer.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations identified. The architecture follows clean architecture principles with proper separation of concerns. All external dependencies are abstracted behind interfaces, enabling testability and future extensibility.

