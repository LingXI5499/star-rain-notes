package com.starrainnotes.tutorial.content.service.impl;

import org.springframework.transaction.annotation.Isolation;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionCardMapper;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.entity.TutorialCategoryEntity;
import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialGroupEntity;
import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import com.starrainnotes.tutorial.content.exception.TutorialInvalidRequestException;
import com.starrainnotes.tutorial.content.exception.TutorialNotFoundException;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialGroupMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
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
    private final TutorialQuestionCardMapper questionCardMapper;
    private final TutorialRevisionMapper revisionMapper;
    private final TutorialContentService contentService;
    private final CurrentActorApi currentActorApi;
    private final ObjectMapper objectMapper;
    private final TutorialEventPublisher eventPublisher;
    private final TutorialMediaReferences mediaReferences;
    private final ReviewSubmissionApi reviews;

    private void requireNotInReview(TutorialEntity tutorial) {
        if ("IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程审核中，请先完成或取消审核");
        }
    }


    private TutorialEntity lock(Long id) {
        TutorialEntity row = tutorialMapper.byIdForUpdate(id);
        if (row == null) throw new TutorialNotFoundException();
        return row;
    }

    private TutorialEntity publicRow(String slug) {
        TutorialEntity row = tutorialMapper.selectPublishedBySlug(slug);
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
        long wordCount = 0;
        for (JsonNode group : frozen.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                String plain = chapter.path("bodyMarkdown").asText("")
                        .replaceAll("[\\s#*_`>\\[\\]()]", "");
                wordCount += plain.codePointCount(0, plain.length());
            }
        }
        item.put("wordCount", wordCount);
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
        List<TutorialGroupEntity> groups = requirePublishable ? groupMapper.listActiveByTutorialId(tutorial.getId()) : groupMapper.listByTutorialId(tutorial.getId());
        List<Map<String, Object>> groupSnapshots = new ArrayList<>();
        int chapterCount = 0;
        for (TutorialGroupEntity group : groups) {
            List<TutorialChapterEntity> chapters = chapterMapper.listByGroupId(group.getId()).stream()
                    .filter(chapter -> !requirePublishable || !"WITHDRAWN".equals(chapter.getStatus())).toList();
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
                item.put("cards", cardMapper.listEnabledByChapterId(chapter.getId())
                        .stream().map(card -> Map.of("id", String.valueOf(card.getId()),
                                "frontText", card.getFrontText(), "backMarkdown", card.getBackMarkdown(),
                                "contentVersion", card.getContentVersion() == null ? 1 : card.getContentVersion())).toList());
                item.put("questions", questionMapper.listEnabledByChapterId(chapter.getId())
                        .stream().map(question -> Map.of("id", String.valueOf(question.getId()),
                                "questionText", question.getQuestionText(),
                                "referenceAnswer", question.getReferenceAnswer(),
                                "knowledgeCardIds", questionCardMapper.cardIds(question.getId()).stream().map(String::valueOf).toList())).toList());
                item.put("cardCount", ((List<?>)item.get("cards")).size());
                item.put("questionCount", ((List<?>)item.get("questions")).size());
                chapterSnapshots.add(item);
            }
            chapterCount += chapterSnapshots.size();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(group.getId()));
            item.put("title", group.getTitle());
            item.put("chapters", chapterSnapshots);
            groupSnapshots.add(item);
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
        return freeze(tutorial, objectMapper.valueToTree(draftSnapshot(tutorial, true)));
    }

    private TutorialRevisionEntity freeze(TutorialEntity tutorial, JsonNode candidate) {
        String json;
        try {
            json = objectMapper.writeValueAsString(candidate);
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程版本生成失败");
        }
        int revisionNo = Math.toIntExact(revisionMapper.countByTutorialId(tutorial.getId())) + 1;
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
                for (JsonNode card : chapter.path("cards")) chapterBodies.add(card.path("backMarkdown").asText());
                for (JsonNode question : chapter.path("questions")) chapterBodies.add(question.path("referenceAnswer").asText());
            }
        }
        mediaReferences.attachRevision(revision.getId(), chapterBodies);
        return revision;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialAdminVO publish(Long tutorialId) {
        TutorialEntity tutorial = lock(tutorialId);
        if ("IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程审核中，不能直接发布");
        }
        TutorialRevisionEntity revision = freeze(tutorial);
        markPublishedChapters(revision);
        tutorial.setPublishedRevisionId(revision.getId());
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorial.setWithdrawnAt(null);
        tutorialMapper.update(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialAdminVO withdraw(Long tutorialId) {
        TutorialEntity tutorial = lock(tutorialId);
        requireNotInReview(tutorial);
        if (!"PUBLISHED".equals(tutorial.getPublicationStatus())) {
            throw new TutorialStateException("只有已发布教程可以撤回");
        }
        tutorial.setPublicationStatus("WITHDRAWN");
        tutorial.setWithdrawnAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorialMapper.update(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "WITHDRAWN", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    // Derive from the public snapshot, never from unrelated workspace drafts.
    private ObjectNode chapterCandidate(TutorialEntity tutorial, Long chapterId, boolean remove) {
        ObjectNode base;
        if ("PUBLISHED".equals(tutorial.getPublicationStatus()) && tutorial.getPublishedRevisionId() != null) {
            base = ((ObjectNode) snapshot(revisionMapper.selectById(tutorial.getPublishedRevisionId()))).deepCopy();
        } else {
            if (remove) throw new TutorialStateException("章节当前未公开");
            base = objectMapper.valueToTree(draftSnapshot(tutorial, false));
            base.putArray("groups");
        }
        JsonNode replacement = null;
        JsonNode workspaceGroup = null;
        if (!remove) {
            JsonNode workspace = objectMapper.valueToTree(draftSnapshot(tutorial, false));
            for (JsonNode group : workspace.path("groups")) {
                for (JsonNode chapter : group.path("chapters")) {
                    if (String.valueOf(chapterId).equals(chapter.path("id").asText())) {
                        replacement = chapter;
                        workspaceGroup = group;
                    }
                }
            }
            if (replacement == null || replacement.path("bodyMarkdown").asText().isBlank()) {
                throw new TutorialStateException("章节不存在或正文为空");
            }
        }
        boolean found = false;
        for (JsonNode group : base.path("groups")) {
            var chapters = (ArrayNode) group.path("chapters");
            for (int i = 0; i < chapters.size(); i++) {
                if (String.valueOf(chapterId).equals(chapters.get(i).path("id").asText())) {
                    if (remove) chapters.remove(i); else chapters.set(i, replacement);
                    found = true;
                    break;
                }
            }
        }
        if (!found && remove) throw new TutorialStateException("章节当前未公开");
        if (!found) {
            ObjectNode target = null;
            for (JsonNode group : base.path("groups")) {
                if (group.path("id").equals(workspaceGroup.path("id"))) target = (ObjectNode) group;
            }
            if (target == null) {
                target = ((ObjectNode) workspaceGroup).deepCopy();
                target.putArray("chapters");
                ((ArrayNode) base.path("groups")).add(target);
            }
            ((ArrayNode) target.path("chapters")).add(replacement);
        }
        int count = 0;
        for (JsonNode group : base.path("groups")) count += group.path("chapters").size();
        base.put("chapterCount", count);
        return base;
    }

    private TutorialEntity chapterTutorial(Long chapterId) {
        TutorialChapterEntity chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) throw new TutorialNotFoundException();
        TutorialEntity tutorial = lock(chapter.getTutorialId());
        requireNotInReview(tutorial);
        return tutorial;
    }

    private void markPublishedChapters(TutorialRevisionEntity revision) {
        for (JsonNode group : snapshot(revision).path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                chapterMapper.updateStatus(Long.valueOf(chapter.path("id").asText()), "PUBLISHED", LocalDateTime.now(ZoneOffset.UTC));
            }
        }
    }

    private void activate(TutorialEntity tutorial, TutorialRevisionEntity revision) {
        markPublishedChapters(revision);
        tutorial.setPublishedRevisionId(revision.getId());
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorial.setWithdrawnAt(null);
        tutorialMapper.update(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorial.getId(), tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void refreshPublished(Long tutorialId, String operation, Long changedChapterId) {
        TutorialEntity tutorial = lock(tutorialId);
        if (!"PUBLISHED".equals(tutorial.getPublicationStatus()) || tutorial.getPublishedRevisionId() == null) return;
        JsonNode previous = snapshot(revisionMapper.selectById(tutorial.getPublishedRevisionId()));
        java.util.Map<String, JsonNode> publicChapters = new java.util.HashMap<>();
        for (JsonNode group : previous.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) publicChapters.put(chapter.path("id").asText(), chapter);
        }
        ObjectNode candidate = objectMapper.valueToTree(draftSnapshot(tutorial, false));
        if (!"updateTutorial".equals(operation)) {
            for (String key : List.of("title", "summary", "slug")) {
                if (previous.has(key)) candidate.set(key, previous.get(key));
            }
        }
        JsonNode workspace = candidate.deepCopy();
        ArrayNode visibleGroups = candidate.putArray("groups");
        int count = 0;
        for (JsonNode group : workspace.path("groups")) {
            ObjectNode visibleGroup = ((ObjectNode) group).deepCopy();
            ArrayNode visibleChapters = visibleGroup.putArray("chapters");
            for (JsonNode chapter : group.path("chapters")) {
                String id = chapter.path("id").asText();
                if (publicChapters.containsKey(id)) {
                    visibleChapters.add(id.equals(String.valueOf(changedChapterId)) ? chapter : publicChapters.get(id));
                }
            }
            if (!visibleChapters.isEmpty()) visibleGroups.add(visibleGroup);
            count += visibleChapters.size();
        }
        candidate.put("chapterCount", count);
        // A new draft chapter stays a draft; editing an existing public chapter updates it immediately.
        if (!candidate.equals(previous)) activate(tutorial, freeze(tutorial, candidate));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void publishChapter(Long chapterId) {
        TutorialEntity tutorial = chapterTutorial(chapterId);
        activate(tutorial, freeze(tutorial, chapterCandidate(tutorial, chapterId, false)));
        chapterMapper.updateStatus(chapterId, "PUBLISHED", LocalDateTime.now(ZoneOffset.UTC));
        chapterMapper.bumpVersion(chapterId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void withdrawChapter(Long chapterId) {
        TutorialEntity tutorial = chapterTutorial(chapterId);
        activate(tutorial, freeze(tutorial, chapterCandidate(tutorial, chapterId, true)));
        chapterMapper.updateStatus(chapterId, "WITHDRAWN", LocalDateTime.now(ZoneOffset.UTC));
        chapterMapper.bumpVersion(chapterId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void submitReview(Long tutorialId, Long chapterId) {
        TutorialEntity tutorial = lock(tutorialId);
        requireNotInReview(tutorial);
        if (chapterId != null) {
            TutorialChapterEntity chapter = chapterMapper.selectById(chapterId);
            if (chapter == null || !tutorialId.equals(chapter.getTutorialId())) throw new TutorialNotFoundException();
        }
        TutorialRevisionEntity revision = chapterId == null ? freeze(tutorial)
                : freeze(tutorial, chapterCandidate(tutorial, chapterId, false));
        reviews.submit(ReviewSubmissionCommand.builder()
                .reviewType("tutorial.publish").targetModule("TUTORIAL").targetType("TUTORIAL")
                .targetId(tutorialId).targetRevisionRef(revision.getRevisionRef())
                .targetDisplayName(tutorial.getTitle()).applicantAccountId(currentActorApi.current().getAccountId()).build());
        tutorial.setEditingStatus("IN_REVIEW");
        tutorialMapper.update(tutorial);
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
        tutorialMapper.listPublishedWithRevision()
                .forEach(tutorial -> {
                    String frozenCategoryId = snapshot(revisionMapper.selectById(tutorial.getPublishedRevisionId()))
                            .path("categoryId").asText();
                    publishedCounts.merge(frozenCategoryId, 1L, Long::sum);
                });
        return categoryMapper.listOrdered()
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
        List<JsonNode> rows = tutorialMapper.listPublishedWithRevision().stream()
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
                item.put("cardCount", item.path("cards").size());
                item.put("questionCount", item.path("questions").size());
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

}
