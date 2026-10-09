package com.starrainnotes.english.vocabulary.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

/*
 * 游客本机进度导入请求（登录后把浏览器 IndexedDB 里的记忆合并到账号）。
 *
 * 字段与 V1 vocabulary-study-storage 的导出结构保持一致，因此同时接受三种形态：
 *   memory      —— V1 的结构化记忆数组（wordId / memoryCount / reviewStep / ...）
 *   vocabulary  —— V1 更早的 localStorage 记忆映射（键为 wordId，值为 memoryCount/lastMemoryAt）
 *   reviewLog   —— 真实复习记录数组
 * 服务端只做「只增不减」的合并，不会因为导入把已有的更高进度改小。
 */
@Data
public class VocabularyLocalProgressRequestDTO {

    private List<LocalMemoryPayload> memory;

    private Map<String, LocalMemoryPayload> vocabulary;

    private List<LocalReviewPayload> reviewLog;

    @Data
    public static class LocalMemoryPayload {
        private Long wordId;
        private Integer memoryCount;
        private Integer reviewStep;
        private Integer reviewCount;
        private String firstLearnedAt;
        private String lastReviewedAt;
        private String nextReviewAt;
        private String lastMemoryAt;
        private String learningStatus;
    }

    @Data
    public static class LocalReviewPayload {
        private Long wordId;
        private String reviewSessionId;
        private String direction;
        private Integer reviewNumber;
        private String scheduledAt;
        private String reviewedAt;
        private Long intervalSeconds;
        private String timingStatus;
    }
}
