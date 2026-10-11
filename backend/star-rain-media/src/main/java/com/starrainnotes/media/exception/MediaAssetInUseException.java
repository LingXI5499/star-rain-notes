package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存在业务引用时禁止归档。
 *
 * 错误码 MEDIA_ASSET_IN_USE，HTTP 409。
 */
public class MediaAssetInUseException extends ApiException {

    public MediaAssetInUseException(long referenceCount) {
        super("MEDIA_ASSET_IN_USE", "媒体仍被 " + referenceCount + " 处业务引用，不能归档", 409);
    }
}