package com.starrainnotes.english.reading.domain;

/** Content existence and visibility needed by reading exercise use cases. */
public interface ReadingContentPort {
    void requireExists(Long articleId);
    void requirePublished(Long articleId);
}
