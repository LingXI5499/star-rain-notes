package com.starrainnotes.english.shared.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.english.listening.application.ListeningExerciseApplicationService;
import com.starrainnotes.english.listening.infrastructure.ListeningExerciseRepository;
import com.starrainnotes.english.reading.application.ReadingExerciseApplicationService;
import com.starrainnotes.english.reading.application.ReadingQueryService;
import com.starrainnotes.english.reading.infrastructure.ReadingExerciseRepository;
import com.starrainnotes.english.shared.exercise.domain.EnglishExercisePolicy;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExerciseCheckParityTest {

    @Test
    void readingAndListeningScoreTheSameSubmissionIdentically() {
        ObjectMapper mapper = new ObjectMapper();
        EnglishExercisePolicy rules = new EnglishExercisePolicy(mapper);
        EnglishExerciseSafety safety = new EnglishExerciseSafety(mapper, rules);
        EnglishExercise correct = exercise(7L, true, 2, "Yes.");
        EnglishExercise wrong = exercise(8L, true, 3, "No.");
        ListeningExerciseRepository listeningRepository = mock(ListeningExerciseRepository.class);
        ReadingExerciseRepository readingRepository = mock(ReadingExerciseRepository.class);
        when(listeningRepository.requireOwnedPublished(3L, 7L)).thenReturn(correct);
        when(listeningRepository.requireOwnedPublished(3L, 8L)).thenReturn(wrong);
        when(readingRepository.requirePublishedBinding(3L, 7L)).thenReturn(correct);
        when(readingRepository.requirePublishedBinding(3L, 8L)).thenReturn(wrong);
        ListeningExerciseApplicationService listening = new ListeningExerciseApplicationService(
                listeningRepository, safety, mapper);
        ReadingExerciseApplicationService reading = new ReadingExerciseApplicationService(
                readingRepository, rules, safety, mapper, mock(ReadingQueryService.class));
        CheckAnswerRequest request = new CheckAnswerRequest(List.of(
                new CheckAnswerRequest.Submission(7L, true),
                new CheckAnswerRequest.Submission(8L, false)));

        assertThat(reading.check(3L, request)).isEqualTo(listening.check(3L, request));
    }

    private static EnglishExercise exercise(long id, boolean answer, int score, String explanation) {
        EnglishExercise exercise = new EnglishExercise();
        exercise.setId(id);
        exercise.setQuestionType("TRUE_FALSE");
        exercise.setConfigJson(Map.of("answer", answer));
        exercise.setScoreValue(score);
        exercise.setExplanationMarkdown(explanation);
        return exercise;
    }
}
