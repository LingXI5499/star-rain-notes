package com.starrainnotes.english.shared.exercise.api;

import java.util.Collection;

/** Deletes shared exercise rows without exposing their table to other modules. */
public interface EnglishExerciseRemovalPort {
    void deleteAll(Collection<Long> ids);
}
