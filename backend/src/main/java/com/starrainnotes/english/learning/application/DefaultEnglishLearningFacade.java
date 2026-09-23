package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.dto.LearningInsightsView;
import com.starrainnotes.english.learning.dto.LearningRecordView;
import com.starrainnotes.english.learning.dto.LearningSummaryView;
import com.starrainnotes.english.learning.dto.WritingSubmissionRequest;
import com.starrainnotes.english.learning.vocabulary.VocabularyDisplayRequest;
import com.starrainnotes.english.learning.vocabulary.VocabularyMemoryView;
import com.starrainnotes.english.learning.vocabulary.VocabularyProgressView;
import com.starrainnotes.english.learning.vocabulary.VocabularyQueueView;
import com.starrainnotes.english.learning.vocabulary.VocabularyReviewRequest;
import com.starrainnotes.english.learning.vocabulary.VocabularyReviewResultView;
import com.starrainnotes.english.learning.vocabulary.VocabularyStudyService;
import com.starrainnotes.english.learning.vocabulary.VocabularyStudySettingsRequest;
import com.starrainnotes.english.learning.vocabulary.VocabularyStudySettingsView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DefaultEnglishLearningFacade implements EnglishLearningFacade {
    private final LearningRecordQueryService recordQueries;
    private final LearningRecordCommandService recordCommands;
    private final LearningInsightQueryService insights;
    private final VocabularyStudyQueryService vocabularyQueries;
    private final VocabularyStudyCommandService vocabularyCommands;
    private final VocabularyStudyService vocabularyStudy;
    private final WritingSubmissionService submissions;
    private final LearningProgressMigrationService migration;

    public DefaultEnglishLearningFacade(LearningRecordQueryService recordQueries,
                                        LearningRecordCommandService recordCommands,
                                        LearningInsightQueryService insights,
                                        VocabularyStudyQueryService vocabularyQueries,
                                        VocabularyStudyCommandService vocabularyCommands,
                                        VocabularyStudyService vocabularyStudy,
                                        WritingSubmissionService submissions,
                                        LearningProgressMigrationService migration) {
        this.recordQueries = recordQueries;
        this.recordCommands = recordCommands;
        this.insights = insights;
        this.vocabularyQueries = vocabularyQueries;
        this.vocabularyCommands = vocabularyCommands;
        this.vocabularyStudy = vocabularyStudy;
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
    @Override public List<Map<String, Object>> vocabularyMemory(long accountId) { return vocabularyQueries.memory(accountId); }
    @Override public void putVocabularyMemory(long accountId, long wordId, int memoryCount) {
        vocabularyCommands.putMemory(accountId, wordId, memoryCount);
    }
    @Override public VocabularyStudySettingsView vocabularySettings(long accountId) { return vocabularyStudy.settings(accountId); }
    @Override public VocabularyStudySettingsView updateVocabularySettings(long accountId, VocabularyStudySettingsRequest request) {
        return vocabularyStudy.updateSettings(accountId, request);
    }
    @Override public List<VocabularyMemoryView> vocabularyStates(long accountId, List<Long> wordIds) {
        return vocabularyStudy.memories(accountId, wordIds);
    }
    @Override public VocabularyQueueView vocabularyReviewQueue(long accountId, Long themeId) {
        return vocabularyStudy.queue(accountId, themeId);
    }
    @Override public VocabularyMemoryView startVocabularyWord(long accountId, long wordId) {
        return vocabularyStudy.start(accountId, wordId);
    }
    @Override public VocabularyReviewResultView reviewVocabularyWord(long accountId, long wordId, VocabularyReviewRequest request) {
        return vocabularyStudy.completeReview(accountId, wordId, request);
    }
    @Override public void resetVocabularyWord(long accountId, long wordId) { vocabularyStudy.reset(accountId, wordId); }
    @Override public VocabularyMemoryView setVocabularyDisplay(long accountId, long wordId, VocabularyDisplayRequest request) {
        return vocabularyStudy.setDisplay(accountId, wordId, request);
    }
    @Override public void clearVocabularyDisplay(long accountId, long wordId) { vocabularyStudy.clearDisplay(accountId, wordId); }
    @Override public VocabularyProgressView vocabularyStatistics(long accountId) { return vocabularyStudy.progress(accountId); }
    @Override public void importLocalVocabulary(long accountId, Map<String, Object> payload) {
        vocabularyStudy.importLocal(accountId, payload);
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
