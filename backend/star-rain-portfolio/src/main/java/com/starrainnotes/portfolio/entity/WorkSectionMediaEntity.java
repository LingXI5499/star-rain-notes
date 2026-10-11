package com.starrainnotes.portfolio.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkSectionMediaEntity {
    private Long id;
    private Long sectionId;
    private Long mediaAssetId;
    private String caption;
    private Integer sortOrder;
}
