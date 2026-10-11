/*
 * VocabularyMemoryAssembler：英语学习模块的工具文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：VocabularyMemoryEntryRow、VocabularyMemoryRow、VocabularyReviewHistoryRow、LearningStatus、VocabularyMemoryEntryVO、VocabularyMemoryVO、VocabularyReviewHistoryVO。
 * 阅读入口：toVO（转换为响应对象）；empty（空白处理入口）；toEntryVO（转换为入口响应对象）；toVO（转换为响应对象）。
 */

package com.starrainnotes.english.vocabulary.utils;

import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryEntryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewHistoryRow;
import com.starrainnotes.english.vocabulary.enumeration.LearningStatus;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryEntryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewHistoryVO;

/*
 * 记忆状态的行 -> 视图对象转换。集中在一处，避免各服务各写一份不同口径的时间格式化与缺省值。
 */
/**
 * 词汇记忆组装器，属于英语学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class VocabularyMemoryAssembler {

    /**
     * 构造VocabularyMemoryAssembler，接收本类使用的依赖或初始化参数。
     */
    private VocabularyMemoryAssembler() { }

    /**
     * 转换为响应对象。
     * 关键处理步骤调用 <code>VocabularyMemoryVO.builder</code>、<code>row.getWordId</code>、<code>row.getMemoryCount</code>、<code>row.getReviewStep</code>。
     * 边界判断包含 <code>(row == null)</code>。
     *
     * @param row 行；类型为 <code>VocabularyMemoryRow</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    public static VocabularyMemoryVO toVO(VocabularyMemoryRow row) {
        if (row == null) {
            return null;
        }
        return VocabularyMemoryVO.builder()
                .wordId(row.getWordId())
                .memoryCount(row.getMemoryCount())
                .reviewStep(row.getReviewStep())
                .reviewCount(row.getReviewCount())
                .firstLearnedAt(VocabularySiteTime.format(row.getFirstLearnedAt()))
                .lastReviewedAt(VocabularySiteTime.format(row.getLastReviewedAt()))
                .nextReviewAt(VocabularySiteTime.format(row.getNextReviewAt()))
                .learningStatus(status(row.getLearningStatus()))
                .displayMode(row.getDisplayMode())
                .build();
    }

    /*
     * 没有记忆行的缺省视图：状态 NEW、计数为 0。前端因此不需要为「还没加入计划」另造对象。
     */
    /**
     * 空白处理入口。
     * 关键处理步骤调用 <code>VocabularyMemoryVO.builder</code>、<code>LearningStatus.NEW.name</code>。
     *
     * @param wordId 单词标识；类型为 <code>Long</code>
     * @param displayMode 显示Mode；类型为 <code>String</code>
     * @return 类型为 <code>VocabularyMemoryVO</code> 的处理结果
     */
    public static VocabularyMemoryVO empty(Long wordId, String displayMode) {
        return VocabularyMemoryVO.builder()
                .wordId(wordId)
                .memoryCount(0)
                .reviewStep(0)
                .reviewCount(0)
                .firstLearnedAt(null)
                .lastReviewedAt(null)
                .nextReviewAt(null)
                .learningStatus(LearningStatus.NEW.name())
                .displayMode(displayMode)
                .build();
    }

    /**
     * 转换为入口响应对象。
     * 关键处理步骤调用 <code>VocabularyMemoryEntryVO.builder</code>、<code>row.getWordId</code>、<code>row.getMemoryCount</code>、<code>VocabularySiteTime.format</code>。
     *
     * @param row 行；类型为 <code>VocabularyMemoryEntryRow</code>
     * @return 类型为 <code>VocabularyMemoryEntryVO</code> 的处理结果
     */
    public static VocabularyMemoryEntryVO toEntryVO(VocabularyMemoryEntryRow row) {
        return VocabularyMemoryEntryVO.builder()
                .wordId(row.getWordId())
                .memoryCount(row.getMemoryCount())
                .lastMemoryAt(VocabularySiteTime.format(row.getLastMemoryAt()))
                .build();
    }

    /**
     * 转换为响应对象。
     * 关键处理步骤调用 <code>VocabularyReviewHistoryVO.builder</code>、<code>row.getRating</code>、<code>row.getSource</code>、<code>row.getId</code>。
     *
     * @param row 行；类型为 <code>VocabularyReviewHistoryRow</code>
     * @return 类型为 <code>VocabularyReviewHistoryVO</code> 的处理结果
     */
    public static VocabularyReviewHistoryVO toVO(VocabularyReviewHistoryRow row) {
        return VocabularyReviewHistoryVO.builder()
                .rating(row.getRating())
                .source(row.getSource())
                .id(row.getId())
                .wordId(row.getWordId())
                .word(row.getWord())
                .reviewNumber(row.getReviewNumber())
                .direction(row.getDirection())
                .scheduledAt(VocabularySiteTime.format(row.getScheduledAt()))
                .reviewedAt(VocabularySiteTime.format(row.getReviewedAt()))
                .intervalSeconds(row.getIntervalSeconds())
                .timingStatus(row.getTimingStatus())
                .build();
    }

    /* 历史行可能没有状态值（迁移前的行），一律按 NEW 处理，前端不必判空 */
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束处理入口。
     * 关键处理步骤调用 <code>LearningStatus.isKnown</code>、<code>LearningStatus.NEW.name</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String status(String value) {
        return LearningStatus.isKnown(value) ? value : LearningStatus.NEW.name();
    }
}
