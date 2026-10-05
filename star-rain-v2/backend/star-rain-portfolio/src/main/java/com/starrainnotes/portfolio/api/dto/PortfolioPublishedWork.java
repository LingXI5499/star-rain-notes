package com.starrainnotes.portfolio.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPublishedWork {
    private Long id;
    private String slug;
    private String workType;
    private String title;
    private String summary;
    private String coverUrl;
    private String bodyMarkdown;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
