package com.starrainnotes.english.shared.content;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.api.EnglishReviewContentPort;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.infrastructure.EnglishContentStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DefaultEnglishReviewContentPort implements EnglishReviewContentPort {
    private final EnglishContentStateRepository repository;

    public DefaultEnglishReviewContentPort(EnglishContentStateRepository repository) {
        this.repository = repository;
    }

    @Override public boolean isPublished(String reviewContentType, long contentId) {
        EnglishContentKind kind = switch (reviewContentType) {
            case "ENGLISH_GRAMMAR_LESSON" -> EnglishContentKind.GRAMMAR_LESSON;
            case "ENGLISH_READING_ARTICLE" -> EnglishContentKind.READING;
            case "ENGLISH_LISTENING_ITEM" -> EnglishContentKind.LISTENING;
            case "ENGLISH_PRONUNCIATION_RULE" -> EnglishContentKind.PRONUNCIATION_RULE;
            case "ENGLISH_WRITING_RESOURCE" -> EnglishContentKind.WRITING_RESOURCE;
            case "ENGLISH_WRITING_PROMPT" -> EnglishContentKind.WRITING_PROMPT;
            default -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CONTENT_REVIEW_TYPE_INVALID", "Invalid review type",
                    "Unsupported review content type.");
        };
        var state = repository.state(kind, contentId);
        if (state == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_REVIEW_TARGET_NOT_FOUND",
                    "Review target not found", "The content to review does not exist.");
        }
        return state.published();
    }
}
