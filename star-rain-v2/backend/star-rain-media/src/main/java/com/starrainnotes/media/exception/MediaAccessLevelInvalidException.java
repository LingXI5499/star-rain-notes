package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 访问级别取值非法。
 *
 * 错误码 MEDIA_ACCESS_LEVEL_INVALID，HTTP 400。
 */
public class MediaAccessLevelInvalidException extends ApiException {

    public MediaAccessLevelInvalidException() {
        super("MEDIA_ACCESS_LEVEL_INVALID", "访问级别只能是 PUBLIC 或 PROTECTED", 400);
    }
}