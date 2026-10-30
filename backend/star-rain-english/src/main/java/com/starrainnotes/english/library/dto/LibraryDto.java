package com.starrainnotes.english.library.dto;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import java.util.List;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Vocabulary classification is separate from the immutable identity used by learning records. */
public final class LibraryDto {
    private LibraryDto() { }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Word extends VocabularyDto.Word {
        private String entryId;
        private int legacyIndex;
        private String migrationStatus;
        private String migrationReason;
        private long rowVersion;
        private List<String> topicCodes = List.of();
    }

    @Data
    public static class Classification {
        private String migrationStatus;
        private String migrationReason;
        private Long rowVersion;
        private List<String> topicCodes;
        private VocabularyDto.WordRequest word;
    }

    @Data
    public static class NewWord {
        private String category;
        private String topic;
        private VocabularyDto.WordRequest word;
    }

    public record Count(String code, long total) { }
    @Data
    public static class Essay {
        private String id;
        private String title;
        private String slug;
        private String summary;
        private String genre;
        private LocalDateTime publishedAt;
        @JsonIgnore private String keywordsJson;
        private List<String> keywords = List.of();
    }
    public record Overview(long vocabularyRecords, long confirmedRecords, long reviewRecords,
                           long unclassifiedRecords, long grammarLessons, long worldArticles,
                           long essays) { }
}
