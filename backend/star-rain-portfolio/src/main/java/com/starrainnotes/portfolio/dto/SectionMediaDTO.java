package com.starrainnotes.portfolio.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SectionMediaDTO {
    private Long mediaAssetId;
    private String caption;
}
