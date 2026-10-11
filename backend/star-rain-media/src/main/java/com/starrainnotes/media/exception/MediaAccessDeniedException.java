package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 受保护媒体的读取授权失败。
 *
 * 错误码 MEDIA_ACCESS_DENIED，HTTP 403。
 */
public class MediaAccessDeniedException extends ApiException {

    public MediaAccessDeniedException() {
        super("MEDIA_ACCESS_DENIED", "没有读取该媒体资源的权限", 403);
    }
}