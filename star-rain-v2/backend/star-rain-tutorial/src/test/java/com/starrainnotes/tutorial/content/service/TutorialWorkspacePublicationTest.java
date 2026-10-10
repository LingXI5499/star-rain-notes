package com.starrainnotes.tutorial.content.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.starrainnotes.tutorial.content.entity.*;
import com.starrainnotes.tutorial.content.mapper.*;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.content.service.impl.TutorialPublicationServiceImpl;
import com.starrainnotes.tutorial.content.exception.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

class TutorialWorkspacePublicationTest {
    final ObjectMapper json = new ObjectMapper();
    final TutorialMapper tutorials = mock(TutorialMapper.class);
    final TutorialRevisionMapper revisions = mock(TutorialRevisionMapper.class);
    final TutorialChapterMapper chapters = mock(TutorialChapterMapper.class);
    final TutorialGroupMapper groups = mock(TutorialGroupMapper.class);
    final TutorialCategoryMapper categories = mock(TutorialCategoryMapper.class);
    final TutorialKnowledgeCardMapper cards = mock(TutorialKnowledgeCardMapper.class);
    final TutorialQuestionMapper questions = mock(TutorialQuestionMapper.class);
    final TutorialQuestionCardMapper relations = mock(TutorialQuestionCardMapper.class);
    final TutorialEventPublisher events = mock(TutorialEventPublisher.class);
    final TutorialMediaReferences media = mock(TutorialMediaReferences.class);
    final ReviewSubmissionApi reviews = mock(ReviewSubmissionApi.class);
    final CurrentActorApi actors = mock(CurrentActorApi.class);
    final TutorialEntity tutorial = new TutorialEntity();
    final TutorialRevisionEntity old = new TutorialRevisionEntity();
    TutorialPublicationServiceImpl service;
    @BeforeEach void setup() {
        tutorial.setId(1L); tutorial.setSlug("java"); tutorial.setTitle("Unfinished tutorial title");
        tutorial.setCategoryId(2L); tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setPublishedRevisionId(10L); tutorial.setEditingStatus("DRAFT");
        when(tutorials.byIdForUpdate(1L)).thenReturn(tutorial);
        when(tutorials.selectById(1L)).thenReturn(tutorial);
        when(tutorials.selectPublishedBySlug("java")).thenReturn(tutorial);
        old.setId(10L); old.setSnapshotJson("""
          {"id":"1","slug":"java","title":"Public title","chapterCount":2,"groups":[{"id":"3","title":"Public group","chapters":[
          {"id":"4","slug":"one","title":"Public one","bodyMarkdown":"old body","cards":[],"questions":[]},
          {"id":"5","slug":"two","title":"Public two","bodyMarkdown":"untouched public body","cards":[],"questions":[]}]}]}
          """);
        when(revisions.selectById(10L)).thenReturn(old);
        when(revisions.countByTutorialId(1L)).thenReturn(1L);
        when(revisions.insert(any())).thenAnswer(call -> {
            TutorialRevisionEntity row = call.getArgument(0); row.setId(11L);
            when(revisions.selectById(11L)).thenReturn(row); return 1;
        });
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(7L).build());
        TutorialCategoryEntity category = new TutorialCategoryEntity(); category.setId(2L); category.setName("Java"); category.setSlug("java");
        when(categories.selectById(2L)).thenReturn(category);
        TutorialGroupEntity group = new TutorialGroupEntity();group.setId(3L);group.setTutorialId(1L);group.setTitle("Draft group");
        when(groups.listByTutorialId(1L)).thenReturn(List.of(group));
        when(groups.listActiveByTutorialId(1L)).thenReturn(List.of(group));
        TutorialChapterEntity one = chapter(4L,"one","New body","PUBLISHED");
        TutorialChapterEntity two = chapter(5L,"two","Unfinished body","PUBLISHED");
        TutorialChapterEntity draft = chapter(6L,"draft","Draft only","DRAFT");
        when(chapters.listByGroupId(3L)).thenReturn(List.of(one,two,draft));
        for (var row : List.of(one,two,draft)) when(chapters.selectById(row.getId())).thenReturn(row);
        service = new TutorialPublicationServiceImpl(tutorials,categories,groups,chapters,cards,questions,relations,revisions,
                mock(TutorialContentService.class),actors,json,events,media,reviews);
    }
    private TutorialChapterEntity chapter(Long id,String slug,String body,String status) {
        var row = new TutorialChapterEntity();row.setId(id);row.setTutorialId(1L);row.setGroupId(3L);
        row.setSlug(slug);row.setTitle("Workspace "+slug);row.setBodyMarkdown(body);row.setStatus(status);return row;
    }
    private JsonNode candidate() throws Exception {
        var capture=ArgumentCaptor.forClass(TutorialRevisionEntity.class);verify(revisions).insert(capture.capture());
        return json.readTree(capture.getValue().getSnapshotJson());
    }
    @Test void withdrawalCopiesPublicVersionAndDoesNotPublishUnfinishedDrafts() throws Exception {
        String oldJson=old.getSnapshotJson(); service.withdrawChapter(4L);
        JsonNode result=candidate();
        assertThat(result.path("title").asText()).isEqualTo("Public title");
        assertThat(result.path("chapterCount").asInt()).isEqualTo(1);
        assertThat(result.path("groups").get(0).path("chapters").get(0).path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        assertThat(old.getSnapshotJson()).isEqualTo(oldJson);
        assertThatThrownBy(() -> service.publicChapter("java","one")).isInstanceOf(TutorialNotFoundException.class);
        assertThat(service.publicChapter("java","two").path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        verify(media).attachRevision(eq(11L),any());verify(events).afterCommit(any());
        verify(chapters,never()).deleteById(any());
    }
    @Test void chapterPublicationOnlyReplacesTargetAndKeepsPublicMetadata() throws Exception {
        service.publishChapter(4L);JsonNode result=candidate();
        assertThat(result.path("chapterCount").asInt()).isEqualTo(2);
        assertThat(result.path("title").asText()).isEqualTo("Public title");
        assertThat(service.publicChapter("java","one").path("bodyMarkdown").asText()).isEqualTo("New body");
        assertThat(service.publicChapter("java","two").path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        assertThatThrownBy(() -> service.publicChapter("java","draft")).isInstanceOf(TutorialNotFoundException.class);
    }
    @Test void savingPublishedContentRefreshesItAndKeepsNewChaptersAsDrafts() throws Exception {
        String original = old.getSnapshotJson();
        service.refreshPublished(1L, "updateChapter", 4L);
        assertThat(service.publicChapter("java","one").path("bodyMarkdown").asText()).isEqualTo("New body");
        assertThat(service.publicTutorial("java").path("title").asText()).isEqualTo("Public title");
        assertThat(service.publicChapter("java","two").path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        assertThatThrownBy(() -> service.publicChapter("java","draft")).isInstanceOf(TutorialNotFoundException.class);
        assertThat(candidate().path("chapterCount").asInt()).isEqualTo(2);
        assertThat(old.getSnapshotJson()).isEqualTo(original);
    }
    @Test void newlyCreatedTutorialCanPublishWithoutChapters() throws Exception {
        tutorial.setPublicationStatus("NEVER_PUBLISHED");
        tutorial.setPublishedRevisionId(null);
        when(groups.listActiveByTutorialId(1L)).thenReturn(List.of());
        service.publish(1L);
        assertThat(candidate().path("chapterCount").asInt()).isZero();
        assertThat(tutorial.getPublicationStatus()).isEqualTo("PUBLISHED");
        verify(events).afterCommit(any());
    }
    @Test void draftSaveDoesNotPublishIt() {
        tutorial.setPublicationStatus("NEVER_PUBLISHED");
        service.refreshPublished(1L, "updateChapter", 4L);
        verify(revisions,never()).insert(any());
        verifyNoInteractions(events);
    }
    @Test void previewIncludesDraftWhilePublicStillReadsOldRevision() {
        assertThat(service.previewTutorial(1L).path("groups").get(0).path("chapters").size()).isEqualTo(3);
        assertThat(service.publicChapter("java","one").path("bodyMarkdown").asText()).isEqualTo("old body");
        verifyNoInteractions(events);
    }
    @Test void reviewFreezesCandidateWithoutSwitchingPublicVersion() throws Exception {
        service.submitReview(1L,4L);
        assertThat(tutorial.getPublishedRevisionId()).isEqualTo(10L);
        assertThat(tutorial.getEditingStatus()).isEqualTo("IN_REVIEW");
        assertThat(candidate().path("chapterCount").asInt()).isEqualTo(2);
        verify(reviews).submit(argThat(command -> "tutorial:1:revision:2".equals(command.getTargetRevisionRef())));
        verifyNoInteractions(events);
        assertThatThrownBy(() -> service.withdrawChapter(4L)).isInstanceOf(TutorialStateException.class);
        assertThatThrownBy(() -> service.publishChapter(4L)).isInstanceOf(TutorialStateException.class);
    }
    @Test void approvalPublishesTheFrozenCandidateAndFinishesReview() throws Exception {
        service.submitReview(1L,4L);
        var capture=ArgumentCaptor.forClass(TutorialRevisionEntity.class);verify(revisions).insert(capture.capture());
        var candidate=capture.getValue();
        when(revisions.selectByTutorialIdAndRef(1L,candidate.getRevisionRef())).thenReturn(candidate);
        when(revisions.selectLatestByTutorialId(1L)).thenReturn(candidate);
        var handler=new com.starrainnotes.tutorial.content.handler.TutorialReviewTargetHandler(tutorials,revisions,json,events,chapters);
        var target=com.starrainnotes.review.api.ReviewTargetRef.builder().targetModule("TUTORIAL").targetType("TUTORIAL").targetId(1L).revisionRef(candidate.getRevisionRef()).build();
        handler.onApproved(com.starrainnotes.review.api.ReviewDecisionContext.builder().target(target).build());
        assertThat(tutorial.getEditingStatus()).isEqualTo("DRAFT");
        assertThat(service.publicChapter("java","one").path("bodyMarkdown").asText()).isEqualTo("New body");
        assertThat(service.publicChapter("java","two").path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        verify(events).afterCommit(any());
        var annotation=handler.getClass().getMethod("onApproved",com.starrainnotes.review.api.ReviewDecisionContext.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }

}
