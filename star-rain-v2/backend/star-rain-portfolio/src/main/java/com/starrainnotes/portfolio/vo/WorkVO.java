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
    private String prototypeAssetId;
    private String prototypeEntry;
    private String prototypeUrl;
    private String subtitle;
    private Long categoryId;
    private Long formatId;
    private String role;
    private String techStack;
    private String projectStatus;
    private java.time.LocalDate startedOn;
    private java.time.LocalDate endedOn;
    private Boolean featured;
    private Integer sortOrder;
    private String seoTitle;
    private String seoDescription;
    private WorkTaxonomyVO category;
    private WorkTaxonomyVO format;
    private java.util.List<WorkTaxonomyVO> tags;
    private java.util.List<WorkSectionVO> sections;
    private String searchableText;
    private Boolean hasSections;
    private WorkNavigationVO previousWork;
    private WorkNavigationVO nextWork;

    private String bodyMarkdown;
    private String status;
    private JsonNode typeDetail;
    private List<WorkMediaVO> media;
    private List<WorkLinkVO> links;
    private String coverUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
