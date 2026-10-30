/*
 * VocabularyLearningModels：英语学习模块的模块支持文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：Word。
 */

package com.starrainnotes.english.vocabulary.learning;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Account learning contracts; UTC dates have an explicit Z suffix at the JSON boundary. */
public final class VocabularyLearningModels {
    /**
     * 构造VocabularyLearningModels，接收本类使用的依赖或初始化参数。
     */
    private VocabularyLearningModels() { }

    /**
     * 筛选，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class Filter {
        @Pattern(regexp = "T[0-9]{2}|^$") private String category = "";
        @Pattern(regexp = "T[0-9]{2}-[0-9]{2}|^$") private String topic = "";
        @Pattern(regexp = "confirmed|needs-review|unclassified|^$") private String libraryStatus = "";
        private boolean learnableOnly;
        /**
         * 主题标识。
         * 校验规则 <code>@Positive</code>。
         */
        @Positive private Long themeId;
        /**
         * q。
         * 校验规则 <code>@Size(max = 200)</code>。
         */
        @Size(max = 200) private String q = "";
        /**
         * 已学习。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "ANY|YES|NO")</code>。
         */
        @NotNull @Pattern(regexp = "ANY|YES|NO") private String learned = "ANY";
        /**
         * 掌握。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "ANY|BEGINNER|LEARNER|SKILLED|MASTERED|UNRATED")</code>。
         */
        @NotNull @Pattern(regexp = "ANY|BEGINNER|LEARNER|SKILLED|MASTERED|UNRATED") private String mastery = "ANY";
        /**
         * 内部计划。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "ANY|YES|NO")</code>。
         */
        @NotNull @Pattern(regexp = "ANY|YES|NO") private String inPlan = "ANY";
        /**
         * direction。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "ANY|EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH")</code>。
         */
        @NotNull @Pattern(regexp = "ANY|EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH") private String direction = "ANY";
        /**
         * 最近评分。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "ANY|FORGOT|UNCERTAIN|KNOW")</code>。
         */
        @NotNull @Pattern(regexp = "ANY|FORGOT|UNCERTAIN|KNOW") private String lastRating = "ANY";
    }

    /**
     * Selection，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class Selection extends Filter {
        /**
         * 查询全部Matched。
         */
        private boolean selectAllMatched;
        /**
         * 单词标识集合。
         * 不允许为 null。
         * 校验规则 <code>@Size(max = 1000)</code>。
         */
        @NotNull @Size(max = 1000) private List<@NotNull @Positive Long> wordIds = new ArrayList<>();
        /**
         * excluded单词标识集合。
         * 不允许为 null。
         * 校验规则 <code>@Size(max = 1000)</code>。
         */
        @NotNull @Size(max = 1000) private List<@NotNull @Positive Long> excludedWordIds = new ArrayList<>();
        /**
         * 批量大小。
         * 校验规则 <code>@Min(5)</code>。
         * 校验规则 <code>@Max(100)</code>。
         * 初始值为 <code>20</code>。
         */
        @Min(5) @Max(100) private int batchSize = 20;
        /**
         * 名称。
         * 校验规则 <code>@Size(max = 200)</code>。
         */
        @Size(max = 200) private String name;
        /**
         * expected修订。
         * 校验规则 <code>@Min(0)</code>。
         */
        @Min(0) private long expectedRevision;
        /**
         * 预览Fingerprint。
         * 校验规则 <code>@Size(max = 64)</code>。
         */
        @Size(max = 64) private String previewFingerprint;
    }

    /**
     * 评分请求，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class RatingRequest {
        /**
         * 审核会话标识。
         * 不能为空或仅含空白字符。
         * 校验规则 <code>@Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")</code>。
         */
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        private String reviewSessionId;
        /**
         * direction。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH")</code>。
         */
        @NotNull @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH") private String direction;
        /**
         * 评分。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "FORGOT|UNCERTAIN|KNOW")</code>。
         */
        @NotNull @Pattern(regexp = "FORGOT|UNCERTAIN|KNOW") private String rating;
        /**
         * 来源。
         * 不允许为 null。
         * 校验规则 <code>@Pattern(regexp = "PLAN|REVIEW|FREE")</code>。
         */
        @NotNull @Pattern(regexp = "PLAN|REVIEW|FREE") private String source;
        /**
         * 计划修订。
         * 校验规则 <code>@Min(0)</code>。
         */
        @Min(0) private Long planRevision;
        /**
         * 音频Played。
         */
        private boolean audioPlayed;
    }

    /**
     * 修订请求，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class RevisionRequest {
        /**
         * expected修订。
         * 校验规则 <code>@Min(0)</code>。
         */
        @Min(0) private long expectedRevision;
        /**
         * 批量大小。
         * 校验规则 <code>@Min(5)</code>。
         * 校验规则 <code>@Max(100)</code>。
         * 初始值为 <code>20</code>。
         */
        @Min(5) @Max(100) private int batchSize = 20;
    }

    /**
     * Mode记忆，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class ModeMemory {
        /**
         * 单词标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        /**
         * direction。
         */
        private String direction;
        /**
         * 评分数量。
         */
        private int ratingCount;
        /**
         * emaScore。
         */
        private double emaScore;
        /**
         * 审核Step。
         * 初始值为 <code>-1</code>。
         */
        private int reviewStep = -1;
        /**
         * recentGood数量。
         */
        private int recentGoodCount;
        /**
         * 最近评分。
         */
        private String lastRating;
        /**
         * 首次Rated时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime firstRatedAt;
        /**
         * 最近Rated时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime lastRatedAt;
        /**
         * 下一次计划复习时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime nextReviewAt;
        /**
         * 音频Verified。
         */
        private boolean audioVerified;
    }

    /**
     * 状态，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class State {
        /**
         * 单词标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        /**
         * 记忆数量。
         */
        private int memoryCount;
        /**
         * 学习状态。
         */
        private String learningStatus;
        /**
         * 显示Mode。
         */
        private String displayMode;
        /**
         * 最近一次复习时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime lastReviewedAt;
        /**
         * 下一次计划复习时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime nextReviewAt;
        /**
         * 掌握Score。
         */
        private int masteryScore;
        /**
         * 掌握Rank。
         */
        private String masteryRank;
        /**
         * 首次Rated时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime firstRatedAt;
        /**
         * 最近Rated时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime lastRatedAt;
        /**
         * mode记忆。
         */
        private List<ModeMemory> modeMemory = List.of();
        /**
         * 内部计划。
         */
        private boolean inPlan;
    }

    /**
     * 入口，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class Entry {
        /**
         * 分组不含。
         */
        private int groupNo;
        /**
         * direction。
         */
        private String direction;
        /**
         * 单词标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        /**
         * 排序不含。
         */
        private int sortNo;
    }

    /**
     * 计划，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class Plan {
        /**
         * 账号标识。
         */
        @JsonIgnore private Long accountId;
        /**
         * 修订。
         */
        private long revision;
        /**
         * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
         */
        private String status = "NONE";
        /**
         * 名称。
         */
        private String name;
        /**
         * 来源主题标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class) private Long sourceThemeId;
        /**
         * 来源名称。
         */
        private String sourceName;
        /**
         * 批量大小。
         * 初始值为 <code>20</code>。
         */
        private int batchSize = 20;
        /**
         * 总数Words。
         */
        private int totalWords;
        /**
         * 总数Groups。
         */
        private int totalGroups;
        /**
         * 已完成Ratings。
         */
        private int completedRatings;
        /**
         * 下一项默认入口。
         */
        private Entry nextDefaultEntry;
        /**
         * 创建时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime createdAt;
        /**
         * 更新时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime updatedAt;
    }

    /**
     * 计划Item，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class PlanItem {
        /**
         * 单词标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        /**
         * 排序不含。
         */
        private int sortNo;
        /**
         * 分组不含。
         */
        private int groupNo;
        /**
         * doneMask。
         */
        private int doneMask;
        /**
         * 单词JSON。
         */
        @JsonIgnore private String wordJson;
        /**
         * 单词。
         */
        private Word word;
        /**
         * 记忆。
         */
        private State memory;
        /**
         * unavailable。
         */
        private boolean unavailable;
    }

    /**
     * 分组进度，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class GroupProgress {
        /**
         * 分组不含。
         */
        private int groupNo;
        /**
         * 符合查询条件的总记录数。
         */
        private int total;
        /**
         * en目标Zh。
         */
        private int enToZh;
        /**
         * zh目标En。
         */
        private int zhToEn;
        /**
         * 音频目标Both。
         */
        private int audioToBoth;
    }

    /**
     * 预览，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class Preview {
        /**
         * expected修订。
         */
        private long expectedRevision;
        /**
         * 预览Fingerprint。
         */
        private String previewFingerprint;
        /**
         * 总数Words。
         */
        private int totalWords;
        /**
         * 已学习Words。
         */
        private long learnedWords;
        /**
         * 总数Groups。
         */
        private int totalGroups;
        /**
         * 来源名称。
         */
        private String sourceName;
        /**
         * existing计划。
         */
        private Plan existingPlan;
        /**
         * samples。
         */
        private List<Word> samples = List.of();
        /**
         * 条目集合。
         */
        private List<Word> items = List.of();
        /**
         * 分页页码。
         */
        private int page;
        /**
         * 总数Pages。
         */
        private int totalPages;
    }

    /**
     * Selection行，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class SelectionRow {
        /**
         * 单词标识。
         */
        private Long wordId;
        /**
         * 令牌。
         */
        private String token;
    }

    /**
     * 日数量集合，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class DayCounts {
        /**
         * 符合查询条件的总记录数。
         */
        private int total;
        /**
         * recent。
         */
        private int recent;
    }

    /**
     * 审核摘要，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class ReviewSummary {
        /**
         * due卡片集合。
         */
        private long dueCards;
        /**
         * dueWords。
         */
        private long dueWords;
        /**
         * overdue卡片集合。
         */
        private long overdueCards;
        /**
         * 已完成Today。
         */
        private long completedToday;
        /**
         * earliestDue时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime earliestDueAt;
    }

    /**
     * DueCursor，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class DueCursor {
        /**
         * as所属。
         */
        private LocalDateTime asOf;
        /**
         * due时间。
         */
        private LocalDateTime dueAt;
        /**
         * 单词标识。
         */
        private long wordId;
        /**
         * direction。
         */
        private String direction;
    }

    /**
     * Due卡片，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class DueCard {
        /**
         * 单词。
         */
        private Word word;
        /**
         * mode记忆。
         */
        private ModeMemory modeMemory;
        /**
         * 记忆。
         */
        private State memory;
        /**
         * direction。
         */
        private String direction;
        /**
         * 下一次计划复习时间。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime nextReviewAt;
    }

    /**
     * Queue，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class Queue {
        /**
         * 条目集合。
         */
        private List<DueCard> items = List.of();
        /**
         * 下一项Cursor。
         */
        private String nextCursor;
        /**
         * as所属。
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime asOf;
    }

    /**
     * 评分结果，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class RatingResult {
        /**
         * 记忆。
         */
        private State memory;
        /**
         * mode记忆。
         */
        private ModeMemory modeMemory;
        /**
         * duplicate。
         */
        private boolean duplicate;
        /**
         * 计划Item已完成。
         */
        private boolean planItemCompleted;
        /**
         * 计划。
         */
        private Plan plan;
        /**
         * 下一次复习的间隔，单位为秒。
         */
        private long intervalSeconds;
        /**
         * 调度状态。
         */
        private String timingStatus;
    }

    /**
     * 评分Log，属于英语学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    public static class RatingLog {
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
         * 评分。
         */
        private String rating;
        /**
         * 来源。
         */
        private String source;
        /**
         * 计划修订。
         */
        private Long planRevision;
        /**
         * 音频Played。
         */
        private boolean audioPlayed;
        /**
         * oldStep。
         */
        private int oldStep;
        /**
         * newStep。
         */
        private int newStep;
        /**
         * oldScore。
         */
        private double oldScore;
        /**
         * newScore。
         */
        private double newScore;
        /**
         * 审核请求的业务编号。
         */
        private int reviewNumber;
        /**
         * 计划调度时间。
         */
        private LocalDateTime scheduledAt;
        /**
         * 复习时间。
         */
        private LocalDateTime reviewedAt;
        /**
         * 下一次复习的间隔，单位为秒。
         */
        private long intervalSeconds;
        /**
         * 调度状态。
         */
        private String timingStatus;
        /**
         * 结果JSON。
         */
        private String resultJson;
    }
}
