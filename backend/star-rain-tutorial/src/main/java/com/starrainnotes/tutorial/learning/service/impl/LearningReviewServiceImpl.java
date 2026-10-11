/*
 * LearningReviewServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、KnowledgeMasteryEntity、ReviewResultEntity、ReviewScheduleEntity、ReviewTaskEntity、RecallRating、LearningInvalidRequestException。
 * 阅读入口：initializeChapter（initialize章节处理入口）；queueAllDue（queue全部Due处理入口）；today（today处理入口）；task（任务处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.entity.KnowledgeMasteryEntity;
import com.starrainnotes.tutorial.learning.entity.ReviewResultEntity;
import com.starrainnotes.tutorial.learning.entity.ReviewScheduleEntity;
import com.starrainnotes.tutorial.learning.entity.ReviewTaskEntity;
import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.learning.exception.LearningResourceNotFoundException;
import com.starrainnotes.tutorial.learning.exception.LearningStateConflictException;
import com.starrainnotes.tutorial.learning.exception.ReviewTaskAlreadyCompletedException;
import com.starrainnotes.tutorial.learning.mapper.KnowledgeMasteryMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewResultMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewScheduleMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewTaskMapper;
import com.starrainnotes.tutorial.learning.properties.LearningReviewProperties;
import com.starrainnotes.tutorial.learning.service.LearningReviewService;
import com.starrainnotes.tutorial.learning.service.ReviewIntervalPolicy;
import com.starrainnotes.tutorial.learning.vo.MasteryVO;
import com.starrainnotes.tutorial.learning.vo.ReviewIntervalDecision;
import com.starrainnotes.tutorial.learning.vo.ReviewResultVO;
import com.starrainnotes.tutorial.learning.vo.ReviewTaskVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学习审核业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Deprecated
// Legacy implementation retained for historical persistence tests; not registered as a Spring service.
@RequiredArgsConstructor
public class LearningReviewServiceImpl implements LearningReviewService {
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
     * 掌握数据映射。
     * 协作类型：<code>KnowledgeMasteryMapper</code>。
     */
    private final KnowledgeMasteryMapper masteryMapper;
    /**
     * schedule数据映射。
     * 协作类型：<code>ReviewScheduleMapper</code>。
     */
    private final ReviewScheduleMapper scheduleMapper;
    /**
     * 任务数据映射。
     * 协作类型：<code>ReviewTaskMapper</code>。
     */
    private final ReviewTaskMapper taskMapper;
    /**
     * 结果数据映射。
     * 协作类型：<code>ReviewResultMapper</code>。
     */
    private final ReviewResultMapper resultMapper;
    /**
     * 间隔策略。
     */
    private final ReviewIntervalPolicy intervalPolicy;
    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>LearningReviewProperties</code>。
     */
    private final LearningReviewProperties properties;
    /**
     * 事件集合。
     */
    private final LearningEventWriter events;

    /**
     * 账号标识处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    private Long accountId() {
        return currentActorApi.current().getAccountId();
    }

    /**
     * now处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * initial间隔处理入口。
     * 主要协作调用：<code>properties.getBaseIntervalDays</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>int</code> 的处理结果
     */
    private int initialInterval() {
        List<Integer> days = properties.getBaseIntervalDays();
        if (days == null || days.isEmpty() || days.get(0) == null || days.get(0) < 1) {
            throw new IllegalStateException("初始复习间隔配置无效");
        }
        return days.get(0);
    }

    /**
     * 掌握行处理入口。
     * 主要协作调用：<code>masteryMapper.selectByAccountIdAndCardId</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @return 类型为 <code>KnowledgeMasteryEntity</code> 的处理结果
     */
    private KnowledgeMasteryEntity masteryRow(Long actor, Long cardId) {
        return masteryMapper.selectByAccountIdAndCardId(actor, cardId);
    }

    /**
     * 掌握View处理入口。
     * 主要协作调用：<code>content.card</code>。
     *
     * @param row 行；类型为 <code>KnowledgeMasteryEntity</code>
     * @return 类型为 <code>MasteryVO</code> 的处理结果
     */
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

    /**
     * initialize章节处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.chapter</code>、<code>masteryMapper.ensureLearned</code>、<code>masteryMapper.markLearned</code>、<code>scheduleMapper.ensure</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
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

    /**
     * own任务处理入口。
     * 主要协作调用：<code>taskMapper.selectById</code>。
     * 实现中的显式失败类型包括 <code>LearningResourceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ReviewTaskEntity</code> 的处理结果
     */
    private ReviewTaskEntity ownTask(Long taskId) {
        ReviewTaskEntity row = taskId == null ? null : taskMapper.selectById(taskId);
        if (row == null || !accountId().equals(row.getAccountId())) {
            throw new LearningResourceNotFoundException();
        }
        return row;
    }

    /**
     * 任务View处理入口。
     * 主要协作调用：<code>content.card</code>。
     *
     * @param row 行；类型为 <code>ReviewTaskEntity</code>
     * @return 类型为 <code>ReviewTaskVO</code> 的处理结果
     */
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

    /**
     * queueDue处理入口。
     * 主要协作调用：<code>scheduleMapper.dueUnqueuedForAccount</code>、<code>taskMapper.insertDue</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     */
    private void queueDue(Long actor) {
        List<ReviewScheduleEntity> rows = scheduleMapper.dueUnqueuedForAccount(actor, now());
        for (ReviewScheduleEntity schedule : rows) {
            taskMapper.insertDue(schedule.getId(), actor, schedule.getKnowledgeCardId(), schedule.getNextReviewAt());
        }
    }

    /**
     * queue全部Due处理入口。
     * 调用约束：<code>@Scheduled(cron = "0 0 * * * *")</code>。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>scheduleMapper.dueUnqueued</code>、<code>taskMapper.insertDue</code>。
     */
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

    /**
     * today处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taskMapper.markOverdue</code>、<code>taskMapper.listPendingByAccountId</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional
    public List<ReviewTaskVO> today() {
        Long actor = accountId();
        queueDue(actor);
        taskMapper.markOverdue(actor, now());
        List<ReviewTaskVO> result = new ArrayList<>();
        for (ReviewTaskEntity row : taskMapper.listPendingByAccountId(actor)) {
            try { result.add(taskView(row)); } catch (LearningResourceNotFoundException ignored) { }
        }
        return result;
    }

    /**
     * 任务处理入口。
     * 关键处理步骤调用 <code>taskView</code>、<code>ownTask</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ReviewTaskVO</code> 的处理结果
     */
    @Override
    public ReviewTaskVO task(Long taskId) {
        return taskView(ownTask(taskId));
    }

    /**
     * back处理入口。
     * 主要协作调用：<code>content.card</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String back(Long taskId) {
        ReviewTaskEntity task = ownTask(taskId);
        return content.card(task.getKnowledgeCardId()).getBackMarkdown();
    }

    /**
     * suggestedLevel处理入口。
     * 主要协作调用：<code>resultMapper.countGoodByAccountIdAndCardId</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param evidence 证据；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String suggestedLevel(Long actor, Long cardId, int evidence) {
        long good = resultMapper.countGoodByAccountIdAndCardId(actor, cardId);
        if (evidence >= 16 && good >= 12) return "L4";
        if (evidence >= 8 && good >= 6) return "L3";
        if (evidence >= 3 && good >= 3) return "L2";
        return "L1";
    }

    /**
     * 完成学习审核。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.card</code>、<code>scheduleMapper.selectById</code>、<code>taskMapper.complete</code>、<code>intervalPolicy.next</code>、<code>resultMapper.insert</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>、<code>LearningStateConflictException</code>、<code>ReviewTaskAlreadyCompletedException</code>；具体触发条件见对应分支。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @param rating 评分；类型为 <code>RecallRating</code>
     * @return 类型为 <code>ReviewResultVO</code> 的处理结果
     */
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
        scheduleMapper.updateAfterReview(schedule.getId(), schedule.getStepIndex(),
                schedule.getCurrentIntervalDays(), schedule.getNextReviewAt(), schedule.getLastResultAt());
        masteryMapper.ensureLearned(accountId(), task.getKnowledgeCardId());
        KnowledgeMasteryEntity mastery = masteryRow(accountId(), task.getKnowledgeCardId());
        int evidence = mastery.getEvidenceCount() + 1;
        mastery.setEvidenceCount(evidence);
        mastery.setLastRecallRating(rating.name());
        mastery.setLastEvidenceAt(now());
        mastery.setSystemSuggestedLevel(suggestedLevel(accountId(), task.getKnowledgeCardId(), evidence));
        masteryMapper.updateEvidence(mastery.getId(), mastery.getEvidenceCount(),
                mastery.getLastRecallRating(), mastery.getLastEvidenceAt(), mastery.getSystemSuggestedLevel());
        events.review(accountId(), card.getChapter().getTutorialId(), card.getChapter().getChapterId(),
                task.getKnowledgeCardId(), taskId, "{\"rating\":\"" + rating.name() + "\"}");
        return ReviewResultVO.builder().taskId(String.valueOf(taskId))
                .cardId(String.valueOf(task.getKnowledgeCardId())).rating(rating.name())
                .previousIntervalDays(result.getPreviousIntervalDays())
                .nextIntervalDays(decision.getIntervalDays()).nextReviewAt(nextAt)
                .systemSuggestedLevel(mastery.getSystemSuggestedLevel()).build();
    }

    /**
     * 掌握处理入口。
     * 主要协作调用：<code>masteryMapper.listByAccountId</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<MasteryVO> mastery() {
        List<MasteryVO> result = new ArrayList<>();
        for (KnowledgeMasteryEntity row : masteryMapper.listByAccountId(accountId())) {
            try { result.add(masteryView(row)); } catch (LearningResourceNotFoundException ignored) { }
        }
        return result;
    }

    /**
     * self评分处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.card</code>、<code>masteryMapper.ensure</code>、<code>masteryMapper.updateSelfLevel</code>、<code>events.chapter</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param level level；类型为 <code>String</code>
     * @return 类型为 <code>MasteryVO</code> 的处理结果
     */
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
        masteryMapper.updateSelfLevel(row.getId(), row.getUserSelfLevel());
        events.chapter(actor, "MASTERY_SELF_RATED", card.getChapter());
        return masteryView(row);
    }
}
