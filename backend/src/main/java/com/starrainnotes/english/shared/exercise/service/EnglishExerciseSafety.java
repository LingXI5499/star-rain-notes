package com.starrainnotes.english.shared.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared exercise safety primitives (阶段三 §七). Reading and listening both
 * use these so the answer-leak protections and server-side scoring behave
 * identically and are tested once.
 *
 * <p>(1) {@link #sanitize} recursively strips any key that could expose the
 * answer, then re-shapes ORDERING (rotated display order) and MATCH (leftItems +
 * shuffled rightItems) so the public payload never equals the stored answer.
 * (2) {@link #isCorrect} reassembles the user's answer and compares it to the
 * stored answer server-side.</p>
 */
public final class EnglishExerciseSafety {

    private static final Set<String> ANSWER_KEYS = Set.of(
            "answer", "answers", "correct", "iscorrect", "correctindexes",
            "correctorder", "standardorder", "answerkeys", "solution");

    private EnglishExerciseSafety() { }

    /** Public, safe config for a client. Never returns the stored answer. */
    public static Map<String, Object> sanitize(ObjectMapper mapper, String questionType,
                                               JsonNode config, long seed) {
        ObjectNode safe = config.isObject()
                ? (ObjectNode) sanitizeNode(config)
                : mapper.createObjectNode();
        String kind = kindOf(questionType);
        if ("ORDER".equals(kind) && safe.has("items") && safe.get("items").isArray()) {
            safe.set("items", rotated((ArrayNode) safe.get("items"), seed));
        }
        if ("MATCH".equals(kind) && config.has("pairs") && config.get("pairs").isArray()) {
            ArrayNode left = mapper.createArrayNode();
            ArrayNode right = mapper.createArrayNode();
            for (JsonNode pair : config.get("pairs")) {
                if (pair.isArray() && pair.size() == 2) {
                    left.add(pair.get(0));
                    right.add(pair.get(1));
                }
            }
            safe.remove("pairs");
            safe.set("leftItems", left);
            safe.set("rightItems", rotated(right, seed));
        }
        return mapper.convertValue(safe, new com.fasterxml.jackson.core.type.TypeReference<>() { });
    }

    /** Server-side scoring against the stored answer for a submitted answer. */
    public static boolean isCorrect(ObjectMapper mapper, String questionType,
                                    JsonNode config, Object submitted) {
        String kind = kindOf(questionType);
        JsonNode answer = config.get("answer");
        JsonNode answers = config.get("answers");
        switch (kind == null ? "" : kind) {
            case "CHOICE" -> {
                return answer != null && answer.isTextual()
                        && answer.asText().equals(String.valueOf(submitted));
            }
            case "TRUE_FALSE" -> {
                return answer != null && answer.isBoolean()
                        && answer.asBoolean() == Boolean.TRUE.equals(submitted);
            }
            case "FILL" -> {
                return matchesFill(answer, answers, submitted);
            }
            case "ORDER" -> {
                JsonNode correct = answer != null ? answer : config.get("items");
                return correct != null && correct.equals(mapper.valueToTree(submitted));
            }
            case "MATCH" -> {
                JsonNode correct = answer != null ? answer : config.get("pairs");
                return correct != null && correct.equals(mapper.valueToTree(submitted));
            }
            case "STRUCTURE", "MINIMAL_PAIR" -> {
                JsonNode correct = answer != null ? answer : expectedStructure(mapper, config);
                return correct != null && correct.equals(mapper.valueToTree(submitted));
            }
            default -> {
                return false;
            }
        }
    }

    /** Map a question type to its structural kind, or null when unknown. */
    private static String kindOf(String questionType) {
        if (questionType == null) return null;
        if (questionType.endsWith("_CHOICE") || "MAIN_IDEA".equals(questionType)
                || "INFERENCE".equals(questionType) || "CAUSE_EFFECT".equals(questionType)
                || "REFERENCE".equals(questionType) || "INFO_CHOICE".equals(questionType)
                || "SPEAKER_ATTITUDE".equals(questionType) || "LOGIC_JUDGE".equals(questionType)) {
            return "CHOICE";
        }
        if ("TRUE_FALSE".equals(questionType)) return "TRUE_FALSE";
        if (questionType.endsWith("_MATCH") || "PARAGRAPH_MATCH".equals(questionType)) return "MATCH";
        if ("ORDERING".equals(questionType)) return "ORDER";
        if ("STRUCTURE_FILL".equals(questionType)) return "STRUCTURE";
        if ("MINIMAL_PAIR".equals(questionType)) return "MINIMAL_PAIR";
        return "FILL";
    }

    private static JsonNode expectedStructure(ObjectMapper mapper, JsonNode config) {
        JsonNode structure = config.get("structure");
        if (structure == null || !structure.isArray()) return config.get("pair");
        ObjectNode expected = mapper.createObjectNode();
        for (JsonNode item : structure) {
            if (item.hasNonNull("label") && item.has("answer")) {
                expected.set(item.get("label").asText(), item.get("answer"));
            }
        }
        return expected;
    }

    private static boolean matchesFill(JsonNode answer, JsonNode answers, Object submitted) {
        if (submitted == null) return false;
        String text = String.valueOf(submitted);
        if (answer != null && answer.isTextual() && answer.asText().equalsIgnoreCase(text)) return true;
        if (answers != null && answers.isArray()) {
            for (JsonNode node : answers) {
                if (node.isTextual() && node.asText().equalsIgnoreCase(text)) return true;
            }
        }
        return false;
    }

    private static boolean isAnswerBearing(String key) {
        return ANSWER_KEYS.contains(key.toLowerCase());
    }

    private static JsonNode sanitizeNode(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                if (!isAnswerBearing(entry.getKey())) {
                    result.set(entry.getKey(), sanitizeNode(entry.getValue()));
                }
            });
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            node.forEach(item -> result.add(sanitizeNode(item)));
            return result;
        }
        return node.deepCopy();
    }

    private static ArrayNode rotated(ArrayNode source, long seed) {
        ArrayNode result = source.deepCopy();
        int size = result.size();
        if (size <= 1) return result;
        int shift = (int) (Math.floorMod(seed, size - 1) + 1);
        List<JsonNode> values = new ArrayList<>();
        result.forEach(values::add);
        result.removeAll();
        for (int i = 0; i < size; i++) result.add(values.get((i + shift) % size));
        return result;
    }
}
