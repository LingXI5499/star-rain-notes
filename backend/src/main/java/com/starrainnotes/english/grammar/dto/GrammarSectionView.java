package com.starrainnotes.english.grammar.dto;

import java.util.List;

public record GrammarSectionView(Long id, String title, Integer sortOrder, long lessonCount,
                                 long publishedCount, List<GrammarLessonSummaryView> lessons) {
}
