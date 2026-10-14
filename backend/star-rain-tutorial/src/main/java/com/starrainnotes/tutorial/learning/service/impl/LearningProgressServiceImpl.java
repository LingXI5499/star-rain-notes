/*
 * LearningProgressServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、LearningProgressDTO、LearningProgressEntity、LearningInvalidRequestException、LearningResourceNotFoundException、LearningProgressMapper、LearningProgressService。
 * 阅读入口：save（保存学习进度）；complete（完成学习进度）；chapter（章节处理入口）；tutorial（教程处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import com.starrainnotes.tutorial.learning.exception.LearningResourceNotFoundException;
import com.starrainnotes.tutorial.learning.mapper.LearningProgressMapper;
import com.starrainnotes.tutorial.learning.service.LearningProgressService;
import com.starrainnotes.tutorial.learning.service.LearningReviewService;
import com.starrainnotes.tutorial.learning.vo.LearningProgressVO;
import com.starrainnotes.tutorial.learning.vo.LearningTutorialProgressVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学习进度业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class LearningProgressServiceImpl implements LearningProgressService {
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
     * 进度数据映射。
     * 协作类型：<code>LearningProgressMapper</code>。
     */
    private final LearningProgressMapper progressMapper;
    /**
     * 证据业务服务。
     * 协作类型：<code>EvidenceLearningService</code>。
     */
    private final EvidenceLearningService evidenceService;
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
     * 行处理入口。
     * 主要协作调用：<code>progressMapper.selectByAccountIdAndChapterId</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningProgressEntity</code> 的处理结果
     */
    private LearningProgressEntity row(Long accountId, Long chapterId) {
        return progressMapper.selectByAccountIdAndChapterId(accountId, chapterId);
    }

    /**
     * view处理入口。
     * 关键处理步骤调用 <code>LearningProgressVO.builder</code>、<code>String.valueOf</code>、<code>chapter.getTutorialId</code>、<code>chapter.getChapterId</code>。
     *
     * @param chapter 章节；类型为 <code>LearningContentAccess.ChapterRef</code>
     * @param row 行；类型为 <code>LearningProgressEntity</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    private LearningProgressVO view(LearningContentAccess.ChapterRef chapter, LearningProgressEntity row) {
        return LearningProgressVO.builder()
                .rowVersion(row == null || row.getRowVersion() == null ? 0L : row.getRowVersion())
                .tutorialId(String.valueOf(chapter.getTutorialId()))
                .chapterId(String.valueOf(chapter.getChapterId()))
                .tutorialSlug(chapter.getTutorialSlug()).chapterSlug(chapter.getChapterSlug())
                .chapterTitle(chapter.getTitle())
                .scrollAnchor(row == null ? null : row.getScrollAnchor())
                .progressRatio(row == null ? null : row.getProgressRatio())
                .studySecondsTotal(row == null ? 0L : row.getStudySecondsTotal())
                .completedAt(row == null ? null : row.getCompletedAt())
                .lastStudiedAt(row == null ? null : row.getLastStudiedAt()).build();
    }

    /**
     * 保存学习进度。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.chapter</code>、<code>progressMapper.upsertReading</code>、<code>events.chapter</code>。
     * 实现中的显式失败类型包括 <code>LearningInvalidRequestException</code>；具体触发条件见对应分支。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LearningProgressDTO</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    @Override
    @Transactional
    public LearningProgressVO save(Long chapterId, LearningProgressDTO request) {
        LearningContentAccess.ChapterRef chapter = content.chapter(chapterId);
        if (request == null || request.getProgressRatio() == null
                || request.getProgressRatio().compareTo(BigDecimal.ZERO) < 0
                || request.getProgressRatio().compareTo(BigDecimal.ONE) > 0) {
            throw new LearningInvalidRequestException("阅读进度必须在 0 到 1 之间");
        }
        int seconds = request.getStudySecondsDelta() == null ? 0 : request.getStudySecondsDelta();
        if (seconds < 0 || seconds > 1800) {
            throw new LearningInvalidRequestException("单次学习时长不能超过 30 分钟");
        }
        String anchor = request.getScrollAnchor() == null ? null : request.getScrollAnchor().strip();
        if (anchor != null && anchor.length() > 255) {
            throw new LearningInvalidRequestException("阅读位置标识过长");
        }
        Long actor = accountId();
        boolean firstVisit = row(actor, chapterId) == null;
        boolean protocol = request.getEventId() != null || request.getExpectedVersion() != null;
        if (!protocol && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null) {
            throw new com.starrainnotes.common.exception.ApiException("PROGRESS_VERSION_REQUIRED", "请刷新学习页面后再保存进度。", 428);
        }
        if (protocol) {
            if (request.getEventId() == null || !request.getEventId().matches("[A-Za-z0-9-]{16,64}")
                    || request.getExpectedVersion() == null || request.getExpectedVersion() < 0 || seconds > 60) {
                throw new LearningInvalidRequestException("学习事件、版本或有效时长无效");
            }
            if (progressMapper.acceptEvent(actor, request.getEventId()) == 0) return view(chapter, row(actor, chapterId));
            progressMapper.ensureRow(actor, chapter.getTutorialId(), chapter.getGroupId(), chapterId);
            if (progressMapper.applyReading(actor, chapterId, anchor, request.getProgressRatio(), seconds, request.getExpectedVersion()) != 1) {
                throw new com.starrainnotes.common.exception.ApiException("PROGRESS_VERSION_CONFLICT", "其他页面已更新学习进度，请读取最新记录后继续。", 409);
            }
        } else {
            progressMapper.upsertReading(actor, chapter.getTutorialId(), chapter.getGroupId(), chapterId,
                    anchor, request.getProgressRatio(), seconds);
        }
        if (firstVisit) {
            events.chapter(actor, "CHAPTER_OPENED", chapter);
        }
        return view(chapter, row(actor, chapterId));
    }

    /**
     * 完成学习进度。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.chapter</code>、<code>evidenceService.completeChapter</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    @Override
    @Transactional
    public LearningProgressVO complete(Long chapterId) {
        LearningContentAccess.ChapterRef chapter = content.chapter(chapterId);
        Long actor = accountId();
        evidenceService.completeChapter(chapterId);
        return view(chapter, row(actor, chapterId));
    }

    /**
     * 章节处理入口。
     * 主要协作调用：<code>content.chapter</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    @Override
    public LearningProgressVO chapter(Long chapterId) {
        LearningContentAccess.ChapterRef chapter = content.chapter(chapterId);
        return view(chapter, row(accountId(), chapterId));
    }

    /**
     * 教程处理入口。
     * 主要协作调用：<code>progressMapper.listByAccountIdAndTutorialId</code>、<code>content.chapters</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningTutorialProgressVO</code> 的处理结果
     */
    @Override
    public LearningTutorialProgressVO tutorial(Long tutorialId) {
        Long actor = accountId();
        List<LearningProgressVO> rows = new ArrayList<>();
        Map<Long, LearningProgressEntity> saved = new HashMap<>();
        progressMapper.listByAccountIdAndTutorialId(actor, tutorialId)
                .forEach(item -> saved.put(item.getChapterId(), item));
        int completed = 0;
        for (LearningContentAccess.ChapterRef chapter : content.chapters(tutorialId)) {
            LearningProgressVO item = view(chapter, saved.get(chapter.getChapterId()));
            rows.add(item);
            if (item.getCompletedAt() != null) {
                completed++;
            }
        }
        return LearningTutorialProgressVO.builder().tutorialId(String.valueOf(tutorialId))
                .totalChapters(rows.size()).completedChapters(completed).chapters(rows).build();
    }

    /**
     * recent处理入口。
     * 主要协作调用：<code>progressMapper.listRecentByAccountId</code>、<code>content.chapter</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<LearningProgressVO> recent() {
        Long actor = accountId();
        List<LearningProgressVO> result = new ArrayList<>();
        List<LearningProgressEntity> rows = progressMapper.listRecentByAccountId(actor);
        for (LearningProgressEntity row : rows) {
            try {
                result.add(view(content.chapter(row.getChapterId()), row));
            } catch (LearningResourceNotFoundException ignored) {
                // 撤回的教程保留历史进度，但不提供继续阅读入口。
            }
        }
        return result;
    }
}
