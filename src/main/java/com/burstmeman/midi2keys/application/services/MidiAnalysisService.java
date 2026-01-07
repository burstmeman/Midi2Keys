package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.MidiAnalysis;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser.MidiFileInfo;
import com.burstmeman.midi2keys.infrastructure.adapters.midi.MidiParser.MidiNoteEvent;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import com.burstmeman.midi2keys.infrastructure.persistence.database.dao.MidiAnalysisDao;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for analyzing MIDI files and caching results.
 */
@Slf4j
public class MidiAnalysisService {

    private final MidiAnalysisDao midiAnalysisDao;
    private final MidiParser midiParser;

    public MidiAnalysisService(MidiAnalysisDao midiAnalysisDao, MidiParser midiParser) {
        this.midiAnalysisDao = midiAnalysisDao;
        this.midiParser = midiParser;
    }

    /**
     * Gets analysis for a MIDI file, analyzing if not cached.
     *
     * @param midiFile      The MIDI file to analyze
     * @param rootDirectory The root directory containing the file
     * @return MidiAnalysis with computed statistics
     */
    public MidiAnalysis getOrAnalyze(MidiFile midiFile, RootDirectory rootDirectory) {
        // Check cache first
        Optional<MidiAnalysis> cached = midiAnalysisDao.findByMidiFileId(midiFile.getId());
        if (cached.isPresent()) {
            log.debug("Using cached analysis for: {}", midiFile.getFileName());
            return cached.get();
        }

        // Analyze the file
        return analyze(midiFile, rootDirectory);
    }

    /**
     * Forces re-analysis of a MIDI file.
     *
     * @param midiFile      The MIDI file to analyze
     * @param rootDirectory The root directory containing the file
     * @return Fresh MidiAnalysis
     */
    public MidiAnalysis analyze(MidiFile midiFile, RootDirectory rootDirectory) {
        Path filePath = midiFile.getAbsolutePath(rootDirectory.getPathAsPath());

        log.info("Analyzing MIDI file: {}", filePath);

        try {
            MidiFileInfo info = midiParser.parse(filePath);
            MidiAnalysis analysis = buildAnalysis(midiFile.getId(), info);

            // Save to cache
            MidiAnalysis saved = midiAnalysisDao.save(analysis);

            log.info("Analysis complete: {} notes, {} tracks, {}ms",
                    analysis.getTotalNotes(), analysis.getTrackCount(), analysis.getDurationMs());

            return saved;

        } catch (MidiParser.MidiParseException e) {
            throw new ApplicationException(ErrorCode.MIDI_PARSE_ERROR,
                    "Failed to parse MIDI file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.MIDI_PARSE_ERROR,
                    "Error analyzing MIDI file: " + e.getMessage(), e);
        }
    }

    /**
     * Gets cached analysis if available.
     *
     * @param midiFileId MIDI file ID
     * @return Optional containing analysis if cached
     */
    public Optional<MidiAnalysis> getCached(Long midiFileId) {
        return midiAnalysisDao.findByMidiFileId(midiFileId);
    }

    /**
     * Checks if analysis is cached for a file.
     *
     * @param midiFileId MIDI file ID
     * @return true if cached
     */
    public boolean isCached(Long midiFileId) {
        return midiAnalysisDao.existsByMidiFileId(midiFileId);
    }

    /**
     * Deletes cached analysis for a file.
     *
     * @param midiFileId MIDI file ID
     * @return true if deleted
     */
    public boolean deleteCache(Long midiFileId) {
        return midiAnalysisDao.deleteByMidiFileId(midiFileId);
    }

    /**
     * Validates if a file is a valid MIDI file.
     *
     * @param filePath Path to check
     * @return true if valid MIDI
     */
    public boolean isValidMidiFile(Path filePath) {
        return midiParser.isValidMidiFile(filePath);
    }

    private MidiAnalysis buildAnalysis(Long midiFileId, MidiFileInfo info) {
        MidiAnalysis analysis = new MidiAnalysis(midiFileId);

        // Basic info
        analysis.setFormatType(info.formatType());
        analysis.setTrackCount(info.trackCount());
        analysis.setDurationMs(info.durationMs());
        analysis.setTempoBpm(info.initialTempoBpm());
        analysis.setTempoChangesCount(info.tempoChangeCount());
        analysis.setTimeSignature(info.timeSignature());

        // Process note events
        List<MidiNoteEvent> noteEvents = info.noteEvents();

        int totalNotes = 0;
        Map<Integer, Integer> channelCounts = new HashMap<>();
        Map<Integer, Integer> trackCounts = new HashMap<>();
        Map<Integer, Integer> noteHistogram = new HashMap<>();

        int minVelocity = 127;
        int maxVelocity = 0;
        long velocitySum = 0;
        int velocityCount = 0;

        for (MidiNoteEvent event : noteEvents) {
            if (event.isNoteOn() && event.velocity() > 0) {
                totalNotes++;

                // Channel counts
                channelCounts.merge(event.channel(), 1, Integer::sum);

                // Track counts
                trackCounts.merge(event.trackNumber(), 1, Integer::sum);

                // Note histogram
                noteHistogram.merge(event.noteNumber(), 1, Integer::sum);

                // Velocity stats
                int vel = event.velocity();
                minVelocity = Math.min(minVelocity, vel);
                maxVelocity = Math.max(maxVelocity, vel);
                velocitySum += vel;
                velocityCount++;
            }
        }

        analysis.setTotalNotes(totalNotes);
        analysis.setChannelNoteCounts(channelCounts);
        analysis.setTrackNoteCounts(trackCounts);
        analysis.setNoteHistogram(noteHistogram);

        // Velocity statistics
        if (velocityCount > 0) {
            analysis.setMinVelocity(minVelocity);
            analysis.setMaxVelocity(maxVelocity);
            analysis.setAvgVelocity((float) velocitySum / velocityCount);
        } else {
            analysis.setMinVelocity(0);
            analysis.setMaxVelocity(0);
            analysis.setAvgVelocity(0);
        }

        // Estimated melody length (time from first to last note)
        long melodyLength = estimateMelodyLength(noteEvents);
        analysis.setEstimatedMelodyLengthMs(melodyLength);

        return analysis;
    }

    private long estimateMelodyLength(List<MidiNoteEvent> events) {
        if (events.isEmpty()) return 0;

        long firstNoteTime = Long.MAX_VALUE;
        long lastNoteTime = 0;

        for (MidiNoteEvent event : events) {
            if (event.isNoteOn() && event.velocity() > 0) {
                firstNoteTime = Math.min(firstNoteTime, event.timeMs());
                lastNoteTime = Math.max(lastNoteTime, event.timeMs());
            }
        }

        if (firstNoteTime == Long.MAX_VALUE) return 0;
        return lastNoteTime - firstNoteTime;
    }
}

