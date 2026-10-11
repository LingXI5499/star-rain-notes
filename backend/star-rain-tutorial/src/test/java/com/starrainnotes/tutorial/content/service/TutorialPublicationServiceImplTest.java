/*
 * TutorialPublicationServiceImplTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、TutorialEntity、TutorialRevisionEntity、TutorialEventPublisher、TutorialCategoryMapper、TutorialChapterMapper、TutorialGroupMapper。
 * 阅读入口：publicResponsesKeepRevisionFrozenAndDoNotExposeReferenceAnswers（公开ResponsesKeep修订Frozen及Do不Expose引用答案集合处理入口）。
 */

package com.starrainnotes.tutorial.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.content.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialGroupMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.content.service.impl.TutorialPublicationServiceImpl;
import org.junit.jupiter.api.Test;

/**
 * 教程发布业务服务实现测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialPublicationServiceImplTest {
    /**
     * 公开ResponsesKeep修订Frozen及Do不Expose引用答案集合处理入口。
     * 关键处理步骤调用 <code>tutorial.setId</code>、<code>tutorial.setSlug</code>、<code>tutorial.setPublicationStatus</code>、<code>tutorial.setPublishedRevisionId</code>。
     */
    @Test
    void publicResponsesKeepRevisionFrozenAndDoNotExposeReferenceAnswers() {
        TutorialMapper tutorialMapper = mock(TutorialMapper.class);
        TutorialRevisionMapper revisionMapper = mock(TutorialRevisionMapper.class);
        TutorialEntity tutorial = new TutorialEntity();
        tutorial.setId(7L);
        tutorial.setSlug("java");
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setPublishedRevisionId(9L);
        when(tutorialMapper.selectPublishedBySlug("java")).thenReturn(tutorial);
        TutorialRevisionEntity revision = new TutorialRevisionEntity();
        revision.setSnapshotJson("""
                {"id":"7","slug":"java","title":"公开标题","groups":[{"id":"11","title":"第一部分","chapters":[{"id":"21","slug":"intro","title":"介绍","bodyMarkdown":"# 公开正文","cards":[{"id":"31","frontText":"正面","backMarkdown":"背面"}],"questions":[{"id":"41","questionText":"问题","referenceAnswer":"参考答案"}]}]}]}
                """);
        when(revisionMapper.selectById(9L)).thenReturn(revision);
        TutorialPublicationServiceImpl service = new TutorialPublicationServiceImpl(
                tutorialMapper, mock(TutorialCategoryMapper.class), mock(TutorialGroupMapper.class),
                mock(TutorialChapterMapper.class), mock(TutorialKnowledgeCardMapper.class),
                mock(TutorialQuestionMapper.class), mock(com.starrainnotes.tutorial.content.mapper.TutorialQuestionCardMapper.class), revisionMapper, mock(TutorialContentService.class),
                mock(CurrentActorApi.class), new ObjectMapper(),
                mock(TutorialEventPublisher.class), mock(TutorialMediaReferences.class), mock(com.starrainnotes.review.api.ReviewSubmissionApi.class));

        JsonNode detail = service.publicTutorial("java");
        JsonNode curriculumChapter = detail.path("groups").get(0).path("chapters").get(0);
        assertThat(curriculumChapter.has("bodyMarkdown")).isFalse();
        assertThat(curriculumChapter.has("questions")).isFalse();

        JsonNode publicChapter = service.publicChapter("java", "intro");
        assertThat(publicChapter.path("bodyMarkdown").asText()).isEqualTo("# 公开正文");
        assertThat(publicChapter.path("cards").get(0).path("frontText").asText()).isEqualTo("正面");
        assertThat(publicChapter.path("questions").get(0).has("referenceAnswer")).isFalse();

        assertThat(curriculumChapter.path("cardCount").asInt()).isEqualTo(1);
        assertThat(curriculumChapter.path("questionCount").asInt()).isEqualTo(1);
    }
}
