package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanDTO {
    @NotNull
    private Long tutorialId;
    @NotBlank
    @Size(max = 160)
    private String name;
    @NotBlank
    private String scopeType;
    @NotNull
    private Long scopeId;
    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
    @NotEmpty
    @Size(max = 7)
    private List<@Min(1) @Max(7) Integer> studyWeekdays;
    @Min(5)
    @Max(600)
    private Integer dailyTargetMinutes;
}
