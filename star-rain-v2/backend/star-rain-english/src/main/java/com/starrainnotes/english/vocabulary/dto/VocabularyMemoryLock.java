package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 复习前的状态快照（SELECT ... FOR UPDATE 的结果），用于计算下一次复习排期。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryLock {

    private int memoryCount;

    private int reviewCount;

    /* 计划复习时间；首次复习为空 */
    private LocalDateTime nextReviewAt;
}
