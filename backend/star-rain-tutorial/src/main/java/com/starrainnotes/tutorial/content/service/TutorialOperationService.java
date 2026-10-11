/*
 * TutorialOperationService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：CurrentActorApi、ReviewSubmissionApi、ReviewSubmissionCommand、ChapterBodyDTO、ChapterCreateDTO、ChapterQuestionDTO、ChapterUpdateDTO。
 * 阅读入口：request（本次操作的请求参数对象处理入口）；approve（批准教程操作）；recoverApproved（recover已批准处理入口）；finish（finish处理入口）。
 */

package com.starrainnotes.tutorial.content.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.tutorial.content.dto.ChapterBodyDTO;
import com.starrainnotes.tutorial.content.dto.ChapterCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.dto.KnowledgeCardDTO;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.content.entity.TutorialOperationEntity;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialGroupMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialOperationMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

/**
 * 教程操作业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
@Service
@RequiredArgsConstructor
public class TutorialOperationService {
    /**
     * 内容。
     * 协作类型：<code>TutorialContentService</code>。
     */
    private final TutorialContentService content;
    /**
     * exercises。
     * 协作类型：<code>TutorialExerciseService</code>。
     */
    private final TutorialExerciseService exercises;
    /**
     * 发布。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publication;
    /**
     * operations。
     * 协作类型：<code>TutorialOperationMapper</code>。
     */
    private final TutorialOperationMapper operations;
    /**
     * tutorials。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorials;
    /**
     * groups。
     * 协作类型：<code>TutorialGroupMapper</code>。
     */
    private final TutorialGroupMapper groups;
    /**
     * 章节列表。
     * 协作类型：<code>TutorialChapterMapper</code>。
     */
    private final TutorialChapterMapper chapters;
    /**
     * 卡片集合。
     * 协作类型：<code>TutorialKnowledgeCardMapper</code>。
     */
    private final TutorialKnowledgeCardMapper cards;
    /**
     * 问题集合。
     * 协作类型：<code>TutorialQuestionMapper</code>。
     */
    private final TutorialQuestionMapper questions;
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actors;
    /**
     * reviews。
     * 协作类型：<code>ReviewSubmissionApi</code>。
     */
    private final ReviewSubmissionApi reviews;
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json;

    /**
     * 本次操作的请求参数对象处理入口。
     * 事务声明：<code>@Transactional(isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>operations.lockCatalog</code>、<code>json.valueToTree</code>、<code>operations.pending</code>、<code>operations.pendingOperations</code>、<code>actors.current</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param operation 操作；类型为 <code>String</code>
     * @param arguments arguments；类型为 <code>Object[]</code>
     * @return 类型为 <code>Object</code> 的处理结果
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Object request(String operation, Object[] arguments) {
        operations.lockCatalog();
        JsonNode args = json.valueToTree(arguments);
        String scope = scope(operation, args);
        if (operations.pending(scope) != null || operations.pending("catalog") != null
                || ("catalog".equals(scope) && !operations.pendingOperations().isEmpty())) {
            throw new TutorialStateException("相关内容有待审核操作，请先完成或取消审核");
        }
        JsonNode baseline = baseline(scope);
        validateVersion(operation, args);
        if (actors.current().getRoles().contains("SUPER_ADMIN")) {
            return execute(operation, args, scope);
        }
        TutorialOperationEntity row = new TutorialOperationEntity();
        row.setOperation(operation);
        row.setArgumentsJson(args.toString());
        row.setBaselineJson(baseline.toString());
        row.setScopeKey(scope);
        row.setPendingScope(scope);
        row.setStatus("PENDING");
        row.setApplicantAccountId(actors.current().getAccountId());
        String name = "catalog".equals(scope) ? "教程目录" : baseline.path("curriculum").path("tutorial").path("title").asText("教程");
        if ("createTutorial".equals(operation)) name = args.get(0).path("title").asText(name);
        row.setDisplayName(name + " · " + operationLabel(operation));
        try { operations.insert(row); }
        catch (DuplicateKeyException exception) { throw new TutorialStateException("相关内容已有待审核操作"); }
        var review = reviews.submit(ReviewSubmissionCommand.builder()
                .reviewType("tutorial.operation").targetModule("TUTORIAL").targetType("OPERATION")
                .targetId(row.getId()).targetRevisionRef("tutorial-operation:" + row.getId())
                .targetDisplayName(row.getDisplayName()).applicantAccountId(row.getApplicantAccountId()).build());
        return Map.of("reviewPending", true, "reviewRequestId", String.valueOf(review.getReviewRequestId()));
    }

    /**
     * 操作标签处理入口。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param operation 操作；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String operationLabel(String operation) {
        return switch (operation) {
            case "createCategory" -> "新增知识体系";
            case "updateCategory" -> "修改知识体系";
            case "deleteCategory" -> "删除知识体系";
            case "reorderCategories" -> "调整知识体系顺序";
            case "createTutorial" -> "新增教程";
            case "updateTutorial" -> "修改教程";
            case "deleteTutorial" -> "删除教程";
            case "reorderTutorials" -> "调整教程顺序";
            case "createGroup" -> "新增分组";
            case "updateGroup" -> "修改分组";
            case "deleteGroup" -> "删除分组";
            case "reorderGroups" -> "调整分组顺序";
            case "createChapter" -> "新增章节";
            case "updateChapter" -> "修改章节";
            case "updateChapterBody" -> "修改正文";
            case "deleteChapter" -> "删除章节";
            case "reorderChapters" -> "调整章节顺序";
            case "publish" -> "发布教程";
            case "withdraw" -> "撤回教程";
            case "publishChapter" -> "发布章节";
            case "withdrawChapter" -> "撤回章节";
            case "createCard" -> "新增知识卡片";
            case "updateCard" -> "修改知识卡片";
            case "deleteCard" -> "删除知识卡片";
            case "reorderCards" -> "调整知识卡片顺序";
            case "createQuestion" -> "新增章节问题";
            case "updateQuestion" -> "修改章节问题";
            case "deleteQuestion" -> "删除章节问题";
            case "reorderQuestions" -> "调整章节问题顺序";
            default -> throw new TutorialStateException("不支持的教程操作");
        };
    }

    /**
     * scope处理入口。
     * 主要协作调用：<code>groups.selectById</code>、<code>cards.selectById</code>、<code>questions.selectById</code>、<code>tutorials.byIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param op op；类型为 <code>String</code>
     * @param args args；类型为 <code>JsonNode</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String scope(String op, JsonNode args) {
        if (List.of("createCategory", "updateCategory", "deleteCategory", "reorderCategories", "createTutorial", "reorderTutorials").contains(op)) return "catalog";
        long id = args.get(0).asLong();
        Long tutorialId;
        if (List.of("createChapter", "updateGroup", "deleteGroup", "reorderChapters").contains(op)) {
            var row = groups.selectById(id);
            if (row == null) throw new TutorialStateException("分组不存在");
            tutorialId = row.getTutorialId();
        } else if (List.of("updateCard", "deleteCard", "updateQuestion", "deleteQuestion").contains(op)) {
            Long chapterId;
            if (op.endsWith("Card")) {
                var card = cards.selectById(id);
                if (card == null) throw new TutorialStateException("知识卡片不存在");
                chapterId = card.getChapterId();
            } else {
                var question = questions.selectById(id);
                if (question == null) throw new TutorialStateException("章节问题不存在");
                chapterId = question.getChapterId();
            }
            tutorialId = chapterTutorial(chapterId);
        } else if (List.of("updateChapter", "updateChapterBody", "deleteChapter", "publishChapter", "withdrawChapter", "createCard", "reorderCards", "createQuestion", "reorderQuestions").contains(op)) {
            tutorialId = chapterTutorial(id);
        } else if (List.of("updateTutorial", "deleteTutorial", "createGroup", "reorderGroups", "publish", "withdraw").contains(op)) {
            tutorialId = id;
        } else throw new TutorialStateException("不支持的教程操作");
        if (tutorials.byIdForUpdate(tutorialId) == null) throw new TutorialStateException("教程不存在");
        return "tutorial:" + tutorialId;
    }

    /**
     * 章节教程处理入口。
     * 主要协作调用：<code>chapters.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>Long</code> 的处理结果
     */
    private Long chapterTutorial(Long id) {
        var chapter = chapters.selectById(id);
        if (chapter == null) throw new TutorialStateException("章节不存在");
        return chapter.getTutorialId();
    }

    /**
     * 校验版本。
     * 主要协作调用：<code>chapters.selectById</code>、<code>cards.selectById</code>、<code>questions.selectById</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>、<code>com.starrainnotes.tutorial.content.exception.TutorialConflictException</code>；具体触发条件见对应分支。
     *
     * @param operation 操作；类型为 <code>String</code>
     * @param args args；类型为 <code>JsonNode</code>
     */
    private void validateVersion(String operation, JsonNode args) {
        Long expected = null;
        if (List.of("updateChapter", "updateChapterBody").contains(operation)) {
            var row = chapters.selectById(args.get(0).asLong());
            if (row == null) throw new TutorialStateException("章节不存在或已改变");
            expected = row.getRowVersion();
        }
        if ("updateCard".equals(operation)) {
            var row = cards.selectById(args.get(0).asLong());
            if (row == null) throw new TutorialStateException("知识卡片不存在或已改变");
            expected = row.getRowVersion();
        }
        if ("updateQuestion".equals(operation)) {
            var row = questions.selectById(args.get(0).asLong());
            if (row == null) throw new TutorialStateException("章节问题不存在或已改变");
            expected = row.getRowVersion();
        }
        if (expected != null && (!args.get(1).hasNonNull("rowVersion") || args.get(1).path("rowVersion").asLong() != expected)) {
            throw new com.starrainnotes.tutorial.content.exception.TutorialConflictException("内容版本冲突，请保留当前输入并重新加载后合并");
        }
    }

    /**
     * baseline处理入口。
     * 主要协作调用：<code>json.valueToTree</code>、<code>content.categories</code>、<code>tutorials.listOrdered</code>、<code>content.curriculum</code>、<code>json.createObjectNode</code>。
     *
     * @param scope scope；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    private JsonNode baseline(String scope) {
        if ("catalog".equals(scope)) return json.valueToTree(Map.of("categories", content.categories(), "tutorials", tutorials.listOrdered(null)));
        Long id = Long.valueOf(scope.substring("tutorial:".length()));
        var curriculum = content.curriculum(id);
        var result = json.createObjectNode();
        result.set("curriculum", json.valueToTree(curriculum));
        result.set("content", publication.previewTutorial(id));
        var exerciseRows = result.putArray("exercises");
        for (var group : curriculum.getGroups()) for (var chapter : group.getChapters()) {
            exerciseRows.add(json.valueToTree(Map.of("chapterId", chapter.getId(), "cards", exercises.cards(Long.valueOf(chapter.getId())), "questions", exercises.questions(Long.valueOf(chapter.getId())))));
        }
        return result;
    }

    /**
     * 批准教程操作。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>applyApproved</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void approve(Long id) {
        applyApproved(id, false);
    }

    /**
     * recover已批准处理入口。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>applyApproved</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void recoverApproved(Long id) {
        applyApproved(id, true);
    }

    /**
     * 应用已批准。
     * 主要协作调用：<code>operations.lockCatalog</code>、<code>operations.byIdForUpdate</code>、<code>operations.pending</code>、<code>operations.finish</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param recovery recovery；类型为 <code>boolean</code>
     */
    private void applyApproved(Long id, boolean recovery) {
        operations.lockCatalog();
        TutorialOperationEntity row = operations.byIdForUpdate(id);
        if (row == null) throw new TutorialStateException("审核操作不存在");
        if ("APPLIED".equals(row.getStatus())) return; // Review callbacks may be retried.
        if (!"PENDING".equals(row.getStatus()) && !(recovery && "FAILED".equals(row.getStatus()))) throw new TutorialStateException("操作已结束");
        var otherPending = operations.pending(row.getScopeKey());
        if (otherPending != null && !id.equals(otherPending.getId())) throw new TutorialStateException("该范围已有新的待审核操作，请先核实后处理");
        JsonNode args = read(row.getArgumentsJson());
        String currentScope = scope(row.getOperation(), args);
        if (!currentScope.equals(row.getScopeKey()) || !read(baseline(currentScope).toString()).equals(read(row.getBaselineJson()))) {
            throw new TutorialStateException("内容已改变，本次审核操作不能执行，请取消后重新提交");
        }
        validateVersion(row.getOperation(), args);
        execute(row.getOperation(), args, currentScope);
        operations.finish(id, "APPLIED");
    }

    /**
     * finish处理入口。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>operations.finish</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(Long id, String status) { operations.finish(id, status); }

    /**
     * 记录失败。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>operations.recordFailure</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param cause 原始异常，用于保留失败原因；类型为 <code>RuntimeException</code>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long id, RuntimeException cause) {
        String message = cause instanceof TutorialStateException || cause instanceof com.starrainnotes.tutorial.content.exception.TutorialConflictException
                ? cause.getMessage() : "业务应用失败，请联系超级管理员核实内容与操作日志后恢复。";
        operations.recordFailure(id, message == null ? "业务应用失败" : message.substring(0, Math.min(500, message.length())));
    }

    /**
     * 待处理Operations处理入口。
     * 主要协作调用：<code>operations.pendingOperations</code>。
     * @return 本次处理得到的条目集合
     */
    public List<Map<String, Object>> pendingOperations() {
        return operations.pendingOperations().stream().map(row -> Map.<String, Object>of(
                "scopeKey", row.getScopeKey(), "operation", row.getOperation(), "displayName", row.getDisplayName())).toList();
    }

    /**
     * 读取教程操作。
     * 主要协作调用：<code>json.readTree</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    public JsonNode read(String value) {
        try { return json.readTree(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new TutorialStateException("审核数据无法读取"); }
    }

    /**
     * 执行教程操作。
     * 主要协作调用：<code>cards.selectById</code>、<code>questions.selectById</code>、<code>content.createTutorial</code>、<code>json.convertValue</code>、<code>content.updateTutorial</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param operation 操作；类型为 <code>String</code>
     * @param args args；类型为 <code>JsonNode</code>
     * @param scope scope；类型为 <code>String</code>
     * @return 类型为 <code>Object</code> 的处理结果
     */
    private Object execute(String operation, JsonNode args, String scope) {
        Long changedChapterId = switch (operation) {
            case "updateChapter", "updateChapterBody", "createCard", "reorderCards", "createQuestion", "reorderQuestions" -> args.get(0).asLong();
            case "updateCard", "deleteCard" -> cards.selectById(args.get(0).asLong()).getChapterId();
            case "updateQuestion", "deleteQuestion" -> questions.selectById(args.get(0).asLong()).getChapterId();
            default -> null;
        };
        Object result = switch (operation) {
            case "createTutorial" -> content.createTutorial(json.convertValue(args.get(0), TutorialCreateDTO.class));
            case "updateTutorial" -> content.updateTutorial(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), TutorialUpdateDTO.class));
            case "deleteTutorial" -> { content.deleteTutorial(json.convertValue(args.get(0), Long.class)); yield null; }
            case "createGroup" -> content.createGroup(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), String.class));
            case "reorderGroups" -> { content.reorderGroups(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), new TypeReference<List<Long>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    {})); yield null; }
            case "createCategory" -> content.createCategory(json.convertValue(args.get(0), String.class));
            case "updateCategory" -> content.updateCategory(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), String.class));
            case "deleteCategory" -> { content.deleteCategory(json.convertValue(args.get(0), Long.class)); yield null; }
            case "reorderCategories" -> { content.reorderCategories(json.convertValue(args.get(0), new TypeReference<List<Long>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    {})); yield null; }
            case "reorderTutorials" -> { content.reorderTutorials(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), new TypeReference<List<Long>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    {})); yield null; }
            case "createCard" -> exercises.createCard(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), KnowledgeCardDTO.class));
            case "updateCard" -> exercises.updateCard(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), KnowledgeCardDTO.class));
            case "deleteCard" -> { exercises.deleteCard(json.convertValue(args.get(0), Long.class)); yield null; }
            case "reorderCards" -> { exercises.reorderCards(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), new TypeReference<List<Long>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    {})); yield null; }
            case "createQuestion" -> exercises.createQuestion(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), ChapterQuestionDTO.class));
            case "updateQuestion" -> exercises.updateQuestion(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), ChapterQuestionDTO.class));
            case "deleteQuestion" -> { exercises.deleteQuestion(json.convertValue(args.get(0), Long.class)); yield null; }
            case "reorderQuestions" -> { exercises.reorderQuestions(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), new TypeReference<List<Long>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    {})); yield null; }
            case "publish" -> publication.publish(json.convertValue(args.get(0), Long.class));
            case "withdraw" -> publication.withdraw(json.convertValue(args.get(0), Long.class));
            case "updateGroup" -> content.updateGroup(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), String.class));
            case "deleteGroup" -> { content.deleteGroup(json.convertValue(args.get(0), Long.class)); yield null; }
            case "createChapter" -> content.createChapter(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), ChapterCreateDTO.class));
            case "reorderChapters" -> { content.reorderChapters(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), new TypeReference<List<Long>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    {})); yield null; }
            case "updateChapter" -> content.updateChapter(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), ChapterUpdateDTO.class));
            case "updateChapterBody" -> content.updateChapterBody(json.convertValue(args.get(0), Long.class), json.convertValue(args.get(1), ChapterBodyDTO.class));
            case "deleteChapter" -> { content.deleteChapter(json.convertValue(args.get(0), Long.class)); yield null; }
            case "publishChapter" -> { publication.publishChapter(json.convertValue(args.get(0), Long.class)); yield null; }
            case "withdrawChapter" -> { publication.withdrawChapter(json.convertValue(args.get(0), Long.class)); yield null; }
            default -> throw new TutorialStateException("不支持的教程操作");
        };
        if (!List.of("publish", "withdraw", "publishChapter", "withdrawChapter", "deleteTutorial").contains(operation)) {
            if ("catalog".equals(scope)) {
                for (var tutorial : tutorials.listOrdered(null)) publication.refreshPublished(tutorial.getId(), operation, null);
            } else publication.refreshPublished(Long.valueOf(scope.substring("tutorial:".length())), operation, changedChapterId);
        }
        return result;
    }
}
