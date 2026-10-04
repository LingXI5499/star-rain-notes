package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class LearningInvalidRequestException extends ApiException {
    public LearningInvalidRequestException(String message) {
        super("LEARNING_INVALID_REQUEST", message, 400);
    }
}
