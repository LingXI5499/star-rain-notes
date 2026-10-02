package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * Range 请求区间越界。
 *
 * 错误码 MEDIA_RANGE_INVALID，HTTP 416。
 */
public class MediaRangeInvalidException extends ApiException {

    public MediaRangeInvalidException() {
        super("MEDIA_RANGE_INVALID", "请求的字节区间不可满足", 416);
    }
}