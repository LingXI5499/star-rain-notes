package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 媒体资源不存在。
 *
 * 错误码 MEDIA_NOT_FOUND，HTTP 404。
 */
public class MediaAssetNotFoundException extends ApiException {

    public MediaAssetNotFoundException() {
        super("MEDIA_NOT_FOUND", "媒体资源不存在", 404);
    }
}