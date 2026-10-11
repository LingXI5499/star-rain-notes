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
    private java.time.LocalDateTime updatedAt;

    public TutorialChapterDocument(Long id, String slug, String title, String summary, String bodyMarkdown, String searchableText) {
        this(id, slug, title, summary, bodyMarkdown, searchableText, null);
    }
}
