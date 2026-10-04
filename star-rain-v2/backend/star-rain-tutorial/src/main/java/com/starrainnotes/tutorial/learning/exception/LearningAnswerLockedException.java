package com.starrainnotes.tutorial.learning.exception;

import com.starrainnotes.common.exception.ApiException;

public class LearningAnswerLockedException extends ApiException {
    public LearningAnswerLockedException() {
        super("LEARNING_ANSWER_LOCKED", "请先提交自己的答案，再查看参考答案", 403);
    }
}
