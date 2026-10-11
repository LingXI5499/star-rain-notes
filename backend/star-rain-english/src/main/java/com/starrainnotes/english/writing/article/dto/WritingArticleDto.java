package com.starrainnotes.english.writing.article.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
public final class WritingArticleDto {
    private WritingArticleDto() { }
    @Data public static class Article extends DocumentMetadata {
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        @JsonIgnore private Long ownerAccountId; @JsonIgnore private boolean publicEligible;
        @JsonIgnore private String keywordsJson;
        private String slug; private String title; private String summary; private String bodyMarkdown;
        private String translationZhMarkdown; private String state; private String visibility;
        private LocalDateTime createdAt; private LocalDateTime updatedAt; private LocalDateTime publishedAt;
    }
    @Data @JsonIgnoreProperties(ignoreUnknown=true) public static class Request extends DocumentMetadata {
        private String title; private String summary; private String bodyMarkdown; private String translationZhMarkdown;
    }
    public record Page(List<Article> items,long total,int page,int size) { }
}
