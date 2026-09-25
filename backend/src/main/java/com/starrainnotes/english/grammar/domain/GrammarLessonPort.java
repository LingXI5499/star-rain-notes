package com.starrainnotes.english.grammar.domain;

import java.util.Optional;

/** Lesson existence and display facts needed outside the grammar module. */
public interface GrammarLessonPort {
    Optional<GrammarLessonRef> findRef(long lessonId);
}
