# Feature Specification: MIDI-to-Keys Converter

**Feature Branch**: `001-midi-to-keys`  
**Created**: 2025-01-27  
**Status**: Draft  
**Input**: User description: "Build a Windows 11 desktop Java application using JavaFX with a modern Material Design UI that converts MIDI files into simulated keyboard keystrokes. On first launch (and always accessible later), provide a Settings screen to configure one or more root directories on the PC where MIDI files are stored and where playback will be performed from. From the configured root directory, provide a file browser UI that lists all MIDI files in the current folder, supports navigating subfolders (without allowing navigation above the root), and allows switching between folders while keeping the root constraint. Allow users to create and manage multiple "MIDI-to-Keys" profiles. Each profile maps MIDI events (at minimum Note On/Note Off, with note number and velocity) to keyboard key presses (single keys and optional key combinations). Profiles can be selected per file and saved/loaded locally. Provide an editor UI to add/edit/remove mappings, set per-profile playback options (tempo multiplier, quantization, minimum velocity threshold, ignore channels, transpose, and key press duration), and validate conflicting mappings. On single click of a MIDI file in the list, show a preview/details panel containing metadata and analysis: file name/path, format type, tracks count, total duration, tempo changes, time signature (if present), note histogram (counts per pitch), note counts per channel/track, min/max/average velocity, estimated melody length, and any other useful summary statistics. On double click (or pressing a Play button), start playback by parsing the MIDI file and programmatically simulating keyboard key press/release events in real time according to the selected profile and timing. Provide playback controls (Play/Pause/Stop), progress indicator, current position/time, and an option to enable a short countdown before starting playback. Ensure the application does not create nested albums or nested groupings; instead organize content strictly by directory structure under the chosen root. The app must handle large folders efficiently (virtualized list), support search/filter by filename, and show clear error states for unreadable files, unsupported MIDI content, and missing permissions. Include safeguards: a global "panic stop" hotkey to immediately stop playback and release all pressed keys, and a test mode to preview key mappings without sending real keystrokes."

## Clarifications

### Session 2025-01-27

- Q: What keyboard combination should serve as the panic stop hotkey, and should it be user-configurable? → A: Default non-standard combination (e.g., Ctrl+Shift+Escape) with option to configure a different key
- Q: When a user selects a profile for a MIDI file, should this selection be remembered for that specific file, or must it be reselected each time? → A: Remember last used profile globally (all files use the same profile until changed)
- Q: How long should the countdown be before playback starts, and should it be configurable? → A: 3-second default, user-configurable (range: 1-10 seconds)
- Q: How should the application handle when a configured root directory no longer exists or has been moved? → A: Detect invalid directories (on startup and when accessing), show error message with option to remove or update the path
- Q: What does "quantization" mean in this context, and what are the available options? → A: Snap note events to nearest beat subdivision (options: none, 1/4 note, 1/8 note, 1/16 note, 1/32 note)
- Q: What is the exact mapping limit per profile, and is it fixed or configurable? → A: No hard limit in system; users configure mappings based on their own restrictions (e.g., 24 or 32 notes due to hardware constraints)
- Q: How should the system support users with limited mapping capacity? → A: Provide per-MIDI-file note shift configuration (shift mappings 1, 2, 3, or 4 notes up or down) to allow users to work within their self-imposed mapping limitations

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure Root Directories and Browse MIDI Files (Priority: P1)

A user wants to set up the application to access their MIDI file collection and browse through folders to find files they want to play. On first launch, the user is presented with a Settings screen where they can add one or more root directories. After configuration, the user can browse MIDI files within the configured root directories, navigate into subfolders, and switch between different root directories while the application prevents navigation above the configured roots.

**Why this priority**: This is the foundation of the application. Without the ability to configure directories and browse files, no other functionality can be used. This delivers immediate value by allowing users to access their MIDI file collection.

**Independent Test**: Can be fully tested by launching the application, configuring root directories, browsing folders, and verifying that navigation is constrained to the root directories. This delivers value independently as users can explore their MIDI file collection even before profiles or playback are implemented.

**Acceptance Scenarios**:

1. **Given** the application is launched for the first time, **When** the user opens the application, **Then** the Settings screen is displayed automatically
2. **Given** the Settings screen is open, **When** the user adds a root directory, **Then** the directory is saved and the file browser becomes accessible
3. **Given** a root directory is configured, **When** the user opens the file browser, **Then** all MIDI files in the root directory are displayed in a list
4. **Given** the user is viewing a folder, **When** the user clicks on a subfolder, **Then** the file list updates to show MIDI files in that subfolder
5. **Given** the user is in a subfolder, **When** the user attempts to navigate above the root directory, **Then** navigation is prevented and the user remains within the root boundary
6. **Given** multiple root directories are configured, **When** the user switches between root directories, **Then** the file browser updates to show files from the selected root
7. **Given** the Settings screen is accessible from the main interface, **When** the user opens Settings, **Then** they can add, remove, or modify root directories

---

### User Story 2 - Create and Manage MIDI-to-Keys Profiles (Priority: P2)

A user wants to create custom mappings between MIDI events and keyboard keys so they can control how MIDI notes trigger keyboard presses. The user can create multiple profiles, each with its own set of mappings and playback options. Users can edit existing profiles, delete profiles, and select which profile to use for playback (the selected profile applies to all MIDI files until changed).

**Why this priority**: Profiles are essential for the core value proposition of converting MIDI to keyboard input. Without profiles, playback cannot be customized. This delivers value by allowing users to configure how their MIDI files will be converted to keyboard input.

**Independent Test**: Can be fully tested by creating a new profile, adding MIDI note to keyboard key mappings, configuring playback options, saving the profile, and verifying it can be loaded and selected. This delivers value independently as users can prepare their mappings before playback is implemented.

**Acceptance Scenarios**:

1. **Given** the user is in the profile management interface, **When** the user creates a new profile, **Then** an empty profile is created with default settings
2. **Given** a profile is open for editing, **When** the user adds a mapping from a MIDI note to a keyboard key, **Then** the mapping is saved to the profile
3. **Given** a profile has mappings, **When** the user edits a mapping, **Then** the changes are saved to the profile
4. **Given** a profile has mappings, **When** the user removes a mapping, **Then** the mapping is deleted from the profile
5. **Given** multiple profiles exist, **When** the user selects a profile, **Then** that profile becomes the active profile for all MIDI files until changed
6. **Given** a profile is being edited, **When** the user configures playback options (tempo multiplier, quantization, velocity threshold, etc.), **Then** the options are saved with the profile
7. **Given** conflicting mappings exist in a profile, **When** the user attempts to save, **Then** the conflicts are identified and the user is warned

---

### User Story 3 - Preview MIDI File Details and Analysis (Priority: P3)

A user wants to see detailed information about a MIDI file before playing it, including metadata, statistics, and analysis that helps them understand the file's content and structure. When a user clicks on a MIDI file, a preview panel displays comprehensive information about the file.

**Why this priority**: Preview functionality enhances user experience by providing valuable information before playback. Users can make informed decisions about which files to play and which profiles to use. This delivers value independently by helping users understand their MIDI files.

**Independent Test**: Can be fully tested by selecting a MIDI file, viewing the preview panel, and verifying all metadata and analysis information is displayed correctly. This delivers value independently as users can analyze their MIDI collection even without playback functionality.

**Acceptance Scenarios**:

1. **Given** a MIDI file is displayed in the file list, **When** the user single-clicks on the file, **Then** a preview panel appears showing file details
2. **Given** the preview panel is open, **When** the file is analyzed, **Then** the panel displays file name, path, format type, track count, and total duration
3. **Given** the preview panel is open, **When** tempo and time signature information is available, **Then** the panel displays tempo changes and time signature
4. **Given** the preview panel is open, **When** note analysis is performed, **Then** the panel displays a note histogram showing counts per pitch
5. **Given** the preview panel is open, **When** channel and track analysis is performed, **Then** the panel displays note counts per channel and per track
6. **Given** the preview panel is open, **When** velocity analysis is performed, **Then** the panel displays minimum, maximum, and average velocity values
7. **Given** the preview panel is open, **When** melody analysis is performed, **Then** the panel displays estimated melody length

---

### User Story 4 - Playback MIDI Files with Keyboard Simulation (Priority: P4)

A user wants to play a MIDI file and have it automatically trigger keyboard key presses according to their selected profile. The playback should follow the MIDI timing accurately, respect profile settings, and provide controls to manage playback. Users can start playback by double-clicking a file or using a Play button.

**Why this priority**: This is the core value proposition of the application - converting MIDI files to keyboard input in real-time. While it depends on profiles (P2), it delivers the primary user value. This story can be tested independently with a default profile.

**Independent Test**: Can be fully tested by selecting a MIDI file, choosing a profile (or using a default), starting playback, and verifying that keyboard keys are pressed according to the MIDI events and profile mappings. This delivers the core value of the application.

**Acceptance Scenarios**:

1. **Given** a MIDI file is selected and a profile is chosen, **When** the user double-clicks the file or clicks Play, **Then** playback begins and keyboard keys are pressed according to MIDI events (applying any configured per-file note shift)
2. **Given** playback is active, **When** MIDI Note On events occur, **Then** the corresponding keyboard keys are pressed according to the profile mapping
3. **Given** playback is active, **When** MIDI Note Off events occur, **Then** the corresponding keyboard keys are released
4. **Given** playback is active, **When** the user clicks Pause, **Then** playback pauses and all pressed keys remain held
5. **Given** playback is paused, **When** the user clicks Play, **Then** playback resumes from the paused position
6. **Given** playback is active, **When** the user clicks Stop, **Then** playback stops and all pressed keys are released
7. **Given** playback is active, **When** time progresses, **Then** a progress indicator shows current position and elapsed time
8. **Given** countdown is enabled, **When** the user starts playback, **Then** a countdown is displayed before playback begins (default 3 seconds, configurable 1-10 seconds)
9. **Given** a profile has playback options configured, **When** playback occurs, **Then** the options (tempo multiplier, quantization, velocity threshold, transpose, etc.) are applied
10. **Given** a MIDI file has a note shift configured, **When** playback occurs, **Then** the profile mappings are shifted by the specified number of notes (1-4 up or down) before being applied
11. **Given** a MIDI file is selected, **When** the user configures a note shift for that file, **Then** the shift setting is saved and associated with that specific MIDI file

---

### User Story 5 - Advanced Features: Search, Error Handling, and Safeguards (Priority: P5)

A user wants to efficiently find files in large folders, see clear error messages when something goes wrong, and have safety features to prevent issues during playback. The application should handle large directories efficiently, support searching, display helpful error messages, and provide emergency controls.

**Why this priority**: These features enhance usability and safety but are not required for core functionality. They improve the experience for power users and edge cases. This delivers value by making the application more robust and user-friendly.

**Independent Test**: Can be fully tested by searching for files, encountering various error conditions, and using safety features. This delivers value independently by improving overall application quality.

**Acceptance Scenarios**:

1. **Given** a folder contains many MIDI files, **When** the user views the file list, **Then** the list is virtualized and displays efficiently without performance degradation
2. **Given** the file browser is open, **When** the user enters text in a search field, **Then** the file list filters to show only files matching the search term
3. **Given** a MIDI file is unreadable, **When** the user attempts to access it, **Then** a clear error message is displayed explaining the issue
4. **Given** a MIDI file contains unsupported content, **When** the user attempts to play it, **Then** a clear error message is displayed explaining the limitation
5. **Given** the application lacks permissions to access a directory, **When** the user attempts to browse it, **Then** a clear error message is displayed with guidance
6. **Given** playback is active, **When** the user presses the panic stop hotkey, **Then** playback immediately stops and all pressed keys are released
7. **Given** test mode is enabled, **When** the user starts playback, **Then** key mappings are previewed without sending actual keystrokes to the system

---

### Edge Cases

- What happens when a root directory is deleted or moved outside the application? (Resolved: System detects invalid directories on startup and when accessing, shows error message with option to remove or update the path)
- How does the system handle MIDI files with no note events?
- What happens when a profile mapping references a keyboard key that doesn't exist on the current keyboard layout?
- How does the system handle MIDI files with extremely long durations (hours)?
- What happens when multiple MIDI files are selected simultaneously?
- How does the system handle MIDI files with overlapping notes on the same key mapping?
- What happens when playback is started while another application has focus?
- How does the system handle MIDI files with tempo changes that result in very fast or very slow playback?
- What happens when a user tries to create a profile with duplicate mappings?
- How does the system handle MIDI files with velocity values outside the normal range?
- What happens when the panic stop hotkey conflicts with system shortcuts? (Resolved: Users can configure a different hotkey in Settings)
- How does the system handle MIDI files with corrupted or incomplete data?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a Settings screen on first application launch
- **FR-002**: System MUST allow users to add one or more root directories for MIDI file storage
- **FR-003**: System MUST persist root directory configurations between application sessions
- **FR-004**: System MUST provide access to Settings screen from the main application interface at any time
- **FR-005**: System MUST display all MIDI files in the current folder in a file browser list
- **FR-006**: System MUST allow users to navigate into subfolders within configured root directories
- **FR-007**: System MUST prevent navigation above configured root directories
- **FR-008**: System MUST allow users to switch between multiple configured root directories
- **FR-009**: System MUST organize content strictly by directory structure (no nested albums or groupings)
- **FR-010**: System MUST handle large folders efficiently using virtualized lists
- **FR-011**: System MUST support searching and filtering MIDI files by filename
- **FR-012**: System MUST allow users to create multiple MIDI-to-Keys profiles
- **FR-013**: System MUST allow users to edit existing profiles
- **FR-014**: System MUST allow users to delete profiles
- **FR-015**: System MUST allow users to select a profile for playback (selection applies globally to all files until changed)
- **FR-016**: System MUST persist profiles locally between application sessions
- **FR-017**: System MUST support mapping MIDI Note On events to keyboard key presses
- **FR-018**: System MUST support mapping MIDI Note Off events to keyboard key releases
- **FR-019**: System MUST support mapping MIDI notes (by note number) to keyboard keys (no hard limit on number of mappings; users may self-limit based on hardware constraints)
- **FR-020**: System MUST support mapping MIDI velocity values for velocity-sensitive playback
- **FR-021**: System MUST support mapping to single keyboard keys
- **FR-022**: System MUST support mapping to keyboard key combinations
- **FR-023**: System MUST provide a profile editor UI for adding mappings
- **FR-024**: System MUST provide a profile editor UI for editing mappings
- **FR-025**: System MUST provide a profile editor UI for removing mappings
- **FR-026**: System MUST allow per-profile configuration of tempo multiplier
- **FR-027**: System MUST allow per-profile configuration of quantization settings (options: none, 1/4 note, 1/8 note, 1/16 note, 1/32 note - snaps note events to nearest beat subdivision)
- **FR-028**: System MUST allow per-profile configuration of minimum velocity threshold
- **FR-029**: System MUST allow per-profile configuration of ignored MIDI channels
- **FR-030**: System MUST allow per-profile configuration of transpose settings
- **FR-031**: System MUST allow per-profile configuration of key press duration
- **FR-032**: System MUST validate and warn about conflicting mappings in profiles
- **FR-033**: System MUST display a preview/details panel when a MIDI file is single-clicked
- **FR-034**: System MUST display file name and path in the preview panel
- **FR-035**: System MUST display MIDI format type in the preview panel
- **FR-036**: System MUST display track count in the preview panel
- **FR-037**: System MUST display total duration in the preview panel
- **FR-038**: System MUST display tempo changes in the preview panel when available
- **FR-039**: System MUST display time signature in the preview panel when available
- **FR-040**: System MUST display a note histogram (counts per pitch) in the preview panel
- **FR-041**: System MUST display note counts per channel in the preview panel
- **FR-042**: System MUST display note counts per track in the preview panel
- **FR-043**: System MUST display minimum, maximum, and average velocity in the preview panel
- **FR-044**: System MUST display estimated melody length in the preview panel
- **FR-045**: System MUST start playback when a MIDI file is double-clicked
- **FR-046**: System MUST start playback when a Play button is pressed
- **FR-047**: System MUST parse MIDI files and extract events for playback
- **FR-048**: System MUST simulate keyboard key press events in real-time according to MIDI timing
- **FR-049**: System MUST simulate keyboard key release events in real-time according to MIDI timing
- **FR-050**: System MUST apply the selected profile mappings during playback
- **FR-051**: System MUST apply profile playback options during playback
- **FR-052**: System MUST provide Play/Pause/Stop playback controls
- **FR-053**: System MUST display a progress indicator during playback
- **FR-054**: System MUST display current playback position and elapsed time
- **FR-055**: System MUST provide an option to enable a countdown before playback starts (default: 3 seconds, configurable range: 1-10 seconds)
- **FR-056**: System MUST display clear error messages for unreadable MIDI files
- **FR-057**: System MUST display clear error messages for unsupported MIDI content
- **FR-058**: System MUST display clear error messages for missing directory permissions
- **FR-063**: System MUST detect invalid root directories on application startup
- **FR-064**: System MUST detect invalid root directories when user attempts to access them
- **FR-065**: System MUST display error message for invalid root directories with option to remove or update the path
- **FR-059**: System MUST provide a global panic stop hotkey to immediately stop playback (default: Ctrl+Shift+Escape, user-configurable)
- **FR-060**: System MUST release all pressed keys when panic stop is activated
- **FR-062**: System MUST allow users to configure a custom panic stop hotkey in Settings
- **FR-061**: System MUST provide a test mode that previews key mappings without sending real keystrokes
- **FR-066**: System MUST allow users to configure a per-MIDI-file note shift (1, 2, 3, or 4 notes up or down) to adjust profile mappings for that specific file
- **FR-067**: System MUST persist per-MIDI-file note shift settings between application sessions
- **FR-068**: System MUST apply the configured note shift to profile mappings during playback of the associated MIDI file

### Key Entities *(include if feature involves data)*

- **Root Directory**: Represents a configured directory path where MIDI files are stored. Has a unique identifier, path, display name, and creation timestamp. Can be added, removed, or modified by the user.

- **MIDI File**: Represents a MIDI file in the file system. Has a file path, name, size, modification date, and parent directory. Can be analyzed to extract metadata and statistics. Can be selected for preview or playback. Can have a per-file note shift configuration (1-4 notes up or down) to adjust profile mappings for that specific file.

- **Profile**: Represents a MIDI-to-Keys mapping configuration. Has a unique identifier, name, creation date, and modification date. Contains a collection of note mappings and playback options. Can be created, edited, deleted, and selected as the active profile for playback (applies globally to all MIDI files until changed).

- **Note Mapping**: Represents a mapping from a MIDI note to a keyboard key or key combination. Belongs to a profile. Has a MIDI note number, target keyboard key(s), optional velocity range, and optional channel filter. Can be added, edited, or removed from a profile.

- **Playback Options**: Represents configuration settings for how a profile plays back MIDI files. Belongs to a profile. Includes tempo multiplier, quantization settings (none, 1/4, 1/8, 1/16, 1/32 note subdivisions), minimum velocity threshold, ignored channels list, transpose value, and key press duration.

- **MIDI Analysis**: Represents analyzed data about a MIDI file. Belongs to a MIDI file. Includes format type, track count, duration, tempo changes, time signature, note histogram, channel/track note counts, velocity statistics, and estimated melody length.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can configure their first root directory and browse MIDI files within 30 seconds of application launch
- **SC-002**: Users can create a basic profile with 10 note mappings in under 2 minutes
- **SC-003**: Users can preview MIDI file details within 1 second of selecting a file
- **SC-004**: Users can start playback of a MIDI file within 2 seconds of double-clicking or pressing Play
- **SC-005**: Playback maintains timing accuracy within 10 milliseconds of MIDI file timing
- **SC-006**: The application handles folders with 10,000+ MIDI files without performance degradation (list remains responsive)
- **SC-007**: Search results appear within 500 milliseconds of entering search text
- **SC-008**: 95% of standard MIDI files (format 0, 1) are successfully parsed and playable
- **SC-009**: Error messages are displayed within 1 second of encountering an error condition
- **SC-010**: Panic stop hotkey stops playback and releases all keys within 100 milliseconds of activation
- **SC-011**: Users can successfully complete a full workflow (configure directory, browse files, create profile, play file) in under 5 minutes on first use
