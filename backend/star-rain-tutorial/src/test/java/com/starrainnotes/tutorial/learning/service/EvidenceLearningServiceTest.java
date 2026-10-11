/*
 * EvidenceLearningServiceTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、TutorialMapper。
 * 阅读入口：setup（setup处理入口）；foreignSessionIsNotReadableOrWritable（foreign会话是否不Readable或Writable处理入口）；serverRejectsRatingBeforeReveal（server拒绝评分之前Reveal处理入口）；getSessionHidesUnrevealedBack（读取会话HidesUnrevealedBack）。
 */

package com.starrainnotes.tutorial.learning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.learning.dto.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.exception.*;
import com.starrainnotes.tutorial.learning.mapper.*;
import com.starrainnotes.tutorial.learning.service.impl.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 证据学习业务服务测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EvidenceLearningServiceTest {
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    CurrentActorApi actors=mock(CurrentActorApi.class);
    /**
     * 内容。
     */
    LearningContentAccess content=mock(LearningContentAccess.class);
    /**
     * tutorials。
     * 协作类型：<code>TutorialMapper</code>。
     */
    TutorialMapper tutorials=mock(TutorialMapper.class);
    /**
     * 数据映射。
     * 协作类型：<code>EvidenceLearningMapper</code>。
     */
    EvidenceLearningMapper mapper=mock(EvidenceLearningMapper.class);
    /**
     * 进度。
     * 协作类型：<code>LearningProgressMapper</code>。
     */
    LearningProgressMapper progress=mock(LearningProgressMapper.class);
    /**
     * plans。
     * 协作类型：<code>EvidenceStudyPlanService</code>。
     */
    EvidenceStudyPlanService plans=mock(EvidenceStudyPlanService.class);
    /**
     * 事件集合。
     */
    LearningEventWriter events=mock(LearningEventWriter.class);
    /**
     * 业务服务。
     * 协作类型：<code>EvidenceLearningService</code>。
     */
    EvidenceLearningService service=new EvidenceLearningService(actors,content,tutorials,mapper,progress,plans,events,new ObjectMapper());
    /**
     * 当前业务使用的会话对象。
     */
    Session session;
    /**
     * item。
     */
    Item item;
    /**
     * setup处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>session.setId</code>、<code>session.setAccountId</code>、<code>session.setSessionType</code>、<code>session.setStatus</code>。
     */
    @BeforeEach void setup() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(42L).build());
        session=new Session();session.setId(5L);session.setAccountId(42L);session.setSessionType("REVIEW");session.setStatus("IN_PROGRESS");
        item=new Item();item.setKnowledgeCardId(9L);item.setStatus("PENDING");item.setCardContentVersion(1);item.setSourcePriority(3);
        item.setTutorialId(1L);item.setChapterId(2L);item.setBackMarkdown("secret");
        when(mapper.session(42L,5L)).thenReturn(session);when(mapper.items(42L,5L)).thenAnswer(i->new ArrayList<>(List.of(item)));
    }
    /**
     * foreign会话是否不Readable或Writable处理入口。
     * 主要协作调用：<code>service.session</code>、<code>service.reveal</code>。
     */
    @Test void foreignSessionIsNotReadableOrWritable() {
        assertThrows(LearningResourceNotFoundException.class,()->service.session(999L));
        assertThrows(LearningResourceNotFoundException.class,()->service.reveal(999L,9L,false));
        verify(mapper,never()).revealItem(any(),any(),any());
    }
    /**
     * server拒绝评分之前Reveal处理入口。
     * 主要协作调用：<code>service.rate</code>。
     */
    @Test void serverRejectsRatingBeforeReveal() {
        EvidenceRatingDTO request=new EvidenceRatingDTO();request.setRating("REMEMBERED");
        assertThrows(LearningStateConflictException.class,()->service.rate(5L,9L,request,false));
        verify(mapper,never()).insertEvidence(any());
    }
    /**
     * 读取会话HidesUnrevealedBack。
     * 主要协作调用：<code>service.session</code>。
     */
    @Test void getSessionHidesUnrevealedBack() { assertNull(service.session(5L).getItems().get(0).getBackMarkdown()); }
    /**
     * 已完成评分重试Does不创建Another证据处理入口。
     * 主要协作调用：<code>item.setStatus</code>、<code>session.setStatus</code>、<code>service.rate</code>。
     */
    @Test void completedRatingRetryDoesNotCreateAnotherEvidence() {
        item.setStatus("COMPLETED");session.setStatus("COMPLETED");
        EvidenceRatingDTO request=new EvidenceRatingDTO();request.setRating("FORGOT");
        service.rate(5L,9L,request,false);verify(mapper,never()).insertEvidence(any());
    }
    /**
     * new审核写入Immutable证据及Downgrades来源RecentThree处理入口。
     * 主要协作调用：<code>item.setRevealedAt</code>、<code>mapper.cardEvidence</code>、<code>service.rate</code>。
     */
    @Test void newReviewWritesImmutableEvidenceAndDowngradesFromRecentThree() {
        item.setRevealedAt(LocalDateTime.now());
        List<Evidence> history=new ArrayList<>();for(int i=0;i<3;i++) { Evidence e=new Evidence();e.setRating("REMEMBERED");e.setCardContentVersion(1);e.setCreatedAt(LocalDateTime.now().minusDays(i+1));history.add(e); }
        when(mapper.cardEvidence(42L,9L)).thenReturn(history);
        EvidenceRatingDTO request=new EvidenceRatingDTO();request.setRating("FORGOT");service.rate(5L,9L,request,false);
        ArgumentCaptor<Evidence> evidence=ArgumentCaptor.forClass(Evidence.class);verify(mapper).insertEvidence(evidence.capture());
        assertEquals("STABLE_MASTERED",evidence.getValue().getPreviousMasteryStatus());assertEquals("BASIC_MASTERED",evidence.getValue().getMasteryStatus());
        ArgumentCaptor<Mastery> mastery=ArgumentCaptor.forClass(Mastery.class);verify(mapper).upsertMastery(mastery.capture());
        assertEquals(4,mastery.getValue().getEvidenceCount());assertEquals(4,mastery.getValue().getRecentScore());
    }
    /**
     * 首次答案及引用AreBoth账号Scoped处理入口。
     * 主要协作调用：<code>content.question</code>、<code>service.reference</code>、<code>service.saveVersion</code>。
     */
    @Test void firstAnswerAndReferenceAreBothAccountScoped() {
        when(content.question(9L)).thenReturn(LearningContentAccess.QuestionRef.builder().referenceAnswer("reference").build());
        assertThrows(LearningAnswerLockedException.class,()->service.reference(9L));
        AnswerVersionDTO request=new AnswerVersionDTO();request.setAnswerText("answer");request.setAnswerPhase("AFTER_REFERENCE");
        assertThrows(LearningAnswerLockedException.class,()->service.saveVersion(9L,request));verify(mapper,never()).insertAnswerVersion(any());
    }
    /**
     * 按阅读Alone读取initial完成CannotBeEstablished。
     * 主要协作调用：<code>content.chapter</code>、<code>service.completeChapter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void initialCompletionCannotBeEstablishedByReadingAlone() throws Exception {
        when(content.chapter(2L)).thenReturn(LearningContentAccess.ChapterRef.builder().chapterId(2L)
                .snapshot(new ObjectMapper().readTree("{\"cards\":[{\"id\":9}],\"questions\":[]}")).build());
        assertThrows(LearningStateConflictException.class,()->service.completeChapter(2L));verify(progress,never()).markCompleted(any(),any());
    }
    /**
     * 卡片集合不含之前引用答案StillDo不Complete章节处理入口。
     * 主要协作调用：<code>content.chapter</code>、<code>mapper.initialCardIds</code>、<code>service.completeChapter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void cardsWithoutBeforeReferenceAnswerStillDoNotCompleteChapter() throws Exception {
        when(content.chapter(2L)).thenReturn(LearningContentAccess.ChapterRef.builder().chapterId(2L)
                .snapshot(new ObjectMapper().readTree("{\"cards\":[{\"id\":9}],\"questions\":[{\"id\":10}]}")).build());
        when(mapper.initialCardIds(42L,2L)).thenReturn(List.of(9L));
        assertThrows(LearningStateConflictException.class,()->service.completeChapter(2L));verify(progress,never()).markCompleted(any(),any());
    }
    /**
     * parallel审核尝试RequiresContinuing或AbandoningExisting会话处理入口。
     * 主要协作调用：<code>mapper.currentReview</code>、<code>service.review</code>。
     */
    @Test void parallelReviewAttemptRequiresContinuingOrAbandoningExistingSession() {
        when(mapper.currentReview(42L)).thenReturn(session);
        assertThrows(LearningStateConflictException.class,()->service.review(new ReviewSessionDTO()));verify(mapper,never()).insertSession(any());
    }
}
