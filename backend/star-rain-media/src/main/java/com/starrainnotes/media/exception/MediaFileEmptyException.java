package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 上传内容为空。
 *
 * 错误码 MEDIA_FILE_EMPTY，HTTP 400。
 */
public class MediaFileEmptyException extends ApiException {

    public MediaFileEmptyException() {
        super("MEDIA_FILE_EMPTY", "上传文件为空", 400);
    }
}