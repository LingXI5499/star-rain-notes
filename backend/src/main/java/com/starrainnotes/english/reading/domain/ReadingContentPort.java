package com.starrainnotes.english.reading.domain;

import java.util.Optional;

/** Content existence and visibility needed by reading exercise use cases. */
public interface ReadingContentPort {
    void requireExists(Long articleId);
    void requirePublished(Long articleId);

    /** Title and slug for another module. Empty when the article does not exist. */
    Optional<ReadingArticleRef> findRef(long articleId);
}
