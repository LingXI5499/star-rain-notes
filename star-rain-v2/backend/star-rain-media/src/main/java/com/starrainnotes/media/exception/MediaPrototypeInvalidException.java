package com.starrainnotes.media.exception;
import com.starrainnotes.common.exception.ApiException;
public class MediaPrototypeInvalidException extends ApiException {
    public MediaPrototypeInvalidException(String message) { super("MEDIA_PROTOTYPE_INVALID", message, 400); }
}
