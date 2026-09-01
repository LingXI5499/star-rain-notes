package com.starrainnotes.english.writing.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record WritingPromptRequest(@NotBlank @Size(max=200) String title, @Size(max=150) String slug,
 @NotBlank @Size(max=1000) String summary, @NotBlank String backgroundMarkdown, @NotBlank String requirementsMarkdown,
 @NotBlank @Size(max=2) String cefrLevel, @NotNull Integer wordMin, @NotNull Integer wordMax, @NotNull Integer estimatedMinutes,
 String rubricJson, String checklistJson, Long templateResourceId, Long modelResourceId, Long coverMediaId, Integer sortOrder, List<Long> tagIds) {}
