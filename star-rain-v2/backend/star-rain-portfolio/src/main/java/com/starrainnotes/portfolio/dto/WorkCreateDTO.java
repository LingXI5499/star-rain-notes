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
    @Size(max = 1000)
    private String summary;
}
