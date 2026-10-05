package com.starrainnotes.english.listening.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

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

    public record Page(List<Item> items, long total, int page, int size) { }
    public record Request(String title, String summary, String bodyMarkdown, String cefrLevel,
                          Integer difficultyLevel, String audioMediaId, Integer durationSeconds,
                          String sourceName, String sourceUrl, Integer sortOrder) { }
}
