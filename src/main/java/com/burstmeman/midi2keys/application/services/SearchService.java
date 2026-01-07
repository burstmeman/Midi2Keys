package com.burstmeman.midi2keys.application.services;

import com.burstmeman.midi2keys.domain.entities.MidiFile;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Application service for searching and filtering MIDI files.
 */
@Slf4j
public class SearchService {

    private final ExecutorService searchExecutor;

    public SearchService() {
        this.searchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "search-service");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Filter MIDI files by filename using a search query.
     */
    public List<MidiFile> filterByFilename(List<MidiFile> files, String query) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(files);
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

        if (normalizedQuery.contains("*") || normalizedQuery.contains("?")) {
            return filterWithWildcard(files, normalizedQuery);
        }

        return files.stream()
                .filter(file -> file.getFileName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .collect(Collectors.toList());
    }

    private List<MidiFile> filterWithWildcard(List<MidiFile> files, String query) {
        try {
            String regex = wildcardToRegex(query);
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            return files.stream()
                    .filter(file -> pattern.matcher(file.getFileName()).matches())
                    .collect(Collectors.toList());
        } catch (PatternSyntaxException e) {
            log.warn("Invalid search pattern: {}", query, e);
            return Collections.emptyList();
        }
    }

    private String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        regex.append(".*");

        for (char c : wildcard.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '.', '(', ')', '[', ']', '{', '}', '\\', '^', '$', '|', '+' -> regex.append("\\").append(c);
                default -> regex.append(c);
            }
        }

        regex.append(".*");
        return regex.toString();
    }

    /**
     * Filter files by extension.
     */
    public List<MidiFile> filterByExtension(List<MidiFile> files, String extension) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        if (extension == null || extension.trim().isEmpty()) {
            return new ArrayList<>(files);
        }

        String normalizedExt = extension.startsWith(".")
                ? extension.toLowerCase(Locale.ROOT)
                : "." + extension.toLowerCase(Locale.ROOT);

        return files.stream()
                .filter(file -> file.getFileName().toLowerCase(Locale.ROOT).endsWith(normalizedExt))
                .collect(Collectors.toList());
    }

    /**
     * Asynchronously filter files by filename.
     */
    public CompletableFuture<List<MidiFile>> filterByFilenameAsync(List<MidiFile> files, String query) {
        return CompletableFuture.supplyAsync(() -> filterByFilename(files, query), searchExecutor);
    }

    /**
     * Combined search with multiple criteria.
     */
    public List<MidiFile> search(List<MidiFile> files, SearchCriteria criteria) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<MidiFile> results = new ArrayList<>(files);

        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            results = filterByFilename(results, criteria.getQuery());
        }

        if (criteria.getExtension() != null && !criteria.getExtension().trim().isEmpty()) {
            results = filterByExtension(results, criteria.getExtension());
        }

        log.debug("Search returned {} results for criteria: {}", results.size(), criteria);
        return results;
    }

    /**
     * Shutdown the search executor service.
     */
    public void shutdown() {
        searchExecutor.shutdown();
    }

    /**
     * Search criteria container.
     */
    @Getter
    @Setter
    @ToString
    public static class SearchCriteria {
        private String query;
        private String extension;

        public SearchCriteria setQuery(String query) {
            this.query = query;
            return this;
        }

        public SearchCriteria setExtension(String extension) {
            this.extension = extension;
            return this;
        }
    }
}
