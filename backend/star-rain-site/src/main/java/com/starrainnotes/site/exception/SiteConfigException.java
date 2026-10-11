package com.starrainnotes.site.exception;

import com.starrainnotes.common.exception.ApiException;

public class SiteConfigException extends ApiException {
    public SiteConfigException(String code, String message, int status) {
        super(code, message, status);
    }
}
