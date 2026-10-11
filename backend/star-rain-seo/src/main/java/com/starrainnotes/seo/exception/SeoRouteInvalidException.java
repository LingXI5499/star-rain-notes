package com.starrainnotes.seo.exception;

import com.starrainnotes.common.exception.ApiException;

public class SeoRouteInvalidException extends ApiException {
    public SeoRouteInvalidException() { super("SEO_ROUTE_INVALID", "页面路径无效", 400); }
}
