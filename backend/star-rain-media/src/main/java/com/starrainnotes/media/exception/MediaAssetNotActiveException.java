package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 目标媒体存在但不是 ACTIVE，不能建立新的业务引用。
 *
 * 错误码 MEDIA_NOT_ACTIVE，HTTP 409。
 */
public class MediaAssetNotActiveException extends ApiException {

    public MediaAssetNotActiveException() {
        super("MEDIA_NOT_ACTIVE", "媒体资源当前不可用", 409);
    }
}