package com.starrainnotes.english.writing.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

public final class WritingResourceDto {
    private WritingResourceDto() { }

    @Data
    public static class Resource {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String slug;
        private String resourceKind;
        private String title;
        private String summary;
        private String bodyMarkdown;
        private String cefrLevel;
        private String publishStatus;
        private int sortOrder;
    }

    public record Page(List<Resource> items, long total, int page, int size) { }
    public record Request(String resourceKind, String title, String summary,
                          String bodyMarkdown, String cefrLevel, Integer sortOrder) { }
}
