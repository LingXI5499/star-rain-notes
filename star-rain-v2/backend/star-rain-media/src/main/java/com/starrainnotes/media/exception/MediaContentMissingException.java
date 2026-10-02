package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 数据库有记录但文件缺失，需运维介入。
 *
 * 错误码 MEDIA_CONTENT_MISSING，HTTP 500。
 */
public class MediaContentMissingException extends ApiException {

    public MediaContentMissingException() {
        super("MEDIA_CONTENT_MISSING", "媒体文件在存储中缺失", 500);
    }
}