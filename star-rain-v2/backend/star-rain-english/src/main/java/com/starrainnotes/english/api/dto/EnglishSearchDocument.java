package com.starrainnotes.english.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EnglishSearchDocument {
    private String contentType;
    private Long id;
    private String title;
    private String summary;
    private String searchableText;
    private String routePath;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private Long themeId;
}
