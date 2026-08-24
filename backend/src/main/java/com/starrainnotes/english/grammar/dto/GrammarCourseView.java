package com.starrainnotes.english.grammar.dto;

public record GrammarCourseView(Long id, String title, String subtitle, String summary,
                                String introduction, String roadmapMarkdown, Long coverMediaId,
                                String coverUrl, String seoTitle, String seoDescription,
                                String publishStatus, String publishedAt, String updatedAt) {
}
