package com.starrainnotes.analytics.exception;

import com.starrainnotes.common.exception.ApiException;

public class AnalyticsContentTypeInvalidException extends ApiException {
    public AnalyticsContentTypeInvalidException() { super("ANALYTICS_CONTENT_TYPE_INVALID", "内容类型或数量限制无效", 400); }
}
