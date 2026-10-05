package com.starrainnotes.english.vocabulary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 已有复习日志行的回放结果。同一个 review_session_id 被重放时直接返回这一行，
 * 服务端不再自增记忆次数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyReviewLogRow {

    private long wordId;

    private int reviewNumber;

    private long intervalSeconds;

    private String timingStatus;
}
