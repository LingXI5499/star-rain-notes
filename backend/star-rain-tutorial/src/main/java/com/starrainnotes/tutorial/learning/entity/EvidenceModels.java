/*
 * EvidenceModels：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.learning.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** Persisted evidence-domain rows and explicitly shaped API projections. */
public final class EvidenceModels {
    /**
     * 构造EvidenceModels，接收本类使用的依赖或初始化参数。
     */
    private EvidenceModels() { }
    /**
     * 计划，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Plan {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 名称。
         */
        private String name;
        /**
         * 教程标题。
         */
        private String tutorialTitle;
        /**
         * 教程固定链接标识。
         */
        private String tutorialSlug;
        /**
         * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
         */
        private String status;
        /**
         * 目标卡片集合Per任务。
         */
        private Integer targetCardsPerTask;
        /**
         * 学习Round。
         * 初始值为 <code>1</code>。
         */
        private Integer studyRound = 1;
        /**
         * 创建时间。
         */
        private LocalDateTime createdAt;
        /**
         * 更新时间。
         */
        private LocalDateTime updatedAt;
        /**
         * 章节列表。
         */
        private List<Chapter> chapters;
        /**
         * tasks。
         */
        private List<Task> tasks;
        /**
         * 已完成章节列表。
         */
        private Integer completedChapters;
        /**
         * 总数章节列表。
         */
        private Integer totalChapters;
    }
    /**
     * 章节，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Chapter {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 计划标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 分组标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long groupId;
        /**
         * 章节标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        /**
         * 分组顺序。
         */
        private Integer groupOrder;
        /**
         * 章节顺序。
         */
        private Integer chapterOrder;
        /**
         * 卡片数量。
         */
        private Integer cardCount;
        /**
         * 问题数量。
         */
        private Integer questionCount;
        /**
         * 章节标题。
         */
        private String chapterTitle;
        /**
         * 章节固定链接标识。
         */
        private String chapterSlug;
        /**
         * 分组标题。
         */
        private String groupTitle;
        /**
         * 已完成。
         */
        private Boolean completed;
    }
    /**
     * 任务，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Task {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 计划标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 分组标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long groupId;
        /**
         * 学习Round。
         * 初始值为 <code>1</code>。
         */
        private Integer studyRound = 1;
        /**
         * sequence不含。
         */
        private Integer sequenceNo;
        /**
         * 卡片数量。
         */
        private Integer cardCount;
        /**
         * 问题数量。
         */
        private Integer questionCount;
        /**
         * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
         */
        private String status;
        /**
         * started时间。
         */
        private LocalDateTime startedAt;
        /**
         * 已完成时间。
         */
        private LocalDateTime completedAt;
        /**
         * 章节列表。
         */
        private List<Chapter> chapters;
    }
    /**
     * 会话，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Session {
        /**
         * 教程标题。
         */
        private String tutorialTitle;
        /**
         * 章节标题。
         */
        private String chapterTitle;
        /**
         * 计划名称。
         */
        private String planName;
        /**
         * 摘要JSON。
         */
        @JsonIgnore
        private String summaryJson;
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 学习Round。
         * 初始值为 <code>1</code>。
         */
        private Integer studyRound = 1;
        /**
         * 会话类型。
         */
        private String sessionType;
        /**
         * 学习计划标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long studyPlanId;
        /**
         * 学习任务标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long studyTaskId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 章节标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        /**
         * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
         */
        private String status;
        /**
         * requestedItem数量。
         */
        private Integer requestedItemCount;
        /**
         * started时间。
         */
        private LocalDateTime startedAt;
        /**
         * 已完成时间。
         */
        private LocalDateTime completedAt;
        /**
         * abandoned时间。
         */
        private LocalDateTime abandonedAt;
        /**
         * 条目集合。
         */
        private List<Item> items;
        /**
         * 摘要。
         */
        private SessionSummary summary;
    }
    /**
     * Item，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Item {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 会话标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sessionId;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 章节标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        /**
         * 知识卡片标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        /**
         * 卡片内容版本。
         */
        private Integer cardContentVersion;
        /**
         * 知识卡片正面的提示文本。
         */
        private String frontText;
        /**
         * 知识卡片背面的 Markdown 内容。
         */
        private String backMarkdown;
        /**
         * 来源Priority。
         */
        private Integer sourcePriority;
        /**
         * sequence不含。
         */
        private Integer sequenceNo;
        /**
         * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
         */
        private String status;
        /**
         * revealed时间。
         */
        private LocalDateTime revealedAt;
        /**
         * 已完成时间。
         */
        private LocalDateTime completedAt;
    }
    /**
     * 证据，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Evidence {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 会话标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sessionId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 章节标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        /**
         * 知识卡片标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        /**
         * 卡片内容版本。
         */
        private Integer cardContentVersion;
        /**
         * 证据类型。
         */
        private String evidenceType;
        /**
         * 评分。
         */
        private String rating;
        /**
         * 上一项掌握状态。
         */
        private String previousMasteryStatus;
        /**
         * 掌握状态。
         */
        private String masteryStatus;
        /**
         * 创建时间。
         */
        private LocalDateTime createdAt;
    }
    /**
     * 掌握，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Mastery {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 知识卡片标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        /**
         * 掌握状态。
         */
        private String masteryStatus;
        /**
         * 最新评分。
         */
        private String latestRating;
        /**
         * 证据数量。
         */
        private Integer evidenceCount;
        /**
         * forgot数量。
         */
        private Integer forgotCount;
        /**
         * fuzzy数量。
         */
        private Integer fuzzyCount;
        /**
         * remembered数量。
         */
        private Integer rememberedCount;
        /**
         * recentScore。
         */
        private Integer recentScore;
        /**
         * 最近证据时间。
         */
        private LocalDateTime lastEvidenceAt;
        /**
         * 最新证据卡片版本。
         */
        private Integer latestEvidenceCardVersion;
    }
    /**
     * 卡片掌握，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class CardMastery {
        /**
         * 知识卡片标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        /**
         * 教程标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        /**
         * 章节标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        /**
         * 教程标题。
         */
        private String tutorialTitle;
        /**
         * 章节标题。
         */
        private String chapterTitle;
        /**
         * 知识卡片正面的提示文本。
         */
        private String frontText;
        /**
         * 内容版本。
         */
        private Integer contentVersion;
        /**
         * 掌握状态。
         */
        private String masteryStatus;
        /**
         * 最新评分。
         */
        private String latestRating;
        /**
         * 证据数量。
         */
        private Integer evidenceCount;
        /**
         * recentScore。
         */
        private Integer recentScore;
        /**
         * 最近证据时间。
         */
        private LocalDateTime lastEvidenceAt;
        /**
         * needsRevalidation。
         */
        private Boolean needsRevalidation;
        /**
         * priority。
         */
        private Integer priority;
        /**
         * 章节顺序。
         */
        @JsonIgnore
        private Integer chapterOrder;
        /**
         * 卡片顺序。
         */
        @JsonIgnore
        private Integer cardOrder;
        /**
         * 知识卡片背面的 Markdown 内容。
         */
        @JsonIgnore
        private String backMarkdown;
    }
    /**
     * 答案，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Answer {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 账号标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        /**
         * 问题标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        /**
         * 最新版本标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long latestVersionId;
        /**
         * 版本数量。
         */
        private Integer versionCount;
        /**
         * 用户提交的回答文本。
         */
        private String answerText;
        /**
         * 首次Answered时间。
         */
        private LocalDateTime firstAnsweredAt;
        /**
         * 最近Answered时间。
         */
        private LocalDateTime lastAnsweredAt;
        /**
         * 引用Unlocked时间。
         */
        private LocalDateTime referenceUnlockedAt;
        /**
         * versions。
         */
        private List<AnswerVersion> versions;
    }
    /**
     * 答案版本，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class AnswerVersion {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 答案标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long answerId;
        /**
         * 版本不含。
         */
        private Integer versionNo;
        /**
         * 用户提交的回答文本。
         */
        private String answerText;
        /**
         * 答案Phase。
         */
        private String answerPhase;
        /**
         * 创建时间。
         */
        private LocalDateTime createdAt;
    }
    /**
     * 学习状态，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class StudyState {
        /**
         * 教程固定链接标识。
         */
        private String tutorialSlug;
        /**
         * 章节固定链接标识。
         */
        private String chapterSlug;
        /**
         * 章节标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        /**
         * 标题。
         */
        private String title;
        /**
         * Markdown 格式的正文。
         */
        private String bodyMarkdown;
        /**
         * 问题集合。
         */
        private List<Question> questions;
        /**
         * initial卡片标识集合。
         */
        private List<Long> initialCardIds;
        /**
         * 必需卡片标识集合。
         */
        private List<String> requiredCardIds;
        /**
         * 已完成。
         */
        private Boolean completed;
        /**
         * 当前业务使用的会话对象。
         */
        private Session session;
    }
    /**
     * 问题，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Question {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 问题文本。
         */
        private String questionText;
        /**
         * 知识卡片标识集合。
         */
        private List<Long> knowledgeCardIds;
        /**
         * answered。
         */
        private Boolean answered;
    }
    /**
     * 会话摘要，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class SessionSummary {
        /**
         * 卡片数量。
         */
        private Integer cardCount;
        /**
         * forgot数量。
         */
        private Integer forgotCount;
        /**
         * fuzzy数量。
         */
        private Integer fuzzyCount;
        /**
         * remembered数量。
         */
        private Integer rememberedCount;
        /**
         * revalidation数量。
         */
        private Integer revalidationCount;
        /**
         * 问题数量。
         */
        private Integer questionCount;
        /**
         * upgraded数量。
         */
        private Integer upgradedCount;
        /**
         * downgraded数量。
         */
        private Integer downgradedCount;
        /**
         * transitions。
         */
        private Map<String,Integer> transitions;
    }
    /**
     * 审核摘要，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class ReviewSummary {
        /**
         * candidate数量。
         */
        private Integer candidateCount;
        /**
         * recommended数量。
         */
        private Integer recommendedCount;
        /**
         * stable数量。
         */
        private Integer stableCount;
        /**
         * priorities。
         */
        private Map<String,Integer> priorities;
    }
    /**
     * Statistics，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class Statistics {
        /**
         * unfinished计划数量。
         */
        private Integer unfinishedPlanCount;
        /**
         * 已学习卡片数量。
         */
        private Integer learnedCardCount;
        /**
         * 学习数量。
         */
        private Integer learningCount;
        /**
         * basicMastered数量。
         */
        private Integer basicMasteredCount;
        /**
         * stableMastered数量。
         */
        private Integer stableMasteredCount;
        /**
         * recommended数量。
         */
        private Integer recommendedCount;
        /**
         * plans。
         */
        private List<Plan> plans;
    }
    /**
     * 教程Option，属于教程与学习模块。
     * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
     */
    @Data
    public static class TutorialOption {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 标题。
         */
        private String title;
    }
}
