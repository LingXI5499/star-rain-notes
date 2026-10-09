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

class EvidenceLearningServiceTest {
    CurrentActorApi actors=mock(CurrentActorApi.class);
    LearningContentAccess content=mock(LearningContentAccess.class);
    TutorialMapper tutorials=mock(TutorialMapper.class);
    EvidenceLearningMapper mapper=mock(EvidenceLearningMapper.class);
    LearningProgressMapper progress=mock(LearningProgressMapper.class);
    EvidenceStudyPlanService plans=mock(EvidenceStudyPlanService.class);
    LearningEventWriter events=mock(LearningEventWriter.class);
    EvidenceLearningService service=new EvidenceLearningService(actors,content,tutorials,mapper,progress,plans,events,new ObjectMapper());
    Session session;
    Item item;
    @BeforeEach void setup() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(42L).build());
        session=new Session();session.setId(5L);session.setAccountId(42L);session.setSessionType("REVIEW");session.setStatus("IN_PROGRESS");
        item=new Item();item.setKnowledgeCardId(9L);item.setStatus("PENDING");item.setCardContentVersion(1);item.setSourcePriority(3);
        item.setTutorialId(1L);item.setChapterId(2L);item.setBackMarkdown("secret");
        when(mapper.session(42L,5L)).thenReturn(session);when(mapper.items(42L,5L)).thenAnswer(i->new ArrayList<>(List.of(item)));
    }
    @Test void foreignSessionIsNotReadableOrWritable() {
        assertThrows(LearningResourceNotFoundException.class,()->service.session(999L));
        assertThrows(LearningResourceNotFoundException.class,()->service.reveal(999L,9L,false));
        verify(mapper,never()).revealItem(any(),any(),any());
    }
    @Test void serverRejectsRatingBeforeReveal() {
        EvidenceRatingDTO request=new EvidenceRatingDTO();request.setRating("REMEMBERED");
        assertThrows(LearningStateConflictException.class,()->service.rate(5L,9L,request,false));
        verify(mapper,never()).insertEvidence(any());
    }
    @Test void getSessionHidesUnrevealedBack() { assertNull(service.session(5L).getItems().get(0).getBackMarkdown()); }
    @Test void completedRatingRetryDoesNotCreateAnotherEvidence() {
        item.setStatus("COMPLETED");session.setStatus("COMPLETED");
        EvidenceRatingDTO request=new EvidenceRatingDTO();request.setRating("FORGOT");
        service.rate(5L,9L,request,false);verify(mapper,never()).insertEvidence(any());
    }
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
    @Test void firstAnswerAndReferenceAreBothAccountScoped() {
        when(content.question(9L)).thenReturn(LearningContentAccess.QuestionRef.builder().referenceAnswer("reference").build());
        assertThrows(LearningAnswerLockedException.class,()->service.reference(9L));
        AnswerVersionDTO request=new AnswerVersionDTO();request.setAnswerText("answer");request.setAnswerPhase("AFTER_REFERENCE");
        assertThrows(LearningAnswerLockedException.class,()->service.saveVersion(9L,request));verify(mapper,never()).insertAnswerVersion(any());
    }
    @Test void initialCompletionCannotBeEstablishedByReadingAlone() throws Exception {
        when(content.chapter(2L)).thenReturn(LearningContentAccess.ChapterRef.builder().chapterId(2L)
                .snapshot(new ObjectMapper().readTree("{\"cards\":[{\"id\":9}],\"questions\":[]}")).build());
        assertThrows(LearningStateConflictException.class,()->service.completeChapter(2L));verify(progress,never()).markCompleted(any(),any());
    }
    @Test void cardsWithoutBeforeReferenceAnswerStillDoNotCompleteChapter() throws Exception {
        when(content.chapter(2L)).thenReturn(LearningContentAccess.ChapterRef.builder().chapterId(2L)
                .snapshot(new ObjectMapper().readTree("{\"cards\":[{\"id\":9}],\"questions\":[{\"id\":10}]}")).build());
        when(mapper.initialCardIds(42L,2L)).thenReturn(List.of(9L));
        assertThrows(LearningStateConflictException.class,()->service.completeChapter(2L));verify(progress,never()).markCompleted(any(),any());
    }
    @Test void parallelReviewAttemptRequiresContinuingOrAbandoningExistingSession() {
        when(mapper.currentReview(42L)).thenReturn(session);
        assertThrows(LearningStateConflictException.class,()->service.review(new ReviewSessionDTO()));verify(mapper,never()).insertSession(any());
    }
}
