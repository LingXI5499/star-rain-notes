package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkSlugConflictException extends ApiException {
    public WorkSlugConflictException() {
        super("WORK_SLUG_CONFLICT", "作品地址已被使用", 409);
    }
}
