package com.starrainnotes.english.vocabulary.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一次复习写入的结果：新状态 + 本次复习序号/间隔/时机判定。
 *
 * duplicate 为 true 表示同一个 reviewSessionId 被重放（网络重试、双击），
 * 服务端没有重复计数，前端应据此提示「已计入」而不是再写一次。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewResultVO {

    private VocabularyMemoryVO memory;

    private int reviewNumber;

    private long intervalSeconds;

    private String timingStatus;

    private boolean duplicate;
}
