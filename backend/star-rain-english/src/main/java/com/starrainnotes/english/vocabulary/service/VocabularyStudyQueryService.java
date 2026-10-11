/*
 * VocabularyStudyQueryService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：VocabularyMemoryEntryVO、VocabularyMemoryVO、VocabularyProgressVO、VocabularyQueueVO、VocabularyStudySettingsVO、VocabularySummaryVO。
 * 阅读入口：settings（settings处理入口）；memorySnapshot（记忆快照处理入口）；memories（memories处理入口）；memoryOrDefault（记忆或默认处理入口）。
 */

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
/**
 * 词汇学习查询参数业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface VocabularyStudyQueryService {

    /* 学习设置；账户还没有设置行时返回缺省值，不写库 */
    /**
     * settings处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyStudySettingsVO</code> 的处理结果
     */
    VocabularyStudySettingsVO settings(long accountId);

    /* 记忆集合（wordId + memoryCount + lastMemoryAt），供「已加入计划」筛选 */
    /**
     * 记忆快照处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 本次处理得到的条目集合
     */
    List<VocabularyMemoryEntryVO> memorySnapshot(long accountId);

    /* 批量状态查询，返回值一定包含请求的每个词（缺省为 NEW） */
    /**
     * memories处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 本次处理得到的条目集合
     */
    List<VocabularyMemoryVO> memories(long accountId, List<Long> wordIds);

    /* 单个单词的记忆状态；没有进度时返回 NEW 缺省对象 */
    /**
     * 记忆或默认处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    VocabularyMemoryVO memoryOrDefault(long accountId, long wordId);

    /* 单个单词的记忆状态；没有进度时抛 404（写入后的回读用这个，避免把失败伪装成缺省值） */
    /**
     * 必需记忆处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    VocabularyMemoryVO requiredMemory(long accountId, long wordId);

    /* 今日学习队列：到期复习优先，其次是主题新词 */
    /**
     * queue处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param themeId 主题标识；类型为 <code>Long</code>
     * @return 类型为 <code>VocabularyQueueVO</code> 的处理结果
     */
    VocabularyQueueVO queue(long accountId, Long themeId);

    /* 进度页汇总：计划间隔 + 真实完成记录 */
    /**
     * 进度处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyProgressVO</code> 的处理结果
     */
    VocabularyProgressVO progress(long accountId);

    /* 英语首页四格统计 */
    /**
     * 摘要处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularySummaryVO</code> 的处理结果
     */
    VocabularySummaryVO summary(long accountId);
}
