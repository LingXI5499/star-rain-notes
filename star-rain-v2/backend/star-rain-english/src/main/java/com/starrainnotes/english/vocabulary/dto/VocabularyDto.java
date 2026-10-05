package com.starrainnotes.english.vocabulary.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

public final class VocabularyDto {
    private VocabularyDto() { }

    @Data
    public static class Theme {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String layer;
        private int layerOrder;
        private String name;
        private int sortOrder;
        private int wordCount;
    }

    @Data
    public static class Word {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long themeId;
        private String word;
        private String partOfSpeech;
        private String phoneticUs;
        private String phoneticUk;
        private String translation;
        private String examples;
        private int sortOrder;
    }

    public record Page<T>(List<T> items, long total, int page, int size) { }
    public record ThemeRequest(String layer, Integer layerOrder, String name, Integer sortOrder) { }
    public record WordRequest(String themeId, String word, String partOfSpeech,
                              String phoneticUs, String phoneticUk, String translation,
                              String examples, Integer sortOrder) { }
}
