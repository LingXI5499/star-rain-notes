package com.starrainnotes.english.grammar.domain;

/** Minimal lesson facts that other English modules may read. */
public record GrammarLessonRef(long id, String title, String slug, int sortOrder) {
}
