package com.starrainnotes.tutorial.learning.exception;

import com.starrainnotes.common.exception.ApiException;

public class LearningStateConflictException extends ApiException {
    public LearningStateConflictException(String message) {
        super("LEARNING_STATE_CONFLICT", message, 409);
    }
}
