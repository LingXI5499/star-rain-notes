package com.starrainnotes.portfolio.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkTemplateEntity {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String sectionsJson;
}
