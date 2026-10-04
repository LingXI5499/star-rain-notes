package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgressDTO {
    @Size(max = 255)
    private String scrollAnchor;
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal progressRatio;
    @Min(0)
    @Max(1800)
    private Integer studySecondsDelta;
}
