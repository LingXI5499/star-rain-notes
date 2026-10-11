/*
 * VocabularyStudyQueryServiceImplTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：Word、VocabularyMemoryRow、VocabularyStudyMapper、VocabularyService、VocabularyMemoryVO、VocabularyQueueVO、VocabularyStudyCardVO。
 * 阅读入口：word（单词处理入口）；settings（settings处理入口）；missingSettingsFallBackToLegacyDefaultsWithoutWriting（缺失SettingsFallBack目标Legacy默认值不含写作处理入口）；queueKeepsDueWordsBeforeNewWordsEvenWhenBatchQueryReordersThem（queue保留DueWords之前NewWordsEven条件批量查询参数ReordersThem处理入口）。
 */

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

/**
 * 词汇学习查询参数业务服务实现测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class VocabularyStudyQueryServiceImplTest {

    /**
     * 数据映射。
     * 协作类型：<code>VocabularyStudyMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock VocabularyStudyMapper mapper;
    /**
     * 词汇。
     * 协作类型：<code>VocabularyService</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock VocabularyService vocabulary;

    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks VocabularyStudyQueryServiceImpl service;

    /**
     * 单词处理入口。
     * 关键处理步骤调用 <code>word.setId</code>、<code>word.setWord</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    private static Word word(long id) {
        Word word = new Word();
        word.setId(id);
        word.setWord("word-" + id);
        return word;
    }

    /**
     * settings处理入口。
     * 关键处理步骤调用 <code>VocabularyStudySettingsVO.builder</code>。
     *
     * @param newLimit new上限；类型为 <code>int</code>
     * @param reviewLimit 审核上限；类型为 <code>int</code>
     * @return 类型为 <code>VocabularyStudySettingsVO</code> 的处理结果
     */
    private static VocabularyStudySettingsVO settings(int newLimit, int reviewLimit) {
        return VocabularyStudySettingsVO.builder()
                .showEnglish(true).showChinese(true).reviewDirection("MIXED")
                .dailyNewLimit(newLimit).dailyReviewLimit(reviewLimit).build();
    }

    /**
     * 缺失SettingsFallBack目标Legacy默认值不含写作处理入口。
     * 主要协作调用：<code>mapper.settings</code>、<code>service.settings</code>。
     */
    @Test
    void missingSettingsFallBackToLegacyDefaultsWithoutWriting() {
        when(mapper.settings(1L)).thenReturn(null);

        VocabularyStudySettingsVO result = service.settings(1L);

        assertThat(result.getReviewDirection()).isEqualTo("EN_TO_ZH");
        assertThat(result.getDailyNewLimit()).isEqualTo(20);
        assertThat(result.getDailyReviewLimit()).isEqualTo(200);
    }

    /**
     * queue保留DueWords之前NewWordsEven条件批量查询参数ReordersThem处理入口。
     * 主要协作调用：<code>mapper.settings</code>、<code>mapper.dueCount</code>、<code>mapper.dueWordIds</code>、<code>mapper.introducedSince</code>、<code>mapper.newWordIds</code>。
     */
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

    /**
     * queueSkipsNewWords条件Daily上限AlreadyReached处理入口。
     * 主要协作调用：<code>mapper.settings</code>、<code>mapper.dueCount</code>、<code>mapper.dueWordIds</code>、<code>mapper.introducedSince</code>、<code>service.queue</code>。
     */
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

    /**
     * statesAlways封面EveryRequested单词处理入口。
     * 主要协作调用：<code>mapper.memories</code>、<code>service.memories</code>。
     */
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

    /**
     * queue卡片CarriesAConcreteDirection面向MixedSetting处理入口。
     * 主要协作调用：<code>mapper.settings</code>、<code>mapper.dueCount</code>、<code>mapper.dueWordIds</code>、<code>mapper.introducedSince</code>、<code>vocabulary.wordsByIds</code>。
     */
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
