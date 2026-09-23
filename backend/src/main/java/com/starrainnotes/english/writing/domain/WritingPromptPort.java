package com.starrainnotes.english.writing.domain;

/** Prompt capabilities used by the Writing exercise aggregate. */
public interface WritingPromptPort {
    void requireExists(Long id);
    boolean isPublished(Long id);
}
