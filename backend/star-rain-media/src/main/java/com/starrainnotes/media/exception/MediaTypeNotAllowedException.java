package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 扩展名不在允许白名单内。
 *
 * 错误码 MEDIA_TYPE_NOT_ALLOWED，HTTP 415。
 */
public class MediaTypeNotAllowedException extends ApiException {

    public MediaTypeNotAllowedException() {
        super("MEDIA_TYPE_NOT_ALLOWED", "不支持该文件类型", 415);
    }
}