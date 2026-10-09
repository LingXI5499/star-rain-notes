package com.starrainnotes.tutorial.content.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialCategoryVO {
    private String id;
    private String name;
    private String slug;
    private Integer sortOrder;
}
