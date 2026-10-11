/*
 * EvidenceStudyPlanService：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、EvidencePlanDTO、LearningPageQueryDTO、PageResult、EvidenceLearningMapper、EvidenceAlgorithms。
 * 阅读入口：list（列出证据学习计划）；list（列出证据学习计划）；get（读取证据学习计划）；history（历史处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.dto.EvidencePlanDTO;
import com.starrainnotes.tutorial.learning.dto.LearningPageQueryDTO;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.exception.*;
import com.starrainnotes.tutorial.learning.mapper.EvidenceLearningMapper;
import com.starrainnotes.tutorial.learning.service.EvidenceAlgorithms;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 证据学习计划业务服务，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class EvidenceStudyPlanService {
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actors;
    /**
     * 内容。
     */
    private final LearningContentAccess content;
    /**
     * 数据映射。
     * 协作类型：<code>EvidenceLearningMapper</code>。
     */
    private final EvidenceLearningMapper mapper;

    /**
     * 当前操作对应的身份对象处理入口。
     * 主要协作调用：<code>actors.current</code>。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    private Long actor() { return actors.current().getAccountId(); }
    /**
     * 锁处理入口。
     * 主要协作调用：<code>mapper.ensureAccountLock</code>、<code>mapper.lockAccount</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     */
    private void lock(Long accountId) {
        mapper.ensureAccountLock(accountId);
        mapper.lockAccount(accountId);
    }

    /**
     * 列出证据学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>list</code>。
     * @return 本次处理得到的条目集合
     */
    @Transactional
    public List<Plan> list() {
        return list(false);
    }

    /**
     * 列出证据学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.reconcileTasks</code>、<code>mapper.reconcilePlans</code>、<code>mapper.currentPlans</code>、<code>mapper.listPlans</code>。
     *
     * @param currentOnly 当前Only；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    @Transactional
    public List<Plan> list(boolean currentOnly) {
        Long accountId = actor();
        lock(accountId);
        mapper.reconcileTasks(accountId);
        mapper.reconcilePlans(accountId);
        return (currentOnly ? mapper.currentPlans(accountId) : mapper.listPlans(accountId)).stream()
                .map(plan -> decorate(accountId, plan, false)).toList();
    }

    /**
     * 读取证据学习计划。
     * 关键处理步骤调用 <code>decorate</code>、<code>actor</code>、<code>require</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    public Plan get(Long id) { return decorate(actor(), require(actor(), id), true); }

    /**
     * 检查所需证据学习计划。
     * 主要协作调用：<code>mapper.plan</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    private Plan require(Long accountId, Long id) {
        Plan plan = mapper.plan(accountId, id);
        if (plan == null) throw new LearningResourceNotFoundException();
        return plan;
    }

    /**
     * decorate处理入口。
     * 主要协作调用：<code>mapper.planChapters</code>、<code>content.publishedTutorial</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param plan 计划；类型为 <code>Plan</code>
     * @param withTasks 包含Tasks；类型为 <code>boolean</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    private Plan decorate(Long accountId, Plan plan, boolean withTasks) {
        plan.setChapters(mapper.planChapters(accountId, plan.getId()));
        plan.setTotalChapters(plan.getChapters().size());
        plan.setCompletedChapters((int) plan.getChapters().stream().filter(c -> Boolean.TRUE.equals(c.getCompleted())).count());
        if (withTasks) {
            plan.setTasks(tasks(plan.getId()));
            try {
                if (plan.getTutorialId() == null) return plan;
                JsonNode snapshot = content.publishedTutorial(plan.getTutorialId());
                plan.setTutorialTitle(snapshot.path("title").asText());
                plan.setTutorialSlug(snapshot.path("slug").asText());
                for (JsonNode group : snapshot.path("groups")) for (JsonNode chapter : group.path("chapters")) {
                    plan.getChapters().stream().filter(c -> c.getChapterId() == chapter.path("id").asLong()).forEach(c -> {
                        c.setChapterSlug(chapter.path("slug").asText());
                        c.setGroupTitle(group.path("title").asText());
                    });
                }
            } catch (LearningResourceNotFoundException ignored) {
                // Ended plans remain readable after the source is withdrawn.
                plan.setTutorialSlug(null);
            }
        }
        return plan;
    }

    /**
     * 历史处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.reconcileTasks</code>、<code>mapper.reconcilePlans</code>、<code>mapper.planHistory</code>、<code>mapper.countPlanHistory</code>。
     *
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;Plan&gt;</code> 的处理结果
     */
    @Transactional
    public PageResult<Plan> history(LearningPageQueryDTO query) {
        query.validate(Set.of("COMPLETED", "CANCELLED"));
        Long accountId = actor();lock(accountId);mapper.reconcileTasks(accountId);mapper.reconcilePlans(accountId);
        return PageResult.<Plan>builder().items(mapper.planHistory(accountId,query)).total(mapper.countPlanHistory(accountId,query))
                .page(query.getPage()).pageSize(query.getPageSize()).build();
    }

    /**
     * 预览证据学习计划。
     * 关键处理步骤调用 <code>require</code>、<code>actor</code>、<code>definition</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>
     * @param excludePlanId exclude计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    public Plan preview(EvidencePlanDTO request, Long excludePlanId) {
        if (excludePlanId != null) require(actor(), excludePlanId);
        return definition(actor(), request, excludePlanId);
    }

    /**
     * definition处理入口。
     * 主要协作调用：<code>content.publishedTutorial</code>、<code>mapper.conflictingChapters</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>、<code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>
     * @param excludePlanId exclude计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    private Plan definition(Long accountId, EvidencePlanDTO request, Long excludePlanId) {
        if (request.getTutorialId() == null || request.getName() == null || request.getName().isBlank()
                || request.getName().length() > 160 || request.getTargetCardsPerTask() == null
                || request.getTargetCardsPerTask() < 5 || request.getTargetCardsPerTask() > 30) {
            throw new LearningInvalidRequestException("请填写计划名称和 5～30 个目标知识点");
        }
        JsonNode tutorial = content.publishedTutorial(request.getTutorialId());
        Set<Long> groupIds = new HashSet<>(request.getGroupIds() == null ? List.of() : request.getGroupIds());
        Set<Long> chapterIds = new HashSet<>(request.getChapterIds() == null ? List.of() : request.getChapterIds());
        Set<Long> knownGroups = new HashSet<>(), knownChapters = new HashSet<>();
        Set<Long> conflicts = new HashSet<>(mapper.conflictingChapters(accountId, excludePlanId));
        List<Chapter> selected = new ArrayList<>();
        int groupOrder = 0;
        for (JsonNode group : tutorial.path("groups")) {
            Long groupId = group.path("id").asLong();
            knownGroups.add(groupId);
            int chapterOrder = 0;
            for (JsonNode node : group.path("chapters")) {
                Long chapterId = node.path("id").asLong();
                knownChapters.add(chapterId);
                if (request.isEntireTutorial() || groupIds.contains(groupId) || chapterIds.contains(chapterId)) {
                    if (node.path("cards").isEmpty()) throw new LearningInvalidRequestException(node.path("title").asText() + "没有启用的知识卡片");
                    if (conflicts.contains(chapterId)) throw new LearningStateConflictException(node.path("title").asText() + "已在另一个未结束计划中");
                    Chapter chapter = new Chapter();
                    chapter.setAccountId(accountId);chapter.setTutorialId(request.getTutorialId());
                    chapter.setGroupId(groupId);chapter.setChapterId(chapterId);
                    chapter.setGroupOrder(groupOrder);chapter.setChapterOrder(chapterOrder);
                    chapter.setCardCount(node.path("cards").size());chapter.setQuestionCount(node.path("questions").size());
                    chapter.setChapterTitle(node.path("title").asText());selected.add(chapter);
                }
                chapterOrder++;
            }
            groupOrder++;
        }
        if (!knownGroups.containsAll(groupIds) || !knownChapters.containsAll(chapterIds)) {
            throw new LearningInvalidRequestException("所选分组或章节不属于此公开教程");
        }
        if (selected.isEmpty()) throw new LearningInvalidRequestException("请选择至少一个章节");
        Plan plan = new Plan();plan.setAccountId(accountId);plan.setTutorialId(request.getTutorialId());
        plan.setName(request.getName().strip());plan.setStatus("DRAFT");plan.setTargetCardsPerTask(request.getTargetCardsPerTask());
        plan.setChapters(selected);plan.setTasks(EvidenceAlgorithms.tasks(selected, request.getTargetCardsPerTask()));
        plan.setTotalChapters(selected.size());plan.setCompletedChapters(0);
        return plan;
    }

    /**
     * 创建证据学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.listPlans</code>、<code>mapper.planChapters</code>、<code>mapper.insertPlan</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    @Transactional
    public Plan create(EvidencePlanDTO request) {
        Long accountId = actor();lock(accountId);
        long count = mapper.listPlans(accountId).stream().filter(p -> Set.of("DRAFT","ACTIVE","PAUSED").contains(p.getStatus())).count();
        if (count >= 5) throw new LearningStateConflictException("最多同时保留五个未结束计划");
        Plan plan = definition(accountId, request, null);
        Set<Long> scope = new HashSet<>(plan.getChapters().stream().map(Chapter::getChapterId).toList());
        for (Plan existing : mapper.listPlans(accountId)) {
            if (!request.getTutorialId().equals(existing.getTutorialId()) || !"COMPLETED".equals(existing.getStatus())) continue;
            Set<Long> oldScope = new HashSet<>(mapper.planChapters(accountId, existing.getId()).stream().map(Chapter::getChapterId).toList());
            if (scope.equals(oldScope)) return restart(existing.getId());
        }
        mapper.insertPlan(plan);persist(plan);
        return get(plan.getId());
    }

    /**
     * 更新证据学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.planChapters</code>、<code>mapper.updatePlan</code>、<code>mapper.deleteTaskChapters</code>、<code>mapper.deleteTasks</code>、<code>mapper.deletePlanChapters</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    @Transactional
    public Plan update(Long id, EvidencePlanDTO request) {
        Long accountId = actor();lock(accountId);Plan old = require(accountId,id);
        if (!"DRAFT".equals(old.getStatus())) throw new LearningStateConflictException("只有草稿计划可以修改学习范围");
        Plan plan = definition(accountId,request,id);
        if (old.getStudyRound() > 1 && !new HashSet<>(plan.getChapters().stream().map(Chapter::getChapterId).toList()).equals(
                new HashSet<>(mapper.planChapters(accountId,id).stream().map(Chapter::getChapterId).toList()))) {
            throw new LearningStateConflictException("再次学习保留原知识范围；学习其他分组请单独选择范围");
        }
        plan.setId(id);plan.setStudyRound(old.getStudyRound());mapper.updatePlan(plan);
        mapper.deleteTaskChapters(accountId,id);mapper.deleteTasks(accountId,id);mapper.deletePlanChapters(accountId,id);
        persist(plan);return get(id);
    }

    /**
     * 持久化证据学习计划。
     * 主要协作调用：<code>mapper.insertPlanChapter</code>、<code>mapper.insertTask</code>、<code>mapper.insertTaskChapter</code>。
     *
     * @param plan 计划；类型为 <code>Plan</code>
     */
    private void persist(Plan plan) {
        for (Chapter chapter : plan.getChapters()) { chapter.setPlanId(plan.getId());mapper.insertPlanChapter(chapter); }
        for (Task task : plan.getTasks()) {
            task.setPlanId(plan.getId());task.setAccountId(plan.getAccountId());task.setStudyRound(plan.getStudyRound());mapper.insertTask(task);
            int sequence = 0;
            for (Chapter chapter : task.getChapters()) mapper.insertTaskChapter(task.getId(), chapter, ++sequence);
        }
    }

    /** Reuse the plan identity while keeping all earlier task/session evidence. */
    @Transactional
    public Plan restart(Long id) {
        Long accountId = actor();lock(accountId);
        mapper.reconcileTasks(accountId);mapper.reconcilePlans(accountId);
        Plan old = require(accountId, id);
        if (!"COMPLETED".equals(old.getStatus())) throw conflict();
        if (mapper.currentPlans(accountId).size() >= 5) throw new LearningStateConflictException("最多同时保留五个未结束计划");
        EvidencePlanDTO request = new EvidencePlanDTO();request.setTutorialId(old.getTutorialId());request.setName(old.getName());
        request.setTargetCardsPerTask(old.getTargetCardsPerTask());
        request.setChapterIds(mapper.planChapters(accountId,id).stream().map(Chapter::getChapterId).toList());
        Plan next = definition(accountId,request,id);next.setId(id);next.setStudyRound(old.getStudyRound()+1);
        mapper.restartPlan(accountId,id);mapper.clearPlanCompletion(accountId,id);
        for (Task task : next.getTasks()) {
            task.setPlanId(id);task.setAccountId(accountId);task.setStudyRound(next.getStudyRound());mapper.insertTask(task);
            int sequence = 0;
            for (Chapter chapter : task.getChapters()) mapper.insertTaskChapter(task.getId(),chapter,++sequence);
        }
        return get(id);
    }

    /**
     * transition处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.planChapters</code>、<code>content.chapter</code>、<code>mapper.tasks</code>、<code>mapper.planStatus</code>、<code>mapper.reconcileTasks</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>Plan</code> 的处理结果
     */
    @Transactional
    public Plan transition(Long id, String action) {
        Long accountId = actor();lock(accountId);Plan plan = require(accountId,id);
        if ("restart".equals(action)) return restart(id);
        String from = plan.getStatus();String to;
        switch (action) {
            case "activate": if (!"DRAFT".equals(from)) throw conflict();to="ACTIVE";break;
            case "pause": if (!"ACTIVE".equals(from)) throw conflict();to="PAUSED";break;
            case "resume": if (!"PAUSED".equals(from)) throw conflict();to="ACTIVE";break;
            case "cancel": if (!Set.of("DRAFT","ACTIVE","PAUSED").contains(from)) throw conflict();to="CANCELLED";break;
            default: throw conflict();
        }
        if ("ACTIVE".equals(to)) {
            // Scope remains frozen, but every selected chapter must still be learnable.
            for (Chapter chapter : mapper.planChapters(accountId,id)) {
                if (content.chapter(chapter.getChapterId()).getSnapshot().path("cards").isEmpty()) throw conflict();
            }
            if (mapper.tasks(accountId,id).isEmpty()) throw new LearningStateConflictException("计划没有任务，请编辑草稿重新生成");
        }
        mapper.planStatus(accountId,id,to);mapper.reconcileTasks(accountId);mapper.reconcilePlans(accountId);return get(id);
    }

    /**
     * 冲突处理入口。
     * @return 类型为 <code>LearningStateConflictException</code> 的处理结果
     */
    private LearningStateConflictException conflict() { return new LearningStateConflictException("当前计划状态不支持此操作"); }

    /**
     * tasks处理入口。
     * 主要协作调用：<code>mapper.tasks</code>、<code>mapper.taskChapters</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    public List<Task> tasks(Long planId) {
        Long accountId = actor();require(accountId,planId);
        List<Task> tasks = mapper.tasks(accountId,planId);
        for (Task task : tasks) task.setChapters(mapper.taskChapters(accountId,task.getId()));
        return tasks;
    }

    /**
     * 任务处理入口。
     * 主要协作调用：<code>mapper.task</code>、<code>mapper.taskChapters</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>Task</code> 的处理结果
     */
    public Task task(Long id) {
        Long accountId = actor();Task task = mapper.task(accountId,id);
        if (task == null) throw new LearningResourceNotFoundException();
        task.setChapters(mapper.taskChapters(accountId,id));return task;
    }

    /**
     * 开始处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.startTask</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>Task</code> 的处理结果
     */
    @Transactional
    public Task start(Long id) {
        Long accountId = actor();lock(accountId);Task task=task(id);
        Plan plan = require(accountId,task.getPlanId());
        if (!task.getStudyRound().equals(plan.getStudyRound())) throw new LearningStateConflictException("此任务属于历史学习轮次");
        if (!"ACTIVE".equals(plan.getStatus())) throw new LearningStateConflictException("请先启动或恢复计划");
        mapper.startTask(accountId,id);return task(id);
    }
}
