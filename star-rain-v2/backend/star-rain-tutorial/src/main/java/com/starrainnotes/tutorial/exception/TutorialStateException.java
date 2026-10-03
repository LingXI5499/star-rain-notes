package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialStateException extends ApiException {
    public TutorialStateException(String message) {
        super("TUTORIAL_STATE_INVALID", message, 409);
    }
}
