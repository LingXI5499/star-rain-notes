package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文件超过该媒体大类的大小上限。
 *
 * 错误码 MEDIA_FILE_TOO_LARGE，HTTP 413。
 */
public class MediaFileTooLargeException extends ApiException {

    public MediaFileTooLargeException(long maxBytes) {
        super("MEDIA_FILE_TOO_LARGE", "文件超过允许大小 " + describeLimit(maxBytes), 413);
    }

    // 小于 1MB 时按 KB 展示，避免出现“0MB”这种无意义提示
    private static String describeLimit(long bytes) {
        long mb = bytes / (1024 * 1024);
        return mb >= 1 ? mb + "MB" : Math.max(1, bytes / 1024) + "KB";
    }
}