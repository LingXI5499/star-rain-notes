package com.starrainnotes.english.shared.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record WritingSubmissionRequest(
        @NotBlank String bodyText,
        @NotBlank String status,
        @Min(0) @Max(100) BigDecimal selfScore) {
}
