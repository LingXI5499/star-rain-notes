package com.starrainnotes.english.knowledge.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
public final class KnowledgeDto {
    private KnowledgeDto() { }
    @Data public static class Item {
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        private String type; private String title; private String slug; private String summary;
        @JsonSerialize(using=ToStringSerializer.class) private Long primaryTopicId;
        private LocalDateTime publishedAt;
    }
    public record Page(List<Item> items,long total,int page,int size) { }
}
