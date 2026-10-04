package com.starrainnotes.analytics.exception;

import com.starrainnotes.common.exception.ApiException;

public class AnalyticsEventRejectedException extends ApiException {
    public AnalyticsEventRejectedException() { super("ANALYTICS_EVENT_REJECTED", "访问记录过于频繁", 429); }
}
