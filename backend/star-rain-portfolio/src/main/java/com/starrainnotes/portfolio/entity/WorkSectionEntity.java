package com.starrainnotes.portfolio.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkSectionEntity {
    private Long id;
    private Long workId;
    private String sectionType;
    private String title;
    private String content;
    private String dataJson;
    private Integer blockVersion;
    private Boolean visible;
    private Integer sortOrder;
}
