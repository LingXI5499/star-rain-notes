package com.starrainnotes.analytics.dto;

import lombok.Data;

@Data
public class PageViewDTO {
    private String routeKey;
    private String referrer;
}
