package com.starrainnotes.english.reading.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ReadingDto {
    private ReadingDto() { }
    @Data public static class Rights {
        private String rightsStatus;
        private String rightsBasis;
        private String originalAuthor;
        private String licenseNotice;
    }

    @Data
    public static class Article extends DocumentMetadata {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String cefrLevel;
        private Integer difficultyLevel;

        private String translationZhMarkdown;
        private String contentOrigin;
        private boolean levelAssessed;
        private Rights rights;
        private String sourceName;
        private String sourceUrl;
        private String publishStatus;
        private int sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page {
        private List<Article> items;
        private long total;
        private int page;
        private int size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public static class Request extends DocumentMetadata {
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String cefrLevel;
        private Integer difficultyLevel;

        private String translationZhMarkdown;
        private String contentOrigin;
        private boolean levelAssessed;
        private Rights rights;
        private String sourceName;
        private String sourceUrl;
        private Integer sortOrder;
    }
}
