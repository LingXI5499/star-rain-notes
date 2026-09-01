package com.starrainnotes.english.grammar.dto;

public record GrammarLessonDetailView(Long id, Long sectionId, String sectionTitle, String title,
                                      String slug, String summary, String bodyMarkdown,
                                      String publishStatus, Integer sortOrder, String publishedAt,
                                      String updatedAt, GrammarLessonLinkView previous,
                                      GrammarLessonLinkView next) {
}
