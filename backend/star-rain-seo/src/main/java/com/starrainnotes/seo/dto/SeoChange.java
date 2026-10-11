package com.starrainnotes.seo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoChange {
    private String routePath;
    private String canonicalUrl;
    private String changeType;
}
