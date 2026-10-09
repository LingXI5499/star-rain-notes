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
}
