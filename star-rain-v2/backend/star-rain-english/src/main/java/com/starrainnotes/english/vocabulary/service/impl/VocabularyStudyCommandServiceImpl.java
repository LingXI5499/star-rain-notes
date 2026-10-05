package com.starrainnotes.english.vocabulary.service.impl;

import com.starrainnotes.english.vocabulary.constant.VocabularyStudyConstants;
import com.starrainnotes.english.vocabulary.dto.VocabularyDisplayRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryLock;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewLogRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.dto.VocabularyStudySettingsRequestDTO;
import com.starrainnotes.english.vocabulary.enumeration.ReviewDirection;
import com.starrainnotes.english.vocabulary.exception.VocabularyProgressNotFoundException;
import com.starrainnotes.english.vocabulary.exception.VocabularyReviewSessionConflictException;
import com.starrainnotes.english.vocabulary.exception.VocabularySettingsInvalidException;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyCommandService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyQueryService;
import com.starrainnotes.english.vocabulary.utils.VocabularyReviewPolicy;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewResultVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 记忆体系写入侧实现。
 *
 * 复习写入的两处保护与 V1 一致：
 *   1. review_session_id 幂等：先查日志，重放直接回放既有结果，不再自增；
 *   2. SELECT ... FOR UPDATE 锁住记忆行后再算排期，避免并发双击把 review_count 算两次。
 */
@Service
@RequiredArgsConstructor
public class VocabularyStudyCommandServiceImpl implements VocabularyStudyCommandService {

    private final VocabularyStudyMapper mapper;
    private final VocabularyStudyQueryService query;
    private final VocabularyService vocabulary;

    @Override
    @Transactional
    public VocabularyStudySettingsVO updateSettings(long accountId, VocabularyStudySettingsRequestDTO request) {
        if (!request.isShowEnglish() && !request.isShowChinese()) {
            throw new VocabularySettingsInvalidException("英文和中文至少保留一组");
        }
        if (!ReviewDirection.isKnown(request.getReviewDirection())) {
            throw new VocabularySettingsInvalidException("复习方向只能是英译中、中译英或随机混合");
        }
        if (request.getDailyNewLimit() < VocabularyStudyConstants.MIN_DAILY_NEW_LIMIT
                || request.getDailyNewLimit() > VocabularyStudyConstants.MAX_DAILY_NEW_LIMIT) {
            throw new VocabularySettingsInvalidException("每日新词上限必须在 "
                    + VocabularyStudyConstants.MIN_DAILY_NEW_LIMIT + " ~ "
                    + VocabularyStudyConstants.MAX_DAILY_NEW_LIMIT + " 之间");
        }
        if (request.getDailyReviewLimit() < VocabularyStudyConstants.MIN_DAILY_REVIEW_LIMIT
                || request.getDailyReviewLimit() > VocabularyStudyConstants.MAX_DAILY_REVIEW_LIMIT) {
            throw new VocabularySettingsInvalidException("每日复习上限必须在 "
                    + VocabularyStudyConstants.MIN_DAILY_REVIEW_LIMIT + " ~ "
                    + VocabularyStudyConstants.MAX_DAILY_REVIEW_LIMIT + " 之间");
        }
        VocabularyStudySettingsVO settings = VocabularyStudySettingsVO.builder()
                .showEnglish(request.isShowEnglish())
                .showChinese(request.isShowChinese())
                .reviewDirection(request.getReviewDirection())
                .dailyNewLimit(request.getDailyNewLimit())
                .dailyReviewLimit(request.getDailyReviewLimit())
                .build();
        mapper.upsertSettings(accountId, settings);
        return query.settings(accountId);
    }

    @Override
    @Transactional
    public VocabularyMemoryVO start(long accountId, long wordId) {
        vocabulary.word(String.valueOf(wordId));
        LocalDateTime now = utcNow();
        mapper.startMemory(accountId, wordId, now);
        return query.requiredMemory(accountId, wordId);
    }

    @Override
    @Transactional
    public VocabularyReviewResultVO completeReview(long accountId, long wordId, VocabularyReviewRequestDTO request) {
        vocabulary.word(String.valueOf(wordId));
        String sessionId = request.getReviewSessionId();

        VocabularyReviewLogRow replayed = mapper.findReviewBySession(accountId, sessionId);
        if (replayed != null) {
            return replayedResult(accountId, wordId, replayed);
        }

        LocalDateTime now = utcNow();
        VocabularyMemoryLock locked = mapper.lockMemory(accountId, wordId);
        if (locked == null) {
            /* 没点过「加入记忆计划」也能直接复习：先补建记忆行再锁 */
            mapper.initializeMemory(accountId, wordId, now);
            locked = mapper.lockMemory(accountId, wordId);
        }
        if (locked == null) {
            throw new VocabularyProgressNotFoundException();
        }

        VocabularyReviewSchedule schedule = VocabularyReviewPolicy.next(
                locked.getReviewCount(), locked.getNextReviewAt(), now);

        int inserted = mapper.insertReview(accountId, wordId, sessionId, request.getDirection(), schedule);
        if (inserted == 0) {
            /* 唯一键拦下的并发重放：以已落库的那一行为准 */
            VocabularyReviewLogRow existing = mapper.findReviewBySession(accountId, sessionId);
            if (existing == null) {
                throw new VocabularyProgressNotFoundException();
            }
            return replayedResult(accountId, wordId, existing);
        }
        mapper.advanceMemory(accountId, wordId, schedule);
        return VocabularyReviewResultVO.builder()
                .memory(query.requiredMemory(accountId, wordId))
                .reviewNumber(schedule.getReviewNumber())
                .intervalSeconds(schedule.getIntervalSeconds())
                .timingStatus(schedule.getTimingStatus())
                .duplicate(false)
                .build();
    }

    @Override
    @Transactional
    public void reset(long accountId, long wordId) {
        vocabulary.word(String.valueOf(wordId));
        mapper.resetMemory(accountId, wordId);
    }

    @Override
    @Transactional
    public VocabularyMemoryVO setDisplay(long accountId, long wordId, VocabularyDisplayRequestDTO request) {
        vocabulary.word(String.valueOf(wordId));
        mapper.upsertDisplay(accountId, wordId, request.getDisplayMode());
        VocabularyMemoryVO memory = query.memoryOrDefault(accountId, wordId);
        memory.setDisplayMode(request.getDisplayMode());
        return memory;
    }

    @Override
    @Transactional
    public void clearDisplay(long accountId, long wordId) {
        mapper.deleteDisplay(accountId, wordId);
    }

    /*
     * 同一次复习标识被重放：以日志里的那一行为准，不再自增记忆次数。
     * 若该标识此前用在别的词上，宁可 409 也不能把复习记到错的词上。
     */
    private VocabularyReviewResultVO replayedResult(long accountId, long wordId, VocabularyReviewLogRow row) {
        if (row.getWordId() != wordId) {
            throw new VocabularyReviewSessionConflictException();
        }
        VocabularyMemoryVO memory = query.memoryOrDefault(accountId, wordId);
        return VocabularyReviewResultVO.builder()
                .memory(memory)
                .reviewNumber(row.getReviewNumber())
                .intervalSeconds(row.getIntervalSeconds())
                .timingStatus(row.getTimingStatus())
                .duplicate(true)
                .build();
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
