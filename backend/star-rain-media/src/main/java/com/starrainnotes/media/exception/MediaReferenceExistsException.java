package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一来源与用途的引用重复登记。
 *
 * 错误码 MEDIA_REFERENCE_EXISTS，HTTP 409。
 */
public class MediaReferenceExistsException extends ApiException {

    public MediaReferenceExistsException() {
        super("MEDIA_REFERENCE_EXISTS", "该媒体引用已经存在", 409);
    }
}