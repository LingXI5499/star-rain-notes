/*
 * VocabularyStudyQueryServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：VocabularyStudyConstants、VocabularyDto、VocabularyMemoryEntryRow、VocabularyMemoryRow、LearningStatus、VocabularyProgressNotFoundException、VocabularyStudyMapper。
 * 阅读入口：settings（settings处理入口）；memorySnapshot（记忆快照处理入口）；memories（memories处理入口）；memoryOrDefault（记忆或默认处理入口）。
 */

package com.starrainnotes.english.vocabulary.service.impl;

import com.starrainnotes.english.vocabulary.constant.VocabularyStudyConstants;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryEntryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryRow;
import com.starrainnotes.english.vocabulary.enumeration.LearningStatus;
import com.starrainnotes.english.vocabulary.exception.VocabularyProgressNotFoundException;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyQueryService;
import com.starrainnotes.english.vocabulary.utils.VocabularyMemoryAssembler;
import com.starrainnotes.english.vocabulary.utils.VocabularySiteTime;
import com.starrainnotes.english.vocabulary.utils.VocabularyStudyDirectionPolicy;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryEntryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyProgressVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyQueueVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewHistoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudyCardVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import com.starrainnotes.english.vocabulary.vo.VocabularySummaryVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 记忆体系读取侧实现。
 *
 * 队列顺序照抄 V1：先按 next_review_at 排到期词，再补主题新词（去重），
 * 最后按这个固定顺序装配卡片——不能直接用批量查询返回的顺序，那会按 sort_order 打乱到期优先。
 */
/**
 * 词汇学习查询参数业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VocabularyStudyQueryServiceImpl implements VocabularyStudyQueryService {

    /**
     * 数据映射。
     * 协作类型：<code>VocabularyStudyMapper</code>。
     */
    private final VocabularyStudyMapper mapper;
    /**
     * 词汇。
     * 协作类型：<code>VocabularyService</code>。
     */
    private final VocabularyService vocabulary;

    /**
     * settings处理入口。
     * 主要协作调用：<code>mapper.settings</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyStudySettingsVO</code> 的处理结果
     */
    @Override
    public VocabularyStudySettingsVO settings(long accountId) {
        VocabularyStudySettingsVO stored = mapper.settings(accountId);
        if (stored != null) {
            if ("MIXED".equals(stored.getReviewDirection())) { stored.setReviewDirection("EN_TO_ZH"); }
            return stored;
        }
        return VocabularyStudySettingsVO.builder()
                .showEnglish(VocabularyStudyConstants.DEFAULT_SHOW_ENGLISH)
                .showChinese(VocabularyStudyConstants.DEFAULT_SHOW_CHINESE)
                .reviewDirection(VocabularyStudyConstants.DEFAULT_REVIEW_DIRECTION)
                .dailyNewLimit(VocabularyStudyConstants.DEFAULT_DAILY_NEW_LIMIT)
                .dailyReviewLimit(VocabularyStudyConstants.DEFAULT_DAILY_REVIEW_LIMIT)
                .build();
    }

    /**
     * 记忆快照处理入口。
     * 主要协作调用：<code>mapper.memorySnapshot</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<VocabularyMemoryEntryVO> memorySnapshot(long accountId) {
        return mapper.memorySnapshot(accountId).stream()
                .map(VocabularyMemoryAssembler::toEntryVO)
                .toList();
    }

    /**
     * memories处理入口。
     * 主要协作调用：<code>mapper.memories</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<VocabularyMemoryVO> memories(long accountId, List<Long> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = wordIds.stream().filter(Objects::nonNull).distinct()
                .limit(VocabularyStudyConstants.MAX_STATE_QUERY_WORDS).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, VocabularyMemoryVO> found = new LinkedHashMap<>();
        for (VocabularyMemoryRow row : mapper.memories(accountId, ids)) {
            found.put(row.getWordId(), VocabularyMemoryAssembler.toVO(row));
        }
        /* 返回值一定覆盖请求的每个词：缺失补 NEW 缺省，前端无需再判空 */
        List<VocabularyMemoryVO> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            result.add(found.getOrDefault(id, VocabularyMemoryAssembler.empty(id, null)));
        }
        return result;
    }

    /**
     * 记忆或默认处理入口。
     * 主要协作调用：<code>mapper.memory</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    @Override
    public VocabularyMemoryVO memoryOrDefault(long accountId, long wordId) {
        VocabularyMemoryRow row = mapper.memory(accountId, wordId);
        return row == null ? VocabularyMemoryAssembler.empty(wordId, null) : VocabularyMemoryAssembler.toVO(row);
    }

    /**
     * 必需记忆处理入口。
     * 主要协作调用：<code>mapper.memory</code>。
     * 实现中的显式失败类型包括 <code>VocabularyProgressNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    @Override
    public VocabularyMemoryVO requiredMemory(long accountId, long wordId) {
        VocabularyMemoryRow row = mapper.memory(accountId, wordId);
        if (row == null) {
            throw new VocabularyProgressNotFoundException();
        }
        return VocabularyMemoryAssembler.toVO(row);
    }

    /**
     * queue处理入口。
     * 主要协作调用：<code>mapper.dueCount</code>、<code>mapper.dueWordIds</code>、<code>mapper.introducedSince</code>、<code>mapper.newWordIds</code>、<code>vocabulary.wordsByIds</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param themeId 主题标识；类型为 <code>Long</code>
     * @return 类型为 <code>VocabularyQueueVO</code> 的处理结果
     */
    @Override
    public VocabularyQueueVO queue(long accountId, Long themeId) {
        VocabularyStudySettingsVO setting = settings(accountId);
        LocalDateTime now = utcNow();

        long dueCount = mapper.dueCount(accountId, now);
        List<Long> dueIds = mapper.dueWordIds(accountId, now, setting.getDailyReviewLimit());

        int introducedToday = mapper.introducedSince(accountId, VocabularySiteTime.todayStartUtc());
        int remainingNew = Math.max(0, setting.getDailyNewLimit() - introducedToday);
        List<Long> newIds = themeId == null || remainingNew == 0
                ? List.of()
                : mapper.newWordIds(themeId, accountId, remainingNew);

        LinkedHashSet<Long> ordered = new LinkedHashSet<>(dueIds);
        ordered.addAll(newIds);
        List<Long> orderedIds = List.copyOf(ordered);

        Map<Long, VocabularyDto.Word> wordById = new LinkedHashMap<>();
        if (!orderedIds.isEmpty()) {
            for (VocabularyDto.Word word : vocabulary.wordsByIds(orderedIds, orderedIds.size())) {
                wordById.put(word.getId(), word);
            }
        }
        Map<Long, VocabularyMemoryVO> stateById = new LinkedHashMap<>();
        for (VocabularyMemoryVO state : memories(accountId, orderedIds)) {
            stateById.put(state.getWordId(), state);
        }

        long today = VocabularySiteTime.todayEpochDay();
        List<VocabularyStudyCardVO> items = new ArrayList<>();
        for (Long id : orderedIds) {
            VocabularyDto.Word word = wordById.get(id);
            if (word == null) {
                continue;
            }
            VocabularyMemoryVO state = stateById.getOrDefault(id, VocabularyMemoryAssembler.empty(id, null));
            boolean newWord = !LearningStatus.ACTIVE.name().equals(state.getLearningStatus());
            items.add(VocabularyStudyCardVO.builder()
                    .word(word)
                    .memory(state)
                    .direction(VocabularyStudyDirectionPolicy.direction(setting.getReviewDirection(), id, today))
                    .newWord(newWord)
                    .build());
        }
        return VocabularyQueueVO.builder()
                .items(items)
                .dueCount((int) Math.min(Integer.MAX_VALUE, dueCount))
                .newCount(newIds.size())
                .generatedAt(VocabularySiteTime.format(now))
                .build();
    }

    /**
     * 进度处理入口。
     * 主要协作调用：<code>mapper.recentReviews</code>、<code>mapper.activeCount</code>、<code>mapper.dueCount</code>、<code>mapper.countReviewsSince</code>、<code>mapper.countReviews</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyProgressVO</code> 的处理结果
     */
    @Override
    public VocabularyProgressVO progress(long accountId) {
        LocalDateTime now = utcNow();
        List<VocabularyReviewHistoryVO> recent = mapper
                .recentReviews(accountId, VocabularyStudyConstants.RECENT_REVIEW_LIMIT).stream()
                .map(VocabularyMemoryAssembler::toVO)
                .toList();
        return VocabularyProgressVO.builder()
                .activeWords(mapper.activeCount(accountId))
                .dueWords(mapper.dueCount(accountId, now))
                .completedToday(mapper.countReviewsSince(accountId, VocabularySiteTime.todayStartUtc()))
                .totalReviews(mapper.countReviews(accountId))
                .totalMemoryCount(mapper.totalMemoryCount(accountId))
                .recentReviews(recent)
                .build();
    }

    /**
     * 摘要处理入口。
     * 主要协作调用：<code>mapper.summary</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularySummaryVO</code> 的处理结果
     */
    @Override
    public VocabularySummaryVO summary(long accountId) {
        VocabularySummaryVO summary = mapper.summary(accountId, utcNow(), VocabularyStudyConstants.MAX_REVIEW_STEP);
        return summary == null ? VocabularySummaryVO.builder().build() : summary;
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
