package com.starrainnotes.tutorial.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterBodyDTO {
    private Long rowVersion;
    @NotBlank
    private String bodyMarkdown;
}
