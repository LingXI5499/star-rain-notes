package com.starrainnotes.vocabulary.service;

import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.vocabulary.dto.VocabularyFamilyView;
import com.starrainnotes.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.vocabulary.entity.VocabularyWord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds the shared public/admin vocabulary response from an entity and its relations. */
@Component
public class VocabularyWordViewAssembler {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public VocabularyWordViewAssembler(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public VocabularyWordView toView(VocabularyWord word) {
        return toViews(List.of(word)).getFirst();
    }

    public List<VocabularyWordView> toViews(List<VocabularyWord> words) {
        if (words == null || words.isEmpty()) return List.of();
        List<Long> ids = words.stream().map(VocabularyWord::getId).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, List<VocabularyAudioView>> audioByWord = new HashMap<>();
        jdbc.query("""
                SELECT a.id,a.word_id,a.accent,a.media_asset_id,m.public_url,a.provider,a.source_url,
                       a.license_note,a.is_primary
                FROM vocabulary_word_audio a JOIN media_asset m ON m.id=a.media_asset_id
                WHERE a.word_id IN (%s)
                ORDER BY a.word_id,a.is_primary DESC,a.accent,a.id
                """.formatted(placeholders), rs -> {
            audioByWord.computeIfAbsent(rs.getLong("word_id"), ignored -> new ArrayList<>()).add(new VocabularyAudioView(
                    rs.getLong("id"), rs.getString("accent"), rs.getLong("media_asset_id"),
                    rs.getString("public_url"), rs.getString("provider"), rs.getString("source_url"),
                    rs.getString("license_note"), rs.getBoolean("is_primary")));
        }, ids.toArray());

        Map<Long, List<VocabularyFamilyView>> familyByWord = new HashMap<>();
        jdbc.query("""
                SELECT l.word_id,f.id,f.head_word,f.slug
                FROM vocabulary_word_family_link l JOIN vocabulary_word_family f ON f.id=l.family_id
                WHERE l.word_id IN (%s) ORDER BY l.word_id,f.id
                """.formatted(placeholders), rs -> {
            familyByWord.computeIfAbsent(rs.getLong("word_id"), ignored -> new ArrayList<>()).add(
                    new VocabularyFamilyView(rs.getLong("id"), rs.getString("head_word"), rs.getString("slug")));
        }, ids.toArray());

        return words.stream().map(word -> new VocabularyWordView(
                word.getId(),
                word.getThemeId(),
                word.getPartOfSpeech(),
                word.getWord(),
                word.getPhoneticUs(),
                word.getPhoneticUk(),
                word.getTranslation(),
                word.getSceneMeaning(),
                word.getInflections(),
                word.getExamples() == null ? List.of() : word.getExamples(),
                word.getMemoryCount() == null ? 0 : word.getMemoryCount(),
                word.getLastMemoryAt() == null ? null : timezone.atSite(word.getLastMemoryAt()).format(ISO_OFFSET),
                audioByWord.getOrDefault(word.getId(), List.of()),
                familyByWord.getOrDefault(word.getId(), List.of()))).toList();
    }
}
