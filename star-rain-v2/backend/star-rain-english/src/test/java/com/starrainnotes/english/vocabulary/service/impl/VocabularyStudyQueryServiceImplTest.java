package com.starrainnotes.english.vocabulary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryRow;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyQueueVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudyCardVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VocabularyStudyQueryServiceImplTest {

    @Mock VocabularyStudyMapper mapper;
    @Mock VocabularyService vocabulary;

    @InjectMocks VocabularyStudyQueryServiceImpl service;

    private static Word word(long id) {
        Word word = new Word();
        word.setId(id);
        word.setWord("word-" + id);
        return word;
    }

    private static VocabularyStudySettingsVO settings(int newLimit, int reviewLimit) {
        return VocabularyStudySettingsVO.builder()
                .showEnglish(true).showChinese(true).reviewDirection("MIXED")
                .dailyNewLimit(newLimit).dailyReviewLimit(reviewLimit).build();
    }

    @Test
    void missingSettingsFallBackToLegacyDefaultsWithoutWriting() {
        when(mapper.settings(1L)).thenReturn(null);

        VocabularyStudySettingsVO result = service.settings(1L);

        assertThat(result.getReviewDirection()).isEqualTo("EN_TO_ZH");
        assertThat(result.getDailyNewLimit()).isEqualTo(20);
        assertThat(result.getDailyReviewLimit()).isEqualTo(200);
    }

    @Test
    void queueKeepsDueWordsBeforeNewWordsEvenWhenBatchQueryReordersThem() {
        when(mapper.settings(1L)).thenReturn(settings(20, 200));
        when(mapper.dueCount(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        when(mapper.dueWordIds(eq(1L), org.mockito.ArgumentMatchers.any(), anyInt())).thenReturn(List.of(2L));
        when(mapper.introducedSince(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(mapper.newWordIds(eq(5L), eq(1L), anyInt())).thenReturn(List.of(1L));
        /* 批量取词按主题顺序返回 1、2，队列必须按「到期优先」重排成 2、1 */
        when(vocabulary.wordsByIds(anyList(), anyInt())).thenReturn(List.of(word(1L), word(2L)));
        when(mapper.memories(eq(1L), anyList())).thenReturn(List.of(
                VocabularyMemoryRow.builder().wordId(2L).memoryCount(1).reviewCount(1)
                        .nextReviewAt(LocalDateTime.of(2026, 3, 1, 0, 0)).learningStatus("ACTIVE").build()));

        VocabularyQueueVO queue = service.queue(1L, 5L);

        assertThat(queue.getItems()).extracting(item -> item.getWord().getId()).containsExactly(2L, 1L);
        assertThat(queue.getDueCount()).isEqualTo(1);
        assertThat(queue.getNewCount()).isEqualTo(1);
    }

    @Test
    void queueSkipsNewWordsWhenDailyLimitAlreadyReached() {
        when(mapper.settings(1L)).thenReturn(settings(1, 200));
        when(mapper.dueCount(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(mapper.dueWordIds(eq(1L), org.mockito.ArgumentMatchers.any(), anyInt())).thenReturn(List.of());
        when(mapper.introducedSince(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(1);

        VocabularyQueueVO queue = service.queue(1L, 5L);

        assertThat(queue.getItems()).isEmpty();
        assertThat(queue.getNewCount()).isZero();
        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.never()).newWordIds(anyInt(), eq(1L), anyInt());
    }

    @Test
    void statesAlwaysCoverEveryRequestedWord() {
        when(mapper.memories(eq(1L), anyList())).thenReturn(List.of(
                VocabularyMemoryRow.builder().wordId(3L).memoryCount(4).reviewCount(2).reviewStep(2)
                        .learningStatus("ACTIVE").displayMode("ENGLISH_ONLY").build()));

        List<VocabularyMemoryVO> states = service.memories(1L, List.of(3L, 9L));

        assertThat(states).hasSize(2);
        assertThat(states.get(0).getMemoryCount()).isEqualTo(4);
        assertThat(states.get(0).getDisplayMode()).isEqualTo("ENGLISH_ONLY");
        /* 没有记忆行的词补 NEW 缺省，而不是从结果里消失 */
        assertThat(states.get(1).getLearningStatus()).isEqualTo("NEW");
        assertThat(states.get(1).getWordId()).isEqualTo(9L);
    }

    @Test
    void queueCardCarriesAConcreteDirectionForMixedSetting() {
        when(mapper.settings(1L)).thenReturn(settings(20, 200));
        when(mapper.dueCount(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        when(mapper.dueWordIds(eq(1L), org.mockito.ArgumentMatchers.any(), anyInt())).thenReturn(List.of(4L));
        when(mapper.introducedSince(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(vocabulary.wordsByIds(anyList(), anyInt())).thenReturn(List.of(word(4L)));
        when(mapper.memories(eq(1L), anyList())).thenReturn(List.of());

        VocabularyQueueVO queue = service.queue(1L, null);

        VocabularyStudyCardVO card = queue.getItems().get(0);
        assertThat(card.getDirection()).isIn("EN_TO_ZH", "ZH_TO_EN");
        assertThat(card.isNewWord()).isTrue();
    }
}
