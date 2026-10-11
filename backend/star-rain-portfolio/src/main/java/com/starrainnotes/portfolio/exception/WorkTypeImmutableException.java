package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkTypeImmutableException extends ApiException {
    public WorkTypeImmutableException() {
        super("WORK_TYPE_IMMUTABLE", "作品类型创建后不可直接修改", 409);
    }
}
