package com.starrainnotes.tutorial.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.entity.KnowledgeMasteryEntity;
import com.starrainnotes.tutorial.entity.LearningHistoryEntity;
import com.starrainnotes.tutorial.entity.LearningProgressEntity;
import com.starrainnotes.tutorial.entity.ReviewTaskEntity;
import com.starrainnotes.tutorial.entity.StudyPlanEntity;
import com.starrainnotes.tutorial.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.mapper.KnowledgeMasteryMapper;
import com.starrainnotes.tutorial.mapper.LearningHistoryMapper;
import com.starrainnotes.tutorial.mapper.LearningProgressMapper;
import com.starrainnotes.tutorial.mapper.ReviewTaskMapper;
import com.starrainnotes.tutorial.mapper.StudyPlanMapper;
import com.starrainnotes.tutorial.service.LearningDashboardService;
import com.starrainnotes.tutorial.service.LearningReviewService;
import com.starrainnotes.tutorial.service.StudyPlanService;
import com.starrainnotes.tutorial.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.vo.LearningStatisticsVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LearningDashboardServiceImpl implements LearningDashboardService {
    private final CurrentActorApi currentActorApi;
    private final LearningHistoryMapper historyMapper;
    private final LearningProgressMapper progressMapper;
    private final StudyPlanMapper planMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final StudyPlanService plans;
    private final LearningReviewService reviews;

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public PageResult<LearningHistoryVO> history(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new LearningInvalidRequestException("分页参数无效");
        }
        Long actor = currentActorApi.current().getAccountId();
        long total = historyMapper.selectCount(new LambdaQueryWrapper<LearningHistoryEntity>()
                .eq(LearningHistoryEntity::getAccountId, actor));
        long offset = (long) (page - 1) * pageSize;
        List<LearningHistoryEntity> rows = total == 0 ? List.of() : historyMapper.selectList(
                new LambdaQueryWrapper<LearningHistoryEntity>()
                        .eq(LearningHistoryEntity::getAccountId, actor)
                        .orderByDesc(LearningHistoryEntity::getOccurredAt, LearningHistoryEntity::getId)
                        .last("LIMIT " + pageSize + " OFFSET " + offset));
        List<LearningHistoryVO> items = rows.stream().map(row -> LearningHistoryVO.builder()
                .id(id(row.getId())).eventType(row.getEventType())
                .tutorialId(id(row.getTutorialId())).chapterId(id(row.getChapterId()))
                .cardId(id(row.getKnowledgeCardId())).studyTaskId(id(row.getStudyTaskId()))
                .reviewTaskId(id(row.getReviewTaskId())).occurredAt(row.getOccurredAt()).build()).toList();
        return PageResult.<LearningHistoryVO>builder().items(items).total(total)
                .page(page).pageSize(pageSize).build();
    }

    @Override
    public LearningStatisticsVO statistics() {
        Long actor = currentActorApi.current().getAccountId();
        List<LearningProgressEntity> progress = progressMapper.selectList(new LambdaQueryWrapper<LearningProgressEntity>()
                .eq(LearningProgressEntity::getAccountId, actor));
        long completed = progress.stream().filter(row -> row.getCompletedAt() != null).count();
        long seconds = progress.stream().mapToLong(row -> row.getStudySecondsTotal() == null ? 0L
                : row.getStudySecondsTotal()).sum();
        long activePlans = planMapper.selectCount(new LambdaQueryWrapper<StudyPlanEntity>()
                .eq(StudyPlanEntity::getAccountId, actor).eq(StudyPlanEntity::getStatus, "ACTIVE"));
        long doneReviews = reviewTaskMapper.selectCount(new LambdaQueryWrapper<ReviewTaskEntity>()
                .eq(ReviewTaskEntity::getAccountId, actor).eq(ReviewTaskEntity::getStatus, "COMPLETED"));
        Map<String, Long> levels = new HashMap<>(Map.of("L1", 0L, "L2", 0L, "L3", 0L, "L4", 0L));
        masteryMapper.selectList(new LambdaQueryWrapper<KnowledgeMasteryEntity>()
                .eq(KnowledgeMasteryEntity::getAccountId, actor))
                .forEach(row -> levels.merge(row.getSystemSuggestedLevel(), 1L, Long::sum));
        return LearningStatisticsVO.builder().startedChapters(progress.size())
                .completedChapters(completed).studySecondsTotal(seconds)
                .activePlans(activePlans).todayStudyTasks(plans.today().size())
                .dueReviews(reviews.today().size()).completedReviews(doneReviews)
                .masteryDistribution(levels).build();
    }
}
