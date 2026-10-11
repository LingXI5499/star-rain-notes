package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存储写入失败。
 *
 * 错误码 MEDIA_STORAGE_WRITE_FAILED，HTTP 500。
 */
public class MediaStorageWriteException extends ApiException {

    public MediaStorageWriteException(Throwable cause) {
        super("MEDIA_STORAGE_WRITE_FAILED", "媒体存储目录无法写入，请检查目录权限和磁盘空间", 500, cause);
    }
}
