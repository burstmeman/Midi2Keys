package com.burstmeman.midi2keys.application.usecases;

import com.burstmeman.midi2keys.application.services.MidiAnalysisService;
import com.burstmeman.midi2keys.application.services.MidiFileService;
import com.burstmeman.midi2keys.application.services.RootDirectoryService;
import com.burstmeman.midi2keys.domain.entities.MidiAnalysis;
import com.burstmeman.midi2keys.domain.entities.MidiFile;
import com.burstmeman.midi2keys.domain.entities.RootDirectory;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Use case for analyzing MIDI files and retrieving analysis data.
 */
public class AnalyzeMidiFileUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMidiFileUseCase.class);
    
    private final MidiAnalysisService midiAnalysisService;
    private final MidiFileService midiFileService;
    private final RootDirectoryService rootDirectoryService;
    
    public AnalyzeMidiFileUseCase(MidiAnalysisService midiAnalysisService,
                                  MidiFileService midiFileService,
                                  RootDirectoryService rootDirectoryService) {
        this.midiAnalysisService = midiAnalysisService;
        this.midiFileService = midiFileService;
        this.rootDirectoryService = rootDirectoryService;
    }
    
    /**
     * Gets or creates analysis for a MIDI file.
     * Uses cached analysis if available.
     * 
     * @param midiFileId MIDI file ID
     * @return Analysis result with file and analysis data
     */
    public AnalysisResult getAnalysis(Long midiFileId) {
        MidiFile midiFile = midiFileService.findById(midiFileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FILE_NOT_FOUND,
                        "MIDI file not found: " + midiFileId));
        
        RootDirectory rootDir = rootDirectoryService.findById(midiFile.getRootDirectoryId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found for file"));
        
        MidiAnalysis analysis = midiAnalysisService.getOrAnalyze(midiFile, rootDir);
        
        return new AnalysisResult(midiFile, rootDir, analysis);
    }
    
    /**
     * Forces re-analysis of a MIDI file.
     * 
     * @param midiFileId MIDI file ID
     * @return Fresh analysis result
     */
    public AnalysisResult reanalyze(Long midiFileId) {
        MidiFile midiFile = midiFileService.findById(midiFileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FILE_NOT_FOUND,
                        "MIDI file not found: " + midiFileId));
        
        RootDirectory rootDir = rootDirectoryService.findById(midiFile.getRootDirectoryId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROOT_DIRECTORY_INVALID,
                        "Root directory not found for file"));
        
        // Delete cached analysis
        midiAnalysisService.deleteCache(midiFileId);
        
        // Perform fresh analysis
        MidiAnalysis analysis = midiAnalysisService.analyze(midiFile, rootDir);
        
        return new AnalysisResult(midiFile, rootDir, analysis);
    }
    
    /**
     * Gets cached analysis without triggering new analysis.
     * 
     * @param midiFileId MIDI file ID
     * @return Optional containing cached analysis
     */
    public Optional<MidiAnalysis> getCachedAnalysis(Long midiFileId) {
        return midiAnalysisService.getCached(midiFileId);
    }
    
    /**
     * Checks if analysis is available (cached).
     * 
     * @param midiFileId MIDI file ID
     * @return true if cached analysis exists
     */
    public boolean isAnalysisAvailable(Long midiFileId) {
        return midiAnalysisService.isCached(midiFileId);
    }
    
    /**
     * Result containing file info and analysis.
     */
    public record AnalysisResult(
            MidiFile midiFile,
            RootDirectory rootDirectory,
            MidiAnalysis analysis
    ) {
        /**
         * Gets the full file path.
         */
        public String getFullPath() {
            return midiFile.getAbsolutePath(rootDirectory.getPathAsPath()).toString();
        }
    }
}

