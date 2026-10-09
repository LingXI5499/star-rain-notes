package com.starrainnotes.tutorial.content.api;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublishedTutorial {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String coverUrl;
    private LocalDateTime publishedAt;
}
