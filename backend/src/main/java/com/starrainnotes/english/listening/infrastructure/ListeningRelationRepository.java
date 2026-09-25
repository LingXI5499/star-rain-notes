package com.starrainnotes.english.listening.infrastructure;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.dto.ReadingPairRef;
import com.starrainnotes.english.listening.dto.ReadingPairRequest;
import com.starrainnotes.english.reading.domain.ReadingArticleRef;
import com.starrainnotes.english.reading.domain.ReadingContentPort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Persists cross-module Reading ↔ Listening links. */
@Repository
public class ListeningRelationRepository {
    private final JdbcTemplate jdbc;
    private final ReadingContentPort reading;
    public ListeningRelationRepository(JdbcTemplate jdbc, ReadingContentPort reading) {
        this.jdbc = jdbc;
        this.reading = reading;
    }

    @Transactional
    public void addReadingPair(Long itemId, ReadingPairRequest request) {
        Integer itemCount = jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_item WHERE id=?", Integer.class, itemId);
        if (itemCount == null || itemCount == 0) throw new ApiException(HttpStatus.NOT_FOUND,
                "ENGLISH_LISTENING_ITEM_NOT_FOUND", "Item not found", "The listening material does not exist.");
        requireReading(request.readingArticleId());
        try {
            jdbc.update("INSERT INTO english_reading_listening_pair(reading_article_id,listening_item_id,relation_type,sort_order) VALUES (?,?,?,?)",
                    request.readingArticleId(), itemId, request.relationType(), 10);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_READING_LISTENING_PAIR_DUPLICATE",
                    "Pair already exists", "This reading–listening pair is already linked.");
        }
    }

    @Transactional
    public void removeReadingPair(Long itemId, Long readingId) {
        jdbc.update("DELETE FROM english_reading_listening_pair WHERE listening_item_id=? AND reading_article_id=?",
                itemId, readingId);
    }

    public List<ReadingPairRef> readingPairs(Long itemId) { return query(itemId, false); }
    public List<ReadingPairRef> publishedReadingPairs(Long itemId) { return query(itemId, true); }

    public List<Long> listeningIdsForReading(long readingId) {
        return jdbc.queryForList("""
                SELECT listening_item_id FROM english_reading_listening_pair
                WHERE reading_article_id=? ORDER BY sort_order,listening_item_id
                """, Long.class, readingId);
    }

    public List<Long> readingIdsForListening(long listeningId) {
        return jdbc.queryForList("""
                SELECT reading_article_id FROM english_reading_listening_pair
                WHERE listening_item_id=? ORDER BY sort_order,reading_article_id
                """, Long.class, listeningId);
    }

    private List<ReadingPairRef> query(Long itemId, boolean publishedOnly) {
        List<PairRow> rows = jdbc.query("""
                SELECT reading_article_id, relation_type
                FROM english_reading_listening_pair
                WHERE listening_item_id=?
                ORDER BY sort_order, reading_article_id
                """, (rs, row) -> new PairRow(rs.getLong("reading_article_id"), rs.getString("relation_type")), itemId);
        List<ReadingPairRef> result = new ArrayList<>();
        for (PairRow row : rows) {
            ReadingArticleRef article = reading.findRef(row.articleId()).orElse(null);
            if (article == null || (publishedOnly && !article.published())) {
                continue;
            }
            result.add(new ReadingPairRef(article.id(), article.title(), article.slug(), row.relationType()));
        }
        return result;
    }

    private void requireReading(Long readingId) {
        if (readingId == null || reading.findRef(readingId).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_PAIR_READING_INVALID",
                    "Invalid reading article", "The reading article does not exist.");
        }
    }

    private record PairRow(long articleId, String relationType) {
    }
}
