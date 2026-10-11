/*
 * LearningEventWriter：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：LearningHistoryEntity、LearningHistoryMapper。
 * 阅读入口：session（当前业务使用的会话对象处理入口）；chapter（章节处理入口）；task（任务处理入口）；review（审核处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import com.starrainnotes.tutorial.learning.mapper.LearningHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 学习事件Writer，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Component
@RequiredArgsConstructor
public class LearningEventWriter {
    /**
     * 历史数据映射。
     * 协作类型：<code>LearningHistoryMapper</code>。
     */
    private final LearningHistoryMapper historyMapper;

    /**
     * 当前业务使用的会话对象处理入口。
     * 主要协作调用：<code>historyMapper.insert</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param session 当前业务使用的会话对象；类型为 <code>com.starrainnotes.tutorial.learning.entity.EvidenceModels.Session</code>
     * @param summary 摘要；类型为 <code>com.starrainnotes.tutorial.learning.entity.EvidenceModels.SessionSummary</code>
     */
    public void session(Long accountId, com.starrainnotes.tutorial.learning.entity.EvidenceModels.Session session,
                        com.starrainnotes.tutorial.learning.entity.EvidenceModels.SessionSummary summary) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType("INITIAL_STUDY".equals(session.getSessionType()) ? "INITIAL_SESSION_COMPLETED" : "REVIEW_SESSION_COMPLETED");
        row.setTutorialId(session.getTutorialId());row.setChapterId(session.getChapterId());row.setStudyTaskId(session.getStudyTaskId());
        try { row.setDetailJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(summary)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new IllegalStateException("学习摘要序列化失败", exception); }
        historyMapper.insert(row);
    }

    /**
     * 章节处理入口。
     * 主要协作调用：<code>historyMapper.insert</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param eventType 事件类型；类型为 <code>String</code>
     * @param chapter 章节；类型为 <code>LearningContentAccess.ChapterRef</code>
     */
    public void chapter(Long accountId, String eventType, LearningContentAccess.ChapterRef chapter) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType(eventType);
        row.setTutorialId(chapter.getTutorialId());
        row.setGroupId(chapter.getGroupId());
        row.setChapterId(chapter.getChapterId());
        historyMapper.insert(row);
    }

    /**
     * 任务处理入口。
     * 主要协作调用：<code>historyMapper.insert</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param eventType 事件类型；类型为 <code>String</code>
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param taskId 任务标识；类型为 <code>Long</code>
     */
    public void task(Long accountId, String eventType, Long tutorialId, Long chapterId, Long taskId) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType(eventType);
        row.setTutorialId(tutorialId);
        row.setChapterId(chapterId);
        row.setStudyTaskId(taskId);
        historyMapper.insert(row);
    }

    /**
     * 审核处理入口。
     * 主要协作调用：<code>historyMapper.insert</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param reviewTaskId 审核任务标识；类型为 <code>Long</code>
     * @param detailJson 详情JSON；类型为 <code>String</code>
     */
    public void review(Long accountId, Long tutorialId, Long chapterId, Long cardId, Long reviewTaskId,
                       String detailJson) {
        LearningHistoryEntity row = new LearningHistoryEntity();
        row.setAccountId(accountId);
        row.setEventType("REVIEW_COMPLETED");
        row.setTutorialId(tutorialId);
        row.setChapterId(chapterId);
        row.setKnowledgeCardId(cardId);
        row.setReviewTaskId(reviewTaskId);
        row.setDetailJson(detailJson);
        historyMapper.insert(row);
    }
}
