/*
 * VocabularyStudyCommandServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：VocabularyStudyConstants、VocabularyDisplayRequestDTO、VocabularyMemoryLock、VocabularyReviewLogRow、VocabularyReviewRequestDTO、VocabularyReviewSchedule、VocabularyStudySettingsRequestDTO。
 * 阅读入口：updateSettings（更新Settings）；start（开始处理入口）；completeReview（完成审核）；reset（重置词汇学习Command）。
 */

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
/**
 * 词汇学习Command业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class VocabularyStudyCommandServiceImpl implements VocabularyStudyCommandService {

    /**
     * 数据映射。
     * 协作类型：<code>VocabularyStudyMapper</code>。
     */
    private final VocabularyStudyMapper mapper;
    /**
     * 查询参数。
     * 协作类型：<code>VocabularyStudyQueryService</code>。
     */
    private final VocabularyStudyQueryService query;
    /**
     * 词汇。
     * 协作类型：<code>VocabularyService</code>。
     */
    private final VocabularyService vocabulary;

    /**
     * 更新Settings。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.upsertSettings</code>、<code>query.settings</code>。
     * 实现中的显式失败类型包括 <code>VocabularySettingsInvalidException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyStudySettingsRequestDTO</code>
     * @return 类型为 <code>VocabularyStudySettingsVO</code> 的处理结果
     */
    @Override
    @Transactional
    public VocabularyStudySettingsVO updateSettings(long accountId, VocabularyStudySettingsRequestDTO request) {
        if (!request.isShowEnglish() && !request.isShowChinese()) {
            throw new VocabularySettingsInvalidException("英文和中文至少保留一组");
        }
        if (!ReviewDirection.isKnown(request.getReviewDirection())) {
            throw new VocabularySettingsInvalidException("复习方向只能是英译中、中译英或听音辨词");
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
                .reviewDirection("MIXED".equals(request.getReviewDirection()) ? "EN_TO_ZH" : request.getReviewDirection())
                .dailyNewLimit(request.getDailyNewLimit())
                .dailyReviewLimit(request.getDailyReviewLimit())
                .build();
        mapper.upsertSettings(accountId, settings);
        return query.settings(accountId);
    }

    /**
     * 开始处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.startMemory</code>、<code>query.requiredMemory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    @Override
    @Transactional
    public VocabularyMemoryVO start(long accountId, long wordId) {
        vocabulary.word(String.valueOf(wordId));
        LocalDateTime now = utcNow();
        mapper.startMemory(accountId, wordId, now);
        return query.requiredMemory(accountId, wordId);
    }

    /**
     * 完成审核。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.findReviewBySession</code>、<code>mapper.lockMemory</code>、<code>mapper.initializeMemory</code>、<code>mapper.insertReview</code>。
     * 实现中的显式失败类型包括 <code>VocabularyProgressNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyReviewRequestDTO</code>
     * @return 类型为 <code>VocabularyReviewResultVO</code> 的处理结果
     */
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

    /**
     * 重置词汇学习Command。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.resetMemory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     */
    @Override
    @Transactional
    public void reset(long accountId, long wordId) {
        vocabulary.word(String.valueOf(wordId));
        mapper.resetMemory(accountId, wordId);
    }

    /**
     * 设置显示。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>vocabulary.word</code>、<code>mapper.upsertDisplay</code>、<code>query.memoryOrDefault</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDisplayRequestDTO</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    @Override
    @Transactional
    public VocabularyMemoryVO setDisplay(long accountId, long wordId, VocabularyDisplayRequestDTO request) {
        vocabulary.word(String.valueOf(wordId));
        mapper.upsertDisplay(accountId, wordId, request.getDisplayMode());
        VocabularyMemoryVO memory = query.memoryOrDefault(accountId, wordId);
        memory.setDisplayMode(request.getDisplayMode());
        return memory;
    }

    /**
     * 清理显示。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteDisplay</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     */
    @Override
    @Transactional
    public void clearDisplay(long accountId, long wordId) {
        mapper.deleteDisplay(accountId, wordId);
    }

    /*
     * 同一次复习标识被重放：以日志里的那一行为准，不再自增记忆次数。
     * 若该标识此前用在别的词上，宁可 409 也不能把复习记到错的词上。
     */
    /**
     * replayed结果处理入口。
     * 主要协作调用：<code>query.memoryOrDefault</code>。
     * 实现中的显式失败类型包括 <code>VocabularyReviewSessionConflictException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param row 行；类型为 <code>VocabularyReviewLogRow</code>
     * @return 类型为 <code>VocabularyReviewResultVO</code> 的处理结果
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

    /**
     * utcNow处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
