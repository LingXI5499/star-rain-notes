package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyDisplayRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewResultVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;

/*
 * 词汇记忆体系的写入侧：加入/移出计划、完成复习、显示覆盖、学习设置。
 */
public interface VocabularyStudyCommandService {

    /* 学习设置写入；英文与中文不能同时隐藏 */
    VocabularyStudySettingsVO updateSettings(long accountId, VocabularyStudySettingsRequest request);

    /* 加入记忆计划（幂等）；返回写入后的状态 */
    VocabularyMemoryVO start(long accountId, long wordId);

    /* 完成一次复习；reviewSessionId 是幂等键，重放不会重复计数 */
    VocabularyReviewResultVO completeReview(long accountId, long wordId, VocabularyReviewRequest request);

    /* 移出记忆计划（清空个人进度；已产生的复习日志保留） */
    void reset(long accountId, long wordId);

    /* 单卡显示覆盖 */
    VocabularyMemoryVO setDisplay(long accountId, long wordId, VocabularyDisplayRequest request);

    /* 取消单卡覆盖，回到跟随全局 */
    void clearDisplay(long accountId, long wordId);
}
