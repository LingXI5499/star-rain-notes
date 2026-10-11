package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryEntryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyProgressVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyQueueVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import com.starrainnotes.english.vocabulary.vo.VocabularySummaryVO;
import java.util.List;

/*
 * 词汇记忆体系的读取侧：设置、记忆状态、今日队列、进度汇总。
 *
 * 所有方法都以 accountId 为第一参数，账户归属由 Controller 从当前登录主体取得，
 * 服务层不接受「客户端传进来的账户」。
 */
public interface VocabularyStudyQueryService {

    /* 学习设置；账户还没有设置行时返回缺省值，不写库 */
    VocabularyStudySettingsVO settings(long accountId);

    /* 记忆集合（wordId + memoryCount + lastMemoryAt），供「已加入计划」筛选 */
    List<VocabularyMemoryEntryVO> memorySnapshot(long accountId);

    /* 批量状态查询，返回值一定包含请求的每个词（缺省为 NEW） */
    List<VocabularyMemoryVO> memories(long accountId, List<Long> wordIds);

    /* 单个单词的记忆状态；没有进度时返回 NEW 缺省对象 */
    VocabularyMemoryVO memoryOrDefault(long accountId, long wordId);

    /* 单个单词的记忆状态；没有进度时抛 404（写入后的回读用这个，避免把失败伪装成缺省值） */
    VocabularyMemoryVO requiredMemory(long accountId, long wordId);

    /* 今日学习队列：到期复习优先，其次是主题新词 */
    VocabularyQueueVO queue(long accountId, Long themeId);

    /* 进度页汇总：计划间隔 + 真实完成记录 */
    VocabularyProgressVO progress(long accountId);

    /* 英语首页四格统计 */
    VocabularySummaryVO summary(long accountId);
}
