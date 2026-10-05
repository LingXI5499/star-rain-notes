package com.starrainnotes.english.writing.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

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

    public record Page(List<Prompt> items, long total, int page, int size) { }
    public record Request(String title, String summary, String bodyMarkdown, String requirementsMarkdown,
                          String cefrLevel, Integer wordMin, Integer wordMax,
                          Integer estimatedMinutes, Integer sortOrder) { }
}
