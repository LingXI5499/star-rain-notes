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
public final class VocabularyMemoryAssembler {

    private VocabularyMemoryAssembler() { }

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

    public static VocabularyMemoryEntryVO toEntryVO(VocabularyMemoryEntryRow row) {
        return VocabularyMemoryEntryVO.builder()
                .wordId(row.getWordId())
                .memoryCount(row.getMemoryCount())
                .lastMemoryAt(VocabularySiteTime.format(row.getLastMemoryAt()))
                .build();
    }

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
    private static String status(String value) {
        return LearningStatus.isKnown(value) ? value : LearningStatus.NEW.name();
    }
}
