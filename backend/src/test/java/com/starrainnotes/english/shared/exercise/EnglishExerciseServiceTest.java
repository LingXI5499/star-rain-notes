package com.starrainnotes.english.shared.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnglishExerciseServiceTest {

    private static final String CODE = "ENGLISH_EXERCISE_CONFIG_INVALID";

    private final EnglishExerciseService service = new EnglishExerciseService(new ObjectMapper());

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
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void unknownQuestionTypeIsRejected() {
        assertThatThrownBy(() -> service.validateConfig("READING", "NOT_A_TYPE", "{}"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void moduleTypeMustMatchQuestionType() {
        assertThatThrownBy(() -> service.validateConfig("LISTENING", "TRUE_FALSE", "{\"answer\":true}"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void invalidJsonIsRejected() {
        assertThatThrownBy(() -> service.validateConfig("READING", "SINGLE_CHOICE", "{not-json"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void booleanAnswerMustBeBoolean() {
        assertThatThrownBy(() -> service.validateConfig("READING", "TRUE_FALSE", "{\"answer\":\"yes\"}"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getCode())
                        .isEqualTo(CODE));
    }

    @Test
    void orderingNeedsNonEmptyItems() {
        assertThatCode(() -> service.validateConfig("READING", "ORDERING",
                "{\"items\":[\"first\",\"second\"]}")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateConfig("READING", "ORDERING", "{\"items\":[]}"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void listeningFillAcceptsAnswerOrAnswers() {
        assertThatCode(() -> service.validateConfig("LISTENING", "INFO_FILL", "{\"answer\":\"Tokyo\"}"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateConfig("LISTENING", "INFO_FILL", "{}"))
                .isInstanceOf(ApiException.class);
    }
}
