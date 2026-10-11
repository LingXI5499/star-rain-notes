package com.starrainnotes.portfolio.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkFilterDTO {
    private Long categoryId;
    private Long formatId;
    private Long tagId;
    private Boolean featured;
}
