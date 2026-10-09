package com.starrainnotes.tutorial.content.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterUpdateDTO {
    private Long rowVersion;
    @Size(max = 200)
    private String title;
    @Size(max = 1000)
    private String summary;
    @Size(max = 2000000)
    private String bodyMarkdown;
}
