package com.starrainnotes.english.listening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ListeningItemRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150) String slug,
        @NotBlank @Size(max = 1000) String summary,
        String transcriptMarkdown,
        @NotBlank @Size(max = 2) String cefrLevel,
        @NotNull Integer listeningLevel,
        Long audioMediaId,
        Long coverMediaId,
        Integer durationSeconds,
        @Size(max = 200) String sourceName,
        @Size(max = 500) String sourceUrl,
        @Size(max = 500) String copyrightNote,
        Integer sortOrder,
        List<Long> topicTagIds,
        List<Long> sceneTagIds,
        List<Long> formatTagIds,
        List<Long> abilityTagIds,
        List<Long> functionTagIds) {
}
