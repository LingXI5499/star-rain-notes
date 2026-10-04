package com.starrainnotes.tutorial.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.dto.StudyPlanDTO;
import com.starrainnotes.tutorial.entity.StudyPlanEntity;
import com.starrainnotes.tutorial.entity.StudyTaskEntity;
import com.starrainnotes.tutorial.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.exception.LearningResourceNotFoundException;
import com.starrainnotes.tutorial.exception.LearningStateConflictException;
import com.starrainnotes.tutorial.mapper.StudyPlanMapper;
import com.starrainnotes.tutorial.mapper.StudyTaskMapper;
import com.starrainnotes.tutorial.service.LearningProgressService;
import com.starrainnotes.tutorial.service.StudyPlanService;
import com.starrainnotes.tutorial.vo.StudyPlanVO;
import com.starrainnotes.tutorial.vo.StudyTaskVO;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyPlanServiceImpl implements StudyPlanService {
    private static final ZoneId STUDY_ZONE = ZoneId.of("Asia/Shanghai");
    private final CurrentActorApi currentActorApi;
    private final LearningContentAccess content;
    private final StudyPlanMapper planMapper;
    private final StudyTaskMapper taskMapper;
    private final LearningProgressService progressService;
    private final LearningEventWriter events;
    private final ObjectMapper objectMapper;

    private Long accountId() {
        return currentActorApi.current().getAccountId();
    }

    private LocalDate todayDate() {
        return LocalDate.now(STUDY_ZONE);
    }

    private StudyPlanEntity ownPlan(Long planId) {
        StudyPlanEntity row = planId == null ? null : planMapper.selectById(planId);
        if (row == null || !accountId().equals(row.getAccountId())) {
            throw new LearningResourceNotFoundException();
        }
        return row;
    }

    private StudyTaskEntity ownTask(Long taskId) {
        StudyTaskEntity row = taskId == null ? null : taskMapper.selectById(taskId);
        if (row == null || !accountId().equals(row.getAccountId())) {
            throw new LearningResourceNotFoundException();
        }
        return row;
    }

    private List<Integer> weekdays(StudyPlanEntity row) {
        try {
            return objectMapper.readValue(row.getStudyWeekdaysJson(), new TypeReference<List<Integer>>() { });
        } catch (JsonProcessingException exception) {
            throw new LearningStateConflictException("学习日配置不可读取");
        }
    }

    private StudyPlanVO view(StudyPlanEntity row) {
        return StudyPlanVO.builder().id(String.valueOf(row.getId()))
                .tutorialId(String.valueOf(row.getTutorialId())).name(row.getName())
                .scopeType(row.getScopeType()).scopeId(String.valueOf(row.getScopeId()))
                .startDate(row.getStartDate()).endDate(row.getEndDate())
                .studyWeekdays(weekdays(row)).dailyTargetMinutes(row.getDailyTargetMinutes())
                .status(row.getStatus()).generatedVersion(row.getGeneratedVersion()).build();
    }

    private StudyTaskVO view(StudyTaskEntity row) {
        LearningContentAccess.ChapterRef chapter = content.chapter(row.getChapterId());
        return StudyTaskVO.builder().id(String.valueOf(row.getId()))
                .planId(String.valueOf(row.getPlanId()))
                .tutorialId(String.valueOf(row.getTutorialId()))
                .chapterId(String.valueOf(row.getChapterId()))
                .tutorialSlug(chapter.getTutorialSlug()).chapterSlug(chapter.getChapterSlug())
                .chapterTitle(chapter.getTitle()).taskDate(row.getTaskDate())
                .status(row.getStatus()).startedAt(row.getStartedAt())
                .completedAt(row.getCompletedAt()).build();
    }

    private void apply(StudyPlanEntity row, StudyPlanDTO request) {
        if (request == null || request.getTutorialId() == null || request.getScopeId() == null
                || request.getStartDate() == null || request.getName() == null
                || request.getName().isBlank() || request.getName().strip().length() > 160
                || request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())
                || request.getStudyWeekdays() == null || request.getStudyWeekdays().isEmpty()) {
            throw new LearningInvalidRequestException("请填写有效的学习计划");
        }
        Set<Integer> days = new HashSet<>(request.getStudyWeekdays());
        if (days.size() != request.getStudyWeekdays().size() || days.stream().anyMatch(day -> day == null || day < 1 || day > 7)) {
            throw new LearningInvalidRequestException("学习日必须是互不重复的周一至周日");
        }
        Integer target = request.getDailyTargetMinutes();
        if (target != null && (target < 5 || target > 600)) {
            throw new LearningInvalidRequestException("每日目标应在 5 到 600 分钟之间");
        }
        List<Long> chapterIds = content.scopedChapterIds(request.getTutorialId(), request.getScopeType(), request.getScopeId());
        if (chapterIds.size() > 1000) {
            throw new LearningInvalidRequestException("单个计划不能超过 1000 个章节");
        }
        row.setTutorialId(request.getTutorialId());
        row.setName(request.getName().strip());
        row.setScopeType(request.getScopeType());
        row.setScopeId(request.getScopeId());
        row.setStartDate(request.getStartDate());
        row.setEndDate(request.getEndDate());
        row.setDailyTargetMinutes(target);
        try {
            row.setStudyWeekdaysJson(objectMapper.writeValueAsString(request.getStudyWeekdays()));
        } catch (JsonProcessingException exception) {
            throw new LearningInvalidRequestException("学习日配置无效");
        }
    }

    @Override
    @Transactional
    public StudyPlanVO create(StudyPlanDTO request) {
        StudyPlanEntity row = new StudyPlanEntity();
        row.setAccountId(accountId());
        row.setStatus("DRAFT");
        row.setGeneratedVersion(0);
        apply(row, request);
        planMapper.insert(row);
        events.task(row.getAccountId(), "STUDY_PLAN_CREATED", row.getTutorialId(), null, null);
        return view(row);
    }

    @Override
    @Transactional
    public StudyPlanVO update(Long planId, StudyPlanDTO request) {
        StudyPlanEntity row = ownPlan(planId);
        if (!Set.of("DRAFT", "ACTIVE", "PAUSED").contains(row.getStatus())) {
            throw new LearningStateConflictException("已结束的计划不能修改");
        }
        apply(row, request);
        if (!"DRAFT".equals(row.getStatus())) {
            row.setGeneratedVersion(row.getGeneratedVersion() + 1);
            taskMapper.delete(new LambdaQueryWrapper<StudyTaskEntity>()
                    .eq(StudyTaskEntity::getPlanId, row.getId())
                    .ne(StudyTaskEntity::getStatus, "COMPLETED"));
        }
        planMapper.updateById(row);
        if ("ACTIVE".equals(row.getStatus())) {
            generate(row);
        }
        return view(row);
    }

    private void generate(StudyPlanEntity plan) {
        List<Long> chapters = content.scopedChapterIds(plan.getTutorialId(), plan.getScopeType(), plan.getScopeId());
        Set<Long> completed = new HashSet<>();
        taskMapper.selectList(new LambdaQueryWrapper<StudyTaskEntity>()
                .eq(StudyTaskEntity::getPlanId, plan.getId())
                .eq(StudyTaskEntity::getStatus, "COMPLETED"))
                .forEach(row -> completed.add(row.getChapterId()));
        LocalDate day = plan.getStartDate().isAfter(todayDate()) ? plan.getStartDate() : todayDate();
        Set<Integer> studyDays = new HashSet<>(weekdays(plan));
        int capacity = Math.max(1, (plan.getDailyTargetMinutes() == null ? 40 : plan.getDailyTargetMinutes()) / 20);
        int sequence = 0;
        for (Long chapterId : chapters) {
            if (completed.contains(chapterId)) {
                continue;
            }
            while (!studyDays.contains(day.getDayOfWeek().getValue()) || sequence >= capacity) {
                day = day.plusDays(1);
                sequence = 0;
            }
            if (plan.getEndDate() != null && day.isAfter(plan.getEndDate())) {
                throw new LearningInvalidRequestException("结束日期不足以安排全部章节，请延长计划时间");
            }
            taskMapper.insertPlanned(plan.getId(), plan.getAccountId(), plan.getTutorialId(),
                    chapterId, day, ++sequence, plan.getGeneratedVersion());
        }
    }

    @Override
    @Transactional
    public StudyPlanVO transition(Long planId, String action) {
        StudyPlanEntity row = ownPlan(planId);
        String old = row.getStatus();
        switch (action) {
            case "activate" -> {
                if (!"DRAFT".equals(old)) throw new LearningStateConflictException("只有草稿计划可以启动");
                row.setStatus("ACTIVE");
                row.setGeneratedVersion(row.getGeneratedVersion() + 1);
            }
            case "pause" -> {
                if (!"ACTIVE".equals(old)) throw new LearningStateConflictException("只有进行中的计划可以暂停");
                row.setStatus("PAUSED");
            }
            case "resume" -> {
                if (!"PAUSED".equals(old)) throw new LearningStateConflictException("只有已暂停的计划可以恢复");
                row.setStatus("ACTIVE");
            }
            case "finish", "cancel" -> {
                if (!Set.of("ACTIVE", "PAUSED").contains(old)) {
                    throw new LearningStateConflictException("只能结束已启动的计划");
                }
                row.setStatus("finish".equals(action) ? "COMPLETED" : "CANCELLED");
                taskMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<StudyTaskEntity>()
                        .eq(StudyTaskEntity::getPlanId, row.getId())
                        .in(StudyTaskEntity::getStatus, "TODO", "IN_PROGRESS", "OVERDUE")
                        .set(StudyTaskEntity::getStatus, "SKIPPED"));
            }
            default -> throw new LearningInvalidRequestException("不支持的计划操作");
        }
        planMapper.updateById(row);
        if ("activate".equals(action)) {
            generate(row);
        }
        return view(row);
    }

    @Override
    public StudyPlanVO plan(Long planId) {
        return view(ownPlan(planId));
    }

    @Override
    public List<StudyPlanVO> plans() {
        return planMapper.selectList(new LambdaQueryWrapper<StudyPlanEntity>()
                .eq(StudyPlanEntity::getAccountId, accountId())
                .orderByDesc(StudyPlanEntity::getUpdatedAt).last("LIMIT 100"))
                .stream().map(this::view).toList();
    }

    @Override
    public List<StudyTaskVO> planTasks(Long planId) {
        ownPlan(planId);
        List<StudyTaskVO> result = new ArrayList<>();
        for (StudyTaskEntity row : taskMapper.selectList(new LambdaQueryWrapper<StudyTaskEntity>()
                .eq(StudyTaskEntity::getPlanId, planId)
                .orderByAsc(StudyTaskEntity::getTaskDate, StudyTaskEntity::getSequenceNo).last("LIMIT 1000"))) {
            try { result.add(view(row)); } catch (LearningResourceNotFoundException ignored) { }
        }
        return result;
    }

    @Override
    @Transactional
    public List<StudyTaskVO> today() {
        Long actor = accountId();
        taskMapper.markOverdue(actor, todayDate());
        List<StudyTaskVO> result = new ArrayList<>();
        for (StudyTaskEntity row : taskMapper.selectList(new LambdaQueryWrapper<StudyTaskEntity>()
                .eq(StudyTaskEntity::getAccountId, actor)
                .le(StudyTaskEntity::getTaskDate, todayDate())
                .in(StudyTaskEntity::getStatus, "TODO", "IN_PROGRESS", "OVERDUE")
                .orderByAsc(StudyTaskEntity::getTaskDate, StudyTaskEntity::getSequenceNo).last("LIMIT 100"))) {
            StudyPlanEntity plan = planMapper.selectById(row.getPlanId());
            if (plan != null && "ACTIVE".equals(plan.getStatus())) {
                try { result.add(view(row)); } catch (LearningResourceNotFoundException ignored) { }
            }
        }
        return result;
    }

    private void requireActivePlan(StudyTaskEntity task) {
        StudyPlanEntity plan = ownPlan(task.getPlanId());
        if (!"ACTIVE".equals(plan.getStatus()) || !task.getGenerationVersion().equals(plan.getGeneratedVersion())) {
            throw new LearningStateConflictException("任务所属计划未启用或任务已过期");
        }
        content.chapter(task.getChapterId());
    }

    @Override
    @Transactional
    public StudyTaskVO start(Long taskId) {
        StudyTaskEntity task = ownTask(taskId);
        requireActivePlan(task);
        if (taskMapper.start(taskId, accountId()) != 1) {
            throw new LearningStateConflictException("任务当前状态不能开始");
        }
        events.task(accountId(), "STUDY_TASK_STARTED", task.getTutorialId(), task.getChapterId(), taskId);
        return view(ownTask(taskId));
    }

    @Override
    @Transactional
    public StudyTaskVO complete(Long taskId) {
        StudyTaskEntity task = ownTask(taskId);
        requireActivePlan(task);
        if (taskMapper.complete(taskId, accountId()) != 1) {
            throw new LearningStateConflictException("请先开始任务，已完成任务不能重复提交");
        }
        progressService.complete(task.getChapterId());
        events.task(accountId(), "STUDY_TASK_COMPLETED", task.getTutorialId(), task.getChapterId(), taskId);
        return view(ownTask(taskId));
    }

    @Override
    @Transactional
    public StudyTaskVO skip(Long taskId) {
        StudyTaskEntity task = ownTask(taskId);
        requireActivePlan(task);
        if (taskMapper.skip(taskId, accountId()) != 1) {
            throw new LearningStateConflictException("任务当前状态不能跳过");
        }
        return view(ownTask(taskId));
    }
}
