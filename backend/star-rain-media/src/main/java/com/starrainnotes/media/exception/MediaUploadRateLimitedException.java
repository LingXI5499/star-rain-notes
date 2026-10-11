package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

public class MediaUploadRateLimitedException extends ApiException {

    public MediaUploadRateLimitedException() {
        super("MEDIA_UPLOAD_RATE_LIMITED", "上传过于频繁，请稍后再试", 429);
    }
}
