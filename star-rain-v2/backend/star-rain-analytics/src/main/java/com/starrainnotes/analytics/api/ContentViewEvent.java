package com.starrainnotes.analytics.api;

public class ContentViewEvent {
    private final String contentType;
    private final Long contentId;
    private final String routeKey;

    public ContentViewEvent(String contentType, Long contentId, String routeKey) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.routeKey = routeKey;
    }

    public String getContentType() { return contentType; }
    public Long getContentId() { return contentId; }
    public String getRouteKey() { return routeKey; }
}
