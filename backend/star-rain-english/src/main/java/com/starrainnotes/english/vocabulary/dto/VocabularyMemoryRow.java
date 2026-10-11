package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 记忆状态行（仓储 -> 服务）。时间保持 UTC LocalDateTime，由服务统一转成站点时区的 ISO 串。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryRow {

    private Long wordId;

    private int memoryCount;

    private int reviewStep;

    private int reviewCount;

    private LocalDateTime firstLearnedAt;

    private LocalDateTime lastReviewedAt;

    private LocalDateTime nextReviewAt;

    /* 数据库列可能为空（历史行），服务侧按 NEW 处理 */
    private String learningStatus;

    /* 单卡显示覆盖；为空表示跟随全局 */
    private String displayMode;
}
