package com.starrainnotes.tutorial.learning.exception;

import com.starrainnotes.common.exception.ApiException;

public class ReviewTaskAlreadyCompletedException extends ApiException {
    public ReviewTaskAlreadyCompletedException() {
        super("REVIEW_TASK_ALREADY_COMPLETED", "这项复习已经完成", 409);
    }
}
