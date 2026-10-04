package com.starrainnotes.analytics.exception;

import com.starrainnotes.common.exception.ApiException;

public class AnalyticsDateRangeInvalidException extends ApiException {
    public AnalyticsDateRangeInvalidException() { super("ANALYTICS_DATE_RANGE_INVALID", "统计日期范围无效", 400); }
}
