package com.starrainnotes.english.grammar.dto;

import java.util.List;

public record GrammarCurriculumView(GrammarCourseView course, List<GrammarSectionView> sections) {
}
