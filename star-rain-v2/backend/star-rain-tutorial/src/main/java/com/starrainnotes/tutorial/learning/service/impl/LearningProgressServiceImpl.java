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

@Service
@RequiredArgsConstructor
public class LearningProgressServiceImpl implements LearningProgressService {
    private final CurrentActorApi currentActorApi;
    private final LearningContentAccess content;
    private final LearningProgressMapper progressMapper;
    private final LearningReviewService reviewService;
    private final LearningEventWriter events;

    private Long accountId() {
        return currentActorApi.current().getAccountId();
    }

    private LearningProgressEntity row(Long accountId, Long chapterId) {
        return progressMapper.selectByAccountIdAndChapterId(accountId, chapterId);
    }

    private LearningProgressVO view(LearningContentAccess.ChapterRef chapter, LearningProgressEntity row) {
        return LearningProgressVO.builder()
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
        progressMapper.upsertReading(actor, chapter.getTutorialId(), chapter.getGroupId(), chapterId,
                anchor, request.getProgressRatio(), seconds);
        if (firstVisit) {
            events.chapter(actor, "CHAPTER_OPENED", chapter);
        }
        return view(chapter, row(actor, chapterId));
    }

    @Override
    @Transactional
    public LearningProgressVO complete(Long chapterId) {
        LearningContentAccess.ChapterRef chapter = content.chapter(chapterId);
        Long actor = accountId();
        progressMapper.ensureRow(actor, chapter.getTutorialId(), chapter.getGroupId(), chapterId);
        if (progressMapper.markCompleted(actor, chapterId) == 1) {
            events.chapter(actor, "CHAPTER_COMPLETED", chapter);
        }
        reviewService.initializeChapter(actor, chapterId);
        return view(chapter, row(actor, chapterId));
    }

    @Override
    public LearningProgressVO chapter(Long chapterId) {
        LearningContentAccess.ChapterRef chapter = content.chapter(chapterId);
        return view(chapter, row(accountId(), chapterId));
    }

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
