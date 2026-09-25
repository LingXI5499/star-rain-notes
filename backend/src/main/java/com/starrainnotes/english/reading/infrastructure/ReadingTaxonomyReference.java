package com.starrainnotes.english.reading.infrastructure;

import com.starrainnotes.english.shared.taxonomy.api.TaxonomyTermReferencePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReadingTaxonomyReference implements TaxonomyTermReferencePort {
    private final JdbcTemplate jdbc;

    public ReadingTaxonomyReference(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long countReferences(long termId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM english_reading_article_tag WHERE term_id=?", Long.class, termId);
        return count == null ? 0L : count;
    }
}
