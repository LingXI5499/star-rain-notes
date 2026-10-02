package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存储删除失败。
 *
 * 错误码 MEDIA_STORAGE_DELETE_FAILED，HTTP 500。
 */
public class MediaStorageDeleteException extends ApiException {

    public MediaStorageDeleteException(Throwable cause) {
        super("MEDIA_STORAGE_DELETE_FAILED", "媒体文件删除失败：" + cause.getMessage(), 500, cause);
    }
}