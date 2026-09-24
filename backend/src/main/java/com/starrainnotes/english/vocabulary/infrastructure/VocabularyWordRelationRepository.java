package com.starrainnotes.english.vocabulary.infrastructure;

import com.starrainnotes.english.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.english.vocabulary.dto.VocabularyFamilyView;
import com.starrainnotes.english.api.MediaPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class VocabularyWordRelationRepository {
    private final JdbcTemplate jdbc;
    private final MediaPort media;

    public VocabularyWordRelationRepository(JdbcTemplate jdbc, MediaPort media) {
        this.jdbc = jdbc;
        this.media = media;
    }

    public Map<Long, List<VocabularyAudioView>> audiosForWords(List<Long> wordIds) {
        if (wordIds.isEmpty()) return Map.of();
        String placeholders = placeholders(wordIds);
        List<AudioRow> rows = jdbc.query("""
                SELECT id,word_id,accent,media_asset_id,provider,source_url,license_note,is_primary
                FROM vocabulary_word_audio WHERE word_id IN (%s)
                ORDER BY word_id,is_primary DESC,accent,id
                """.formatted(placeholders), (rs, row) -> new AudioRow(
                rs.getLong("id"), rs.getLong("word_id"), rs.getString("accent"),
                rs.getLong("media_asset_id"), rs.getString("provider"),
                rs.getString("source_url"), rs.getString("license_note"),
                rs.getBoolean("is_primary")), wordIds.toArray());
        Map<Long, String> urls = media.publicUrls(rows.stream().map(AudioRow::mediaId).toList());
        Map<Long, List<VocabularyAudioView>> result = new HashMap<>();
        for (AudioRow row : rows) {
            result.computeIfAbsent(row.wordId(), ignored -> new ArrayList<>())
                    .add(new VocabularyAudioView(row.id(), row.accent(), row.mediaId(),
                            urls.get(row.mediaId()), row.provider(), row.sourceUrl(),
                            row.licenseNote(), row.primary()));
        }
        return result;
    }

    public Map<Long, List<VocabularyFamilyView>> familiesForWords(List<Long> wordIds) {
        if (wordIds.isEmpty()) return Map.of();
        String placeholders = placeholders(wordIds);
        Map<Long, List<VocabularyFamilyView>> result = new HashMap<>();
        jdbc.query("""
                SELECT l.word_id,f.id,f.head_word,f.slug
                FROM vocabulary_word_family_link l
                JOIN vocabulary_word_family f ON f.id=l.family_id
                WHERE l.word_id IN (%s) ORDER BY l.word_id,f.id
                """.formatted(placeholders), rs -> {
            result.computeIfAbsent(rs.getLong("word_id"), ignored -> new ArrayList<>())
                    .add(new VocabularyFamilyView(rs.getLong("id"),
                            rs.getString("head_word"), rs.getString("slug")));
        }, wordIds.toArray());
        return result;
    }

    private String placeholders(List<Long> ids) {
        return String.join(",", Collections.nCopies(ids.size(), "?"));
    }

    private record AudioRow(long id, long wordId, String accent, long mediaId, String provider,
                            String sourceUrl, String licenseNote, boolean primary) { }
}
