package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkMediaNotFoundException extends ApiException {
    public WorkMediaNotFoundException() {
        super("WORK_MEDIA_NOT_FOUND", "作品媒体不存在", 404);
    }
}
