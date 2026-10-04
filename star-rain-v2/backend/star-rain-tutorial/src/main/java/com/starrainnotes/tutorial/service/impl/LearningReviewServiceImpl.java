package com.starrainnotes.tutorial.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.entity.KnowledgeMasteryEntity;
import com.starrainnotes.tutorial.entity.ReviewResultEntity;
import com.starrainnotes.tutorial.entity.ReviewScheduleEntity;
import com.starrainnotes.tutorial.entity.ReviewTaskEntity;
import com.starrainnotes.tutorial.enumeration.RecallRating;
import com.starrainnotes.tutorial.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.exception.LearningResourceNotFoundException;
import com.starrainnotes.tutorial.exception.LearningStateConflictException;
import com.starrainnotes.tutorial.exception.ReviewTaskAlreadyCompletedException;
import com.starrainnotes.tutorial.mapper.KnowledgeMasteryMapper;
import com.starrainnotes.tutorial.mapper.ReviewResultMapper;
import com.starrainnotes.tutorial.mapper.ReviewScheduleMapper;
import com.starrainnotes.tutorial.mapper.ReviewTaskMapper;
import com.starrainnotes.tutorial.properties.LearningReviewProperties;
import com.starrainnotes.tutorial.service.LearningReviewService;
import com.starrainnotes.tutorial.service.ReviewIntervalPolicy;
import com.starrainnotes.tutorial.vo.MasteryVO;
import com.starrainnotes.tutorial.vo.ReviewIntervalDecision;
import com.starrainnotes.tutorial.vo.ReviewResultVO;
import com.starrainnotes.tutorial.vo.ReviewTaskVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningReviewServiceImpl implements LearningReviewService {
    private final CurrentActorApi currentActorApi;
    private final LearningContentAccess content;
    private final KnowledgeMasteryMapper masteryMapper;
    private final ReviewScheduleMapper scheduleMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewResultMapper resultMapper;
    private final ReviewIntervalPolicy intervalPolicy;
    private final LearningReviewProperties properties;
    private final LearningEventWriter events;

    private Long accountId() {
        return currentActorApi.current().getAccountId();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private int initialInterval() {
        List<Integer> days = properties.getBaseIntervalDays();
        if (days == null || days.isEmpty() || days.get(0) == null || days.get(0) < 1) {
            throw new IllegalStateException("初始复习间隔配置无效");
        }
        return days.get(0);
    }

    private KnowledgeMasteryEntity masteryRow(Long actor, Long cardId) {
        return masteryMapper.selectOne(new LambdaQueryWrapper<KnowledgeMasteryEntity>()
                .eq(KnowledgeMasteryEntity::getAccountId, actor)
                .eq(KnowledgeMasteryEntity::getKnowledgeCardId, cardId));
    }

    private MasteryVO masteryView(KnowledgeMasteryEntity row) {
        LearningContentAccess.CardRef card = content.card(row.getKnowledgeCardId());
        return MasteryVO.builder().cardId(String.valueOf(row.getKnowledgeCardId()))
                .chapterId(String.valueOf(card.getChapter().getChapterId()))
                .frontText(card.getFrontText())
                .systemSuggestedLevel(row.getSystemSuggestedLevel())
                .userSelfLevel(row.getUserSelfLevel())
                .lastRecallRating(row.getLastRecallRating())
                .evidenceCount(row.getEvidenceCount())
                .firstLearnedAt(row.getFirstLearnedAt())
                .lastEvidenceAt(row.getLastEvidenceAt()).build();
    }

    @Override
    @Transactional
    public void initializeChapter(Long actor, Long chapterId) {
        LearningContentAccess.ChapterRef chapter = content.chapter(chapterId);
        int interval = initialInterval();
        for (com.fasterxml.jackson.databind.JsonNode card : chapter.getSnapshot().path("cards")) {
            Long cardId = Long.valueOf(card.path("id").asText());
            masteryMapper.ensureLearned(actor, cardId);
            masteryMapper.markLearned(actor, cardId);
            scheduleMapper.ensure(actor, cardId, interval, now().plusDays(interval));
        }
    }

    private ReviewTaskEntity ownTask(Long taskId) {
        ReviewTaskEntity row = taskId == null ? null : taskMapper.selectById(taskId);
        if (row == null || !accountId().equals(row.getAccountId())) {
            throw new LearningResourceNotFoundException();
        }
        return row;
    }

    private ReviewTaskVO taskView(ReviewTaskEntity row) {
        LearningContentAccess.CardRef card = content.card(row.getKnowledgeCardId());
        return ReviewTaskVO.builder().id(String.valueOf(row.getId()))
                .cardId(String.valueOf(row.getKnowledgeCardId()))
                .chapterId(String.valueOf(card.getChapter().getChapterId()))
                .tutorialSlug(card.getChapter().getTutorialSlug())
                .chapterSlug(card.getChapter().getChapterSlug())
                .frontText(card.getFrontText()).dueAt(row.getDueAt())
                .status(row.getStatus()).build();
    }

    private void queueDue(Long actor) {
        List<ReviewScheduleEntity> rows = scheduleMapper.dueUnqueuedForAccount(actor, now());
        for (ReviewScheduleEntity schedule : rows) {
            taskMapper.insertDue(schedule.getId(), actor, schedule.getKnowledgeCardId(), schedule.getNextReviewAt());
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void queueAllDue() {
        for (int batch = 0; batch < 10; batch++) {
            List<ReviewScheduleEntity> rows = scheduleMapper.dueUnqueued(now());
            if (rows.isEmpty()) {
                return;
            }
            for (ReviewScheduleEntity schedule : rows) {
                taskMapper.insertDue(schedule.getId(), schedule.getAccountId(),
                        schedule.getKnowledgeCardId(), schedule.getNextReviewAt());
            }
        }
    }

    @Override
    @Transactional
    public List<ReviewTaskVO> today() {
        Long actor = accountId();
        queueDue(actor);
        taskMapper.markOverdue(actor, now());
        List<ReviewTaskVO> result = new ArrayList<>();
        for (ReviewTaskEntity row : taskMapper.selectList(new LambdaQueryWrapper<ReviewTaskEntity>()
                .eq(ReviewTaskEntity::getAccountId, actor)
                .in(ReviewTaskEntity::getStatus, "PENDING", "OVERDUE")
                .orderByAsc(ReviewTaskEntity::getDueAt).last("LIMIT 100"))) {
            try { result.add(taskView(row)); } catch (LearningResourceNotFoundException ignored) { }
        }
        return result;
    }

    @Override
    public ReviewTaskVO task(Long taskId) {
        return taskView(ownTask(taskId));
    }

    @Override
    public String back(Long taskId) {
        ReviewTaskEntity task = ownTask(taskId);
        return content.card(task.getKnowledgeCardId()).getBackMarkdown();
    }

    private String suggestedLevel(Long actor, Long cardId, int evidence) {
        long good = resultMapper.selectCount(new LambdaQueryWrapper<ReviewResultEntity>()
                .eq(ReviewResultEntity::getAccountId, actor)
                .eq(ReviewResultEntity::getKnowledgeCardId, cardId)
                .in(ReviewResultEntity::getRecallRating, "NORMAL", "EASY"));
        if (evidence >= 16 && good >= 12) return "L4";
        if (evidence >= 8 && good >= 6) return "L3";
        if (evidence >= 3 && good >= 3) return "L2";
        return "L1";
    }

    @Override
    @Transactional
    public ReviewResultVO complete(Long taskId, RecallRating rating) {
        if (rating == null) {
            throw new LearningInvalidRequestException("请选择本次回忆结果");
        }
        ReviewTaskEntity task = ownTask(taskId);
        LearningContentAccess.CardRef card = content.card(task.getKnowledgeCardId());
        ReviewScheduleEntity schedule = scheduleMapper.selectById(task.getScheduleId());
        if (schedule == null || !task.getAccountId().equals(schedule.getAccountId())
                || !"ACTIVE".equals(schedule.getStatus())) {
            throw new LearningStateConflictException("复习计划不可用");
        }
        if (taskMapper.complete(taskId, accountId()) != 1) {
            throw new ReviewTaskAlreadyCompletedException();
        }
        ReviewIntervalDecision decision = intervalPolicy.next(schedule.getStepIndex(),
                schedule.getCurrentIntervalDays(), rating);
        LocalDateTime nextAt = now().plusDays(decision.getIntervalDays());
        ReviewResultEntity result = new ReviewResultEntity();
        result.setReviewTaskId(taskId);
        result.setScheduleId(schedule.getId());
        result.setAccountId(accountId());
        result.setKnowledgeCardId(task.getKnowledgeCardId());
        result.setRecallRating(rating.name());
        result.setPreviousIntervalDays(schedule.getCurrentIntervalDays());
        result.setNextIntervalDays(decision.getIntervalDays());
        result.setPreviousNextReviewAt(schedule.getNextReviewAt());
        result.setCalculatedNextReviewAt(nextAt);
        resultMapper.insert(result);
        schedule.setStepIndex(decision.getStepIndex());
        schedule.setCurrentIntervalDays(decision.getIntervalDays());
        schedule.setNextReviewAt(nextAt);
        schedule.setLastResultAt(now());
        scheduleMapper.updateById(schedule);
        masteryMapper.ensureLearned(accountId(), task.getKnowledgeCardId());
        KnowledgeMasteryEntity mastery = masteryRow(accountId(), task.getKnowledgeCardId());
        int evidence = mastery.getEvidenceCount() + 1;
        mastery.setEvidenceCount(evidence);
        mastery.setLastRecallRating(rating.name());
        mastery.setLastEvidenceAt(now());
        mastery.setSystemSuggestedLevel(suggestedLevel(accountId(), task.getKnowledgeCardId(), evidence));
        masteryMapper.updateById(mastery);
        events.review(accountId(), card.getChapter().getTutorialId(), card.getChapter().getChapterId(),
                task.getKnowledgeCardId(), taskId, "{\"rating\":\"" + rating.name() + "\"}");
        return ReviewResultVO.builder().taskId(String.valueOf(taskId))
                .cardId(String.valueOf(task.getKnowledgeCardId())).rating(rating.name())
                .previousIntervalDays(result.getPreviousIntervalDays())
                .nextIntervalDays(decision.getIntervalDays()).nextReviewAt(nextAt)
                .systemSuggestedLevel(mastery.getSystemSuggestedLevel()).build();
    }

    @Override
    public List<MasteryVO> mastery() {
        List<MasteryVO> result = new ArrayList<>();
        for (KnowledgeMasteryEntity row : masteryMapper.selectList(new LambdaQueryWrapper<KnowledgeMasteryEntity>()
                .eq(KnowledgeMasteryEntity::getAccountId, accountId())
                .orderByDesc(KnowledgeMasteryEntity::getUpdatedAt).last("LIMIT 1000"))) {
            try { result.add(masteryView(row)); } catch (LearningResourceNotFoundException ignored) { }
        }
        return result;
    }

    @Override
    @Transactional
    public MasteryVO selfRating(Long cardId, String level) {
        LearningContentAccess.CardRef card = content.card(cardId);
        if (level == null || !List.of("L1", "L2", "L3", "L4").contains(level)) {
            throw new LearningInvalidRequestException("掌握程度只能选择 L1 到 L4");
        }
        Long actor = accountId();
        masteryMapper.ensure(actor, cardId);
        KnowledgeMasteryEntity row = masteryRow(actor, cardId);
        row.setUserSelfLevel(level);
        masteryMapper.updateById(row);
        events.chapter(actor, "MASTERY_SELF_RATED", card.getChapter());
        return masteryView(row);
    }
}
