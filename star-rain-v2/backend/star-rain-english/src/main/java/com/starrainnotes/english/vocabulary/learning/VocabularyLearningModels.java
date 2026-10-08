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
    private VocabularyLearningModels() { }

    @Data
    public static class Filter {
        @Positive private Long themeId;
        @Size(max = 200) private String q = "";
        @NotNull @Pattern(regexp = "ANY|YES|NO") private String learned = "ANY";
        @NotNull @Pattern(regexp = "ANY|BEGINNER|LEARNER|SKILLED|MASTERED|UNRATED") private String mastery = "ANY";
        @NotNull @Pattern(regexp = "ANY|YES|NO") private String inPlan = "ANY";
        @NotNull @Pattern(regexp = "ANY|EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH") private String direction = "ANY";
        @NotNull @Pattern(regexp = "ANY|FORGOT|UNCERTAIN|KNOW") private String lastRating = "ANY";
    }

    @Data
    public static class Selection extends Filter {
        private boolean selectAllMatched;
        @NotNull @Size(max = 1000) private List<@NotNull @Positive Long> wordIds = new ArrayList<>();
        @NotNull @Size(max = 1000) private List<@NotNull @Positive Long> excludedWordIds = new ArrayList<>();
        @Min(5) @Max(100) private int batchSize = 20;
        @Size(max = 200) private String name;
        @Min(0) private long expectedRevision;
        @Size(max = 64) private String previewFingerprint;
    }

    @Data
    public static class RatingRequest {
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        private String reviewSessionId;
        @NotNull @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH") private String direction;
        @NotNull @Pattern(regexp = "FORGOT|UNCERTAIN|KNOW") private String rating;
        @NotNull @Pattern(regexp = "PLAN|REVIEW|FREE") private String source;
        @Min(0) private Long planRevision;
        private boolean audioPlayed;
    }

    @Data
    public static class RevisionRequest {
        @Min(0) private long expectedRevision;
        @Min(5) @Max(100) private int batchSize = 20;
    }

    @Data
    public static class ModeMemory {
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        private String direction;
        private int ratingCount;
        private double emaScore;
        private int reviewStep = -1;
        private int recentGoodCount;
        private String lastRating;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime firstRatedAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime lastRatedAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime nextReviewAt;
        private boolean audioVerified;
    }

    @Data
    public static class State {
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        private int memoryCount;
        private String learningStatus;
        private String displayMode;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime lastReviewedAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime nextReviewAt;
        private int masteryScore;
        private String masteryRank;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime firstRatedAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime lastRatedAt;
        private List<ModeMemory> modeMemory = List.of();
        private boolean inPlan;
    }

    @Data
    public static class Entry {
        private int groupNo;
        private String direction;
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        private int sortNo;
    }

    @Data
    public static class Plan {
        @JsonIgnore private Long accountId;
        private long revision;
        private String status = "NONE";
        private String name;
        @JsonSerialize(using = ToStringSerializer.class) private Long sourceThemeId;
        private String sourceName;
        private int batchSize = 20;
        private int totalWords;
        private int totalGroups;
        private int completedRatings;
        private Entry nextDefaultEntry;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime createdAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime updatedAt;
    }

    @Data
    public static class PlanItem {
        @JsonSerialize(using = ToStringSerializer.class) private Long wordId;
        private int sortNo;
        private int groupNo;
        private int doneMask;
        @JsonIgnore private String wordJson;
        private Word word;
        private State memory;
        private boolean unavailable;
    }

    @Data
    public static class GroupProgress {
        private int groupNo;
        private int total;
        private int enToZh;
        private int zhToEn;
        private int audioToBoth;
    }

    @Data
    public static class Preview {
        private long expectedRevision;
        private String previewFingerprint;
        private int totalWords;
        private long learnedWords;
        private int totalGroups;
        private String sourceName;
        private Plan existingPlan;
        private List<Word> samples = List.of();
        private List<Word> items = List.of();
        private int page;
        private int totalPages;
    }

    @Data
    public static class SelectionRow {
        private Long wordId;
        private String token;
    }

    @Data
    public static class DayCounts {
        private int total;
        private int recent;
    }

    @Data
    public static class ReviewSummary {
        private long dueCards;
        private long dueWords;
        private long overdueCards;
        private long completedToday;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime earliestDueAt;
    }

    @Data
    public static class DueCursor {
        private LocalDateTime asOf;
        private LocalDateTime dueAt;
        private long wordId;
        private String direction;
    }

    @Data
    public static class DueCard {
        private Word word;
        private ModeMemory modeMemory;
        private State memory;
        private String direction;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime nextReviewAt;
    }

    @Data
    public static class Queue {
        private List<DueCard> items = List.of();
        private String nextCursor;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") private LocalDateTime asOf;
    }

    @Data
    public static class RatingResult {
        private State memory;
        private ModeMemory modeMemory;
        private boolean duplicate;
        private boolean planItemCompleted;
        private Plan plan;
        private long intervalSeconds;
        private String timingStatus;
    }

    @Data
    public static class RatingLog {
        private Long wordId;
        private String reviewSessionId;
        private String direction;
        private String rating;
        private String source;
        private Long planRevision;
        private boolean audioPlayed;
        private int oldStep;
        private int newStep;
        private double oldScore;
        private double newScore;
        private int reviewNumber;
        private LocalDateTime scheduledAt;
        private LocalDateTime reviewedAt;
        private long intervalSeconds;
        private String timingStatus;
        private String resultJson;
    }
}
