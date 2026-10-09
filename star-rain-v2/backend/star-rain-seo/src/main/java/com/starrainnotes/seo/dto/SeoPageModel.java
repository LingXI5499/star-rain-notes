package com.starrainnotes.seo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoPageModel {
    private String canonicalUrl;
    private String title;
    private String description;
    private String robotsDirective;
    private String bodyMarkdown;
    private String coverUrl;
}
