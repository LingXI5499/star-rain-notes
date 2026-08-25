package com.starrainnotes.english.shared.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public record LearningRecordRequest(
        @NotBlank String status,
        @Min(0) @Max(100) BigDecimal score,
        @Min(0) Integer timeSpentSeconds,
        List<String> weakPoints,
        @Min(0) @Max(1) BigDecimal mastery) {
}
