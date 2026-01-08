# Midi2Keys

> **⚠️ Important Note**: This project was **fully generated** using [Cursor](https://cursor.sh) and [spec-kit](https://github.com/github/spec-kit) following Spec-Driven Development principles. This is primarily an **experiment** to evaluate the quality of code that AI tools can generate when following structured development methodologies. **No manual code was written** - everything was generated through AI-assisted development.
>
> The main goal of this experiment is to assess whether AI tools can produce code that follows important software engineering principles and is maintainable, readable, and doesn't cause "blood from eyes" or "WTF expression" moments when reviewing it. In my experience, simple prompts often lead to poor quality code that shouldn't be seen by any living creature, so this project serves as a test case for structured, spec-driven AI development.

A Windows 11 desktop application that converts MIDI files into simulated keyboard keystrokes. Built with Java 17 and JavaFX with Material Design UI.

## Building

```bash
# Clone the repository
git clone https://github.com/yourusername/Midi2Keys.git
cd Midi2Keys

# Build with Maven
mvn clean package

# Or use the Maven wrapper
.\mvnw.cmd clean package
```

## Running

```bash
# Run directly with Maven
mvn javafx:run

# Or run the packaged JAR
java -jar target/Midi2Keys-1.0-SNAPSHOT.jar
```

## Project Structure

```
src/main/java/com/burstmeman/midi2keys/
├── application/          # Application services and use cases
│   ├── services/        # Business logic services
│   └── usecases/        # Use case implementations
├── domain/              # Domain entities and repositories
│   ├── entities/        # Core domain models
│   ├── repositories/    # Repository interfaces
│   └── valueobjects/    # Value objects (immutable)
├── infrastructure/      # External adapters and persistence
│   ├── adapters/        # External system adapters
│   ├── config/          # Application configuration
│   ├── di/              # Dependency injection
│   ├── error/           # Error handling
│   └── persistence/     # Database and JSON storage
├── ui/                  # User interface layer
│   ├── components/      # Reusable UI components
│   └── controllers/     # FXML controllers
├── Application.java     # JavaFX Application entry
└── Main.java           # Main entry point
```

## Architecture

The application follows **Clean Architecture** principles:

1. **Domain Layer**: Core business entities and repository interfaces
2. **Application Layer**: Use cases and services orchestrating business logic
3. **Infrastructure Layer**: Adapters for external systems (database, filesystem, MIDI, keyboard)
4. **UI Layer**: JavaFX controllers and components

## Key Components

### MIDI Parsing
Uses `javax.sound.midi` for reading and analyzing MIDI files.

### Keyboard Simulation
Uses `java.awt.Robot` API for simulating keyboard keystrokes on Windows.

### Data Storage
- **SQLite Database**: Application settings, root directories, MIDI file metadata
- **JSON Files**: User-created mapping profiles

### UI Framework
- **JavaFX 21**: Core UI framework
- **MaterialFX**: Material Design components
- **Custom CSS**: Dark theme styling

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Escape` | Panic Stop - immediately stop playback and release all keys |
| `Ctrl+Shift+P` | Alternative panic stop hotkey |
| `Space` | Play/Pause playback |
| `Enter` | Start playback from selected file |

## Configuration

The application stores configuration in:
- **Windows**: `%APPDATA%\Midi2Keys\`
  - `midi2keys.db` - SQLite database
  - `profiles/` - JSON profile files

## Playback Options

Each profile supports:
- **Tempo Multiplier**: Speed up or slow down playback (0.25x - 4.0x)
- **Quantization**: Snap notes to grid (None, 1/4, 1/8, 1/16, 1/32)
- **Velocity Threshold**: Minimum velocity to trigger key press (0-127)
- **Ignore Channels**: Skip specific MIDI channels
- **Transpose**: Shift all notes up/down by semitones (-24 to +24)
- **Key Press Duration**: Fixed or velocity-based duration

## Note Shift Feature

For keyboards with limited mappable keys (e.g., 24 or 32 notes), configure a per-file note shift:
- Shift mappings 1-4 semitones up or down
- Allows same profile to work across different octave ranges
- Stored per MIDI file in the database

## Test Mode

Enable test mode to preview key mappings without sending actual keystrokes:
- Shows which keys would be pressed in the event log
- Useful for verifying mappings before actual playback
- Toggle via Settings or toolbar

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn verify
```

### Code Style

The project follows:
- Clean Code principles
- AAA (Arrange-Act-Assert) testing pattern
- JavaDoc for public APIs

## License

AGPL-3.0 License - see [LICENSE](LICENSE) for details.

## Acknowledgments

- [MaterialFX](https://github.com/palexdev/MaterialFX) for the Material Design components
- [SQLite](https://www.sqlite.org/) for embedded database
- [Jackson](https://github.com/FasterXML/jackson) for JSON processing

