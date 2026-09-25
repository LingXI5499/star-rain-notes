package com.starrainnotes.english.reading.domain;

/** Minimal reading article facts that other English modules may read. */
public record ReadingArticleRef(long id, String title, String slug, boolean published) {
}
