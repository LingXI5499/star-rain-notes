package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialConflictException extends ApiException {
    public TutorialConflictException(String message) {
        super("TUTORIAL_CONFLICT", message, 409);
    }
}
