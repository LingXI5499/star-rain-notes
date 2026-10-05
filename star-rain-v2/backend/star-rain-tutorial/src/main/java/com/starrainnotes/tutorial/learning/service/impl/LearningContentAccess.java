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

@Component
@RequiredArgsConstructor
public class LearningContentAccess {
    private final TutorialMapper tutorialMapper;
    private final TutorialRevisionMapper revisionMapper;
    private final TutorialChapterMapper chapterMapper;
    private final TutorialKnowledgeCardMapper cardMapper;
    private final TutorialQuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

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
    @Data
    @AllArgsConstructor
    private static class PublishedChild {
        private ChapterRef chapter;
        private JsonNode child;
    }

    // 删除工作区卡片或题目不会改写已发布快照。仅在原始行已删除时扫描公开修订，
    // 让尚未重新发布的读者内容仍可答题和复习。
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterRef {
        private Long tutorialId;
        private Long groupId;
        private Long chapterId;
        private String tutorialSlug;
        private String chapterSlug;
        private String title;
        private JsonNode snapshot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardRef {
        private Long cardId;
        private ChapterRef chapter;
        private String frontText;
        private String backMarkdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionRef {
        private Long questionId;
        private ChapterRef chapter;
        private String referenceAnswer;
    }
}
