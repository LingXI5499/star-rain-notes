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

@Service
@RequiredArgsConstructor
public class EvidenceStudyPlanService {
    private final CurrentActorApi actors;
    private final LearningContentAccess content;
    private final EvidenceLearningMapper mapper;

    private Long actor() { return actors.current().getAccountId(); }
    private void lock(Long accountId) {
        mapper.ensureAccountLock(accountId);
        mapper.lockAccount(accountId);
    }

    @Transactional
    public List<Plan> list() {
        return list(false);
    }

    @Transactional
    public List<Plan> list(boolean currentOnly) {
        Long accountId = actor();
        lock(accountId);
        mapper.reconcileTasks(accountId);
        mapper.reconcilePlans(accountId);
        return (currentOnly ? mapper.currentPlans(accountId) : mapper.listPlans(accountId)).stream()
                .map(plan -> decorate(accountId, plan, false)).toList();
    }

    public Plan get(Long id) { return decorate(actor(), require(actor(), id), true); }

    private Plan require(Long accountId, Long id) {
        Plan plan = mapper.plan(accountId, id);
        if (plan == null) throw new LearningResourceNotFoundException();
        return plan;
    }

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

    @Transactional
    public PageResult<Plan> history(LearningPageQueryDTO query) {
        query.validate(Set.of("COMPLETED", "CANCELLED"));
        Long accountId = actor();lock(accountId);mapper.reconcileTasks(accountId);mapper.reconcilePlans(accountId);
        return PageResult.<Plan>builder().items(mapper.planHistory(accountId,query)).total(mapper.countPlanHistory(accountId,query))
                .page(query.getPage()).pageSize(query.getPageSize()).build();
    }

    public Plan preview(EvidencePlanDTO request, Long excludePlanId) {
        if (excludePlanId != null) require(actor(), excludePlanId);
        return definition(actor(), request, excludePlanId);
    }

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

    private LearningStateConflictException conflict() { return new LearningStateConflictException("当前计划状态不支持此操作"); }

    public List<Task> tasks(Long planId) {
        Long accountId = actor();require(accountId,planId);
        List<Task> tasks = mapper.tasks(accountId,planId);
        for (Task task : tasks) task.setChapters(mapper.taskChapters(accountId,task.getId()));
        return tasks;
    }

    public Task task(Long id) {
        Long accountId = actor();Task task = mapper.task(accountId,id);
        if (task == null) throw new LearningResourceNotFoundException();
        task.setChapters(mapper.taskChapters(accountId,id));return task;
    }

    @Transactional
    public Task start(Long id) {
        Long accountId = actor();lock(accountId);Task task=task(id);
        Plan plan = require(accountId,task.getPlanId());
        if (!task.getStudyRound().equals(plan.getStudyRound())) throw new LearningStateConflictException("此任务属于历史学习轮次");
        if (!"ACTIVE".equals(plan.getStatus())) throw new LearningStateConflictException("请先启动或恢复计划");
        mapper.startTask(accountId,id);return task(id);
    }
}
