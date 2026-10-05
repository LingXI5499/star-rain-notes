package com.starrainnotes.english.listening.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

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

    public record Request(Integer startMs, Integer endMs, String transcriptText,
                          String translationText, Integer sortOrder) { }
}
