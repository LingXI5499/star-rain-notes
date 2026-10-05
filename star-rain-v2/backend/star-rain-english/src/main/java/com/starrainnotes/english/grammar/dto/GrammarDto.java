package com.starrainnotes.english.grammar.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

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

    public record Curriculum(Course course, List<Section> sections) { }
    public record CourseRequest(String title, String subtitle, String summary,
                                String introduction, String roadmapMarkdown) { }
    public record SectionRequest(String title, Integer sortOrder) { }
    public record LessonRequest(String sectionId, String title, String summary,
                                String bodyMarkdown, Integer sortOrder) { }
}
