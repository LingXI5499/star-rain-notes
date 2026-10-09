package com.starrainnotes.tutorial.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterQuestionDTO {
    @NotBlank
    private String questionText;
    @NotBlank
    private String referenceAnswer;
    private String status;
    private java.util.List<Long> knowledgeCardIds = new java.util.ArrayList<>();
}
