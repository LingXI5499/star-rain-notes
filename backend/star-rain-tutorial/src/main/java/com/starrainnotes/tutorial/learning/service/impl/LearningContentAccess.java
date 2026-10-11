/*
 * LearningContentAccess：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：TutorialChapterEntity、TutorialEntity、TutorialKnowledgeCardEntity、TutorialQuestionEntity、TutorialRevisionEntity、LearningInvalidRequestException、LearningResourceNotFoundException。
 * 阅读入口：publishedTutorial（已发布教程处理入口）；chapter（章节处理入口）；card（卡片处理入口）；question（问题处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialKnowledgeCardEntity;
import com.starrainnotes.tutorial.content.entity.TutorialQuestionEntity;
import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.learning.exception.LearningResourceNotFoundException;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 学习内容访问，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Component
@RequiredArgsConstructor
public class LearningContentAccess {
    /**
     * 教程数据映射。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorialMapper;
    /**
     * 修订数据映射。
     * 协作类型：<code>TutorialRevisionMapper</code>。
     */
    private final TutorialRevisionMapper revisionMapper;
    /**
     * 章节数据映射。
     * 协作类型：<code>TutorialChapterMapper</code>。
     */
    private final TutorialChapterMapper chapterMapper;
    /**
     * 卡片数据映射。
     * 协作类型：<code>TutorialKnowledgeCardMapper</code>。
     */
    private final TutorialKnowledgeCardMapper cardMapper;
    /**
     * 问题数据映射。
     * 协作类型：<code>TutorialQuestionMapper</code>。
     */
    private final TutorialQuestionMapper questionMapper;
    /**
     * 项目配置的 Jackson JSON 读写器。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper objectMapper;

    /**
     * 已发布教程处理入口。
     * 主要协作调用：<code>tutorialMapper.selectById</code>、<code>revisionMapper.selectById</code>、<code>objectMapper.readTree</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    public JsonNode publishedTutorial(Long tutorialId) {
        TutorialEntity tutorial = tutorialId == null ? null : tutorialMapper.selectById(tutorialId);
        if (tutorial == null || !"PUBLISHED".equals(tutorial.getPublicationStatus())
                || tutorial.getPublishedRevisionId() == null) {
            throw new LearningResourceNotFoundException();
        }
        TutorialRevisionEntity revision = revisionMapper.selectById(tutorial.getPublishedRevisionId());
        if (revision == null) {
            throw new LearningResourceNotFoundException();
        }
        try {
            return objectMapper.readTree(revision.getSnapshotJson());
        } catch (JsonProcessingException exception) {
            throw new LearningResourceNotFoundException();
        }
    }

    /**
     * 章节处理入口。
     * 主要协作调用：<code>chapterMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ChapterRef</code> 的处理结果
     */
    public ChapterRef chapter(Long chapterId) {
        TutorialChapterEntity row = chapterId == null ? null : chapterMapper.selectById(chapterId);
        if (row == null) {
            throw new LearningResourceNotFoundException();
        }
        JsonNode tutorial = publishedTutorial(row.getTutorialId());
        for (JsonNode group : tutorial.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                if (String.valueOf(chapterId).equals(chapter.path("id").asText())) {
                    return ChapterRef.builder().tutorialId(row.getTutorialId())
                            .groupId(Long.valueOf(group.path("id").asText()))
                            .chapterId(chapterId).tutorialSlug(tutorial.path("slug").asText())
                            .chapterSlug(chapter.path("slug").asText())
                            .title(chapter.path("title").asText()).snapshot(chapter).build();
                }
            }
        }
        throw new LearningResourceNotFoundException();
    }

    /**
     * 卡片处理入口。
     * 主要协作调用：<code>cardMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @return 类型为 <code>CardRef</code> 的处理结果
     */
    public CardRef card(Long cardId) {
        TutorialKnowledgeCardEntity row = cardId == null ? null : cardMapper.selectById(cardId);
        if (row == null) {
            PublishedChild published = publishedChild(cardId, "cards");
            return CardRef.builder().cardId(cardId).chapter(published.getChapter())
                    .frontText(published.getChild().path("frontText").asText())
                    .backMarkdown(published.getChild().path("backMarkdown").asText()).build();
        }
        ChapterRef chapter = chapter(row.getChapterId());
        for (JsonNode card : chapter.getSnapshot().path("cards")) {
            if (String.valueOf(cardId).equals(card.path("id").asText())) {
                return CardRef.builder().cardId(cardId).chapter(chapter)
                        .frontText(card.path("frontText").asText())
                        .backMarkdown(card.path("backMarkdown").asText()).build();
            }
        }
        throw new LearningResourceNotFoundException();
    }

    /**
     * 问题处理入口。
     * 主要协作调用：<code>questionMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>QuestionRef</code> 的处理结果
     */
    public QuestionRef question(Long questionId) {
        TutorialQuestionEntity row = questionId == null ? null : questionMapper.selectById(questionId);
        if (row == null) {
            PublishedChild published = publishedChild(questionId, "questions");
            return QuestionRef.builder().questionId(questionId).chapter(published.getChapter())
                    .referenceAnswer(published.getChild().path("referenceAnswer").asText()).build();
        }
        ChapterRef chapter = chapter(row.getChapterId());
        for (JsonNode question : chapter.getSnapshot().path("questions")) {
            if (String.valueOf(questionId).equals(question.path("id").asText())) {
                return QuestionRef.builder().questionId(questionId).chapter(chapter)
                        .referenceAnswer(question.path("referenceAnswer").asText()).build();
            }
        }
        throw new LearningResourceNotFoundException();
    }

    /* 项目禁止 record，这里用 Lombok POJO 保持一致。 */
    /**
     * 已发布Child，属于教程与学习模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    @Data
    @AllArgsConstructor
    private static class PublishedChild {
        /**
         * 章节。
         */
        private ChapterRef chapter;
        /**
         * child。
         */
        private JsonNode child;
    }

    // 删除工作区卡片或题目不会改写已发布快照。仅在原始行已删除时扫描公开修订，
    // 让尚未重新发布的读者内容仍可答题和复习。
    /**
     * 已发布Child处理入口。
     * 主要协作调用：<code>tutorialMapper.listPublished</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param childId child标识；类型为 <code>Long</code>
     * @param collection 集合；类型为 <code>String</code>
     * @return 类型为 <code>PublishedChild</code> 的处理结果
     */
    private PublishedChild publishedChild(Long childId, String collection) {
        if (childId == null) {
            throw new LearningResourceNotFoundException();
        }
        List<TutorialEntity> tutorials = tutorialMapper.listPublished();
        for (TutorialEntity row : tutorials) {
            JsonNode snapshot = publishedTutorial(row.getId());
            for (JsonNode group : snapshot.path("groups")) {
                for (JsonNode chapter : group.path("chapters")) {
                    for (JsonNode child : chapter.path(collection)) {
                        if (String.valueOf(childId).equals(child.path("id").asText())) {
                            ChapterRef ref = ChapterRef.builder().tutorialId(row.getId())
                                    .groupId(Long.valueOf(group.path("id").asText()))
                                    .chapterId(Long.valueOf(chapter.path("id").asText()))
                                    .tutorialSlug(snapshot.path("slug").asText())
                                    .chapterSlug(chapter.path("slug").asText())
                                    .title(chapter.path("title").asText()).snapshot(chapter).build();
                            return new PublishedChild(ref, child);
                        }
                    }
                }
            }
        }
        throw new LearningResourceNotFoundException();
    }

    /**
     * scoped章节标识集合处理入口。
     * 关键处理步骤调用 <code>publishedTutorial</code>、<code>String.valueOf</code>、<code>tutorial.path</code>、<code>group.path</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param scopeType scope类型；类型为 <code>String</code>
     * @param scopeId scope标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    public List<Long> scopedChapterIds(Long tutorialId, String scopeType, Long scopeId) {
        JsonNode tutorial = publishedTutorial(tutorialId);
        if (scopeId == null || scopeType == null) {
            throw new LearningInvalidRequestException("请选择计划范围");
        }
        List<Long> ids = new ArrayList<>();
        boolean matched = "TUTORIAL".equals(scopeType)
                && String.valueOf(scopeId).equals(tutorial.path("id").asText());
        for (JsonNode group : tutorial.path("groups")) {
            boolean groupSelected = "GROUP".equals(scopeType)
                    && String.valueOf(scopeId).equals(group.path("id").asText());
            matched |= groupSelected;
            for (JsonNode chapter : group.path("chapters")) {
                boolean chapterSelected = "CHAPTER".equals(scopeType)
                        && String.valueOf(scopeId).equals(chapter.path("id").asText());
                matched |= chapterSelected;
                if ("TUTORIAL".equals(scopeType) || groupSelected || chapterSelected) {
                    ids.add(Long.valueOf(chapter.path("id").asText()));
                }
            }
        }
        if (!matched || ids.isEmpty()) {
            throw new LearningInvalidRequestException("计划范围没有公开章节");
        }
        return ids;
    }

    /**
     * 章节列表处理入口。
     * 关键处理步骤调用 <code>publishedTutorial</code>、<code>tutorial.path</code>、<code>Long.valueOf</code>、<code>group.path</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    public List<ChapterRef> chapters(Long tutorialId) {
        JsonNode tutorial = publishedTutorial(tutorialId);
        List<ChapterRef> result = new ArrayList<>();
        for (JsonNode group : tutorial.path("groups")) {
            Long groupId = Long.valueOf(group.path("id").asText());
            for (JsonNode chapter : group.path("chapters")) {
                result.add(ChapterRef.builder().tutorialId(tutorialId).groupId(groupId)
                        .chapterId(Long.valueOf(chapter.path("id").asText()))
                        .tutorialSlug(tutorial.path("slug").asText())
                        .chapterSlug(chapter.path("slug").asText())
                        .title(chapter.path("title").asText()).snapshot(chapter).build());
            }
        }
        return result;
    }

    /**
     * 章节Ref，属于教程与学习模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterRef {
        /**
         * 教程标识。
         */
        private Long tutorialId;
        /**
         * 分组标识。
         */
        private Long groupId;
        /**
         * 章节标识。
         */
        private Long chapterId;
        /**
         * 教程固定链接标识。
         */
        private String tutorialSlug;
        /**
         * 章节固定链接标识。
         */
        private String chapterSlug;
        /**
         * 标题。
         */
        private String title;
        /**
         * 快照。
         */
        private JsonNode snapshot;
    }

    /**
     * 卡片Ref，属于教程与学习模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardRef {
        /**
         * 卡片标识。
         */
        private Long cardId;
        /**
         * 章节。
         */
        private ChapterRef chapter;
        /**
         * 知识卡片正面的提示文本。
         */
        private String frontText;
        /**
         * 知识卡片背面的 Markdown 内容。
         */
        private String backMarkdown;
    }

    /**
     * 问题Ref，属于教程与学习模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionRef {
        /**
         * 问题标识。
         */
        private Long questionId;
        /**
         * 章节。
         */
        private ChapterRef chapter;
        /**
         * 引用答案。
         */
        private String referenceAnswer;
    }
}
