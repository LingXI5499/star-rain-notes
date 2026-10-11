package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkMediaConflictException extends ApiException {
    public WorkMediaConflictException() {
        super("WORK_MEDIA_CONFLICT", "该媒体已关联作品", 409);
    }
}
