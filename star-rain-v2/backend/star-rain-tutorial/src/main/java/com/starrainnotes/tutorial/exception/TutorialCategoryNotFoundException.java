package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialCategoryNotFoundException extends ApiException {
    public TutorialCategoryNotFoundException() {
        super("TUTORIAL_CATEGORY_NOT_FOUND", "知识体系不存在", 404);
    }
}
