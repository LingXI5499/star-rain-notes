/*
 * EvidenceStudyPlanServiceTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、EvidencePlanDTO、EvidenceLearningMapper。
 * 阅读入口：setup（setup处理入口）；groupPlusChapterExpandsOnlyOnceAndPreviewIsDeterministic（分组Plus章节ExpandsOnlyOnce及预览是否Deterministic处理入口）；sixthUnendedPlanIsRejectedBeforeWriting（sixthUnended计划是否已拒绝之前写作处理入口）；completedAndCancelledPlansReleaseQuota（已完成及CancelledPlansReleaseQuota处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.dto.EvidencePlanDTO;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.exception.*;
import com.starrainnotes.tutorial.learning.mapper.EvidenceLearningMapper;
import com.starrainnotes.tutorial.learning.service.impl.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 证据学习计划业务服务测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EvidenceStudyPlanServiceTest {
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
     * 数据映射。
     * 协作类型：<code>EvidenceLearningMapper</code>。
     */
    EvidenceLearningMapper mapper=mock(EvidenceLearningMapper.class);
    /**
     * 业务服务。
     * 协作类型：<code>EvidenceStudyPlanService</code>。
     */
    EvidenceStudyPlanService service=new EvidenceStudyPlanService(actors,content,mapper);
    /**
     * 本次操作的请求参数对象。
     */
    EvidencePlanDTO request;
    /**
     * setup处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>content.publishedTutorial</code>、<code>request.setTutorialId</code>、<code>request.setName</code>、<code>request.setGroupIds</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @BeforeEach void setup() throws Exception {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(42L).build());
        when(content.publishedTutorial(1L)).thenReturn(new ObjectMapper().readTree("""
                {"groups":[{"id":"10","chapters":[{"id":"20","title":"A","cards":[{},{},{},{},{},{},{}],"questions":[]},{"id":"21","title":"B","cards":[{},{},{},{},{}],"questions":[]}]}]}
                """));
        request=new EvidencePlanDTO();request.setTutorialId(1L);request.setName("目标");request.setGroupIds(List.of(10L));request.setChapterIds(List.of(20L));
    }
    /**
     * 分组Plus章节ExpandsOnlyOnce及预览是否Deterministic处理入口。
     * 主要协作调用：<code>service.preview</code>。
     */
    @Test void groupPlusChapterExpandsOnlyOnceAndPreviewIsDeterministic() {
        Plan plan=service.preview(request,null);assertEquals(2,plan.getChapters().size());assertEquals(1,plan.getTasks().size());assertEquals(12,plan.getTasks().get(0).getCardCount());
        assertEquals(plan.getTasks().get(0).getCardCount(),service.preview(request,null).getTasks().get(0).getCardCount());
    }
    /**
     * sixthUnended计划是否已拒绝之前写作处理入口。
     * 主要协作调用：<code>mapper.listPlans</code>、<code>service.create</code>。
     */
    @Test void sixthUnendedPlanIsRejectedBeforeWriting() {
        List<Plan> plans=java.util.stream.IntStream.range(0,5).mapToObj(i->{Plan plan=new Plan();plan.setStatus(i%2==0?"ACTIVE":"DRAFT");return plan;}).toList();
        when(mapper.listPlans(42L)).thenReturn(plans);
        assertThrows(LearningStateConflictException.class,()->service.create(request));verify(mapper,never()).insertPlan(any());
    }
    /**
     * 已完成及CancelledPlansReleaseQuota处理入口。
     * 主要协作调用：<code>mapper.listPlans</code>、<code>mapper.plan</code>、<code>service.create</code>。
     */
    @Test void completedAndCancelledPlansReleaseQuota() {
        Plan complete=new Plan();complete.setStatus("COMPLETED");Plan cancelled=new Plan();cancelled.setStatus("CANCELLED");
        when(mapper.listPlans(42L)).thenReturn(List.of(complete,cancelled));
        doAnswer(i->{Plan row=i.getArgument(0);row.setId(99L);when(mapper.plan(42L,99L)).thenReturn(row);return 1;}).when(mapper).insertPlan(any());
        assertEquals("DRAFT",service.create(request).getStatus());verify(mapper).insertPlan(any());
    }
    /**
     * 有效计划Does不区块AnotherActivation处理入口。
     * 主要协作调用：<code>mapper.plan</code>、<code>mapper.tasks</code>、<code>service.transition</code>。
     */
    @Test void activePlanDoesNotBlockAnotherActivation() {
        Plan draft=new Plan();draft.setId(99L);draft.setStatus("DRAFT");draft.setAccountId(42L);
        when(mapper.plan(42L,99L)).thenReturn(draft);when(mapper.tasks(42L,99L)).thenReturn(List.of(new Task()));
        service.transition(99L,"activate");verify(mapper).planStatus(42L,99L,"ACTIVE");
    }
    /**
     * cross教程章节及DuplicateUnendedScopeAre已拒绝处理入口。
     * 主要协作调用：<code>request.setChapterIds</code>、<code>service.preview</code>、<code>mapper.conflictingChapters</code>。
     */
    @Test void crossTutorialChapterAndDuplicateUnendedScopeAreRejected() {
        request.setChapterIds(List.of(999L));assertThrows(LearningInvalidRequestException.class,()->service.preview(request,null));
        request.setChapterIds(List.of(20L));when(mapper.conflictingChapters(42L,null)).thenReturn(List.of(20L));
        assertThrows(LearningStateConflictException.class,()->service.preview(request,null));
    }
    /**
     * foreign计划是否Hidden及有效ScopeCannotBeEdited处理入口。
     * 主要协作调用：<code>service.get</code>、<code>mapper.plan</code>、<code>service.update</code>。
     */
    @Test void foreignPlanIsHiddenAndActiveScopeCannotBeEdited() {
        assertThrows(LearningResourceNotFoundException.class,()->service.get(999L));
        Plan active=new Plan();active.setStatus("ACTIVE");when(mapper.plan(42L,99L)).thenReturn(active);
        assertThrows(LearningStateConflictException.class,()->service.update(99L,request));
    }
}
