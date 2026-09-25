package com.starrainnotes.english.grammar.domain;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Preserves the current grammar lifecycle transition rules. */
@Component
public class GrammarPublishPolicy {
    public void validateCourseWithdrawal(String publishStatus) {
        if ("DRAFT".equals(publishStatus)) {
            throw invalidTransition("A draft grammar course cannot be withdrawn.");
        }
    }

    public void validateLessonWithdrawal(String publishStatus) {
        if ("DRAFT".equals(publishStatus)) {
            throw invalidTransition("A draft grammar lesson cannot be withdrawn.");
        }
    }

    public void requireEmptySection(long lessonCount) {
        if (lessonCount > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GRAMMAR_SECTION_NOT_EMPTY",
                    "Section is not empty", "Move or delete all lessons before deleting this section.");
        }
    }

    public void requireSameSection(Long currentSectionId, Long requestedSectionId) {
        if (currentSectionId == null || !currentSectionId.equals(requestedSectionId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GRAMMAR_CROSS_SECTION_EDIT_FORBIDDEN",
                    "Use the move action", "A lesson can change sections only through the explicit reassign endpoint.");
        }
    }

    public void requireCover(Long mediaId, boolean image) {
        if (mediaId == null || image) {
            return;
        }
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                "Invalid grammar cover", "The selected cover must be an existing image asset.");
    }

    public ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "GRAMMAR_LESSON_SLUG_CONFLICT",
                "Grammar lesson slug already exists", "Choose another stable lesson number.");
    }

    public boolean isPublished(String publishStatus) {
        return "PUBLISHED".equals(publishStatus);
    }

    private ApiException invalidTransition(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                "Invalid publish transition", detail);
    }
}
