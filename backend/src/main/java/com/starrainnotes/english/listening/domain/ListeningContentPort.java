package com.starrainnotes.english.listening.domain;

/** Visibility and existence checks required by listening child capabilities. */
public interface ListeningContentPort {
    void requireExists(Long id);
    void requirePublished(Long id);
}
