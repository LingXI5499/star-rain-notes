/*
 * VocabularyLearningServiceTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、Word、VocabularyService。
 * 阅读入口：setup（setup处理入口）；request（本次操作的请求参数对象处理入口）；previewHasNoMemoryWritesOrPlanMutation（预览是否具备不含记忆写入或计划Mutation）；stalePlanRevisionRejectsBeforeMemoryWrite（stale计划修订拒绝之前记忆写入处理入口）。
 */

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

/**
 * 词汇学习业务服务测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class VocabularyLearningServiceTest {
    /**
     * 数据映射。
     * 协作类型：<code>VocabularyLearningMapper</code>。
     */
    private VocabularyLearningMapper mapper;
    /**
     * 词汇。
     * 协作类型：<code>VocabularyService</code>。
     */
    private VocabularyService vocabulary;
    /**
     * 业务服务。
     * 协作类型：<code>VocabularyLearningService</code>。
     */
    private VocabularyLearningService service;
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private ObjectMapper json;
    /**
     * setup处理入口。
     * 主要协作调用：<code>mapper.lockSlot</code>。
     */
    @BeforeEach void setup() {
        mapper = mock(VocabularyLearningMapper.class); vocabulary = mock(VocabularyService.class);
        json = new ObjectMapper().findAndRegisterModules();
        service = new VocabularyLearningService(mapper, mock(VocabularyMapper.class), vocabulary, json,
                Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC));
        when(mapper.lockSlot(1)).thenReturn(new Plan());
    }
    /**
     * 本次操作的请求参数对象处理入口。
     * 关键处理步骤调用 <code>request.setReviewSessionId</code>、<code>request.setDirection</code>、<code>request.setRating</code>、<code>request.setSource</code>。
     * @return 类型为 <code>RatingRequest</code> 的处理结果
     */
    private RatingRequest request() {
        RatingRequest request = new RatingRequest(); request.setReviewSessionId("11111111-1111-4111-8111-111111111111");
        request.setDirection("EN_TO_ZH"); request.setRating("KNOW"); request.setSource("FREE"); return request;
    }
    /**
     * 预览是否具备不含记忆写入或计划Mutation。
     * 主要协作调用：<code>mapper.selected</code>、<code>vocabulary.wordsByIds</code>、<code>service.preview</code>。
     */
    @Test void previewHasNoMemoryWritesOrPlanMutation() {
        Selection selection = new Selection(); SelectionRow row = new SelectionRow(); row.setWordId(7L); row.setToken("word");
        when(mapper.selected(eq(1L), any(), eq(0L), eq(500))).thenReturn(List.of(row));
        Word word = new Word(); word.setId(7L); when(vocabulary.wordsByIds(any(), anyInt())).thenReturn(List.of(word));
        Preview preview = service.preview(1, selection, 1, 24);
        assertEquals(1, preview.getTotalWords());
        verify(mapper, never()).ensureSlot(anyLong()); verify(mapper, never()).initializeMemory(anyLong(), anyLong(), any());
        verify(mapper, never()).clearItems(anyLong()); verify(mapper, never()).insertLog(anyLong(), any());
    }
    /**
     * stale计划修订拒绝之前记忆写入处理入口。
     * 主要协作调用：<code>service.rate</code>。
     */
    @Test void stalePlanRevisionRejectsBeforeMemoryWrite() {
        var request = request(); request.setSource("PLAN"); request.setPlanRevision(9L);
        assertThrows(ApiException.class, () -> service.rate(1, 7, request));
        verify(mapper, never()).initializeMemory(anyLong(), anyLong(), any());
    }
    /**
     * 音频MustHavePlayed之前ItCanBeRated处理入口。
     * 主要协作调用：<code>service.rate</code>。
     */
    @Test void audioMustHavePlayedBeforeItCanBeRated() {
        var request = request(); request.setDirection("AUDIO_TO_BOTH");
        assertThrows(ApiException.class, () -> service.rate(1, 7, request)); verifyNoInteractions(mapper);
    }
    /**
     * malformedCursor是否Bad请求不含LoadingQueue处理入口。
     * 主要协作调用：<code>service.queue</code>。
     */
    @Test void malformedCursorIsBadRequestWithoutLoadingQueue() {
        ApiException error = assertThrows(ApiException.class, () -> service.queue(1, 20, "not-json"));
        assertEquals(400, error.getStatus()); verifyNoInteractions(mapper);
    }
    /**
     * replayReturns原始快照不含Increment及Conflicting提交内容是否已拒绝处理入口。
     * 主要协作调用：<code>json.writeValueAsString</code>、<code>mapper.log</code>、<code>service.rate</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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
    /**
     * deletion保留修订及TouchesOnly计划Tables处理入口。
     * 主要协作调用：<code>service.cancel</code>。
     */
    @Test void deletionKeepsRevisionAndTouchesOnlyPlanTables() {
        service.cancel(1, 0); verify(mapper).clearItems(1); verify(mapper).cancelPlan(eq(1L), any());
        verify(mapper, never()).saveMastery(anyLong(), anyLong(), anyInt(), anyString(), anyString(), any());
    }
    /**
     * resizeFreezesStartedGroups及OnlyRegroups剩余Suffix处理入口。
     * 主要协作调用：<code>mapper.lockSlot</code>、<code>mapper.lastStartedGroup</code>、<code>mapper.firstRemainingSort</code>、<code>service.resize</code>。
     */
    @Test void resizeFreezesStartedGroupsAndOnlyRegroupsRemainingSuffix() {
        Plan plan = new Plan(); plan.setStatus("ACTIVE"); plan.setRevision(3); when(mapper.lockSlot(1)).thenReturn(plan);
        when(mapper.lastStartedGroup(1)).thenReturn(2); when(mapper.firstRemainingSort(1, 2)).thenReturn(41);
        RevisionRequest request = new RevisionRequest(); request.setExpectedRevision(3); request.setBatchSize(10);
        service.resize(1, request); verify(mapper).regroup(1, 2, 41, 10); verify(mapper).resize(eq(1L), eq(10), any());
    }
}
