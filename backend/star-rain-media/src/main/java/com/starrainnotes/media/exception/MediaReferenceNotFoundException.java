package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 要解除的引用不存在。
 *
 * 错误码 MEDIA_REFERENCE_NOT_FOUND，HTTP 404。
 */
public class MediaReferenceNotFoundException extends ApiException {

    public MediaReferenceNotFoundException() {
        super("MEDIA_REFERENCE_NOT_FOUND", "媒体引用不存在", 404);
    }
}