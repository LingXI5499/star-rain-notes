package com.starrainnotes.search.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SearchableDocument {
    private String contentType;
    private Long contentId;
    private String documentKey;
    private String title;
    private String summary;
    private String searchableText;
    private String routePath;
    private LocalDateTime publishedAt;
    private LocalDateTime sourceUpdatedAt;
}
