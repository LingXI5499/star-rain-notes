package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class LearningResourceNotFoundException extends ApiException {
    public LearningResourceNotFoundException() {
        super("LEARNING_RESOURCE_NOT_FOUND", "学习内容不存在或尚未公开", 404);
    }
}
