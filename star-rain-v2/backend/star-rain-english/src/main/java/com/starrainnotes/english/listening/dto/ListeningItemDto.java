package com.starrainnotes.english.listening.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ListeningItemDto {
    private ListeningItemDto() { }

    @Data
    public static class Item {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String cefrLevel;
        private Integer difficultyLevel;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long audioMediaId;
        private Integer durationSeconds;
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
        private List<Item> items;
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
        private String audioMediaId;
        private Integer durationSeconds;
        private String sourceName;
        private String sourceUrl;
        private Integer sortOrder;
    }
}
