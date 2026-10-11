package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkStateInvalidException extends ApiException {
    public WorkStateInvalidException(String message) {
        super("WORK_STATE_INVALID", message, 409);
    }
}
