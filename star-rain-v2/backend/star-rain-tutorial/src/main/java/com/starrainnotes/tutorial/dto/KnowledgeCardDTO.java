package com.starrainnotes.tutorial.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeCardDTO {
    @NotBlank
    private String frontText;
    @NotBlank
    private String backMarkdown;
    private String status;
}
