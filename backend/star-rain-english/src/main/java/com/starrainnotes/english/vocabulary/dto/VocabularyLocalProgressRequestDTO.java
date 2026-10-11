/*
 * VocabularyLocalProgressRequestDTO：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

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
/**
 * 词汇Local进度请求传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class VocabularyLocalProgressRequestDTO {

    /**
     * 记忆。
     */
    private List<LocalMemoryPayload> memory;

    /**
     * 词汇。
     */
    private Map<String, LocalMemoryPayload> vocabulary;

    /**
     * 审核Log。
     */
    private List<LocalReviewPayload> reviewLog;

    /**
     * Local记忆提交内容，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class LocalMemoryPayload {
        /**
         * 单词标识。
         */
        private Long wordId;
        /**
         * 记忆数量。
         */
        private Integer memoryCount;
        /**
         * 审核Step。
         */
        private Integer reviewStep;
        /**
         * 审核数量。
         */
        private Integer reviewCount;
        /**
         * 首次学习时间。
         */
        private String firstLearnedAt;
        /**
         * 最近一次复习时间。
         */
        private String lastReviewedAt;
        /**
         * 下一次计划复习时间。
         */
        private String nextReviewAt;
        /**
         * 最近记忆时间。
         */
        private String lastMemoryAt;
        /**
         * 学习状态。
         */
        private String learningStatus;
    }

    /**
     * Local审核提交内容，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class LocalReviewPayload {
        /**
         * 单词标识。
         */
        private Long wordId;
        /**
         * 审核会话标识。
         */
        private String reviewSessionId;
        /**
         * direction。
         */
        private String direction;
        /**
         * 审核请求的业务编号。
         */
        private Integer reviewNumber;
        /**
         * 计划调度时间。
         */
        private String scheduledAt;
        /**
         * 复习时间。
         */
        private String reviewedAt;
        /**
         * 下一次复习的间隔，单位为秒。
         */
        private Long intervalSeconds;
        /**
         * 调度状态。
         */
        private String timingStatus;
    }
}
