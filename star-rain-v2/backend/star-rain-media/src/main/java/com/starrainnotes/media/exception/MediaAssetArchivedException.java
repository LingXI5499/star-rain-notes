package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 已归档媒体不允许公开读取。
 *
 * 错误码 MEDIA_ARCHIVED，HTTP 403。
 */
public class MediaAssetArchivedException extends ApiException {

    public MediaAssetArchivedException() {
        super("MEDIA_ARCHIVED", "媒体资源已归档", 403);
    }
}