package com.burstmeman.midi2keys.infrastructure.persistence.json;

import com.burstmeman.midi2keys.domain.entities.NoteMapping;
import com.burstmeman.midi2keys.domain.entities.PlaybackOptions;
import com.burstmeman.midi2keys.domain.entities.PlaybackOptions.Quantization;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.domain.valueobjects.KeyCombination;
import com.burstmeman.midi2keys.domain.valueobjects.MidiNote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serializes and deserializes Profile objects to/from JSON.
 */
@Slf4j
public class ProfileJsonSerializer {

    private static final int CURRENT_VERSION = 1;

    private final ObjectMapper objectMapper;

    public ProfileJsonSerializer() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Serializes a profile to JSON string.
     *
     * @param profile The profile to serialize
     * @return JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public String serialize(Profile profile) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();

        // Metadata
        root.put("version", CURRENT_VERSION);
        root.put("id", profile.getId());
        root.put("name", profile.getName());
        root.put("description", profile.getDescription());
        root.put("isDefault", profile.isDefault());
        root.put("createdAt", profile.getCreatedAt().toString());
        root.put("updatedAt", profile.getUpdatedAt().toString());

        // Playback options
        ObjectNode optionsNode = serializePlaybackOptions(profile.getPlaybackOptions());
        root.set("playbackOptions", optionsNode);

        // Note mappings
        ArrayNode mappingsNode = serializeNoteMappings(profile.getNoteMappings());
        root.set("noteMappings", mappingsNode);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Deserializes a profile from JSON string.
     *
     * @param json JSON string
     * @return Profile instance
     * @throws JsonProcessingException if deserialization fails
     */
    public Profile deserialize(String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);

        // Basic properties
        String id = root.get("id").asText();
        String name = root.get("name").asText();
        String description = root.has("description") ? root.get("description").asText() : "";
        boolean isDefault = root.has("isDefault") && root.get("isDefault").asBoolean();

        LocalDateTime createdAt = parseDateTime(root, "createdAt");
        LocalDateTime updatedAt = parseDateTime(root, "updatedAt");

        // Playback options
        PlaybackOptions options = deserializePlaybackOptions(root.get("playbackOptions"));

        // Note mappings
        List<NoteMapping> mappings = deserializeNoteMappings(root.get("noteMappings"));

        return new Profile(id, name, description, mappings, options, createdAt, updatedAt, isDefault);
    }

    private ObjectNode serializePlaybackOptions(PlaybackOptions options) {
        ObjectNode node = objectMapper.createObjectNode();

        node.put("tempoMultiplier", options.getTempoMultiplier());
        node.put("quantization", options.getQuantization().name());
        node.put("minVelocityThreshold", options.getMinVelocityThreshold());
        node.put("transpose", options.getTranspose());
        node.put("keyPressDurationMs", options.getKeyPressDurationMs());

        ArrayNode ignoredChannels = objectMapper.createArrayNode();
        for (int channel : options.getIgnoredChannels()) {
            ignoredChannels.add(channel);
        }
        node.set("ignoredChannels", ignoredChannels);

        return node;
    }

    private PlaybackOptions deserializePlaybackOptions(JsonNode node) {
        if (node == null || node.isNull()) {
            return new PlaybackOptions();
        }

        double tempoMultiplier = node.has("tempoMultiplier") ?
                node.get("tempoMultiplier").asDouble(1.0) : 1.0;

        Quantization quantization = Quantization.NONE;
        if (node.has("quantization")) {
            try {
                quantization = Quantization.valueOf(node.get("quantization").asText());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown quantization value, using NONE");
            }
        }

        int minVelocity = node.has("minVelocityThreshold") ?
                node.get("minVelocityThreshold").asInt(1) : 1;

        int transpose = node.has("transpose") ?
                node.get("transpose").asInt(0) : 0;

        int keyPressDuration = node.has("keyPressDurationMs") ?
                node.get("keyPressDurationMs").asInt(50) : 50;

        Set<Integer> ignoredChannels = new HashSet<>();
        if (node.has("ignoredChannels") && node.get("ignoredChannels").isArray()) {
            for (JsonNode ch : node.get("ignoredChannels")) {
                ignoredChannels.add(ch.asInt());
            }
        }

        return new PlaybackOptions(tempoMultiplier, quantization, minVelocity,
                ignoredChannels, transpose, keyPressDuration);
    }

    private ArrayNode serializeNoteMappings(List<NoteMapping> mappings) {
        ArrayNode array = objectMapper.createArrayNode();

        for (NoteMapping mapping : mappings) {
            ObjectNode node = objectMapper.createObjectNode();

            node.put("noteNumber", mapping.midiNote().noteNumber());
            node.put("noteName", mapping.midiNote().getNoteName());
            node.put("keyCombination", mapping.keyCombination().getDisplayString());
            node.put("channel", mapping.channel());
            node.put("minVelocity", mapping.minVelocity());
            node.put("maxVelocity", mapping.maxVelocity());

            array.add(node);
        }

        return array;
    }

    private List<NoteMapping> deserializeNoteMappings(JsonNode node) {
        List<NoteMapping> mappings = new ArrayList<>();

        if (node == null || !node.isArray()) {
            return mappings;
        }

        for (JsonNode mappingNode : node) {
            try {
                int noteNumber = mappingNode.get("noteNumber").asInt();
                String keyCombStr = mappingNode.get("keyCombination").asText();
                int channel = mappingNode.has("channel") ?
                        mappingNode.get("channel").asInt(-1) : -1;
                int minVelocity = mappingNode.has("minVelocity") ?
                        mappingNode.get("minVelocity").asInt(1) : 1;
                int maxVelocity = mappingNode.has("maxVelocity") ?
                        mappingNode.get("maxVelocity").asInt(127) : 127;

                MidiNote midiNote = new MidiNote(noteNumber);
                KeyCombination keyCombination = KeyCombination.parse(keyCombStr);

                NoteMapping mapping = new NoteMapping(midiNote, keyCombination,
                        channel, minVelocity, maxVelocity);
                mappings.add(mapping);

            } catch (Exception e) {
                log.warn("Failed to deserialize mapping: {}", e.getMessage());
            }
        }

        return mappings;
    }

    private LocalDateTime parseDateTime(JsonNode root, String field) {
        if (root.has(field) && !root.get(field).isNull()) {
            try {
                return LocalDateTime.parse(root.get(field).asText());
            } catch (Exception e) {
                log.warn("Failed to parse datetime field {}: {}", field, e.getMessage());
            }
        }
        return LocalDateTime.now();
    }
}

