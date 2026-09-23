package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.dto.LearningInsightsView;
import com.starrainnotes.english.learning.dto.LearningRecordView;
import com.starrainnotes.english.learning.dto.LearningSummaryView;
import com.starrainnotes.english.learning.dto.WritingSubmissionRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyDisplayRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyMemoryView;
import com.starrainnotes.english.vocabulary.learning.VocabularyProgressView;
import com.starrainnotes.english.vocabulary.learning.VocabularyQueueView;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewResultView;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyProgressImportService;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyStudyQueryService;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyStudyCommandService;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DefaultEnglishLearningFacade implements EnglishLearningFacade {
    private final LearningRecordQueryService recordQueries;
    private final LearningRecordCommandService recordCommands;
    private final LearningInsightQueryService insights;
    private final VocabularyStudyQueryService vocabularyStudyQueries;
    private final VocabularyStudyCommandService vocabularyStudyCommands;
    private final VocabularyProgressImportService vocabularyImport;
    private final WritingSubmissionService submissions;
    private final LearningProgressMigrationService migration;

    public DefaultEnglishLearningFacade(LearningRecordQueryService recordQueries,
                                        LearningRecordCommandService recordCommands,
                                        LearningInsightQueryService insights,
                                        VocabularyStudyQueryService vocabularyStudyQueries,
                                        VocabularyStudyCommandService vocabularyStudyCommands,
                                        VocabularyProgressImportService vocabularyImport,
                                        WritingSubmissionService submissions,
                                        LearningProgressMigrationService migration) {
        this.recordQueries = recordQueries;
        this.recordCommands = recordCommands;
        this.insights = insights;
        this.vocabularyStudyQueries = vocabularyStudyQueries;
        this.vocabularyStudyCommands = vocabularyStudyCommands;
        this.vocabularyImport = vocabularyImport;
        this.submissions = submissions;
        this.migration = migration;
    }

    @Override public List<Map<String, Object>> records(long accountId, int page, int pageSize) {
        return recordQueries.list(accountId, page, pageSize);
    }
    @Override public Map<String, LearningRecordView> batchRecords(long accountId, List<String> refs) {
        return recordQueries.batch(accountId, refs);
    }
    @Override public LearningSummaryView summary(long accountId) { return insights.summary(accountId); }
    @Override public LearningInsightsView insights(long accountId) { return insights.insights(accountId); }
    @Override public Map<String, Object> record(long accountId, String type, long contentId) {
        return recordQueries.get(accountId, type, contentId);
    }
    @Override public void saveRecord(long accountId, String type, long contentId, String status, Integer seconds) {
        recordCommands.saveSimple(accountId, type, contentId, status, seconds);
    }
    @Override public List<Map<String, Object>> vocabularyMemory(long accountId) { return vocabularyStudyQueries.memorySnapshot(accountId); }
    @Override public void putVocabularyMemory(long accountId, long wordId, int memoryCount) {
        vocabularyStudyCommands.putMemoryCount(accountId, wordId, memoryCount);
    }
    @Override public VocabularyStudySettingsView vocabularySettings(long accountId) { return vocabularyStudyQueries.settings(accountId); }
    @Override public VocabularyStudySettingsView updateVocabularySettings(long accountId, VocabularyStudySettingsRequest request) {
        return vocabularyStudyCommands.updateSettings(accountId, request);
    }
    @Override public List<VocabularyMemoryView> vocabularyStates(long accountId, List<Long> wordIds) {
        return vocabularyStudyQueries.memories(accountId, wordIds);
    }
    @Override public VocabularyQueueView vocabularyReviewQueue(long accountId, Long themeId) {
        return vocabularyStudyQueries.queue(accountId, themeId);
    }
    @Override public VocabularyMemoryView startVocabularyWord(long accountId, long wordId) {
        return vocabularyStudyCommands.start(accountId, wordId);
    }
    @Override public VocabularyReviewResultView reviewVocabularyWord(long accountId, long wordId, VocabularyReviewRequest request) {
        return vocabularyStudyCommands.completeReview(accountId, wordId, request);
    }
    @Override public void resetVocabularyWord(long accountId, long wordId) { vocabularyStudyCommands.reset(accountId, wordId); }
    @Override public VocabularyMemoryView setVocabularyDisplay(long accountId, long wordId, VocabularyDisplayRequest request) {
        return vocabularyStudyCommands.setDisplay(accountId, wordId, request);
    }
    @Override public void clearVocabularyDisplay(long accountId, long wordId) { vocabularyStudyCommands.clearDisplay(accountId, wordId); }
    @Override public VocabularyProgressView vocabularyStatistics(long accountId) { return vocabularyStudyQueries.progress(accountId); }
    @Override public void importLocalVocabulary(long accountId, Map<String, Object> payload) {
        vocabularyImport.importLocal(accountId, payload);
    }
    @Override public Map<String, Object> writingSubmission(long accountId, long promptId) {
        return submissions.get(accountId, promptId);
    }
    @Override public void saveWritingSubmission(long accountId, long promptId, WritingSubmissionRequest request) {
        submissions.save(accountId, promptId, request);
    }
    @Override public void importLocalProgress(long accountId, Map<String, Object> payload) {
        migration.importLocal(accountId, payload);
    }
    @Override public long claimLegacyProgress(long accountId, String learnerKeyHash) {
        return migration.claimLegacy(accountId, learnerKeyHash);
    }
}
