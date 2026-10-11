package com.starrainnotes.english.vocabulary.learning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.*;
import com.starrainnotes.english.vocabulary.mapper.*;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VocabularyLearningServiceTest {
    private VocabularyLearningMapper mapper;
    private VocabularyService vocabulary;
    private VocabularyLearningService service;
    private ObjectMapper json;
    @BeforeEach void setup() {
        mapper = mock(VocabularyLearningMapper.class); vocabulary = mock(VocabularyService.class);
        json = new ObjectMapper().findAndRegisterModules();
        service = new VocabularyLearningService(mapper, mock(VocabularyMapper.class), vocabulary, json,
                Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC));
        when(mapper.lockSlot(1)).thenReturn(new Plan());
    }
    private RatingRequest request() {
        RatingRequest request = new RatingRequest(); request.setReviewSessionId("11111111-1111-4111-8111-111111111111");
        request.setDirection("EN_TO_ZH"); request.setRating("KNOW"); request.setSource("FREE"); return request;
    }
    @Test void previewHasNoMemoryWritesOrPlanMutation() {
        Selection selection = new Selection(); SelectionRow row = new SelectionRow(); row.setWordId(7L); row.setToken("word");
        when(mapper.selected(eq(1L), any(), eq(0L), eq(500))).thenReturn(List.of(row));
        Word word = new Word(); word.setId(7L); when(vocabulary.wordsByIds(any(), anyInt())).thenReturn(List.of(word));
        Preview preview = service.preview(1, selection, 1, 24);
        assertEquals(1, preview.getTotalWords());
        verify(mapper, never()).ensureSlot(anyLong()); verify(mapper, never()).initializeMemory(anyLong(), anyLong(), any());
        verify(mapper, never()).clearItems(anyLong()); verify(mapper, never()).insertLog(anyLong(), any());
    }
    @Test void stalePlanRevisionRejectsBeforeMemoryWrite() {
        var request = request(); request.setSource("PLAN"); request.setPlanRevision(9L);
        assertThrows(ApiException.class, () -> service.rate(1, 7, request));
        verify(mapper, never()).initializeMemory(anyLong(), anyLong(), any());
    }
    @Test void audioMustHavePlayedBeforeItCanBeRated() {
        var request = request(); request.setDirection("AUDIO_TO_BOTH");
        assertThrows(ApiException.class, () -> service.rate(1, 7, request)); verifyNoInteractions(mapper);
    }
    @Test void malformedCursorIsBadRequestWithoutLoadingQueue() {
        ApiException error = assertThrows(ApiException.class, () -> service.queue(1, 20, "not-json"));
        assertEquals(400, error.getStatus()); verifyNoInteractions(mapper);
    }
    @Test void replayReturnsOriginalSnapshotWithoutIncrementAndConflictingPayloadIsRejected() throws Exception {
        RatingLog log = new RatingLog(); log.setWordId(7L); log.setDirection("EN_TO_ZH"); log.setRating("KNOW"); log.setSource("FREE");
        RatingResult result = new RatingResult(); result.setIntervalSeconds(300); log.setResultJson(json.writeValueAsString(result));
        when(mapper.log(anyLong(), anyString())).thenReturn(log);
        assertTrue(service.rate(1, 7, request()).isDuplicate());
        var wrong = request(); wrong.setRating("FORGOT");
        assertThrows(ApiException.class, () -> service.rate(1, 7, wrong));
        var directionConflict = request(); directionConflict.setDirection("ZH_TO_EN");
        assertThrows(ApiException.class, () -> service.rate(1, 7, directionConflict));
        verify(mapper, never()).saveMode(anyLong(), any()); verify(mapper, never()).insertLog(anyLong(), any());
    }
    @Test void deletionKeepsRevisionAndTouchesOnlyPlanTables() {
        service.cancel(1, 0); verify(mapper).clearItems(1); verify(mapper).cancelPlan(eq(1L), any());
        verify(mapper, never()).saveMastery(anyLong(), anyLong(), anyInt(), anyString(), anyString(), any());
    }
    @Test void resizeFreezesStartedGroupsAndOnlyRegroupsRemainingSuffix() {
        Plan plan = new Plan(); plan.setStatus("ACTIVE"); plan.setRevision(3); when(mapper.lockSlot(1)).thenReturn(plan);
        when(mapper.lastStartedGroup(1)).thenReturn(2); when(mapper.firstRemainingSort(1, 2)).thenReturn(41);
        RevisionRequest request = new RevisionRequest(); request.setExpectedRevision(3); request.setBatchSize(10);
        service.resize(1, request); verify(mapper).regroup(1, 2, 41, 10); verify(mapper).resize(eq(1L), eq(10), any());
    }
}
