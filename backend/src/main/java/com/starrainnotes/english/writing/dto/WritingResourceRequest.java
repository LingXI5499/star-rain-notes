package com.starrainnotes.english.writing.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record WritingResourceRequest(@NotBlank @Size(max=32) String resourceKind, String expressionLevel,
 @NotBlank @Size(max=200) String title, @NotBlank @Size(max=150) String slug, @NotBlank @Size(max=1000) String summary,
 @NotBlank String bodyMarkdown, Long coverMediaId, @NotBlank @Size(max=2) String cefrLevel,
 Integer wordMin, Integer wordMax, Integer estimatedMinutes, String templateSchemaJson, Integer sortOrder, List<Long> tagIds) {}
