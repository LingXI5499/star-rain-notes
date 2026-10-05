package com.starrainnotes.english.listening.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ListeningSegmentDto {
    private ListeningSegmentDto() { }

    @Data
    public static class Segment {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long listeningItemId;
        private int startMs;
        private int endMs;
        private String transcriptText;
        private String translationText;
        private int sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Integer startMs;
        private Integer endMs;
        private String transcriptText;
        private String translationText;
        private Integer sortOrder;
    }
}
