package com.starrainnotes.portfolio.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkTaxonomyVO {
    private String id;
    private String code;
    private String name;
    private String groupCode;
}
