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
    private EvidenceModels() { }
    @Data
    public static class Plan {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        private String name;
        private String tutorialTitle;
        private String tutorialSlug;
        private String status;
        private Integer targetCardsPerTask;
        private Integer studyRound = 1;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<Chapter> chapters;
        private List<Task> tasks;
        private Integer completedChapters;
        private Integer totalChapters;
    }
    @Data
    public static class Chapter {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long groupId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        private Integer groupOrder;
        private Integer chapterOrder;
        private Integer cardCount;
        private Integer questionCount;
        private String chapterTitle;
        private String chapterSlug;
        private String groupTitle;
        private Boolean completed;
    }
    @Data
    public static class Task {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long groupId;
        private Integer studyRound = 1;
        private Integer sequenceNo;
        private Integer cardCount;
        private Integer questionCount;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private List<Chapter> chapters;
    }
    @Data
    public static class Session {
        private String tutorialTitle;
        private String chapterTitle;
        private String planName;
        @JsonIgnore
        private String summaryJson;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        private Integer studyRound = 1;
        private String sessionType;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long studyPlanId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long studyTaskId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        private String status;
        private Integer requestedItemCount;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime abandonedAt;
        private List<Item> items;
        private SessionSummary summary;
    }
    @Data
    public static class Item {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sessionId;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        private Integer cardContentVersion;
        private String frontText;
        private String backMarkdown;
        private Integer sourcePriority;
        private Integer sequenceNo;
        private String status;
        private LocalDateTime revealedAt;
        private LocalDateTime completedAt;
    }
    @Data
    public static class Evidence {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sessionId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        private Integer cardContentVersion;
        private String evidenceType;
        private String rating;
        private String previousMasteryStatus;
        private String masteryStatus;
        private LocalDateTime createdAt;
    }
    @Data
    public static class Mastery {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        private String masteryStatus;
        private String latestRating;
        private Integer evidenceCount;
        private Integer forgotCount;
        private Integer fuzzyCount;
        private Integer rememberedCount;
        private Integer recentScore;
        private LocalDateTime lastEvidenceAt;
        private Integer latestEvidenceCardVersion;
    }
    @Data
    public static class CardMastery {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long knowledgeCardId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long tutorialId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        private String tutorialTitle;
        private String chapterTitle;
        private String frontText;
        private Integer contentVersion;
        private String masteryStatus;
        private String latestRating;
        private Integer evidenceCount;
        private Integer recentScore;
        private LocalDateTime lastEvidenceAt;
        private Boolean needsRevalidation;
        private Integer priority;
        @JsonIgnore
        private Integer chapterOrder;
        @JsonIgnore
        private Integer cardOrder;
        @JsonIgnore
        private String backMarkdown;
    }
    @Data
    public static class Answer {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonIgnore
        @JsonSerialize(using = ToStringSerializer.class)
        private Long accountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long latestVersionId;
        private Integer versionCount;
        private String answerText;
        private LocalDateTime firstAnsweredAt;
        private LocalDateTime lastAnsweredAt;
        private LocalDateTime referenceUnlockedAt;
        private List<AnswerVersion> versions;
    }
    @Data
    public static class AnswerVersion {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long answerId;
        private Integer versionNo;
        private String answerText;
        private String answerPhase;
        private LocalDateTime createdAt;
    }
    @Data
    public static class StudyState {
        private String tutorialSlug;
        private String chapterSlug;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long chapterId;
        private String title;
        private String bodyMarkdown;
        private List<Question> questions;
        private List<Long> initialCardIds;
        private List<String> requiredCardIds;
        private Boolean completed;
        private Session session;
    }
    @Data
    public static class Question {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String questionText;
        private List<Long> knowledgeCardIds;
        private Boolean answered;
    }
    @Data
    public static class SessionSummary {
        private Integer cardCount;
        private Integer forgotCount;
        private Integer fuzzyCount;
        private Integer rememberedCount;
        private Integer revalidationCount;
        private Integer questionCount;
        private Integer upgradedCount;
        private Integer downgradedCount;
        private Map<String,Integer> transitions;
    }
    @Data
    public static class ReviewSummary {
        private Integer candidateCount;
        private Integer recommendedCount;
        private Integer stableCount;
        private Map<String,Integer> priorities;
    }
    @Data
    public static class Statistics {
        private Integer unfinishedPlanCount;
        private Integer learnedCardCount;
        private Integer learningCount;
        private Integer basicMasteredCount;
        private Integer stableMasteredCount;
        private Integer recommendedCount;
        private List<Plan> plans;
    }
    @Data
    public static class TutorialOption {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String title;
    }
}
