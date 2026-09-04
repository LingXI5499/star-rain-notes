package com.starrainnotes.vocabulary.provider;

import java.util.Optional;

/** Replaceable licensed pronunciation lookup. Results are never persisted implicitly. */
public interface PronunciationProvider {
    Optional<String> pronunciationUrl(String word, String accent);
}
