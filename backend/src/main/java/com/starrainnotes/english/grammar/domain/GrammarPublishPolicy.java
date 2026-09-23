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

    private ApiException invalidTransition(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                "Invalid publish transition", detail);
    }
}
