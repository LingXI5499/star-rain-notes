package com.starrainnotes.english.reading.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ReadingDto {
    private ReadingDto() { }

    @Data
    public static class Article {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String cefrLevel;
        private Integer difficultyLevel;
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
    public static class Request {
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String cefrLevel;
        private Integer difficultyLevel;
        private String sourceName;
        private String sourceUrl;
        private Integer sortOrder;
    }
}
