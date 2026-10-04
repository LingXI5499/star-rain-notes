package com.starrainnotes.analytics.entity;

import lombok.Data;

@Data
public class AnalyticsEventEntity {
    private String eventType;
    private String routeKey;
    private String contentType;
    private Long contentId;
    private String referrerCategory;
    private String referrerHost;
}
