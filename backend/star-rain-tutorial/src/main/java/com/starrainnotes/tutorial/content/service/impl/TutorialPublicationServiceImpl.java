/*
 * TutorialPublicationServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：TutorialQuestionCardMapper、ReviewSubmissionCommand、ReviewSubmissionApi、CurrentActorApi、PageResult、TutorialCategoryEntity、TutorialChapterEntity。
 * 阅读入口：publish（发布教程发布）；withdraw（撤回教程发布）；refreshPublished（刷新已发布）；publishChapter（发布章节）。
 */

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

/**
 * 教程发布业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class TutorialPublicationServiceImpl implements TutorialPublicationService {
    /**
     * 教程数据映射。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorialMapper;
    /**
     * 分类数据映射。
     * 协作类型：<code>TutorialCategoryMapper</code>。
     */
    private final TutorialCategoryMapper categoryMapper;
    /**
     * 分组数据映射。
     * 协作类型：<code>TutorialGroupMapper</code>。
     */
    private final TutorialGroupMapper groupMapper;
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
     * 问题卡片数据映射。
     * 协作类型：<code>TutorialQuestionCardMapper</code>。
     */
    private final TutorialQuestionCardMapper questionCardMapper;
    /**
     * 修订数据映射。
     * 协作类型：<code>TutorialRevisionMapper</code>。
     */
    private final TutorialRevisionMapper revisionMapper;
    /**
     * 内容业务服务。
     * 协作类型：<code>TutorialContentService</code>。
     */
    private final TutorialContentService contentService;
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 项目配置的 Jackson JSON 读写器。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper objectMapper;
    /**
     * 事件发布器。
     */
    private final TutorialEventPublisher eventPublisher;
    /**
     * 媒体References。
     */
    private final TutorialMediaReferences mediaReferences;
    /**
     * reviews。
     * 协作类型：<code>ReviewSubmissionApi</code>。
     */
    private final ReviewSubmissionApi reviews;

    /**
     * 检查所需不内部审核。
     * 关键处理步骤调用 <code>tutorial.getEditingStatus</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     */
    private void requireNotInReview(TutorialEntity tutorial) {
        if ("IN_REVIEW".equals(tutorial.getEditingStatus())) {
            throw new TutorialStateException("教程审核中，请先完成或取消审核");
        }
    }


    /**
     * 锁处理入口。
     * 主要协作调用：<code>tutorialMapper.byIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    private TutorialEntity lock(Long id) {
        TutorialEntity row = tutorialMapper.byIdForUpdate(id);
        if (row == null) throw new TutorialNotFoundException();
        return row;
    }

    /**
     * 公开行处理入口。
     * 主要协作调用：<code>tutorialMapper.selectPublishedBySlug</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    private TutorialEntity publicRow(String slug) {
        TutorialEntity row = tutorialMapper.selectPublishedBySlug(slug);
        if (row == null || row.getPublishedRevisionId() == null) throw new TutorialNotFoundException();
        return row;
    }

    /**
     * 快照处理入口。
     * 主要协作调用：<code>objectMapper.readTree</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>、<code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param revision 修订；类型为 <code>TutorialRevisionEntity</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    private JsonNode snapshot(TutorialRevisionEntity revision) {
        if (revision == null) throw new TutorialNotFoundException();
        try {
            return objectMapper.readTree(revision.getSnapshotJson());
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程公开版本不可读取");
        }
    }

    /**
     * 公开摘要处理入口。
     * 关键处理步骤调用 <code>item.remove</code>、<code>frozen.path</code>、<code>group.path</code>、<code>chapter.path</code>。
     *
     * @param row 行；类型为 <code>TutorialEntity</code>
     * @param frozen frozen；类型为 <code>JsonNode</code>
     * @return 类型为 <code>ObjectNode</code> 的处理结果
     */
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

    /**
     * 公开快照处理入口。
     * 主要协作调用：<code>revisionMapper.selectById</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    private JsonNode publicSnapshot(String slug) {
        TutorialEntity row = publicRow(slug);
        return publishedSnapshot(row.getPublishedRevisionId()).root();
    }

    /**
     * frozen章节处理入口。
     * 关键处理步骤调用 <code>tutorial.path</code>、<code>group.path</code>、<code>chapterSlug.equals</code>、<code>chapter.path</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param tutorial 教程；类型为 <code>JsonNode</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    private JsonNode frozenChapter(JsonNode tutorial, String chapterSlug) {
        for (JsonNode group : tutorial.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                if (chapterSlug.equals(chapter.path("slug").asText())) return chapter;
            }
        }
        throw new TutorialNotFoundException();
    }

    /**
     * 草稿快照处理入口。
     * 主要协作调用：<code>categoryMapper.selectById</code>、<code>groupMapper.listActiveByTutorialId</code>、<code>groupMapper.listByTutorialId</code>、<code>chapterMapper.listByGroupId</code>、<code>cardMapper.listEnabledByChapterId</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @param requirePublishable 要求Publishable；类型为 <code>boolean</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
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

    /**
     * freeze处理入口。
     * 主要协作调用：<code>objectMapper.valueToTree</code>。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @return 类型为 <code>TutorialRevisionEntity</code> 的处理结果
     */
    private TutorialRevisionEntity freeze(TutorialEntity tutorial) {
        return freeze(tutorial, objectMapper.valueToTree(draftSnapshot(tutorial, true)));
    }

    /**
     * freeze处理入口。
     * 主要协作调用：<code>objectMapper.writeValueAsString</code>、<code>revisionMapper.countByTutorialId</code>、<code>currentActorApi.current</code>、<code>revisionMapper.insert</code>、<code>mediaReferences.attachRevision</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>、<code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @param candidate candidate；类型为 <code>JsonNode</code>
     * @return 类型为 <code>TutorialRevisionEntity</code> 的处理结果
     */
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
        revision.setSummaryJson(publicSummary(tutorial, snapshot(revision)).toString());
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

    /**
     * 发布教程发布。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorialMapper.update</code>、<code>eventPublisher.afterCommit</code>、<code>contentService.tutorial</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
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
        eventPublisher.publish(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    /**
     * 撤回教程发布。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorialMapper.update</code>、<code>eventPublisher.afterCommit</code>、<code>contentService.tutorial</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
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
        eventPublisher.publish(new TutorialPublicationChangedEvent(tutorialId, tutorial.getSlug(),
                tutorial.getTitle(), "WITHDRAWN", tutorial.getPublishedAt()));
        return contentService.tutorial(tutorialId);
    }

    // Derive from the public snapshot, never from unrelated workspace drafts.
    /**
     * 章节Candidate处理入口。
     * 主要协作调用：<code>revisionMapper.selectById</code>、<code>objectMapper.valueToTree</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param remove remove；类型为 <code>boolean</code>
     * @return 类型为 <code>ObjectNode</code> 的处理结果
     */
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

    /**
     * 章节教程处理入口。
     * 主要协作调用：<code>chapterMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    private TutorialEntity chapterTutorial(Long chapterId) {
        TutorialChapterEntity chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) throw new TutorialNotFoundException();
        TutorialEntity tutorial = lock(chapter.getTutorialId());
        requireNotInReview(tutorial);
        return tutorial;
    }

    /**
     * mark已发布章节列表处理入口。
     * 主要协作调用：<code>chapterMapper.updateStatus</code>。
     *
     * @param revision 修订；类型为 <code>TutorialRevisionEntity</code>
     */
    private void markPublishedChapters(TutorialRevisionEntity revision) {
        for (JsonNode group : snapshot(revision).path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                mediaReferences.assertPublicContent(chapter.path("bodyMarkdown").asText());
                for (JsonNode card : chapter.path("cards")) mediaReferences.assertPublicContent(card.path("backMarkdown").asText());
                for (JsonNode question : chapter.path("questions")) mediaReferences.assertPublicContent(question.path("referenceAnswer").asText());
            }
        }
        for (JsonNode group : snapshot(revision).path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                chapterMapper.updateStatus(Long.valueOf(chapter.path("id").asText()), "PUBLISHED", LocalDateTime.now(ZoneOffset.UTC));
            }
        }
    }

    /**
     * activate处理入口。
     * 主要协作调用：<code>tutorialMapper.update</code>、<code>eventPublisher.afterCommit</code>。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @param revision 修订；类型为 <code>TutorialRevisionEntity</code>
     */
    private void activate(TutorialEntity tutorial, TutorialRevisionEntity revision) {
        markPublishedChapters(revision);
        tutorial.setPublishedRevisionId(revision.getId());
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorial.setWithdrawnAt(null);
        tutorialMapper.update(tutorial);
        eventPublisher.publish(new TutorialPublicationChangedEvent(tutorial.getId(), tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
    }

    /**
     * 刷新已发布。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>revisionMapper.selectById</code>、<code>objectMapper.valueToTree</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param operation 操作；类型为 <code>String</code>
     * @param changedChapterId changed章节标识；类型为 <code>Long</code>
     */
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

    /**
     * 发布章节。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>chapterMapper.updateStatus</code>、<code>chapterMapper.bumpVersion</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void publishChapter(Long chapterId) {
        TutorialEntity tutorial = chapterTutorial(chapterId);
        activate(tutorial, freeze(tutorial, chapterCandidate(tutorial, chapterId, false)));
        chapterMapper.updateStatus(chapterId, "PUBLISHED", LocalDateTime.now(ZoneOffset.UTC));
        chapterMapper.bumpVersion(chapterId);
    }

    /**
     * 撤回章节。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>chapterMapper.updateStatus</code>、<code>chapterMapper.bumpVersion</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void withdrawChapter(Long chapterId) {
        TutorialEntity tutorial = chapterTutorial(chapterId);
        activate(tutorial, freeze(tutorial, chapterCandidate(tutorial, chapterId, true)));
        chapterMapper.updateStatus(chapterId, "WITHDRAWN", LocalDateTime.now(ZoneOffset.UTC));
        chapterMapper.bumpVersion(chapterId);
    }

    /**
     * 提交审核。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>chapterMapper.selectById</code>、<code>reviews.submit</code>、<code>currentActorApi.current</code>、<code>tutorialMapper.update</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
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

    /**
     * 预览教程。
     * 主要协作调用：<code>tutorialMapper.selectById</code>、<code>objectMapper.valueToTree</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    @Override
    public JsonNode previewTutorial(Long tutorialId) {
        TutorialEntity tutorial = tutorialMapper.selectById(tutorialId);
        if (tutorial == null) throw new TutorialNotFoundException();
        return objectMapper.valueToTree(draftSnapshot(tutorial, false));
    }

    /**
     * 预览章节。
     * 主要协作调用：<code>chapterMapper.selectById</code>、<code>objectMapper.valueToTree</code>。
     * 实现中的显式失败类型包括 <code>TutorialNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
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

    /**
     * 公开Categories处理入口。
     * 主要协作调用：<code>tutorialMapper.listPublishedWithRevision</code>、<code>revisionMapper.selectById</code>、<code>categoryMapper.listOrdered</code>、<code>objectMapper.valueToTree</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<JsonNode> publicCategories() {
        Map<String, Long> publishedCounts = new LinkedHashMap<>();
        tutorialMapper.publishedCategoryCounts().forEach(row ->
                publishedCounts.put(String.valueOf(row.get("categoryId")), ((Number) row.get("total")).longValue()));
        return categoryMapper.listOrdered()
                .stream().map(category -> {
                    long count = publishedCounts.getOrDefault(String.valueOf(category.getId()), 0L);
                    Map<String, Object> item = Map.of("id", String.valueOf(category.getId()),
                            "slug", category.getSlug(), "name", category.getName(), "tutorialCount", count);
                    return (JsonNode) objectMapper.valueToTree(item);
                }).toList();
    }

    /**
     * 公开Tutorials处理入口。
     * 主要协作调用：<code>tutorialMapper.listPublishedWithRevision</code>、<code>revisionMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;JsonNode&gt;</code> 的处理结果
     */
    @Override
    public PageResult<JsonNode> publicTutorials(Long categoryId, String search, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new TutorialInvalidRequestException("分页参数不合法");
        long offset = (long) (page - 1) * pageSize;
        if (offset > 100000) throw new TutorialInvalidRequestException("请缩小筛选范围后再翻页");
        String query = search == null ? null : search.strip();
        long total = tutorialMapper.publicCount(categoryId, query);
        List<JsonNode> items = tutorialMapper.publicSummaries(categoryId, query, offset, pageSize).stream().map(raw -> {
            try { return objectMapper.readTree(raw); }
            catch (JsonProcessingException invalid) { throw new TutorialStateException("教程公开摘要不可读取"); }
        }).toList();
        return PageResult.<JsonNode>builder().items(items).total(total).page(page).pageSize(pageSize).build();
    }

    /**
     * 公开教程处理入口。
     * 主要协作调用：<code>revisionMapper.selectById</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    @Override
    public JsonNode publicTutorial(String slug) {
        TutorialEntity row = publicRow(slug);
        ObjectNode detail = publishedSnapshot(row.getPublishedRevisionId()).directory().deepCopy();
        detail.put("publishedAt", row.getPublishedAt() == null ? "" : row.getPublishedAt().toString());
        return detail;
    }

    /**
     * 公开章节处理入口。
     * 关键处理步骤调用 <code>frozenChapter</code>、<code>publicSnapshot</code>、<code>result.path</code>。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    @Override
    public JsonNode publicChapter(String tutorialSlug, String chapterSlug) {
        JsonNode raw = frozenChapter(publicSnapshot(tutorialSlug), chapterSlug);
        ObjectNode result = ((ObjectNode) raw).deepCopy();
        for (JsonNode question : result.path("questions")) {
            ((ObjectNode) question).remove("referenceAnswer");
        }
        return result;
    }

    private final java.util.LinkedHashMap<Long, PublishedRead> publicReads = new java.util.LinkedHashMap<>(16, .75f, true);
    private long publicReadBytes;
    private record PublishedRead(JsonNode root, ObjectNode directory, long weight) { }

    /** Revision payloads are immutable; publication pointers are always read from the database first. */
    private synchronized PublishedRead publishedSnapshot(Long revisionId) {
        PublishedRead existing = publicReads.get(revisionId);
        if (existing != null) return existing;
        var revision = revisionMapper.selectById(revisionId);
        JsonNode root = snapshot(revision);
        ObjectNode directory = ((ObjectNode) root).deepCopy();
        for (JsonNode group : directory.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                ObjectNode item = (ObjectNode) chapter;
                item.put("cardCount", item.path("cards").size());
                item.put("questionCount", item.path("questions").size());
                item.remove(java.util.List.of("bodyMarkdown", "cards", "questions"));
            }
        }
        long weight = (long) revision.getSnapshotJson().length() * 4;
        PublishedRead result = new PublishedRead(root, directory, weight);
        if (weight <= 8L * 1024 * 1024) {
            publicReads.put(revisionId, result); publicReadBytes += weight;
            while (publicReadBytes > 32L * 1024 * 1024 || publicReads.size() > 32) {
                var first = publicReads.entrySet().iterator(); var removed = first.next(); publicReadBytes -= removed.getValue().weight(); first.remove();
            }
        }
        return result;
    }

}
