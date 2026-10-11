package com.starrainnotes.seo.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SeoPageSnapshot {
    private String routePath;
    private String contentType;
    private Long contentId;
    private String canonicalUrl;
    private String title;
    private String description;
    private String robotsDirective;
    private String structuredDataJson;
    private String htmlSnapshot;
    private String sourceVersionRef;
    private String status;
    private LocalDateTime generatedAt;
}
