package com.starrainnotes.english.shared.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnglishExerciseSafetyTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EnglishExerciseService exerciseService = new EnglishExerciseService(mapper);
    private final EnglishExerciseSafety safety = new EnglishExerciseSafety(mapper, exerciseService);

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    void sanitizeRecursivelyStripsNestedAnswerFields() {
        Map<String, Object> safe = safety.sanitize("STRUCTURE_FILL",
                parse("{\"structure\":[{\"label\":\"a\",\"answer\":\"x\"}],\"answer\":\"hidden\"}"), 1L);
        assertThat(safe).doesNotContainKey("answer");
        @SuppressWarnings("unchecked")
        List<Object> structure = (List<Object>) safe.get("structure");
        assertThat(structure.get(0)).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> label = (Map<String, Object>) structure.get(0);
        assertThat(label).containsKey("label").doesNotContainKey("answer");
    }

    @Test
    void orderingPublicOrderDiffersFromStandard() {
        JsonNode config = parse("{\"items\":[\"a\",\"b\",\"c\",\"d\"],\"answer\":[\"a\",\"b\",\"c\",\"d\"]}");
        Map<String, Object> safe = safety.sanitize("SEGMENT_ORDERING", config, 1L);
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) safe.get("items");
        assertThat(safe).doesNotContainKey("answer");
        assertThat(items).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(items).isNotEqualTo(List.of("a", "b", "c", "d"));
    }

    @Test
    void matchSplitsIntoLeftAndShuffledRight() {
        Map<String, Object> safe = safety.sanitize("SENTENCE_MATCH",
                parse("{\"pairs\":[[\"l1\",\"r1\"],[\"l2\",\"r2\"],[\"l3\",\"r3\"]],\"answer\":\"l1\"}"), 1L);
        assertThat(safe).doesNotContainKey("pairs").doesNotContainKey("answer");
        assertThat(safe).containsKeys("leftItems", "rightItems");
        @SuppressWarnings("unchecked")
        List<String> left = (List<String>) safe.get("leftItems");
        @SuppressWarnings("unchecked")
        List<String> right = (List<String>) safe.get("rightItems");
        assertThat(left).hasSize(3);
        assertThat(right).hasSize(3).doesNotContainSequence(left);
    }

    @Test
    void minimalPairPublishesPairButNotAnswer() {
        Map<String, Object> safe = safety.sanitize("MINIMAL_PAIR",
                parse("{\"pair\":[\"ship\",\"sheep\"],\"answer\":\"sheep\"}"), 1L);
        assertThat(safe).containsKey("pair");
        @SuppressWarnings("unchecked")
        List<String> pair = (List<String>) safe.get("pair");
        assertThat(pair).containsExactly("ship", "sheep");
        assertThat(safe).doesNotContainKey("answer");
    }

    @Test
    void minimalPairScoresCorrectAndWrong() {
        JsonNode config = parse("{\"pair\":[\"ship\",\"sheep\"],\"answer\":\"sheep\"}");
        assertThat(safety.isCorrect("MINIMAL_PAIR", config, "sheep")).isTrue();
        assertThat(safety.isCorrect("MINIMAL_PAIR", config, "ship")).isFalse();
    }

    @Test
    void trueFalseScoresCorrectly() {
        JsonNode config = parse("{\"answer\":true}");
        assertThat(safety.isCorrect("TRUE_FALSE", config, true)).isTrue();
        assertThat(safety.isCorrect("TRUE_FALSE", config, false)).isFalse();
    }

    @Test
    void fillMatchesMultipleAnswersCaseInsensitively() {
        JsonNode config = parse("{\"answers\":[\"Tokyo\",\"NYC\"]}");
        assertThat(safety.isCorrect("INFO_FILL", config, "tokyo")).isTrue();
        assertThat(safety.isCorrect("INFO_FILL", config, "nyc")).isTrue();
        assertThat(safety.isCorrect("INFO_FILL", config, "Paris")).isFalse();
    }

    @Test
    void unknownQuestionTypeIsNotTreatedAsFill() {
        JsonNode config = parse("{\"answer\":\"x\"}");
        // A type the registry does not know must score false, never be treated as FILL.
        assertThat(safety.isCorrect("NOT_A_TYPE", config, "x")).isFalse();
    }

    @Test
    void registryAndSafetyUseTheSameKindSource() {
        assertThat(exerciseService.kindOf("MINIMAL_PAIR")).isEqualTo("MINIMAL_PAIR");
        assertThat(exerciseService.kindOf("SEGMENT_ORDERING")).isEqualTo("ORDER");
        assertThat(exerciseService.kindOf("WEAK_FORM_FILL")).isEqualTo("FILL");
        assertThat(exerciseService.kindOf("NUMBER_FILL")).isEqualTo("FILL");
        assertThat(exerciseService.questionTypes().get("LISTENING"))
                .contains("MINIMAL_PAIR", "SEGMENT_ORDERING", "WEAK_FORM_FILL", "NUMBER_FILL",
                        "TIME_FILL", "LOCATION_FILL", "TRUE_FALSE", "PHONEME_WORD", "DICTATION");
    }

    @Test
    void unsupportedListeningTypeRejectedByRegistry() {
        assertThatCode(() -> exerciseService.validateConfig("LISTENING", "SENTENCE_REWRITE", "{\"answer\":\"x\"}"))
                .isInstanceOf(ApiException.class);
        assertThatCode(() -> exerciseService.validateConfig("LISTENING", "MINIMAL_PAIR",
                "{\"pair\":[\"a\",\"b\"],\"answer\":\"a\"}")).doesNotThrowAnyException();
    }
}
