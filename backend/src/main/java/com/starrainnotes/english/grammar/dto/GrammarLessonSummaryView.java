package com.starrainnotes.english.grammar.dto;

public record GrammarLessonSummaryView(Long id, Long sectionId, String title, String slug,
                                       String summary, String publishStatus, Integer sortOrder,
                                       String updatedAt) {
}
