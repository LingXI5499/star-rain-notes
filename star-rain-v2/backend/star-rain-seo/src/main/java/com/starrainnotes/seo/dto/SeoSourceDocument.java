package com.starrainnotes.seo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoSourceDocument {
    private String routePath;
    private String contentType;
    private Long contentId;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private String coverUrl;
    private LocalDateTime updatedAt;
}
