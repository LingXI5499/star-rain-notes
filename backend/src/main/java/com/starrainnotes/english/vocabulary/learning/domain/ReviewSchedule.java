package com.starrainnotes.english.vocabulary.learning.domain;

import java.time.LocalDateTime;

public record ReviewSchedule(int reviewNumber, int reviewStep, long intervalSeconds,
                             LocalDateTime scheduledAt, LocalDateTime reviewedAt,
                             LocalDateTime nextReviewAt, String timingStatus) {}
