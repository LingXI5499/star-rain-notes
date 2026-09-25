package com.starrainnotes.english.shared.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.english.shared.domain.EnglishRuleViolation;
import com.starrainnotes.english.shared.exercise.domain.EnglishExercisePolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnglishExercisePolicyTest {

    private static final String CODE = "ENGLISH_EXERCISE_CONFIG_INVALID";

    private final EnglishExercisePolicy service = new EnglishExercisePolicy(new ObjectMapper());

    @Test
    void validSingleChoiceConfigPasses() {
        String config = "{\"options\":[{\"key\":\"a\",\"text\":\"A\"},{\"key\":\"b\",\"text\":\"B\"}],\"answer\":\"a\"}";
        assertThatCode(() -> service.validateConfig("READING", "SINGLE_CHOICE", config))
                .doesNotThrowAnyException();
    }

    @Test
    void singleChoiceMissingValidAnswerIsRejected() {
        String config = "{\"options\":[{\"key\":\"a\",\"text\":\"A\"}],\"answer\":\"z\"}";
        assertThatThrownBy(() -> service.validateConfig("READING", "SINGLE_CHOICE", config))
                .isInstanceOf(EnglishRuleViolation.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((EnglishRuleViolation) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void unknownQuestionTypeIsRejected() {
        assertThatThrownBy(() -> service.validateConfig("READING", "NOT_A_TYPE", "{}"))
                .isInstanceOf(EnglishRuleViolation.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((EnglishRuleViolation) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void moduleTypeMustMatchQuestionType() {
        // SENTENCE_REWRITE is a writing-only type; using it for LISTENING is rejected.
        assertThatThrownBy(() -> service.validateConfig("LISTENING", "SENTENCE_REWRITE", "{\"answer\":\"x\"}"))
                .isInstanceOf(EnglishRuleViolation.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((EnglishRuleViolation) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void invalidJsonIsRejected() {
        assertThatThrownBy(() -> service.validateConfig("READING", "SINGLE_CHOICE", "{not-json"))
                .isInstanceOf(EnglishRuleViolation.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((EnglishRuleViolation) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void booleanAnswerMustBeBoolean() {
        assertThatThrownBy(() -> service.validateConfig("READING", "TRUE_FALSE", "{\"answer\":\"yes\"}"))
                .isInstanceOf(EnglishRuleViolation.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((EnglishRuleViolation) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void orderingNeedsNonEmptyItems() {
        assertThatCode(() -> service.validateConfig("READING", "ORDERING",
                "{\"items\":[\"first\",\"second\"]}")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateConfig("READING", "ORDERING", "{\"items\":[]}"))
                .isInstanceOf(EnglishRuleViolation.class);
    }

    @Test
    void listeningFillAcceptsAnswerOrAnswers() {
        assertThatCode(() -> service.validateConfig("LISTENING", "INFO_FILL", "{\"answer\":\"Tokyo\"}"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateConfig("LISTENING", "INFO_FILL", "{}"))
                .isInstanceOf(EnglishRuleViolation.class);
    }
}
