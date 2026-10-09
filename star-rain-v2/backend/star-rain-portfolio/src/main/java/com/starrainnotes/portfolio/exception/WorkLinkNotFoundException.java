package com.starrainnotes.portfolio.exception;

import com.starrainnotes.common.exception.ApiException;

public class WorkLinkNotFoundException extends ApiException {
    public WorkLinkNotFoundException() {
        super("WORK_LINK_NOT_FOUND", "作品链接不存在", 404);
    }
}
