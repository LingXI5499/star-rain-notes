package com.starrainnotes.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkPatchDTO {
    private String workType;
    @Size(max = 180)
    private String slug;
    @Size(max = 255)
    private String title;
    @Size(max = 1000)
    private String summary;
    private String subtitle;
    private Long categoryId;
    private Long formatId;
    private String role;
    private String techStack;
    private String projectStatus;
    private java.time.LocalDate startedOn;
    private java.time.LocalDate endedOn;
    private Boolean clearStartedOn;
    private Boolean clearEndedOn;
    private Boolean featured;
    private Integer sortOrder;
    private String seoTitle;
    private String seoDescription;
    private java.util.List<Long> tagIds;

}
