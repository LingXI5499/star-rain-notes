package com.starrainnotes.english.vocabulary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.english.vocabulary.dto.VocabularyLocalProgressRequestDTO;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * 本机进度导入：坏数据只丢行不整批失败，且只增不减。
 */
@ExtendWith(MockitoExtension.class)
class VocabularyProgressImportServiceImplTest {

    @Mock VocabularyStudyMapper mapper;

    @InjectMocks VocabularyProgressImportServiceImpl service;

    private static VocabularyLocalProgressRequestDTO.LocalMemoryPayload memory(Long wordId, Integer memoryCount,
                                                                           Integer reviewCount, String status) {
        VocabularyLocalProgressRequestDTO.LocalMemoryPayload payload = new VocabularyLocalProgressRequestDTO.LocalMemoryPayload();
        payload.setWordId(wordId);
        payload.setMemoryCount(memoryCount);
        payload.setReviewCount(reviewCount);
        payload.setLearningStatus(status);
        return payload;
    }

    @Test
    void planMembershipWithoutAnyReviewStillImportsSoGuestPlansSurviveLogin() {
        when(mapper.wordExists(20717L)).thenReturn(1);
        VocabularyLocalProgressRequestDTO request = new VocabularyLocalProgressRequestDTO();
        request.setMemory(List.of(memory(20717L, 0, 0, "ACTIVE")));

        service.importLocal(1L, request);

        verify(mapper).importMemory(eq(1L), eq(20717L), eq(0), eq(0), eq(0), any(), any(), any(), any(), any());
    }

    @Test
    void inactiveRowWithZeroCountsIsNotImported() {
        VocabularyLocalProgressRequestDTO request = new VocabularyLocalProgressRequestDTO();
        request.setMemory(List.of(memory(20717L, 0, 0, "NEW")));

        service.importLocal(1L, request);

        verify(mapper, never()).importMemory(anyLong(), anyLong(), anyInt(), anyInt(), anyInt(),
                any(), any(), any(), any(), any());
    }

    @Test
    void unknownWordsAndBrokenRowsAreSkippedWithoutFailingTheBatch() {
        when(mapper.wordExists(20717L)).thenReturn(1);
        when(mapper.wordExists(99999L)).thenReturn(0);
        VocabularyLocalProgressRequestDTO request = new VocabularyLocalProgressRequestDTO();
        request.setMemory(List.of(
                memory(99999L, 5, 5, "ACTIVE"),
                memory(null, 5, 5, "ACTIVE"),
                memory(20717L, 3, 2, "ACTIVE")));

        service.importLocal(1L, request);

        verify(mapper).importMemory(eq(1L), eq(20717L), eq(3), eq(2), eq(2), any(), any(), any(), any(), any());
    }

    @Test
    void legacyVocabularyMapIsAccepted() {
        when(mapper.wordExists(15560L)).thenReturn(1);
        VocabularyLocalProgressRequestDTO request = new VocabularyLocalProgressRequestDTO();
        request.setVocabulary(Map.of("15560", memory(null, 4, 0, "ACTIVE")));

        service.importLocal(1L, request);

        verify(mapper).importMemory(eq(1L), eq(15560L), eq(4), eq(0), eq(0), any(), any(), any(), any(), any());
    }

    @Test
    void reviewLogRowsAreClampedAndBadDirectionsDropped() {
        when(mapper.wordExists(20717L)).thenReturn(1);
        String session = UUID.randomUUID().toString();
        VocabularyLocalProgressRequestDTO.LocalReviewPayload good = new VocabularyLocalProgressRequestDTO.LocalReviewPayload();
        good.setWordId(20717L);
        good.setReviewSessionId(session);
        good.setDirection("EN_TO_ZH");
        good.setReviewNumber(2);
        good.setReviewedAt("2099-01-01T00:00:00.000Z");
        good.setIntervalSeconds(1800L);
        good.setTimingStatus("ON_TIME");

        VocabularyLocalProgressRequestDTO.LocalReviewPayload badDirection = new VocabularyLocalProgressRequestDTO.LocalReviewPayload();
        badDirection.setWordId(20717L);
        badDirection.setReviewSessionId(UUID.randomUUID().toString());
        badDirection.setDirection("MIXED");

        VocabularyLocalProgressRequestDTO.LocalReviewPayload badSession = new VocabularyLocalProgressRequestDTO.LocalReviewPayload();
        badSession.setWordId(20717L);
        badSession.setReviewSessionId("not-a-uuid");
        badSession.setDirection("EN_TO_ZH");

        VocabularyLocalProgressRequestDTO request = new VocabularyLocalProgressRequestDTO();
        request.setReviewLog(List.of(good, badDirection, badSession));

        service.importLocal(1L, request);

        ArgumentCaptor<java.time.LocalDateTime> reviewedAt = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        verify(mapper).importReview(eq(1L), eq(20717L), eq(session), eq("EN_TO_ZH"), eq(2), any(),
                reviewedAt.capture(), eq(1800L), eq("ON_TIME"));
        /* 客户端时钟跑到 2099 时按服务端时间记，不允许出现未来的复习记录 */
        assertThat(reviewedAt.getValue()).isBefore(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(1));
    }
}
