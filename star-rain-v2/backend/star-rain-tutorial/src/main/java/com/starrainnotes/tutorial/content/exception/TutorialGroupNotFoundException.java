package com.starrainnotes.tutorial.content.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialGroupNotFoundException extends ApiException {
    public TutorialGroupNotFoundException() {
        super("TUTORIAL_GROUP_NOT_FOUND", "课程分组不存在", 404);
    }
}
