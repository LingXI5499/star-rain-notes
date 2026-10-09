package com.starrainnotes.tutorial.content.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialChapterDocument {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private String searchableText;
}
