package com.starrainnotes.english.api;

/** Publication check for account content review without exposing English tables. */
public interface EnglishReviewContentPort {
    boolean isPublished(String reviewContentType, long contentId);
}
