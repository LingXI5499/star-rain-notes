package com.starrainnotes.english.shared.taxonomy.api;

/** How many rows in one content module still point at a taxonomy term. */
public interface TaxonomyTermReferencePort {
    long countReferences(long termId);
}
