package com.starrainnotes.english.reading.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

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

    public record Page(List<Article> items, long total, int page, int size) { }
    public record Request(String title, String summary, String bodyMarkdown, String cefrLevel,
                          Integer difficultyLevel, String sourceName, String sourceUrl,
                          Integer sortOrder) { }
}
