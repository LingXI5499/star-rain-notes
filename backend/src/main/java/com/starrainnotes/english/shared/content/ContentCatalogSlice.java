package com.starrainnotes.english.shared.content;

import java.util.List;

/** Matching row count and the first requested candidates in catalog order. */
public record ContentCatalogSlice(long total, List<ContentDescriptor> items) { }
