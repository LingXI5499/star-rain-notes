/*
 * VocabularyStudyCommandService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：VocabularyDisplayRequestDTO、VocabularyReviewRequestDTO、VocabularyStudySettingsRequestDTO、VocabularyMemoryVO、VocabularyReviewResultVO、VocabularyStudySettingsVO。
 * 阅读入口：updateSettings（更新Settings）；start（开始处理入口）；completeReview（完成审核）；reset（重置词汇学习Command）。
 */

package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyDisplayRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyStudySettingsRequestDTO;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewResultVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;

/*
 * 词汇记忆体系的写入侧：加入/移出计划、完成复习、显示覆盖、学习设置。
 */
/**
 * 词汇学习Command业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface VocabularyStudyCommandService {

    /* 学习设置写入；英文与中文不能同时隐藏 */
    /**
     * 更新Settings。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyStudySettingsRequestDTO</code>
     * @return 类型为 <code>VocabularyStudySettingsVO</code> 的处理结果
     */
    VocabularyStudySettingsVO updateSettings(long accountId, VocabularyStudySettingsRequestDTO request);

    /* 加入记忆计划（幂等）；返回写入后的状态 */
    /**
     * 开始处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    VocabularyMemoryVO start(long accountId, long wordId);

    /* 完成一次复习；reviewSessionId 是幂等键，重放不会重复计数 */
    /**
     * 完成审核。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyReviewRequestDTO</code>
     * @return 类型为 <code>VocabularyReviewResultVO</code> 的处理结果
     */
    VocabularyReviewResultVO completeReview(long accountId, long wordId, VocabularyReviewRequestDTO request);

    /* 移出记忆计划（清空个人进度；已产生的复习日志保留） */
    /**
     * 重置词汇学习Command。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     */
    void reset(long accountId, long wordId);

    /* 单卡显示覆盖 */
    /**
     * 设置显示。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDisplayRequestDTO</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    VocabularyMemoryVO setDisplay(long accountId, long wordId, VocabularyDisplayRequestDTO request);

    /* 取消单卡覆盖，回到跟随全局 */
    /**
     * 清理显示。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param wordId 单词标识；类型为 <code>long</code>
     */
    void clearDisplay(long accountId, long wordId);
}
