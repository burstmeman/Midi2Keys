package com.burstmeman.midi2keys.infrastructure.adapters.midi;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * javax.sound.midi-based implementation of MidiParser.
 */
@Slf4j
public class JavaSoundMidiParser implements MidiParser {

    // MIDI message types
    private static final int NOTE_OFF = 0x80;
    private static final int NOTE_ON = 0x90;
    private static final int META_TEMPO = 0x51;
    private static final int META_TIME_SIGNATURE = 0x58;
    private static final int META_TRACK_NAME = 0x03;

    // Default tempo
    private static final float DEFAULT_TEMPO_BPM = 120.0f;
    private static final int MICROSECONDS_PER_MINUTE = 60_000_000;

    @Override
    public MidiFileInfo parse(Path path) throws IOException, MidiParseException {
        log.debug("Parsing MIDI file: {}", path);

        try {
            Sequence sequence = MidiSystem.getSequence(new File(path.toString()));
            return parseSequence(sequence);
        } catch (InvalidMidiDataException e) {
            throw new MidiParseException("Invalid MIDI file format: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isValidMidiFile(Path path) {
        try {
            MidiSystem.getSequence(new File(path.toString()));
            return true;
        } catch (Exception e) {
            log.debug("File is not a valid MIDI: {} - {}", path, e.getMessage());
            return false;
        }
    }

    private MidiFileInfo parseSequence(Sequence sequence) throws MidiParseException {
        // Validate division type
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new MidiParseException("Only PPQ (pulses per quarter note) timing is supported");
        }

        int formatType = determineFormatType(sequence);
        int trackCount = sequence.getTracks().length;
        int resolution = sequence.getResolution();

        // Parse tracks and collect events
        List<MidiTrackInfo> trackInfos = new ArrayList<>();
        List<MidiNoteEvent> allNoteEvents = new ArrayList<>();

        float initialTempo = DEFAULT_TEMPO_BPM;
        int tempoChangeCount = 0;
        String timeSignature = "4/4";

        // First pass: find initial tempo and time signature
        for (int trackNum = 0; trackNum < trackCount; trackNum++) {
            Track track = sequence.getTracks()[trackNum];

            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                if (message instanceof MetaMessage meta) {
                    if (meta.getType() == META_TEMPO && event.getTick() == 0) {
                        initialTempo = extractTempo(meta);
                    } else if (meta.getType() == META_TEMPO) {
                        tempoChangeCount++;
                    } else if (meta.getType() == META_TIME_SIGNATURE && event.getTick() == 0) {
                        timeSignature = extractTimeSignature(meta);
                    }
                }
            }
        }

        // Second pass: parse tracks and note events
        // Build tempo map for time calculation
        List<TempoChange> tempoMap = buildTempoMap(sequence, resolution);

        for (int trackNum = 0; trackNum < trackCount; trackNum++) {
            Track track = sequence.getTracks()[trackNum];
            MidiTrackInfo trackInfo = parseTrack(track, trackNum, sequence.getResolution(), tempoMap, allNoteEvents);
            trackInfos.add(trackInfo);
        }

        // Sort note events by time
        allNoteEvents.sort((a, b) -> {
            int timeCompare = Long.compare(a.tickPosition(), b.tickPosition());
            if (timeCompare != 0) return timeCompare;
            // Note offs before note ons at same time
            return Boolean.compare(a.isNoteOn(), b.isNoteOn());
        });

        // Calculate duration
        long durationMs = ticksToMs(sequence.getTickLength(), tempoMap, resolution);

        log.info("Parsed MIDI: format={}, tracks={}, duration={}ms, tempo={} BPM, notes={}",
                formatType, trackCount, durationMs, initialTempo, allNoteEvents.size());

        return new MidiFileInfo(
                formatType,
                trackCount,
                resolution,
                durationMs,
                initialTempo,
                tempoChangeCount,
                timeSignature,
                trackInfos,
                allNoteEvents
        );
    }

    private int determineFormatType(Sequence sequence) {
        int trackCount = sequence.getTracks().length;
        if (trackCount == 1) {
            return 0; // Single track
        }
        // Check if first track has only meta events (format 1)
        Track firstTrack = sequence.getTracks()[0];
        boolean hasNotes = false;
        for (int i = 0; i < firstTrack.size(); i++) {
            MidiMessage msg = firstTrack.get(i).getMessage();
            if (msg instanceof ShortMessage sm) {
                int cmd = sm.getCommand();
                if (cmd == NOTE_ON || cmd == NOTE_OFF) {
                    hasNotes = true;
                    break;
                }
            }
        }
        return hasNotes ? 0 : 1; // Format 1 if first track is tempo/meta only
    }

    private MidiTrackInfo parseTrack(Track track, int trackNumber, int resolution,
                                     List<TempoChange> tempoMap, List<MidiNoteEvent> noteEvents) {
        String trackName = "Track " + trackNumber;
        int noteCount = 0;
        int eventCount = track.size();

        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            MidiMessage message = event.getMessage();
            long tick = event.getTick();

            if (message instanceof MetaMessage meta) {
                if (meta.getType() == META_TRACK_NAME) {
                    trackName = new String(meta.getData()).trim();
                }
            } else if (message instanceof ShortMessage sm) {
                int command = sm.getCommand();
                int channel = sm.getChannel();
                int data1 = sm.getData1(); // Note number
                int data2 = sm.getData2(); // Velocity

                if (command == NOTE_ON || command == NOTE_OFF) {
                    boolean isNoteOn = (command == NOTE_ON && data2 > 0);
                    long timeMs = ticksToMs(tick, tempoMap, resolution);

                    MidiNoteEvent noteEvent = new MidiNoteEvent(
                            tick,
                            timeMs,
                            channel,
                            data1,
                            data2,
                            isNoteOn,
                            trackNumber
                    );
                    noteEvents.add(noteEvent);

                    if (isNoteOn) {
                        noteCount++;
                    }
                }
            }
        }

        return new MidiTrackInfo(trackNumber, trackName, noteCount, eventCount);
    }

    private List<TempoChange> buildTempoMap(Sequence sequence, int resolution) {
        List<TempoChange> tempoChanges = new ArrayList<>();
        tempoChanges.add(new TempoChange(0, DEFAULT_TEMPO_BPM, 0));

        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof MetaMessage meta) {
                    if (meta.getType() == META_TEMPO) {
                        float bpm = extractTempo(meta);
                        long tick = event.getTick();

                        // Calculate time of this tempo change
                        long timeMs = ticksToMs(tick, tempoChanges, resolution);
                        tempoChanges.add(new TempoChange(tick, bpm, timeMs));
                    }
                }
            }
        }

        // Sort by tick position
        tempoChanges.sort((a, b) -> Long.compare(a.tick, b.tick));

        return tempoChanges;
    }

    private long ticksToMs(long tick, List<TempoChange> tempoMap, int resolution) {
        if (tick == 0) return 0;

        long totalMs = 0;
        long currentTick = 0;
        float currentBpm = DEFAULT_TEMPO_BPM;
        long lastTempoChangeMs = 0;

        for (TempoChange change : tempoMap) {
            if (change.tick >= tick) {
                break;
            }

            // Calculate time from last position to this tempo change
            long tickDelta = change.tick - currentTick;
            totalMs += ticksToMsAtTempo(tickDelta, currentBpm, resolution);

            currentTick = change.tick;
            currentBpm = change.bpm;
            lastTempoChangeMs = totalMs;
        }

        // Calculate remaining time to target tick
        long remainingTicks = tick - currentTick;
        totalMs += ticksToMsAtTempo(remainingTicks, currentBpm, resolution);

        return totalMs;
    }

    private long ticksToMsAtTempo(long ticks, float bpm, int resolution) {
        if (ticks == 0 || bpm == 0) return 0;
        double msPerTick = (60_000.0 / bpm) / resolution;
        return Math.round(ticks * msPerTick);
    }

    private float extractTempo(MetaMessage meta) {
        byte[] data = meta.getData();
        if (data.length >= 3) {
            int microsecondsPerBeat = ((data[0] & 0xFF) << 16) |
                    ((data[1] & 0xFF) << 8) |
                    (data[2] & 0xFF);
            return MICROSECONDS_PER_MINUTE / (float) microsecondsPerBeat;
        }
        return DEFAULT_TEMPO_BPM;
    }

    private String extractTimeSignature(MetaMessage meta) {
        byte[] data = meta.getData();
        if (data.length >= 2) {
            int numerator = data[0] & 0xFF;
            int denominator = (int) Math.pow(2, data[1] & 0xFF);
            return numerator + "/" + denominator;
        }
        return "4/4";
    }

    private record TempoChange(long tick, float bpm, long timeMs) {
    }
}

