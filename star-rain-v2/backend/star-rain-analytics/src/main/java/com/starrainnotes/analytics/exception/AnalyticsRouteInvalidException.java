package com.starrainnotes.analytics.exception;

import com.starrainnotes.common.exception.ApiException;

public class AnalyticsRouteInvalidException extends ApiException {
    public AnalyticsRouteInvalidException() { super("ANALYTICS_ROUTE_INVALID", "只接受指定公开页面的访问记录", 400); }
}
