package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 声明类型或真实内容与扩展名不符。
 *
 * 错误码 MEDIA_CONTENT_TYPE_MISMATCH，HTTP 415。
 */
public class MediaContentTypeMismatchException extends ApiException {

    public MediaContentTypeMismatchException() {
        super("MEDIA_CONTENT_TYPE_MISMATCH", "声明的 Content-Type 或真实文件内容与扩展名不一致", 415);
    }
}