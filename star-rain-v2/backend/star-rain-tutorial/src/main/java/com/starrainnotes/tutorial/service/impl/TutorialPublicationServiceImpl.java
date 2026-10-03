package com.starrainnotes.tutorial.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.tutorial.entity.TutorialCategoryEntity;
import com.starrainnotes.tutorial.entity.TutorialChapterEntity;
import com.starrainnotes.tutorial.entity.TutorialEntity;
import com.starrainnotes.tutorial.entity.TutorialGroupEntity;
import com.starrainnotes.tutorial.entity.TutorialKnowledgeCardEntity;
import com.starrainnotes.tutorial.entity.TutorialQuestionEntity;
import com.starrainnotes.tutorial.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.event.TutorialPublicationChangedEvent;
import com.starrainnotes.tutorial.exception.TutorialInvalidRequestException;
import com.starrainnotes.tutorial.exception.TutorialNotFoundException;
import com.starrainnotes.tutorial.exception.TutorialStateException;
import com.starrainnotes.tutorial.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.mapper.TutorialGroupMapper;
import com.starrainnotes.tutorial.mapper.TutorialMapper;
import com.starrainnotes.tutorial.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.mapper.TutorialRevisionMapper;
import com.starrainnotes.tutorial.service.TutorialContentService;
import com.starrainnotes.tutorial.service.TutorialMediaReferences;
import com.starrainnotes.tutorial.service.TutorialPublicationService;
import com.starrainnotes.tutorial.vo.TutorialAdminVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialPublicationServiceImpl implements TutorialPublicationService {
    private final TutorialMapper tutorialMapper;
    private final TutorialCategoryMapper categoryMapper;
    private final TutorialGroupMapper groupMapper;
    private final TutorialChapterMapper chapterMapper;
    private final TutorialKnowledgeCardMapper cardMapper;
    private final TutorialQuestionMapper questionMapper;
    private final TutorialRevisionMapper revisionMapper;
    private final TutorialContentService contentService;
    private final CurrentActorApi currentActorApi;
    private final ReviewSubmissionApi reviewSubmissionApi;
    private final ObjectMapper objectMapper;
    private final TutorialEventPublisher eventPublisher;
    private final TutorialMediaReferences mediaReferences;

    private TutorialEntity lock(Long id) {
        TutorialEntity row = tutorialMapper.byIdForUpdate(id);
        if (row == null) throw new TutorialNotFoundException();
        return row;
    }

    private TutorialEntity publicRow(String slug) {
        TutorialEntity row = tutorialMapper.selectOne(new LambdaQueryWrapper<TutorialEntity>()
                .eq(TutorialEntity::getSlug, slug).eq(TutorialEntity::getPublicationStatus, "PUBLISHED"));
        if (row == null || row.getPublishedRevisionId() == null) throw new TutorialNotFoundException();
        return row;
    }

    private JsonNode snapshot(TutorialRevisionEntity revision) {
        if (revision == null) throw new TutorialNotFoundException();
        try {
            return objectMapper.readTree(revision.getSnapshotJson());
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程公开版本不可读取");
        }
    }

    private ObjectNode publicSummary(TutorialEntity row, JsonNode frozen) {
        ObjectNode item = ((ObjectNode) frozen).deepCopy();
        item.remove("groups");
        item.put("publishedAt", row.getPublishedAt() == null ? "" : row.getPublishedAt().toString());
        return item;
    }

    private JsonNode publicSnapshot(String slug) {
        TutorialEntity row = publicRow(slug);
        return snapshot(revisionMapper.selectById(row.getPublishedRevisionId()));
    }

    private JsonNode frozenChapter(JsonNode tutorial, String chapterSlug) {
        for (JsonNode group : tutorial.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                if (chapterSlug.equals(chapter.path("slug").asText())) return chapter;
            }
        }
        throw new TutorialNotFoundException();
    }

    private Map<String, Object> draftSnapshot(TutorialEntity tutorial, boolean requirePublishable) {
        TutorialCategoryEntity category = categoryMapper.selectById(tutorial.getCategoryId());
        if (category == null) throw new TutorialStateException("知识体系不存在");
        List<TutorialGroupEntity> groups = groupMapper.selectList(new LambdaQueryWrapper<TutorialGroupEntity>()
                .eq(TutorialGroupEntity::getTutorialId, tutorial.getId())
                .eq(TutorialGroupEntity::getStatus, "ACTIVE")
                .orderByAsc(TutorialGroupEntity::getSortOrder, TutorialGroupEntity::getId));
        List<Map<String, Object>> groupSnapshots = new ArrayList<>();
        int chapterCount = 0;
        for (TutorialGroupEntity group : groups) {
            List<TutorialChapterEntity> chapters = chapterMapper.selectList(new LambdaQueryWrapper<TutorialChapterEntity>()
                    .eq(TutorialChapterEntity::getGroupId, group.getId())
                    .eq(TutorialChapterEntity::getStatus, "ACTIVE")
                    .orderByAsc(TutorialChapterEntity::getSortOrder, TutorialChapterEntity::getId));
            List<Map<String, Object>> chapterSnapshots = new ArrayList<>();
            for (TutorialChapterEntity chapter : chapters) {
                if (requirePublishable && (chapter.getBodyMarkdown() == null || chapter.getBodyMarkdown().isBlank())) {
                    throw new TutorialStateException("存在正文为空的章节，无法发布");
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", String.valueOf(chapter.getId()));
                item.put("slug", chapter.getSlug());
                item.put("title", chapter.getTitle());
                item.put("summary", chapter.getSummary());
                item.put("bodyMarkdown", chapter.getBodyMarkdown());
                item.put("cards", cardMapper.selectList(new LambdaQueryWrapper<TutorialKnowledgeCardEntity>()
                        .eq(TutorialKnowledgeCardEntity::getChapterId, chapter.getId())
                        .eq(TutorialKnowledgeCardEntity::getStatus, "ENABLED")
                        .orderByAsc(TutorialKnowledgeCardEntity::getSortOrder, TutorialKnowledgeCardEntity::getId))
                        .stream().map(card -> Map.of("id", String.valueOf(card.getId()),
                                "frontText", card.getFrontText(), "backMarkdown", card.getBackMarkdown())).toList());
                item.put("questions", questionMapper.selectList(new LambdaQueryWrapper<TutorialQuestionEntity>()
                        .eq(TutorialQuestionEntity::getChapterId, chapter.getId())
                        .eq(TutorialQuestionEntity::getStatus, "ENABLED")
                        .orderByAsc(TutorialQuestionEntity::getSortOrder, TutorialQuestionEntity::getId))
                        .stream().map(question -> Map.of("id", String.valueOf(question.getId()),
                                "questionText", question.getQuestionText(),
                                "referenceAnswer", question.getReferenceAnswer())).toList());
                chapterSnapshots.add(item);
            }
            chapterCount += chapterSnapshots.size();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(group.getId()));
            item.put("title", group.getTitle());
            item.put("chapters", chapterSnapshots);
            groupSnapshots.add(item);
        }
        if (requirePublishable && chapterCount == 0) {
            throw new TutorialStateException("请先创建至少一个使用中的章节");
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", String.valueOf(tutorial.getId()));
        root.put("categoryId", String.valueOf(category.getId()));
        root.put("categoryName", category.getName());
        root.put("categorySlug", category.getSlug());
        root.put("slug", tutorial.getSlug());
        root.put("title", tutorial.getTitle());
        root.put("summary", tutorial.getSummary());
        root.put("chapterCount", chapterCount);
        root.put("groups", groupSnapshots);
        return root;
    }

    private TutorialRevisionEntity freeze(TutorialEntity tutorial) {
        String json;
        try {
            json = objectMapper.writeValueAsString(draftSnapshot(tutorial, true));
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程版本生成失败");
        }
        int revisionNo = Math.toIntExact(revisionMapper.selectCount(new LambdaQueryWrapper<TutorialRevisionEntity>()
                .eq(TutorialRevisionEntity::getTutorialId, tutorial.getId()))) + 1;
        TutorialRevisionEntity revision = new TutorialRevisionEntity();
        revision.setTutorialId(tutorial.getId());
        revision.setRevisionNo(revisionNo);
        revision.setRevisionRef("tutorial:" + tutorial.getId() + ":revision:" + revisionNo);
        revision.setSnapshotJson(json);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            revision.setContentSha256(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        revision.setCreatedByAccountId(currentActorApi.current().getAccountId());
        revisionMapper.insert(revision);
        List<String> chapterBodies = new ArrayList<>();
        JsonNode frozen = snapshot(revision);
        for (JsonNode group : frozen.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                chapterBodies.add(chapter.path("bodyMarkdown").asText());
            }
        }
        mediaReferences.attachRevision(revision.getId(), chapterBodies);
        return revision;
    }

    @Override
    @Transactional
    public TutorialAdminVO publish(Long tutorialId) {
        TutorialEntity tutorial = lock(tutorialId);
        if ("IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程审核中，不能直接发布");
        }
        TutorialRevisionEntity revision = freeze(tutorial);
        tutorial.setPublishedRevisionId(revision.getId());
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorial.setWithdrawnAt(null);
        tutorialMapper.updateById(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    @Override
    @Transactional
    public TutorialAdminVO withdraw(Long tutorialId) {
        TutorialEntity tutorial = lock(tutorialId);
        if (!"PUBLISHED".equals(tutorial.getPublicationStatus())) {
            throw new TutorialStateException("只有已发布教程可以撤回");
        }
        tutorial.setPublicationStatus("WITHDRAWN");
        tutorial.setWithdrawnAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorialMapper.updateById(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "WITHDRAWN", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    @Override
    @Transactional
    public TutorialAdminVO restore(Long tutorialId) {
        TutorialEntity tutorial = lock(tutorialId);
        if (!"WITHDRAWN".equals(tutorial.getPublicationStatus()) || tutorial.getPublishedRevisionId() == null) {
            throw new TutorialStateException("没有可以重新公开的教程版本");
        }
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setWithdrawnAt(null);
        tutorialMapper.updateById(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "RESTORED", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    @Override
    @Transactional
    public TutorialAdminVO submitReview(Long tutorialId) {
        TutorialEntity tutorial = lock(tutorialId);
        if ("IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程已在审核中");
        }
        TutorialRevisionEntity revision = freeze(tutorial);
        Long actorId = currentActorApi.current().getAccountId();
        reviewSubmissionApi.submit(ReviewSubmissionCommand.builder()
                .reviewType("tutorial.publish").targetModule("TUTORIAL").targetType("TUTORIAL")
                .targetId(tutorialId).targetRevisionRef(revision.getRevisionRef())
                .targetDisplayName(tutorial.getTitle()).applicantAccountId(actorId).build());
        tutorial.setEditingStatus("IN_REVIEW");
        tutorialMapper.updateById(tutorial);
        return contentService.tutorial(tutorialId);
    }

    @Override
    public JsonNode previewTutorial(Long tutorialId) {
        TutorialEntity tutorial = tutorialMapper.selectById(tutorialId);
        if (tutorial == null) throw new TutorialNotFoundException();
        return objectMapper.valueToTree(draftSnapshot(tutorial, false));
    }

    @Override
    public JsonNode previewChapter(Long chapterId) {
        TutorialChapterEntity chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) throw new TutorialNotFoundException();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(chapter.getId()));
        row.put("slug", chapter.getSlug());
        row.put("title", chapter.getTitle());
        row.put("summary", chapter.getSummary());
        row.put("bodyMarkdown", chapter.getBodyMarkdown());
        return objectMapper.valueToTree(row);
    }

    @Override
    public List<JsonNode> publicCategories() {
        Map<String, Long> publishedCounts = new LinkedHashMap<>();
        tutorialMapper.selectList(new LambdaQueryWrapper<TutorialEntity>()
                .eq(TutorialEntity::getPublicationStatus, "PUBLISHED")
                .isNotNull(TutorialEntity::getPublishedRevisionId))
                .forEach(tutorial -> {
                    String frozenCategoryId = snapshot(revisionMapper.selectById(tutorial.getPublishedRevisionId()))
                            .path("categoryId").asText();
                    publishedCounts.merge(frozenCategoryId, 1L, Long::sum);
                });
        return categoryMapper.selectList(new LambdaQueryWrapper<TutorialCategoryEntity>()
                .orderByAsc(TutorialCategoryEntity::getSortOrder, TutorialCategoryEntity::getId))
                .stream().map(category -> {
                    long count = publishedCounts.getOrDefault(String.valueOf(category.getId()), 0L);
                    Map<String, Object> item = Map.of("id", String.valueOf(category.getId()),
                            "slug", category.getSlug(), "name", category.getName(), "tutorialCount", count);
                    return (JsonNode) objectMapper.valueToTree(item);
                }).toList();
    }

    @Override
    public PageResult<JsonNode> publicTutorials(Long categoryId, String search, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new TutorialInvalidRequestException("分页参数不合法");
        LambdaQueryWrapper<TutorialEntity> query = new LambdaQueryWrapper<TutorialEntity>()
                .eq(TutorialEntity::getPublicationStatus, "PUBLISHED")
                .isNotNull(TutorialEntity::getPublishedRevisionId)
                .orderByAsc(TutorialEntity::getCategoryId, TutorialEntity::getSortOrder, TutorialEntity::getId);
        List<JsonNode> rows = tutorialMapper.selectList(query).stream()
                .map(row -> (JsonNode) publicSummary(row, snapshot(revisionMapper.selectById(row.getPublishedRevisionId()))))
                .filter(item -> categoryId == null || String.valueOf(categoryId).equals(item.path("categoryId").asText()))
                .filter(item -> search == null || search.isBlank()
                        || item.path("title").asText().contains(search.strip())).toList();
        long start = (long) (page - 1) * pageSize;
        List<JsonNode> items = start >= rows.size() ? List.of()
                : rows.subList((int) start, Math.min(rows.size(), (int) start + pageSize));
        return PageResult.<JsonNode>builder().items(items).total(rows.size()).page(page).pageSize(pageSize).build();
    }

    @Override
    public JsonNode publicTutorial(String slug) {
        TutorialEntity row = publicRow(slug);
        ObjectNode detail = ((ObjectNode) snapshot(revisionMapper.selectById(row.getPublishedRevisionId()))).deepCopy();
        detail.put("publishedAt", row.getPublishedAt() == null ? "" : row.getPublishedAt().toString());
        for (JsonNode group : detail.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                ObjectNode item = (ObjectNode) chapter;
                item.remove("bodyMarkdown");
                item.remove("cards");
                item.remove("questions");
            }
        }
        return detail;
    }

    @Override
    public JsonNode publicChapter(String tutorialSlug, String chapterSlug) {
        JsonNode raw = frozenChapter(publicSnapshot(tutorialSlug), chapterSlug);
        ObjectNode result = ((ObjectNode) raw).deepCopy();
        for (JsonNode question : result.path("questions")) {
            ((ObjectNode) question).remove("referenceAnswer");
        }
        return result;
    }

    @Override
    public JsonNode publicQuestionAnswer(String tutorialSlug, String chapterSlug, String questionId) {
        JsonNode chapter = frozenChapter(publicSnapshot(tutorialSlug), chapterSlug);
        for (JsonNode question : chapter.path("questions")) {
            if (questionId.equals(question.path("id").asText())) {
                Map<String, Object> result = Map.of("referenceAnswer", question.path("referenceAnswer").asText());
                return objectMapper.valueToTree(result);
            }
        }
        throw new TutorialNotFoundException();
    }
}
