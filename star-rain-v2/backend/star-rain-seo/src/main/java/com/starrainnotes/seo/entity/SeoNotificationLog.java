package com.starrainnotes.seo.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SeoNotificationLog {
    private Long id;
    private String providerCode;
    private String routePath;
    private String canonicalUrl;
    private String changeType;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextRetryAt;
}
