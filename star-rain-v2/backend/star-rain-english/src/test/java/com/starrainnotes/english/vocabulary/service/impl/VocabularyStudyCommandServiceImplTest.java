package com.starrainnotes.english.vocabulary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryLock;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewLogRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.dto.VocabularyStudySettingsRequestDTO;
import com.starrainnotes.english.vocabulary.enumeration.TimingStatus;
import com.starrainnotes.english.vocabulary.exception.VocabularyReviewSessionConflictException;
import com.starrainnotes.english.vocabulary.exception.VocabularySettingsInvalidException;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyQueryService;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewResultVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VocabularyStudyCommandServiceImplTest {

    private static final String SESSION = UUID.randomUUID().toString();

    @Mock VocabularyStudyMapper mapper;
    @Mock VocabularyStudyQueryService query;
    @Mock VocabularyService vocabulary;

    @InjectMocks VocabularyStudyCommandServiceImpl service;

    private static VocabularyMemoryVO memory(long wordId) {
        return VocabularyMemoryVO.builder().wordId(wordId).learningStatus("ACTIVE").build();
    }

    private static VocabularyReviewRequestDTO reviewRequest() {
        VocabularyReviewRequestDTO request = new VocabularyReviewRequestDTO();
        request.setReviewSessionId(SESSION);
        request.setDirection("EN_TO_ZH");
        return request;
    }

    @Test
    void bothLanguagesHiddenIsRejectedWithoutTouchingStorage() {
        VocabularyStudySettingsRequestDTO request = new VocabularyStudySettingsRequestDTO();
        request.setShowEnglish(false);
        request.setShowChinese(false);
        request.setReviewDirection("MIXED");
        request.setDailyNewLimit(20);
        request.setDailyReviewLimit(200);

        assertThatThrownBy(() -> service.updateSettings(1L, request))
                .isInstanceOf(VocabularySettingsInvalidException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void settingsOutsideAllowedRangeAreRejected() {
        VocabularyStudySettingsRequestDTO request = new VocabularyStudySettingsRequestDTO();
        request.setShowEnglish(true);
        request.setShowChinese(true);
        request.setReviewDirection("MIXED");
        request.setDailyNewLimit(500);
        request.setDailyReviewLimit(200);

        assertThatThrownBy(() -> service.updateSettings(1L, request))
                .isInstanceOf(VocabularySettingsInvalidException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void settingsArePersistedThenReadBack() {
        VocabularyStudySettingsRequestDTO request = new VocabularyStudySettingsRequestDTO();
        request.setShowEnglish(true);
        request.setShowChinese(false);
        request.setReviewDirection("ZH_TO_EN");
        request.setDailyNewLimit(5);
        request.setDailyReviewLimit(50);
        VocabularyStudySettingsVO stored = VocabularyStudySettingsVO.builder()
                .showEnglish(true).showChinese(false).reviewDirection("ZH_TO_EN")
                .dailyNewLimit(5).dailyReviewLimit(50).build();
        when(query.settings(1L)).thenReturn(stored);

        VocabularyStudySettingsVO result = service.updateSettings(1L, request);

        verify(mapper).upsertSettings(eq(1L), any(VocabularyStudySettingsVO.class));
        assertThat(result.getReviewDirection()).isEqualTo("ZH_TO_EN");
        assertThat(result.isShowChinese()).isFalse();
    }

    @Test
    void startRequiresAnExistingWordAndReturnsWrittenState() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(query.requiredMemory(1L, 7L)).thenReturn(memory(7L));

        VocabularyMemoryVO result = service.start(1L, 7L);

        verify(mapper).startMemory(eq(1L), eq(7L), any());
        assertThat(result.getWordId()).isEqualTo(7L);
    }

    @Test
    void firstReviewUsesFirstIntervalAndAdvancesMemory() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(mapper.findReviewBySession(1L, SESSION)).thenReturn(null);
        when(mapper.lockMemory(1L, 7L)).thenReturn(
                VocabularyMemoryLock.builder().memoryCount(0).reviewCount(0).nextReviewAt(null).build());
        when(mapper.insertReview(eq(1L), eq(7L), eq(SESSION), eq("EN_TO_ZH"), any())).thenReturn(1);
        when(query.requiredMemory(1L, 7L)).thenReturn(memory(7L));

        VocabularyReviewResultVO result = service.completeReview(1L, 7L, reviewRequest());

        ArgumentCaptor<VocabularyReviewSchedule> captor = ArgumentCaptor.forClass(VocabularyReviewSchedule.class);
        verify(mapper).advanceMemory(eq(1L), eq(7L), captor.capture());
        assertThat(captor.getValue().getReviewNumber()).isEqualTo(1);
        assertThat(captor.getValue().getIntervalSeconds()).isEqualTo(300L);
        assertThat(captor.getValue().getTimingStatus()).isEqualTo(TimingStatus.NEW.name());
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getIntervalSeconds()).isEqualTo(300L);
    }

    @Test
    void missingMemoryRowIsCreatedBeforeReviewSoNewWordsCanBeReviewed() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(mapper.findReviewBySession(1L, SESSION)).thenReturn(null);
        when(mapper.lockMemory(1L, 7L)).thenReturn(null,
                VocabularyMemoryLock.builder().memoryCount(0).reviewCount(0).nextReviewAt(null).build());
        when(mapper.insertReview(eq(1L), eq(7L), eq(SESSION), anyString(), any())).thenReturn(1);
        when(query.requiredMemory(1L, 7L)).thenReturn(memory(7L));

        service.completeReview(1L, 7L, reviewRequest());

        verify(mapper).initializeMemory(eq(1L), eq(7L), any());
    }

    @Test
    void replayedSessionDoesNotCountTwice() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(mapper.findReviewBySession(1L, SESSION)).thenReturn(VocabularyReviewLogRow.builder()
                .wordId(7L).reviewNumber(3).intervalSeconds(43_200L)
                .timingStatus(TimingStatus.ON_TIME.name()).build());
        when(query.memoryOrDefault(1L, 7L)).thenReturn(memory(7L));

        VocabularyReviewResultVO result = service.completeReview(1L, 7L, reviewRequest());

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getReviewNumber()).isEqualTo(3);
        assertThat(result.getIntervalSeconds()).isEqualTo(43_200L);
        verify(mapper, never()).advanceMemory(anyLong(), anyLong(), any());
        verify(mapper, never()).insertReview(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void sessionReusedForAnotherWordIsRejected() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(mapper.findReviewBySession(1L, SESSION)).thenReturn(VocabularyReviewLogRow.builder()
                .wordId(8L).reviewNumber(1).intervalSeconds(300L)
                .timingStatus(TimingStatus.NEW.name()).build());

        assertThatThrownBy(() -> service.completeReview(1L, 7L, reviewRequest()))
                .isInstanceOf(VocabularyReviewSessionConflictException.class);
    }

    @Test
    void resetRemovesPersonalProgressOnly() {
        when(vocabulary.word("7")).thenReturn(new Word());

        service.reset(1L, 7L);

        verify(mapper).resetMemory(1L, 7L);
        verify(mapper, never()).findReviewBySession(anyLong(), anyString());
    }
}
