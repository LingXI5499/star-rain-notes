package com.starrainnotes.search.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SearchHitVO {
    private String contentType;
    private String contentId;
    private String title;
    private String summary;
    private String routePath;
    private LocalDateTime publishedAt;
    private Double score;
}
