package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialNotFoundException extends ApiException {
    public TutorialNotFoundException() {
        super("TUTORIAL_NOT_FOUND", "教程不存在或尚未公开", 404);
    }
}
