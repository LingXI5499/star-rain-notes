package com.starrainnotes.english.shared.content;

/** Normalized filters for a content domain's bundle catalog query. */
public record ContentCatalogFilter(String status, String cefr, String term) { }
