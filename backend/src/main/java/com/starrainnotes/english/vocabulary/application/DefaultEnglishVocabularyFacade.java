package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.english.api.EnglishVocabularyFacade;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.learning.VocabularyMemoryView;
import com.starrainnotes.english.vocabulary.learning.VocabularyProgressView;
import com.starrainnotes.english.vocabulary.learning.VocabularyQueueView;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewResultView;
import com.starrainnotes.english.vocabulary.learning.VocabularyDisplayRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsView;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyProgressImportService;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyStudyCommandService;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyStudyQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DefaultEnglishVocabularyFacade implements EnglishVocabularyFacade {
    private final VocabularyQueryService vocabulary;
    private final VocabularyStudyQueryService studyQueries;
    private final VocabularyStudyCommandService studyCommands;
    private final VocabularyProgressImportService imports;

    public DefaultEnglishVocabularyFacade(VocabularyQueryService vocabulary,
                                          VocabularyStudyQueryService studyQueries,
                                          VocabularyStudyCommandService studyCommands,
                                          VocabularyProgressImportService imports) {
        this.vocabulary = vocabulary;
        this.studyQueries = studyQueries;
        this.studyCommands = studyCommands;
        this.imports = imports;
    }

    @Override public VocabularyWordView getWord(long wordId) { return vocabulary.getWord(wordId); }
    @Override public List<VocabularyWordView> getWords(List<Long> ids) { return vocabulary.getWords(ids); }
    @Override public VocabularyQueueView queue(long accountId, Long themeId) {
        return studyQueries.queue(accountId, themeId);
    }
    @Override public VocabularyProgressView progress(long accountId) {
        return studyQueries.progress(accountId);
    }
    @Override public VocabularyReviewResultView completeReview(
            long accountId, long wordId, VocabularyReviewRequest request) {
        return studyCommands.completeReview(accountId, wordId, request);
    }
    @Override public List<VocabularyMemoryView> memories(long accountId, List<Long> wordIds) {
        return studyQueries.memories(accountId, wordIds);
    }
    @Override public List<Map<String, Object>> memorySnapshot(long accountId) {
        return studyQueries.memorySnapshot(accountId);
    }
    @Override public void putMemoryCount(long accountId, long wordId, int memoryCount) {
        studyCommands.putMemoryCount(accountId, wordId, memoryCount);
    }
    @Override public VocabularyStudySettingsView settings(long accountId) {
        return studyQueries.settings(accountId);
    }
    @Override public VocabularyStudySettingsView updateSettings(
            long accountId, VocabularyStudySettingsRequest request) {
        return studyCommands.updateSettings(accountId, request);
    }
    @Override public VocabularyMemoryView start(long accountId, long wordId) {
        return studyCommands.start(accountId, wordId);
    }
    @Override public void reset(long accountId, long wordId) {
        studyCommands.reset(accountId, wordId);
    }
    @Override public VocabularyMemoryView setDisplay(
            long accountId, long wordId, VocabularyDisplayRequest request) {
        return studyCommands.setDisplay(accountId, wordId, request);
    }
    @Override public void clearDisplay(long accountId, long wordId) {
        studyCommands.clearDisplay(accountId, wordId);
    }
    @Override public void importLocal(long accountId, Map<String, Object> payload) {
        imports.importLocal(accountId, payload);
    }
}
