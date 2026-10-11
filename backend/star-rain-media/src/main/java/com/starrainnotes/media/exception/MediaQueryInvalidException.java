package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 查询参数非法。
 *
 * 错误码 MEDIA_QUERY_INVALID，HTTP 400。
 */
public class MediaQueryInvalidException extends ApiException {

    public MediaQueryInvalidException(String message) {
        super("MEDIA_QUERY_INVALID", message, 400);
    }
}