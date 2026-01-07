# Tasks: MIDI-to-Keys Converter

**Input**: Design documents from `/specs/001-midi-to-keys/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Tests are included following AAA testing standards from constitution (80% coverage for business logic, 60% for UI controllers).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- Paths follow the structure defined in plan.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create project directory structure per implementation plan in src/main/java/com/burstmeman/midi2keys/
- [X] T002 [P] Update pom.xml to Java 17 (upgrade from Java 8) and add MaterialFX dependency (io.github.palexdev:materialfx)
- [X] T003 [P] Add SQLite JDBC driver dependency (org.xerial:sqlite-jdbc) to pom.xml
- [X] T004 [P] Add Jackson dependencies (com.fasterxml.jackson.core:jackson-databind) to pom.xml for JSON serialization
- [X] T005 [P] Add TestFX dependency (org.testfx:testfx-core, org.testfx:testfx-junit5) to pom.xml for UI testing
- [X] T006 [P] Add Mockito dependency (org.mockito:mockito-core) to pom.xml for mocking
- [X] T007 [P] Add SLF4J logging dependencies (org.slf4j:slf4j-api, ch.qos.logback:logback-classic) to pom.xml
- [X] T008 Create test directory structure (tests/unit, tests/integration, tests/ui) per plan.md
- [X] T009 [P] Configure MaterialFX UserAgentBuilder in src/main/java/com/burstmeman/midi2keys/infrastructure/config/ApplicationConfig.java
- [X] T010 Create application entry point in src/main/java/com/burstmeman/midi2keys/Main.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T011 Create DatabaseManager in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/database/DatabaseManager.java
- [X] T012 [P] Create database migration framework in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/database/migrations/MigrationManager.java
- [X] T013 [P] Create SQLite database schema (root_directories, midi_files, midi_analyses, file_note_shifts, application_settings tables)
- [X] T014 [P] Create FileSystemAdapter interface in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/filesystem/FileSystemAdapter.java
- [X] T015 [P] Implement JavaFileSystemAdapter in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/filesystem/JavaFileSystemAdapter.java
- [X] T016 [P] Create KeyboardSimulator interface in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/keyboard/KeyboardSimulator.java
- [X] T017 [P] Implement RobotKeyboardSimulator in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/keyboard/RobotKeyboardSimulator.java
- [X] T018 [P] Implement TestKeyboardSimulator in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/keyboard/TestKeyboardSimulator.java
- [X] T019 [P] Create MidiParser interface in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/midi/MidiParser.java
- [X] T020 [P] Implement JavaSoundMidiParser in src/main/java/com/burstmeman/midi2keys/infrastructure/adapters/midi/JavaSoundMidiParser.java
- [X] T021 [P] Create error handling infrastructure with user-friendly error messages
- [X] T022 [P] Configure logging infrastructure using SLF4J with appropriate log levels
- [X] T023 Create ApplicationConfig class in src/main/java/com/burstmeman/midi2keys/infrastructure/config/ApplicationConfig.java for application-wide configuration

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Configure Root Directories and Browse MIDI Files (Priority: P1) 🎯 MVP

**Goal**: Users can configure root directories for MIDI file storage and browse files within those directories with navigation constraints.

**Independent Test**: Launch application, configure root directories, browse folders, verify navigation is constrained to root directories. Users can explore MIDI collection independently.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T024 [P] [US1] Unit test for RootDirectory entity in tests/unit/domain/entities/RootDirectoryTest.java
- [ ] T025 [P] [US1] Unit test for RootDirectoryService in tests/unit/application/services/RootDirectoryServiceTest.java
- [ ] T026 [P] [US1] Unit test for MidiFileService in tests/unit/application/services/MidiFileServiceTest.java
- [ ] T027 [P] [US1] Integration test for root directory configuration in tests/integration/RootDirectoryIntegrationTest.java
- [ ] T028 [P] [US1] Integration test for file browsing with navigation constraints in tests/integration/FileBrowsingIntegrationTest.java
- [ ] T029 [P] [US1] UI test for SettingsController in tests/ui/controllers/SettingsControllerTest.java using TestFX
- [ ] T030 [P] [US1] UI test for FileBrowserController in tests/ui/controllers/FileBrowserControllerTest.java using TestFX

### Implementation for User Story 1

- [X] T031 [P] [US1] Create RootDirectory entity in src/main/java/com/burstmeman/midi2keys/domain/entities/RootDirectory.java
- [X] T032 [P] [US1] Create MidiFile entity in src/main/java/com/burstmeman/midi2keys/domain/entities/MidiFile.java
- [X] T033 [P] [US1] Create SettingsRepository interface in src/main/java/com/burstmeman/midi2keys/domain/repositories/SettingsRepository.java
- [X] T034 [P] [US1] Create MidiFileRepository interface in src/main/java/com/burstmeman/midi2keys/domain/repositories/MidiFileRepository.java
- [X] T035 [US1] Create RootDirectoryDao in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/database/dao/RootDirectoryDao.java (depends on T011)
- [X] T036 [US1] Create MidiFileDao in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/database/dao/MidiFileDao.java (depends on T011)
- [X] T037 [US1] Implement RootDirectoryService in src/main/java/com/burstmeman/midi2keys/application/services/RootDirectoryService.java (depends on T031, T035)
- [X] T038 [US1] Implement MidiFileService in src/main/java/com/burstmeman/midi2keys/application/services/MidiFileService.java (depends on T032, T036, T015)
- [X] T039 [US1] Implement ConfigureRootDirectoryUseCase in src/main/java/com/burstmeman/midi2keys/application/usecases/ConfigureRootDirectoryUseCase.java (depends on T037)
- [X] T040 [US1] Implement BrowseMidiFilesUseCase in src/main/java/com/burstmeman/midi2keys/application/usecases/BrowseMidiFilesUseCase.java (depends on T038)
- [X] T041 [US1] Create SettingsController in src/main/java/com/burstmeman/midi2keys/ui/controllers/SettingsController.java (depends on T039)
- [X] T042 [US1] Create FileBrowserController in src/main/java/com/burstmeman/midi2keys/ui/controllers/FileBrowserController.java (depends on T040)
- [X] T043 [US1] Create settings-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/settings-view.fxml with MaterialFX components
- [X] T044 [US1] Create file-browser-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/file-browser-view.fxml with virtualized list
- [X] T045 [US1] Implement first-launch detection and automatic Settings screen display in Application.java
- [X] T046 [US1] Implement root directory validation and error handling for deleted/moved directories
- [X] T047 [US1] Implement navigation constraint logic to prevent navigation above root directories
- [X] T048 [US1] Create MainController in src/main/java/com/burstmeman/midi2keys/ui/controllers/MainController.java for main application window
- [X] T049 [US1] Create main-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/main-view.fxml with navigation to Settings and File Browser

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Create and Manage MIDI-to-Keys Profiles (Priority: P2)

**Goal**: Users can create, edit, delete, and select MIDI-to-keyboard mapping profiles with playback options configuration.

**Independent Test**: Create profile, add mappings, configure options, save profile, verify it can be loaded and selected. Users can prepare mappings before playback.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T050 [P] [US2] Unit test for Profile entity in tests/unit/domain/entities/ProfileTest.java
- [ ] T051 [P] [US2] Unit test for NoteMapping entity in tests/unit/domain/entities/NoteMappingTest.java
- [ ] T052 [P] [US2] Unit test for PlaybackOptions entity in tests/unit/domain/entities/PlaybackOptionsTest.java
- [ ] T053 [P] [US2] Unit test for ProfileService in tests/unit/application/services/ProfileServiceTest.java
- [ ] T054 [P] [US2] Integration test for profile JSON persistence in tests/integration/profiles/ProfilePersistenceTest.java
- [ ] T055 [P] [US2] Integration test for profile conflict validation in tests/integration/profiles/ProfileValidationTest.java
- [ ] T056 [P] [US2] UI test for ProfileManagerController in tests/ui/controllers/ProfileManagerControllerTest.java using TestFX
- [ ] T057 [P] [US2] UI test for ProfileEditorController in tests/ui/controllers/ProfileEditorControllerTest.java using TestFX

### Implementation for User Story 2

- [X] T058 [P] [US2] Create Profile entity in src/main/java/com/burstmeman/midi2keys/domain/entities/Profile.java
- [X] T059 [P] [US2] Create NoteMapping entity in src/main/java/com/burstmeman/midi2keys/domain/entities/NoteMapping.java
- [X] T060 [P] [US2] Create PlaybackOptions entity in src/main/java/com/burstmeman/midi2keys/domain/entities/PlaybackOptions.java
- [X] T061 [P] [US2] Create MidiNote value object in src/main/java/com/burstmeman/midi2keys/domain/valueobjects/MidiNote.java
- [X] T062 [P] [US2] Create KeyboardKey value object in src/main/java/com/burstmeman/midi2keys/domain/valueobjects/KeyboardKey.java
- [X] T063 [P] [US2] Create KeyCombination value object in src/main/java/com/burstmeman/midi2keys/domain/valueobjects/KeyCombination.java
- [X] T064 [P] [US2] Create ProfileRepository interface in src/main/java/com/burstmeman/midi2keys/domain/repositories/ProfileRepository.java
- [X] T065 [US2] Implement ProfileJsonRepository in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/json/ProfileJsonRepository.java (depends on T064)
- [X] T066 [US2] Implement ProfileJsonSerializer in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/json/ProfileJsonSerializer.java using Jackson (depends on T004)
- [X] T067 [US2] Implement ProfileService in src/main/java/com/burstmeman/midi2keys/application/services/ProfileService.java (depends on T058, T065)
- [X] T068 [US2] Implement CreateProfileUseCase in src/main/java/com/burstmeman/midi2keys/application/usecases/CreateProfileUseCase.java (depends on T067)
- [X] T069 [US2] Implement profile conflict validation logic in ProfileService
- [X] T070 [US2] Create ProfileManagerController in src/main/java/com/burstmeman/midi2keys/ui/controllers/ProfileManagerController.java (depends on T067)
- [X] T071 [US2] Create ProfileEditorController in src/main/java/com/burstmeman/midi2keys/ui/controllers/ProfileEditorController.java (depends on T067)
- [X] T072 [US2] Create profile-manager-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/profile-manager-view.fxml with MaterialFX components
- [X] T073 [US2] Create profile-editor-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/profile-editor-view.fxml with mapping editor
- [X] T074 [US2] Create NoteMappingEditor component in src/main/java/com/burstmeman/midi2keys/ui/components/NoteMappingEditor.java
- [X] T075 [US2] Implement profile selection UI and global profile state management
- [X] T076 [US2] Implement playback options configuration UI (tempo multiplier, quantization, velocity threshold, ignore channels, transpose, key press duration)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Preview MIDI File Details and Analysis (Priority: P3)

**Goal**: Users can view comprehensive MIDI file metadata and analysis information in a preview panel.

**Independent Test**: Select MIDI file, view preview panel, verify all metadata and analysis information is displayed correctly. Users can analyze MIDI collection without playback.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T077 [P] [US3] Unit test for MidiAnalysis entity in tests/unit/domain/entities/MidiAnalysisTest.java
- [ ] T078 [P] [US3] Unit test for MidiAnalysisService in tests/unit/application/services/MidiAnalysisServiceTest.java
- [ ] T079 [P] [US3] Integration test for MIDI file analysis in tests/integration/midi/MidiAnalysisIntegrationTest.java
- [ ] T080 [P] [US3] UI test for PreviewController in tests/ui/controllers/PreviewControllerTest.java using TestFX

### Implementation for User Story 3

- [X] T081 [P] [US3] Create MidiAnalysis entity in src/main/java/com/burstmeman/midi2keys/domain/entities/MidiAnalysis.java
- [X] T082 [US3] Create MidiAnalysisDao in src/main/java/com/burstmeman/midi2keys/infrastructure/persistence/database/dao/MidiAnalysisDao.java (depends on T011)
- [X] T083 [US3] Implement MidiAnalysisService in src/main/java/com/burstmeman/midi2keys/application/services/MidiAnalysisService.java (depends on T081, T082, T020)
- [X] T084 [US3] Implement AnalyzeMidiFileUseCase in src/main/java/com/burstmeman/midi2keys/application/usecases/AnalyzeMidiFileUseCase.java (depends on T083)
- [X] T085 [US3] Implement MIDI file parsing and analysis logic (format type, tracks, duration, tempo changes, time signature, note histogram, channel/track counts, velocity statistics, melody length estimation)
- [X] T086 [US3] Create PreviewController in src/main/java/com/burstmeman/midi2keys/ui/controllers/PreviewController.java (depends on T084)
- [X] T087 [US3] Create preview-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/preview-view.fxml with analysis display components
- [X] T088 [US3] Implement single-click file selection to show preview panel in FileBrowserController
- [X] T089 [US3] Implement lazy loading of MIDI analysis (analyze on demand, cache results in database)

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently

---

## Phase 6: User Story 4 - Playback MIDI Files with Keyboard Simulation (Priority: P4)

**Goal**: Users can play MIDI files with real-time keyboard simulation according to profile mappings and timing.

**Independent Test**: Select MIDI file, choose profile, start playback, verify keyboard keys are pressed according to MIDI events and profile mappings. Core value delivered.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T090 [P] [US4] Unit test for PlaybackService in tests/unit/application/services/PlaybackServiceTest.java
- [ ] T091 [P] [US4] Integration test for MIDI playback with keyboard simulation in tests/integration/playback/PlaybackIntegrationTest.java
- [ ] T092 [P] [US4] Integration test for playback timing accuracy in tests/integration/playback/PlaybackTimingTest.java
- [ ] T093 [P] [US4] UI test for PlaybackController in tests/ui/controllers/PlaybackControllerTest.java using TestFX

### Implementation for User Story 4

- [X] T094 [P] [US4] Create NoteShift value object in src/main/java/com/burstmeman/midi2keys/domain/valueobjects/NoteShift.java
- [X] T095 [US4] Update MidiFile entity to include note shift configuration (depends on T032, T094)
- [X] T096 [US4] Create file_note_shifts table migration in database (depends on T012)
- [X] T097 [US4] Implement PlaybackService in src/main/java/com/burstmeman/midi2keys/application/services/PlaybackService.java (depends on T067, T020, T016)
- [X] T098 [US4] Implement PlayMidiFileUseCase in src/main/java/com/burstmeman/midi2keys/application/usecases/PlayMidiFileUseCase.java (depends on T097)
- [X] T099 [US4] Implement MIDI event scheduling and timing logic with profile options (tempo multiplier, quantization, velocity threshold, transpose)
- [X] T100 [US4] Implement note shift application logic (shift profile mappings 1-4 notes up/down per file)
- [X] T101 [US4] Implement playback controls (Play/Pause/Stop) with state management
- [X] T102 [US4] Implement progress indicator and current position/time display
- [X] T103 [US4] Implement countdown timer (default 3 seconds, configurable 1-10 seconds) before playback
- [X] T104 [US4] Implement non-blocking playback using JavaFX Task/Service to avoid blocking UI thread
- [X] T105 [US4] Create PlaybackController in src/main/java/com/burstmeman/midi2keys/ui/controllers/PlaybackController.java (depends on T098)
- [X] T106 [US4] Create playback-view.fxml in src/main/resources/com/burstmeman/midi2keys/ui/views/playback-view.fxml with controls and progress indicator
- [X] T107 [US4] Implement double-click file or Play button to start playback in FileBrowserController
- [X] T108 [US4] Implement per-file note shift configuration UI in preview panel
- [X] T109 [US4] Implement key press/release event simulation according to MIDI timing and profile mappings

**Checkpoint**: At this point, User Stories 1, 2, 3, AND 4 should all work independently

---

## Phase 7: User Story 5 - Advanced Features: Search, Error Handling, and Safeguards (Priority: P5)

**Goal**: Users have efficient file search, clear error handling, and safety features (panic stop, test mode).

**Independent Test**: Search files, encounter error conditions, use safety features. Application quality improvements delivered.

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T110 [P] [US5] Unit test for search/filter functionality in tests/unit/application/services/MidiFileServiceTest.java
- [ ] T111 [P] [US5] Integration test for error handling scenarios in tests/integration/ErrorHandlingIntegrationTest.java
- [ ] T112 [P] [US5] Integration test for panic stop hotkey in tests/integration/playback/PanicStopTest.java
- [ ] T113 [P] [US5] Integration test for test mode in tests/integration/playback/TestModeTest.java

### Implementation for User Story 5

- [X] T114 [US5] Implement file search/filter by filename in MidiFileService (depends on T038)
- [X] T115 [US5] Implement virtualized list optimization for large folders (10,000+ files) in FileBrowserController
- [X] T116 [US5] Implement comprehensive error handling with user-friendly messages for unreadable files, unsupported MIDI content, missing permissions
- [X] T117 [US5] Implement global panic stop hotkey (default Ctrl+Shift+Escape, configurable) in Application.java
- [X] T118 [US5] Implement panic stop functionality to immediately stop playback and release all pressed keys
- [X] T119 [US5] Implement test mode toggle that uses TestKeyboardSimulator instead of RobotKeyboardSimulator
- [X] T120 [US5] Add panic stop hotkey configuration to Settings UI
- [X] T121 [US5] Implement error message display UI components with actionable guidance
- [X] T122 [US5] Optimize file list rendering performance for large directories

**Checkpoint**: All user stories should now be independently functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T123 [P] Documentation updates (README.md, user guide)
- [X] T124 Code cleanup and refactoring across all layers
- [X] T125 [P] Performance optimization (startup time, UI responsiveness, memory usage)
- [ ] T126 [P] Additional unit tests to reach 80% coverage target for business logic
- [ ] T127 [P] Additional UI tests to reach 60% coverage target for controllers
- [X] T128 [P] Accessibility improvements (keyboard navigation, screen reader support)
- [X] T129 [P] UI consistency review and Material Design compliance check
- [X] T130 Error handling refinement and user message improvements
- [X] T131 Logging enhancement with appropriate context and levels
- [X] T132 Memory profiling and optimization for large MIDI files
- [X] T133 Application packaging and Windows 11 deployment preparation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4 → P5)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent, no dependencies on other stories
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on US1 for file selection, but analysis is independent
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Depends on US2 for profiles, but can use default profile for testing
- **User Story 5 (P5)**: Can start after Foundational (Phase 2) - Enhances US1 (search), US4 (panic stop, test mode), but is independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Domain entities before services
- Services before use cases
- Use cases before controllers
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T002-T007, T009)
- All Foundational tasks marked [P] can run in parallel (T012-T023)
- Once Foundational phase completes, user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Domain entities within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: T024 [P] [US1] Unit test for RootDirectory entity
Task: T025 [P] [US1] Unit test for RootDirectoryService
Task: T026 [P] [US1] Unit test for MidiFileService
Task: T027 [P] [US1] Integration test for root directory configuration
Task: T028 [P] [US1] Integration test for file browsing
Task: T029 [P] [US1] UI test for SettingsController
Task: T030 [P] [US1] UI test for FileBrowserController

# Launch all domain entities for User Story 1 together:
Task: T031 [P] [US1] Create RootDirectory entity
Task: T032 [P] [US1] Create MidiFile entity
Task: T033 [P] [US1] Create SettingsRepository interface
Task: T034 [P] [US1] Create MidiFileRepository interface
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4 → Test independently → Deploy/Demo
6. Add User Story 5 → Test independently → Deploy/Demo
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently
4. Continue with User Stories 4 and 5

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Follow AAA testing pattern (Arrange-Act-Assert) for all tests
- Maintain 80% coverage for business logic, 60% for UI controllers

