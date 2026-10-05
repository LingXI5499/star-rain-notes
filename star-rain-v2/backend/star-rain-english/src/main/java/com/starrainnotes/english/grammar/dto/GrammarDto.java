package com.starrainnotes.english.grammar.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class GrammarDto {
    private GrammarDto() { }

    @Data
    public static class Course {
        private String title;
        private String subtitle;
        private String summary;
        private String introduction;
        private String roadmapMarkdown;
        private String publishStatus;
    }

    @Data
    public static class Section {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String title;
        private int sortOrder;
        private List<Lesson> lessons;
    }

    @Data
    public static class Lesson {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sectionId;
        private String title;
        private String slug;
        private String summary;
        private String bodyMarkdown;
        private String publishStatus;
        private int sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Curriculum {
        private Course course;
        private List<Section> sections;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRequest {
        private String title;
        private String subtitle;
        private String summary;
        private String introduction;
        private String roadmapMarkdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionRequest {
        private String title;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonRequest {
        private String sectionId;
        private String title;
        private String summary;
        private String bodyMarkdown;
        private Integer sortOrder;
    }
}
