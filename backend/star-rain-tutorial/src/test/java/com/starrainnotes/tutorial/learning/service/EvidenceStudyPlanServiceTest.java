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

class EvidenceStudyPlanServiceTest {
    CurrentActorApi actors=mock(CurrentActorApi.class);LearningContentAccess content=mock(LearningContentAccess.class);
    EvidenceLearningMapper mapper=mock(EvidenceLearningMapper.class);EvidenceStudyPlanService service=new EvidenceStudyPlanService(actors,content,mapper);
    EvidencePlanDTO request;
    @BeforeEach void setup() throws Exception {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(42L).build());
        when(content.publishedTutorial(1L)).thenReturn(new ObjectMapper().readTree("""
                {"groups":[{"id":"10","chapters":[{"id":"20","title":"A","cards":[{},{},{},{},{},{},{}],"questions":[]},{"id":"21","title":"B","cards":[{},{},{},{},{}],"questions":[]}]}]}
                """));
        request=new EvidencePlanDTO();request.setTutorialId(1L);request.setName("目标");request.setGroupIds(List.of(10L));request.setChapterIds(List.of(20L));
    }
    @Test void groupPlusChapterExpandsOnlyOnceAndPreviewIsDeterministic() {
        Plan plan=service.preview(request,null);assertEquals(2,plan.getChapters().size());assertEquals(1,plan.getTasks().size());assertEquals(12,plan.getTasks().get(0).getCardCount());
        assertEquals(plan.getTasks().get(0).getCardCount(),service.preview(request,null).getTasks().get(0).getCardCount());
    }
    @Test void sixthUnendedPlanIsRejectedBeforeWriting() {
        List<Plan> plans=java.util.stream.IntStream.range(0,5).mapToObj(i->{Plan plan=new Plan();plan.setStatus(i%2==0?"ACTIVE":"DRAFT");return plan;}).toList();
        when(mapper.listPlans(42L)).thenReturn(plans);
        assertThrows(LearningStateConflictException.class,()->service.create(request));verify(mapper,never()).insertPlan(any());
    }
    @Test void completedAndCancelledPlansReleaseQuota() {
        Plan complete=new Plan();complete.setStatus("COMPLETED");Plan cancelled=new Plan();cancelled.setStatus("CANCELLED");
        when(mapper.listPlans(42L)).thenReturn(List.of(complete,cancelled));
        doAnswer(i->{Plan row=i.getArgument(0);row.setId(99L);when(mapper.plan(42L,99L)).thenReturn(row);return 1;}).when(mapper).insertPlan(any());
        assertEquals("DRAFT",service.create(request).getStatus());verify(mapper).insertPlan(any());
    }
    @Test void activePlanDoesNotBlockAnotherActivation() {
        Plan draft=new Plan();draft.setId(99L);draft.setStatus("DRAFT");draft.setAccountId(42L);
        when(mapper.plan(42L,99L)).thenReturn(draft);when(mapper.tasks(42L,99L)).thenReturn(List.of(new Task()));
        service.transition(99L,"activate");verify(mapper).planStatus(42L,99L,"ACTIVE");
    }
    @Test void crossTutorialChapterAndDuplicateUnendedScopeAreRejected() {
        request.setChapterIds(List.of(999L));assertThrows(LearningInvalidRequestException.class,()->service.preview(request,null));
        request.setChapterIds(List.of(20L));when(mapper.conflictingChapters(42L,null)).thenReturn(List.of(20L));
        assertThrows(LearningStateConflictException.class,()->service.preview(request,null));
    }
    @Test void foreignPlanIsHiddenAndActiveScopeCannotBeEdited() {
        assertThrows(LearningResourceNotFoundException.class,()->service.get(999L));
        Plan active=new Plan();active.setStatus("ACTIVE");when(mapper.plan(42L,99L)).thenReturn(active);
        assertThrows(LearningStateConflictException.class,()->service.update(99L,request));
    }
}
