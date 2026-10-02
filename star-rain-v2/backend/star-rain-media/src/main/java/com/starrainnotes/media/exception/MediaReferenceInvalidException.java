package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 媒体引用参数非法。
 *
 * 错误码 MEDIA_REFERENCE_INVALID，HTTP 400。
 */
public class MediaReferenceInvalidException extends ApiException {

    public MediaReferenceInvalidException(String message) {
        super("MEDIA_REFERENCE_INVALID", message, 400);
    }
}