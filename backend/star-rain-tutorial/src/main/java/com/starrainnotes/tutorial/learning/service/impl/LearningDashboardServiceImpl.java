/*
 * LearningDashboardServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、PageResult、LearningHistoryEntity、LearningProgressEntity、LearningInvalidRequestException、KnowledgeMasteryMapper、LearningHistoryMapper。
 * 阅读入口：history（历史处理入口）；statistics（statistics处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.learning.mapper.KnowledgeMasteryMapper;
import com.starrainnotes.tutorial.learning.mapper.LearningHistoryMapper;
import com.starrainnotes.tutorial.learning.mapper.LearningProgressMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewTaskMapper;
import com.starrainnotes.tutorial.learning.mapper.StudyPlanMapper;
import com.starrainnotes.tutorial.learning.service.LearningDashboardService;
import com.starrainnotes.tutorial.learning.service.LearningReviewService;
import com.starrainnotes.tutorial.learning.service.StudyPlanService;
import com.starrainnotes.tutorial.learning.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.learning.vo.LearningStatisticsVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 学习概览业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Deprecated
// Legacy implementation retained for historical persistence tests; not registered as a Spring service.
@RequiredArgsConstructor
public class LearningDashboardServiceImpl implements LearningDashboardService {
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 历史数据映射。
     * 协作类型：<code>LearningHistoryMapper</code>。
     */
    private final LearningHistoryMapper historyMapper;
    /**
     * 进度数据映射。
     * 协作类型：<code>LearningProgressMapper</code>。
     */
    private final LearningProgressMapper progressMapper;
    /**
     * 计划数据映射。
     * 协作类型：<code>StudyPlanMapper</code>。
     */
    private final StudyPlanMapper planMapper;
    /**
     * 审核任务数据映射。
     * 协作类型：<code>ReviewTaskMapper</code>。
     */
    private final ReviewTaskMapper reviewTaskMapper;
    /**
     * 掌握数据映射。
     * 协作类型：<code>KnowledgeMasteryMapper</code>。
     */
    private final KnowledgeMasteryMapper masteryMapper;
    /**
     * plans。
     * 协作类型：<code>StudyPlanService</code>。
     */
    private final StudyPlanService plans;
    /**
     * reviews。
     * 协作类型：<code>LearningReviewService</code>。
     */
    private final LearningReviewService reviews;

    /**
     * 当前业务对象的数据库标识处理入口。
     * 关键处理步骤调用 <code>String.valueOf</code>。
     *
     * @param value 值；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 历史处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>historyMapper.countByAccountId</code>、<code>historyMapper.pageByAccountId</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;LearningHistoryVO&gt;</code> 的处理结果
     */
    @Override
    public PageResult<LearningHistoryVO> history(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new LearningInvalidRequestException("分页参数无效");
        }
        Long actor = currentActorApi.current().getAccountId();
        long total = historyMapper.countByAccountId(actor);
        long offset = (long) (page - 1) * pageSize;
        List<LearningHistoryEntity> rows = total == 0 ? List.of()
                : historyMapper.pageByAccountId(actor, pageSize, offset);
        List<LearningHistoryVO> items = rows.stream().map(row -> LearningHistoryVO.builder()
                .id(id(row.getId())).eventType(row.getEventType())
                .tutorialId(id(row.getTutorialId())).chapterId(id(row.getChapterId()))
                .cardId(id(row.getKnowledgeCardId())).studyTaskId(id(row.getStudyTaskId()))
                .reviewTaskId(id(row.getReviewTaskId())).occurredAt(row.getOccurredAt()).build()).toList();
        return PageResult.<LearningHistoryVO>builder().items(items).total(total)
                .page(page).pageSize(pageSize).build();
    }

    /**
     * statistics处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>progressMapper.listByAccountId</code>、<code>planMapper.countActiveByAccountId</code>、<code>reviewTaskMapper.countCompletedByAccountId</code>、<code>masteryMapper.listByAccountId</code>。
     * @return 类型为 <code>LearningStatisticsVO</code> 的处理结果
     */
    @Override
    public LearningStatisticsVO statistics() {
        Long actor = currentActorApi.current().getAccountId();
        List<LearningProgressEntity> progress = progressMapper.listByAccountId(actor);
        long completed = progress.stream().filter(row -> row.getCompletedAt() != null).count();
        long seconds = progress.stream().mapToLong(row -> row.getStudySecondsTotal() == null ? 0L
                : row.getStudySecondsTotal()).sum();
        long activePlans = planMapper.countActiveByAccountId(actor);
        long doneReviews = reviewTaskMapper.countCompletedByAccountId(actor);
        Map<String, Long> levels = new HashMap<>(Map.of("L1", 0L, "L2", 0L, "L3", 0L, "L4", 0L));
        masteryMapper.listByAccountId(actor)
                .forEach(row -> levels.merge(row.getSystemSuggestedLevel(), 1L, Long::sum));
        return LearningStatisticsVO.builder().startedChapters(progress.size())
                .completedChapters(completed).studySecondsTotal(seconds)
                .activePlans(activePlans).todayStudyTasks(plans.today().size())
                .dueReviews(reviews.today().size()).completedReviews(doneReviews)
                .masteryDistribution(levels).build();
    }
}
