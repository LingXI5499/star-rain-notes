package com.starrainnotes.english.shared.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exercise question-type registry and config validation (方案 §6.3, §7.5, §9.2).
 *
 * <p>Every {@code english_exercise.config_json} must match the schema of its
 * {@code question_type} before it is persisted; this validates structure, not
 * just JSON syntax. A failure surfaces as 422
 * {@code ENGLISH_EXERCISE_CONFIG_INVALID} so the admin UI can show field-level
 * problems instead of a generic "operation failed".</p>
 */
@Service
public class EnglishExerciseService {

    public static final Set<String> MODULE_TYPES = Set.of("READING", "LISTENING", "WRITING");

    public static final Map<String, List<String>> MODULE_QUESTION_TYPES = Map.of(
            "READING", List.of("SINGLE_CHOICE", "TRUE_FALSE", "SENTENCE_MATCH", "PARAGRAPH_MATCH",
                    "ORDERING", "REFERENCE", "CAUSE_EFFECT", "MAIN_IDEA", "INFERENCE", "STRUCTURE_FILL"),
            "LISTENING", List.of("PHONEME_WORD", "MINIMAL_PAIR", "LINKING_FILL", "WEAK_FORM_FILL",
                    "INFO_FILL", "INFO_CHOICE", "NUMBER_FILL", "TIME_FILL", "LOCATION_FILL", "TRUE_FALSE",
                    "SEGMENT_ORDERING", "MAIN_IDEA", "SPEAKER_ATTITUDE", "LOGIC_JUDGE", "DICTATION"),
            "WRITING", List.of("SENTENCE_REWRITE", "SENTENCE_COMBINE", "COHESION_FILL", "STYLE_ANALYSIS"));

    private static final Map<String, String> QUESTION_KIND = Map.ofEntries(
            // reading
            Map.entry("SINGLE_CHOICE", "CHOICE"),
            Map.entry("TRUE_FALSE", "TRUE_FALSE"),
            Map.entry("SENTENCE_MATCH", "MATCH"),
            Map.entry("PARAGRAPH_MATCH", "MATCH"),
            Map.entry("ORDERING", "ORDER"),
            Map.entry("REFERENCE", "CHOICE"),
            Map.entry("CAUSE_EFFECT", "CHOICE"),
            Map.entry("MAIN_IDEA", "CHOICE"),
            Map.entry("INFERENCE", "CHOICE"),
            Map.entry("STRUCTURE_FILL", "STRUCTURE"),
            // listening
            Map.entry("PHONEME_WORD", "CHOICE"),
            Map.entry("MINIMAL_PAIR", "MINIMAL_PAIR"),
            Map.entry("LINKING_FILL", "FILL"),
            Map.entry("WEAK_FORM_FILL", "FILL"),
            Map.entry("INFO_FILL", "FILL"),
            Map.entry("INFO_CHOICE", "CHOICE"),
            Map.entry("NUMBER_FILL", "FILL"),
            Map.entry("TIME_FILL", "FILL"),
            Map.entry("LOCATION_FILL", "FILL"),
            Map.entry("SEGMENT_ORDERING", "ORDER"),
            Map.entry("SPEAKER_ATTITUDE", "CHOICE"),
            Map.entry("LOGIC_JUDGE", "CHOICE"),
            Map.entry("DICTATION", "FILL"),
            // writing
            Map.entry("SENTENCE_REWRITE", "FILL"),
            Map.entry("SENTENCE_COMBINE", "FILL"),
            Map.entry("COHESION_FILL", "FILL"),
            Map.entry("STYLE_ANALYSIS", "FILL"));

    private final ObjectMapper objectMapper;

    public EnglishExerciseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validate a raw config JSON string against the module + question type.
     * Throws 422 {@code ENGLISH_EXERCISE_CONFIG_INVALID} on any violation.
     */
    public void validateConfig(String moduleType, String questionType, String configJson) {
        if (moduleType == null || !MODULE_TYPES.contains(moduleType)) {
            throw invalid("Unsupported module type: " + moduleType);
        }
        if (!MODULE_QUESTION_TYPES.getOrDefault(moduleType, List.of()).contains(questionType)) {
            throw invalid("Question type " + questionType + " is not valid for module " + moduleType + ".");
        }
        String kind = QUESTION_KIND.get(questionType);
        if (kind == null) {
            throw invalid("Unsupported question type: " + questionType);
        }
        JsonNode config;
        try {
            config = objectMapper.readTree(configJson == null ? "" : configJson);
        } catch (Exception ex) {
            throw invalid("config_json is not valid JSON.");
        }
        if (config == null || !config.isObject()) {
            throw invalid("config_json must be a JSON object.");
        }
        List<String> problems = validateKind(kind, config);
        if (!problems.isEmpty()) {
            throw invalid(String.join("; ", problems));
        }
    }

    public Map<String, List<String>> questionTypes() {
        return MODULE_QUESTION_TYPES;
    }

    /** The structural kind for a question type, or null when unknown. */
    public String kindOf(String questionType) {
        return QUESTION_KIND.get(questionType);
    }

    private List<String> validateKind(String kind, JsonNode config) {
        List<String> problems = new ArrayList<>();
        switch (kind) {
            case "CHOICE" -> validateChoice(config, problems);
            case "TRUE_FALSE" -> {
                if (!config.has("answer") || !config.get("answer").isBoolean()) {
                    problems.add("answer must be a boolean.");
                }
            }
            case "MATCH" -> validateMatch(config, problems);
            case "ORDER" -> {
                JsonNode items = config.get("items");
                if (items == null || !items.isArray() || items.isEmpty()) {
                    problems.add("items must be a non-empty array.");
                }
            }
            case "FILL" -> {
                JsonNode answer = config.get("answer");
                JsonNode answers = config.get("answers");
                boolean hasAnswer = (answer != null && answer.isTextual() && !answer.asText().isBlank());
                boolean hasAnswers = (answers != null && answers.isArray() && !answers.isEmpty());
                if (!hasAnswer && !hasAnswers) {
                    problems.add("answer (or answers) must be a non-empty string/array.");
                }
            }
            case "STRUCTURE" -> {
                JsonNode structure = config.get("structure");
                if (structure == null || !structure.isArray() || structure.isEmpty()) {
                    problems.add("structure must be a non-empty array.");
                } else {
                    for (JsonNode node : structure) {
                        if (!node.has("label") || !node.has("answer")) {
                            problems.add("each structure node needs label and answer.");
                            break;
                        }
                    }
                }
            }
            case "MINIMAL_PAIR" -> {
                JsonNode pair = config.get("pair");
                if (pair == null || !pair.isArray() || pair.size() != 2
                        || !pair.get(0).isTextual() || !pair.get(1).isTextual()
                        || pair.get(0).asText().isBlank() || pair.get(1).asText().isBlank()) {
                    problems.add("pair must be an array of exactly two non-empty strings.");
                } else {
                    JsonNode answer = config.get("answer");
                    if (answer == null || !answer.isTextual()
                            || (!pair.get(0).asText().equals(answer.asText())
                                && !pair.get(1).asText().equals(answer.asText()))) {
                        problems.add("answer must be one of the two pair strings.");
                    }
                }
            }
            default -> problems.add("Unknown question type kind.");
        }
        return problems;
    }

    private void validateChoice(JsonNode config, List<String> problems) {
        JsonNode options = config.get("options");
        if (options == null || !options.isArray() || options.isEmpty()) {
            problems.add("options must be a non-empty array.");
            return;
        }
        List<String> keys = new ArrayList<>();
        for (JsonNode option : options) {
            if (!option.isObject() || !option.has("key") || !option.has("text")
                    || option.get("key").asText().isBlank()) {
                problems.add("each option needs a non-blank key and text.");
                return;
            }
            keys.add(option.get("key").asText());
        }
        JsonNode answer = config.get("answer");
        if (answer == null || !answer.isTextual() || !keys.contains(answer.asText())) {
            problems.add("answer must be one of the option keys.");
        }
    }

    private void validateMatch(JsonNode config, List<String> problems) {
        JsonNode pairs = config.get("pairs");
        if (pairs == null || !pairs.isArray() || pairs.isEmpty()) {
            problems.add("pairs must be a non-empty array.");
            return;
        }
        for (JsonNode pair : pairs) {
            if (!pair.isArray() || pair.size() != 2) {
                problems.add("each pair must be an array of two strings.");
                return;
            }
        }
    }

    private ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_EXERCISE_CONFIG_INVALID",
                "Invalid exercise config", detail);
    }
}
