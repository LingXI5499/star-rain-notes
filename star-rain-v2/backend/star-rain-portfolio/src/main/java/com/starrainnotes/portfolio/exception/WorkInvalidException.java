package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkInvalidException extends ApiException {
    public WorkInvalidException(String message) {
        super("WORK_DETAIL_INVALID", message, 400);
    }
}
