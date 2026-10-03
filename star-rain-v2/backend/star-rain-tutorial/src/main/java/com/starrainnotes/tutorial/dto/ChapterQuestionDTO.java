package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterQuestionDTO {
    @NotBlank
    private String questionText;
    @NotBlank
    private String referenceAnswer;
    private String status;
}
