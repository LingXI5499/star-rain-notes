package com.starrainnotes.english.writing.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class WritingPromptDto {
    private WritingPromptDto() { }

    @Data
    public static class Prompt {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String requirementsMarkdown;
        private String cefrLevel;
        private Integer wordMin;
        private Integer wordMax;
        private Integer estimatedMinutes;
        private String publishStatus;
        private int sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page {
        private List<Prompt> items;
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
        private String requirementsMarkdown;
        private String cefrLevel;
        private Integer wordMin;
        private Integer wordMax;
        private Integer estimatedMinutes;
        private Integer sortOrder;
    }
}
