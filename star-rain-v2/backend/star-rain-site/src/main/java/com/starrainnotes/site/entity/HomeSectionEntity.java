package com.starrainnotes.site.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HomeSectionEntity {
    private Long id;
    private String sectionCode;
    private String displayName;
    private Boolean enabled;
    private Integer sortOrder;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
