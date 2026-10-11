/*
 * VocabularyStudyCommandServiceImplTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：Word、VocabularyMemoryLock、VocabularyReviewLogRow、VocabularyReviewRequestDTO、VocabularyReviewSchedule、VocabularyStudySettingsRequestDTO、TimingStatus。
 * 阅读入口：memory（记忆处理入口）；reviewRequest（审核请求处理入口）；bothLanguagesHiddenIsRejectedWithoutTouchingStorage（bothLanguagesHidden是否已拒绝不含Touching存储处理入口）；settingsOutsideAllowedRangeAreRejected（settingsOutsideAllowed范围Are已拒绝处理入口）。
 */

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

/**
 * 词汇学习Command业务服务实现测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class VocabularyStudyCommandServiceImplTest {

    /**
     * 会话。
     */
    private static final String SESSION = UUID.randomUUID().toString();

    /**
     * 数据映射。
     * 协作类型：<code>VocabularyStudyMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock VocabularyStudyMapper mapper;
    /**
     * 查询参数。
     * 协作类型：<code>VocabularyStudyQueryService</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock VocabularyStudyQueryService query;
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
    @InjectMocks VocabularyStudyCommandServiceImpl service;

    /**
     * 记忆处理入口。
     * 关键处理步骤调用 <code>VocabularyMemoryVO.builder</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    private static VocabularyMemoryVO memory(long wordId) {
        return VocabularyMemoryVO.builder().wordId(wordId).learningStatus("ACTIVE").build();
    }

    /**
     * 审核请求处理入口。
     * 关键处理步骤调用 <code>request.setReviewSessionId</code>、<code>request.setDirection</code>。
     * @return 类型为 <code>VocabularyReviewRequestDTO</code> 的处理结果
     */
    private static VocabularyReviewRequestDTO reviewRequest() {
        VocabularyReviewRequestDTO request = new VocabularyReviewRequestDTO();
        request.setReviewSessionId(SESSION);
        request.setDirection("EN_TO_ZH");
        return request;
    }

    /**
     * bothLanguagesHidden是否已拒绝不含Touching存储处理入口。
     * 主要协作调用：<code>service.updateSettings</code>。
     */
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

    /**
     * settingsOutsideAllowed范围Are已拒绝处理入口。
     * 主要协作调用：<code>service.updateSettings</code>。
     */
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

    /**
     * settingsArePersistedThen读取Back处理入口。
     * 主要协作调用：<code>query.settings</code>、<code>service.updateSettings</code>。
     */
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

    /**
     * 开始RequiresAnExisting单词及ReturnsWritten状态处理入口。
     * 主要协作调用：<code>vocabulary.word</code>、<code>query.requiredMemory</code>、<code>service.start</code>。
     */
    @Test
    void startRequiresAnExistingWordAndReturnsWrittenState() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(query.requiredMemory(1L, 7L)).thenReturn(memory(7L));

        VocabularyMemoryVO result = service.start(1L, 7L);

        verify(mapper).startMemory(eq(1L), eq(7L), any());
        assertThat(result.getWordId()).isEqualTo(7L);
    }

    /**
     * 首次审核Uses首次间隔及Advances记忆处理入口。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.findReviewBySession</code>、<code>mapper.lockMemory</code>、<code>mapper.insertReview</code>、<code>query.requiredMemory</code>。
     */
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

    /**
     * 缺失记忆行是否创建之前审核SoNewWordsCanBe复习处理入口。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.findReviewBySession</code>、<code>mapper.lockMemory</code>、<code>mapper.insertReview</code>、<code>query.requiredMemory</code>。
     */
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

    /**
     * replayed会话Does不数量Twice处理入口。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.findReviewBySession</code>、<code>query.memoryOrDefault</code>、<code>service.completeReview</code>。
     */
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

    /**
     * 会话Reused面向Another单词是否已拒绝处理入口。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.findReviewBySession</code>、<code>service.completeReview</code>。
     */
    @Test
    void sessionReusedForAnotherWordIsRejected() {
        when(vocabulary.word("7")).thenReturn(new Word());
        when(mapper.findReviewBySession(1L, SESSION)).thenReturn(VocabularyReviewLogRow.builder()
                .wordId(8L).reviewNumber(1).intervalSeconds(300L)
                .timingStatus(TimingStatus.NEW.name()).build());

        assertThatThrownBy(() -> service.completeReview(1L, 7L, reviewRequest()))
                .isInstanceOf(VocabularyReviewSessionConflictException.class);
    }

    /**
     * 重置RemovesPersonal进度Only。
     * 主要协作调用：<code>vocabulary.word</code>、<code>service.reset</code>。
     */
    @Test
    void resetRemovesPersonalProgressOnly() {
        when(vocabulary.word("7")).thenReturn(new Word());

        service.reset(1L, 7L);

        verify(mapper).resetMemory(1L, 7L);
        verify(mapper, never()).findReviewBySession(anyLong(), anyString());
    }
}
