package com.starrainnotes.account.review.api;

import java.util.Map;

/** Applies an approved review to the content module that owns the type. */
public interface ReviewedContentHandler {
    boolean supports(String contentType, String actionType);

    void apply(long contentId, Map<String, Object> payload);
}
