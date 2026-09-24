package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.english.vocabulary.dto.VocabularyFamilyView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.entity.VocabularyWord;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyWordRelationRepository;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Builds the shared public/admin vocabulary response from an entity and its relations. */
@Component
public class VocabularyWordViewAssembler {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final VocabularyWordRelationRepository relations;
    private final SiteSettingsTimezone timezone;

    public VocabularyWordViewAssembler(VocabularyWordRelationRepository relations, SiteSettingsTimezone timezone) {
        this.relations = relations;
        this.timezone = timezone;
    }

    public VocabularyWordView toView(VocabularyWord word) {
        return toViews(List.of(word)).getFirst();
    }

    public List<VocabularyWordView> toViews(List<VocabularyWord> words) {
        if (words == null || words.isEmpty()) return List.of();
        List<Long> ids = words.stream().map(VocabularyWord::getId).toList();
        Map<Long, List<VocabularyAudioView>> audioByWord = relations.audiosForWords(ids);
        Map<Long, List<VocabularyFamilyView>> familyByWord = relations.familiesForWords(ids);

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
