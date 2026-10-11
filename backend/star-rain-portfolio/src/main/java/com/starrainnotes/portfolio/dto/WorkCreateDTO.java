package com.starrainnotes.portfolio.dto;

import com.starrainnotes.portfolio.enumeration.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkCreateDTO {
    @NotNull
    private WorkType workType;
    @NotBlank
    @Size(max = 255)
    private String title;
    private String slug;
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
    private Boolean featured;
    private Integer sortOrder;
    private String seoTitle;
    private String seoDescription;
    private java.util.List<Long> tagIds;
    private Long templateId;

}
