package com.starrainnotes.tutorial.content.api;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialPublishedDocument {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private LocalDateTime publishedAt;
    private List<TutorialChapterDocument> chapters;
    private LocalDateTime updatedAt;

    public TutorialPublishedDocument(Long id, String slug, String title, String summary, LocalDateTime publishedAt, List<TutorialChapterDocument> chapters) {
        this(id, slug, title, summary, publishedAt, chapters, null);
    }
}
