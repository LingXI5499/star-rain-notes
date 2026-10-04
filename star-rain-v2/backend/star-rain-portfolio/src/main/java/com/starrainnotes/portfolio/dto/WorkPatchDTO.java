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
}
