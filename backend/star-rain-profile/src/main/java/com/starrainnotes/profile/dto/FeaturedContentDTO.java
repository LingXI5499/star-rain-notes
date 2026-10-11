package com.starrainnotes.profile.dto;

import lombok.Data;

@Data
public class FeaturedContentDTO {
    private String contentType;
    private Long contentId;
    private String titleOverride;
}
