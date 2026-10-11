package com.starrainnotes.tutorial.content.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialInvalidRequestException extends ApiException {
    public TutorialInvalidRequestException(String message) {
        super("TUTORIAL_INVALID_REQUEST", message, 400);
    }
}
