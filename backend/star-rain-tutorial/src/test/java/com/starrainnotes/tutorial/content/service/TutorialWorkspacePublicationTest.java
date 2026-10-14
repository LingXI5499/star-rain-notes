/*
 * TutorialWorkspacePublicationTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、ReviewSubmissionApi、TutorialEventPublisher、TutorialMediaReferences、TutorialPublicationServiceImpl。
 * 阅读入口：setup（setup处理入口）；chapter（章节处理入口）；candidate（candidate处理入口）；withdrawalCopiesPublicVersionAndDoesNotPublishUnfinishedDrafts（withdrawalCopies公开版本及Does不发布UnfinishedDrafts处理入口）。
 */

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

/**
 * 教程Workspace发布测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialWorkspacePublicationTest {
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    final ObjectMapper json = new ObjectMapper();
    /**
     * tutorials。
     * 协作类型：<code>TutorialMapper</code>。
     */
    final TutorialMapper tutorials = mock(TutorialMapper.class);
    /**
     * 修订集合。
     * 协作类型：<code>TutorialRevisionMapper</code>。
     */
    final TutorialRevisionMapper revisions = mock(TutorialRevisionMapper.class);
    /**
     * 章节列表。
     * 协作类型：<code>TutorialChapterMapper</code>。
     */
    final TutorialChapterMapper chapters = mock(TutorialChapterMapper.class);
    /**
     * groups。
     * 协作类型：<code>TutorialGroupMapper</code>。
     */
    final TutorialGroupMapper groups = mock(TutorialGroupMapper.class);
    /**
     * categories。
     * 协作类型：<code>TutorialCategoryMapper</code>。
     */
    final TutorialCategoryMapper categories = mock(TutorialCategoryMapper.class);
    /**
     * 卡片集合。
     * 协作类型：<code>TutorialKnowledgeCardMapper</code>。
     */
    final TutorialKnowledgeCardMapper cards = mock(TutorialKnowledgeCardMapper.class);
    /**
     * 问题集合。
     * 协作类型：<code>TutorialQuestionMapper</code>。
     */
    final TutorialQuestionMapper questions = mock(TutorialQuestionMapper.class);
    /**
     * relations。
     * 协作类型：<code>TutorialQuestionCardMapper</code>。
     */
    final TutorialQuestionCardMapper relations = mock(TutorialQuestionCardMapper.class);
    /**
     * 事件集合。
     */
    final TutorialEventPublisher events = mock(TutorialEventPublisher.class);
    /**
     * 媒体。
     */
    final TutorialMediaReferences media = mock(TutorialMediaReferences.class);
    /**
     * reviews。
     * 协作类型：<code>ReviewSubmissionApi</code>。
     */
    final ReviewSubmissionApi reviews = mock(ReviewSubmissionApi.class);
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    final CurrentActorApi actors = mock(CurrentActorApi.class);
    /**
     * 教程。
     */
    final TutorialEntity tutorial = new TutorialEntity();
    /**
     * old。
     */
    final TutorialRevisionEntity old = new TutorialRevisionEntity();
    /**
     * 业务服务。
     */
    TutorialPublicationServiceImpl service;
    /**
     * setup处理入口。
     * 主要协作调用：<code>tutorial.setId</code>、<code>tutorial.setSlug</code>、<code>tutorial.setTitle</code>、<code>tutorial.setCategoryId</code>、<code>tutorial.setPublicationStatus</code>。
     */
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
    /**
     * 章节处理入口。
     * 关键处理步骤调用 <code>row.setId</code>、<code>row.setTutorialId</code>、<code>row.setGroupId</code>、<code>row.setSlug</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param body 正文；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>TutorialChapterEntity</code> 的处理结果
     */
    private TutorialChapterEntity chapter(Long id,String slug,String body,String status) {
        var row = new TutorialChapterEntity();row.setId(id);row.setTutorialId(1L);row.setGroupId(3L);
        row.setSlug(slug);row.setTitle("Workspace "+slug);row.setBodyMarkdown(body);row.setStatus(status);return row;
    }
    /**
     * candidate处理入口。
     * 主要协作调用：<code>json.readTree</code>。
     * @return 类型为 <code>JsonNode</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private JsonNode candidate() throws Exception {
        var capture=ArgumentCaptor.forClass(TutorialRevisionEntity.class);verify(revisions).insert(capture.capture());
        return json.readTree(capture.getValue().getSnapshotJson());
    }
    /**
     * withdrawalCopies公开版本及Does不发布UnfinishedDrafts处理入口。
     * 主要协作调用：<code>old.getSnapshotJson</code>、<code>service.withdrawChapter</code>、<code>service.publicChapter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void withdrawalCopiesPublicVersionAndDoesNotPublishUnfinishedDrafts() throws Exception {
        String oldJson=old.getSnapshotJson(); service.withdrawChapter(4L);
        JsonNode result=candidate();
        assertThat(result.path("title").asText()).isEqualTo("Public title");
        assertThat(result.path("chapterCount").asInt()).isEqualTo(1);
        assertThat(result.path("groups").get(0).path("chapters").get(0).path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        assertThat(old.getSnapshotJson()).isEqualTo(oldJson);
        assertThatThrownBy(() -> service.publicChapter("java","one")).isInstanceOf(TutorialNotFoundException.class);
        assertThat(service.publicChapter("java","two").path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        verify(media).attachRevision(eq(11L),any());verify(events).publish(any());
        verify(chapters,never()).deleteById(any());
    }
    /**
     * 章节发布OnlyReplaces目标及保留公开元数据处理入口。
     * 主要协作调用：<code>service.publishChapter</code>、<code>service.publicChapter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void chapterPublicationOnlyReplacesTargetAndKeepsPublicMetadata() throws Exception {
        service.publishChapter(4L);JsonNode result=candidate();
        assertThat(result.path("chapterCount").asInt()).isEqualTo(2);
        assertThat(result.path("title").asText()).isEqualTo("Public title");
        assertThat(service.publicChapter("java","one").path("bodyMarkdown").asText()).isEqualTo("New body");
        assertThat(service.publicChapter("java","two").path("bodyMarkdown").asText()).isEqualTo("untouched public body");
        assertThatThrownBy(() -> service.publicChapter("java","draft")).isInstanceOf(TutorialNotFoundException.class);
    }
    /**
     * saving已发布内容RefreshesIt及保留New章节列表AsDrafts处理入口。
     * 主要协作调用：<code>old.getSnapshotJson</code>、<code>service.refreshPublished</code>、<code>service.publicChapter</code>、<code>service.publicTutorial</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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
    /**
     * newly创建教程Can发布不含章节列表处理入口。
     * 主要协作调用：<code>tutorial.setPublicationStatus</code>、<code>tutorial.setPublishedRevisionId</code>、<code>groups.listActiveByTutorialId</code>、<code>service.publish</code>、<code>tutorial.getPublicationStatus</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void newlyCreatedTutorialCanPublishWithoutChapters() throws Exception {
        tutorial.setPublicationStatus("NEVER_PUBLISHED");
        tutorial.setPublishedRevisionId(null);
        when(groups.listActiveByTutorialId(1L)).thenReturn(List.of());
        service.publish(1L);
        assertThat(candidate().path("chapterCount").asInt()).isZero();
        assertThat(tutorial.getPublicationStatus()).isEqualTo("PUBLISHED");
        verify(events).publish(any());
    }
    /**
     * 草稿SaveDoes不发布It处理入口。
     * 主要协作调用：<code>tutorial.setPublicationStatus</code>、<code>service.refreshPublished</code>。
     */
    @Test void draftSaveDoesNotPublishIt() {
        tutorial.setPublicationStatus("NEVER_PUBLISHED");
        service.refreshPublished(1L, "updateChapter", 4L);
        verify(revisions,never()).insert(any());
        verifyNoInteractions(events);
    }
    /**
     * 预览Includes草稿While公开StillReadsOld修订。
     * 主要协作调用：<code>service.previewTutorial</code>、<code>service.publicChapter</code>。
     */
    @Test void previewIncludesDraftWhilePublicStillReadsOldRevision() {
        assertThat(service.previewTutorial(1L).path("groups").get(0).path("chapters").size()).isEqualTo(3);
        assertThat(service.publicChapter("java","one").path("bodyMarkdown").asText()).isEqualTo("old body");
        verifyNoInteractions(events);
    }
    /**
     * 审核FreezesCandidate不含Switching公开版本处理入口。
     * 主要协作调用：<code>service.submitReview</code>、<code>tutorial.getPublishedRevisionId</code>、<code>tutorial.getEditingStatus</code>、<code>service.withdrawChapter</code>、<code>service.publishChapter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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
    /**
     * approvalPublishesTheFrozenCandidate及Finishes审核处理入口。
     * 主要协作调用：<code>service.submitReview</code>、<code>revisions.selectByTutorialIdAndRef</code>、<code>revisions.selectLatestByTutorialId</code>、<code>tutorial.getEditingStatus</code>、<code>service.publicChapter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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
        verify(events).publish(any());
        var annotation=handler.getClass().getMethod("onApproved",com.starrainnotes.review.api.ReviewDecisionContext.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }

}
