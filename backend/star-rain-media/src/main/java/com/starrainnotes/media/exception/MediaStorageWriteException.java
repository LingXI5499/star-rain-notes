package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存储写入失败。
 *
 * 错误码 MEDIA_STORAGE_WRITE_FAILED，HTTP 500。
 */
public class MediaStorageWriteException extends ApiException {

    public MediaStorageWriteException(Throwable cause) {
        super("MEDIA_STORAGE_WRITE_FAILED", "媒体文件写入失败：" + cause.getMessage(), 500, cause);
    }
}