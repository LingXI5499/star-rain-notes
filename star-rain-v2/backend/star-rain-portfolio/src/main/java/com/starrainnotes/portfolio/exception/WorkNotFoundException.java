package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkNotFoundException extends ApiException {
    public WorkNotFoundException() {
        super("WORK_NOT_FOUND", "作品不存在", 404);
    }
}
