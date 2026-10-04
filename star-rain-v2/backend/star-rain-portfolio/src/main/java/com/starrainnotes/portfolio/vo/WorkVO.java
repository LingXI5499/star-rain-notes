package com.starrainnotes.portfolio.vo;

import com.fasterxml.jackson.databind.JsonNode;
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
public class WorkVO {
    private String id;
    private String slug;
    private String workType;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private String status;
    private JsonNode typeDetail;
    private List<WorkMediaVO> media;
    private List<WorkLinkVO> links;
    private String coverUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
