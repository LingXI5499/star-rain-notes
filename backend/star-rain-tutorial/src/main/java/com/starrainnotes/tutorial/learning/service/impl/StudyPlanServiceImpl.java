/*
 * StudyPlanServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、StudyPlanDTO、StudyPlanEntity、StudyTaskEntity、LearningInvalidRequestException、LearningResourceNotFoundException、LearningStateConflictException。
 * 阅读入口：create（创建学习计划）；update（更新学习计划）；transition（transition处理入口）；plan（计划处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.dto.StudyPlanDTO;
import com.starrainnotes.tutorial.learning.entity.StudyPlanEntity;
import com.starrainnotes.tutorial.learning.entity.StudyTaskEntity;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.learning.exception.LearningResourceNotFoundException;
import com.starrainnotes.tutorial.learning.exception.LearningStateConflictException;
import com.starrainnotes.tutorial.learning.mapper.StudyPlanMapper;
import com.starrainnotes.tutorial.learning.mapper.StudyTaskMapper;
import com.starrainnotes.tutorial.learning.service.LearningProgressService;
import com.starrainnotes.tutorial.learning.service.StudyPlanService;
import com.starrainnotes.tutorial.learning.vo.StudyPlanVO;
import com.starrainnotes.tutorial.learning.vo.StudyTaskVO;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学习计划业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Deprecated
// Legacy implementation retained for historical persistence tests; not registered as a Spring service.
@RequiredArgsConstructor
public class StudyPlanServiceImpl implements StudyPlanService {
    /**
     * ZONE。
     */
    private static final ZoneId STUDY_ZONE = ZoneId.of("Asia/Shanghai");
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 内容。
     */
    private final LearningContentAccess content;
    /**
     * 计划数据映射。
     * 协作类型：<code>StudyPlanMapper</code>。
     */
    private final StudyPlanMapper planMapper;
    /**
     * 任务数据映射。
     * 协作类型：<code>StudyTaskMapper</code>。
     */
    private final StudyTaskMapper taskMapper;
    /**
     * 进度业务服务。
     * 协作类型：<code>LearningProgressService</code>。
     */
    private final LearningProgressService progressService;
    /**
     * 事件集合。
     */
    private final LearningEventWriter events;
    /**
     * 项目配置的 Jackson JSON 读写器。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper objectMapper;

    /**
     * 账号标识处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    private Long accountId() {
        return currentActorApi.current().getAccountId();
    }

    /**
     * today日期处理入口。
     * 关键处理步骤调用 <code>LocalDate.now</code>。
     * @return 类型为 <code>LocalDate</code> 的处理结果
     */
    private LocalDate todayDate() {
        return LocalDate.now(STUDY_ZONE);
    }

    /**
     * own计划处理入口。
     * 主要协作调用：<code>planMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyPlanEntity</code> 的处理结果
     */
    private StudyPlanEntity ownPlan(Long planId) {
        StudyPlanEntity row = planId == null ? null : planMapper.selectById(planId);
        if (row == null || !accountId().equals(row.getAccountId())) {
            throw new LearningResourceNotFoundException();
        }
        return row;
    }

    /**
     * own任务处理入口。
     * 主要协作调用：<code>taskMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskEntity</code> 的处理结果
     */
    private StudyTaskEntity ownTask(Long taskId) {
        StudyTaskEntity row = taskId == null ? null : taskMapper.selectById(taskId);
        if (row == null || !accountId().equals(row.getAccountId())) {
            throw new LearningResourceNotFoundException();
        }
        return row;
    }

    /**
     * weekdays处理入口。
     * 主要协作调用：<code>objectMapper.readValue</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param row 行；类型为 <code>StudyPlanEntity</code>
     * @return 本次处理得到的条目集合
     */
    private List<Integer> weekdays(StudyPlanEntity row) {
        try {
            return objectMapper.readValue(row.getStudyWeekdaysJson(), new TypeReference<List<Integer>>()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    { });
        } catch (JsonProcessingException exception) {
            throw new LearningStateConflictException("学习日配置不可读取");
        }
    }

    /**
     * view处理入口。
     * 关键处理步骤调用 <code>StudyPlanVO.builder</code>、<code>String.valueOf</code>、<code>row.getId</code>、<code>row.getTutorialId</code>。
     *
     * @param row 行；类型为 <code>StudyPlanEntity</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
    private StudyPlanVO view(StudyPlanEntity row) {
        return StudyPlanVO.builder().id(String.valueOf(row.getId()))
                .tutorialId(String.valueOf(row.getTutorialId())).name(row.getName())
                .scopeType(row.getScopeType()).scopeId(String.valueOf(row.getScopeId()))
                .startDate(row.getStartDate()).endDate(row.getEndDate())
                .studyWeekdays(weekdays(row)).dailyTargetMinutes(row.getDailyTargetMinutes())
                .status(row.getStatus()).generatedVersion(row.getGeneratedVersion()).build();
    }

    /**
     * view处理入口。
     * 主要协作调用：<code>content.chapter</code>。
     *
     * @param row 行；类型为 <code>StudyTaskEntity</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
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

    /**
     * 应用学习计划。
     * 主要协作调用：<code>content.scopedChapterIds</code>、<code>objectMapper.writeValueAsString</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param row 行；类型为 <code>StudyPlanEntity</code>
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>
     */
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

    /**
     * 创建学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>planMapper.insert</code>、<code>events.task</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
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

    /**
     * 更新学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.deleteUnfinishedByPlanId</code>、<code>planMapper.updateDefinition</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
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
            taskMapper.deleteUnfinishedByPlanId(row.getId());
        }
        planMapper.updateDefinition(row);
        if ("ACTIVE".equals(row.getStatus())) {
            generate(row);
        }
        return view(row);
    }

    /**
     * generate处理入口。
     * 主要协作调用：<code>content.scopedChapterIds</code>、<code>taskMapper.listCompletedByPlanId</code>、<code>taskMapper.insertPlanned</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param plan 计划；类型为 <code>StudyPlanEntity</code>
     */
    private void generate(StudyPlanEntity plan) {
        List<Long> chapters = content.scopedChapterIds(plan.getTutorialId(), plan.getScopeType(), plan.getScopeId());
        Set<Long> completed = new HashSet<>();
        taskMapper.listCompletedByPlanId(plan.getId())
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

    /**
     * transition处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.skipUnfinishedByPlanId</code>、<code>planMapper.updateStatus</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>、<code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
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
                taskMapper.skipUnfinishedByPlanId(row.getId());
            }
            default -> throw new LearningInvalidRequestException("不支持的计划操作");
        }
        planMapper.updateStatus(row.getId(), row.getStatus(), row.getGeneratedVersion());
        if ("activate".equals(action)) {
            generate(row);
        }
        return view(row);
    }

    /**
     * 计划处理入口。
     * 关键处理步骤调用 <code>view</code>、<code>ownPlan</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
    @Override
    public StudyPlanVO plan(Long planId) {
        return view(ownPlan(planId));
    }

    /**
     * plans处理入口。
     * 主要协作调用：<code>planMapper.listRecentByAccountId</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<StudyPlanVO> plans() {
        return planMapper.listRecentByAccountId(accountId())
                .stream().map(this::view).toList();
    }

    /**
     * 计划Tasks处理入口。
     * 主要协作调用：<code>taskMapper.listByPlanId</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<StudyTaskVO> planTasks(Long planId) {
        ownPlan(planId);
        List<StudyTaskVO> result = new ArrayList<>();
        for (StudyTaskEntity row : taskMapper.listByPlanId(planId)) {
            try { result.add(view(row)); } catch (LearningResourceNotFoundException ignored) { }
        }
        return result;
    }

    /**
     * today处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.markOverdue</code>、<code>taskMapper.listDueByAccountId</code>、<code>planMapper.selectById</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional
    public List<StudyTaskVO> today() {
        Long actor = accountId();
        taskMapper.markOverdue(actor, todayDate());
        List<StudyTaskVO> result = new ArrayList<>();
        for (StudyTaskEntity row : taskMapper.listDueByAccountId(actor, todayDate())) {
            StudyPlanEntity plan = planMapper.selectById(row.getPlanId());
            if (plan != null && "ACTIVE".equals(plan.getStatus())) {
                try { result.add(view(row)); } catch (LearningResourceNotFoundException ignored) { }
            }
        }
        return result;
    }

    /**
     * 检查所需有效计划。
     * 主要协作调用：<code>content.chapter</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param task 任务；类型为 <code>StudyTaskEntity</code>
     */
    private void requireActivePlan(StudyTaskEntity task) {
        StudyPlanEntity plan = ownPlan(task.getPlanId());
        if (!"ACTIVE".equals(plan.getStatus()) || !task.getGenerationVersion().equals(plan.getGeneratedVersion())) {
            throw new LearningStateConflictException("任务所属计划未启用或任务已过期");
        }
        content.chapter(task.getChapterId());
    }

    /**
     * 开始处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.start</code>、<code>events.task</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
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

    /**
     * 完成学习计划。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.complete</code>、<code>progressService.complete</code>、<code>events.task</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
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

    /**
     * skip处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.skip</code>。
     * 实现中的显式失败类型包括 <code>LearningStateConflictException</code>；具体触发条件见对应分支。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
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
