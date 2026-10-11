/*
 * TutorialReviewTargetHandler：教程与学习模块的模块支持文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：TutorialChapterMapper、ReviewDecisionContext、ReviewTargetHandler、ReviewTargetRef、ReviewTargetView、TutorialEntity、TutorialRevisionEntity。
 * 阅读入口：targetModule（目标对象所属模块处理入口）；targetType（目标对象的业务类型处理入口）；supports（判断是否支持教程审核目标处理器）；loadReviewView（加载审核View）。
 */

package com.starrainnotes.tutorial.content.handler;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.review.api.ReviewDecisionContext;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import com.starrainnotes.tutorial.content.vo.TutorialReviewVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教程审核目标处理器，属于教程与学习模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class TutorialReviewTargetHandler implements ReviewTargetHandler {
    /**
     * 教程数据映射。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorialMapper;
    /**
     * 修订数据映射。
     * 协作类型：<code>TutorialRevisionMapper</code>。
     */
    private final TutorialRevisionMapper revisionMapper;
    /**
     * 项目配置的 Jackson JSON 读写器。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper objectMapper;
    /**
     * 事件发布器。
     */
    private final TutorialEventPublisher eventPublisher;
    /**
     * 章节数据映射。
     * 协作类型：<code>TutorialChapterMapper</code>。
     */
    private final TutorialChapterMapper chapterMapper;

    /**
     * 目标对象所属模块处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String targetModule() { return "TUTORIAL"; }

    /**
     * 目标对象的业务类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String targetType() { return "TUTORIAL"; }

    /**
     * 判断是否支持教程审核目标处理器。
     *
     * @param module module；类型为 <code>String</code>
     * @param type 类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String module, String type, String reviewType) {
        return "TUTORIAL".equals(module) && "TUTORIAL".equals(type)
                && "tutorial.publish".equals(reviewType);
    }

    /**
     * 修订处理入口。
     * 主要协作调用：<code>revisionMapper.selectByTutorialIdAndRef</code>。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @return 类型为 <code>TutorialRevisionEntity</code> 的处理结果
     */
    private TutorialRevisionEntity revision(ReviewTargetRef target) {
        if (target == null || !supports(target.getTargetModule(), target.getTargetType(), "tutorial.publish")) {
            return null;
        }
        return revisionMapper.selectByTutorialIdAndRef(target.getTargetId(), target.getRevisionRef());
    }

    /**
     * 判断是否最新修订。
     * 主要协作调用：<code>revisionMapper.selectLatestByTutorialId</code>。
     *
     * @param revision 修订；类型为 <code>TutorialRevisionEntity</code>
     * @return 本方法条件是否成立的判断结果
     */
    private boolean isLatestRevision(TutorialRevisionEntity revision) {
        TutorialRevisionEntity latest = revisionMapper.selectLatestByTutorialId(revision.getTutorialId());
        return latest != null && latest.getId().equals(revision.getId());
    }

    /**
     * 加载审核View。
     * 主要协作调用：<code>objectMapper.readTree</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
    @Override
    public ReviewTargetView loadReviewView(ReviewTargetRef target) {
        TutorialRevisionEntity row = revision(target);
        if (row == null) return null;
        try {
            JsonNode snapshot = objectMapper.readTree(row.getSnapshotJson());
            return new TutorialReviewVO(row.getRevisionRef(), snapshot.path("title").asText(), snapshot);
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程审核版本不可读取");
        }
    }

    /**
     * 响应已批准。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorialMapper.byIdForUpdate</code>、<code>objectMapper.readTree</code>、<code>chapterMapper.updateStatus</code>、<code>chapterMapper.bumpVersion</code>、<code>tutorialMapper.update</code>。
     * 实现中的显式失败类型包括 <code>TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void onApproved(ReviewDecisionContext context) {
        TutorialRevisionEntity revision = revision(context.getTarget());
        if (revision == null) throw new TutorialStateException("审核版本不存在");
        TutorialEntity tutorial = tutorialMapper.byIdForUpdate(revision.getTutorialId());
        if (tutorial == null) throw new TutorialStateException("教程不存在");
        if (revision.getId().equals(tutorial.getPublishedRevisionId())
                && "PUBLISHED".equals(tutorial.getPublicationStatus())) return;
        if (!"IN_REVIEW".equals(tutorial.getEditingStatus()) || !isLatestRevision(revision)) {
            throw new TutorialStateException("教程不在审核中");
        }
        try {
            for (JsonNode group : objectMapper.readTree(revision.getSnapshotJson()).path("groups")) {
                for (JsonNode chapter : group.path("chapters")) {
                    Long id = Long.valueOf(chapter.path("id").asText());
                    chapterMapper.updateStatus(id, "PUBLISHED", LocalDateTime.now(ZoneOffset.UTC));
                    chapterMapper.bumpVersion(id);
                }
            }
        } catch (JsonProcessingException exception) {
            throw new TutorialStateException("教程审核版本不可读取");
        }
        tutorial.setPublishedRevisionId(revision.getId());
        tutorial.setPublicationStatus("PUBLISHED");
        tutorial.setEditingStatus("DRAFT");
        tutorial.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        tutorial.setWithdrawnAt(null);
        tutorialMapper.update(tutorial);
        eventPublisher.afterCommit(new TutorialPublicationChangedEvent(tutorial.getId(), tutorial.getSlug(),
                tutorial.getTitle(), "PUBLISHED", tutorial.getPublishedAt()));
    }

    /**
     * 响应已拒绝。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>resetReview</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void onRejected(ReviewDecisionContext context) {
        resetReview(context);
    }

    /**
     * 响应Canceled。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>resetReview</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void onCanceled(ReviewDecisionContext context) {
        resetReview(context);
    }

    /**
     * 重置审核。
     * 主要协作调用：<code>tutorialMapper.byIdForUpdate</code>、<code>tutorialMapper.update</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    private void resetReview(ReviewDecisionContext context) {
        TutorialRevisionEntity revision = revision(context.getTarget());
        if (revision == null) return;
        TutorialEntity tutorial = tutorialMapper.byIdForUpdate(revision.getTutorialId());
        if (tutorial == null || !"IN_REVIEW".equals(tutorial.getEditingStatus())
                || !isLatestRevision(revision)) return;
        tutorial.setEditingStatus("DRAFT");
        tutorialMapper.update(tutorial);
    }
}
