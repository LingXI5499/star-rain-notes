package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.api.EnglishLearningFacade;
import com.starrainnotes.english.learning.dto.LearningInsightsView;
import com.starrainnotes.english.learning.dto.LearningRecordView;
import com.starrainnotes.english.learning.dto.LearningSummaryView;
import com.starrainnotes.english.learning.dto.WritingSubmissionRequest;
import com.starrainnotes.english.learning.dto.WritingSubmissionView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DefaultEnglishLearningFacade implements EnglishLearningFacade {
    private final LearningRecordQueryService recordQueries;
    private final LearningRecordCommandService recordCommands;
    private final LearningInsightQueryService insights;
    private final WritingSubmissionService submissions;
    private final LearningProgressMigrationService migration;

    public DefaultEnglishLearningFacade(LearningRecordQueryService recordQueries,
                                        LearningRecordCommandService recordCommands,
                                        LearningInsightQueryService insights,
                                        WritingSubmissionService submissions,
                                        LearningProgressMigrationService migration) {
        this.recordQueries = recordQueries;
        this.recordCommands = recordCommands;
        this.insights = insights;
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
    @Override public LearningRecordView getRecord(long accountId, String type, long contentId) {
        return recordQueries.getView(accountId, type, contentId);
    }
    @Override public void saveRecord(long accountId, String type, long contentId,
                                     String status, Integer seconds) {
        recordCommands.saveSimple(accountId, type, contentId, status, seconds);
    }
    @Override public Map<String, Object> writingSubmission(long accountId, long promptId) {
        return submissions.get(accountId, promptId);
    }
    @Override public WritingSubmissionView getWritingSubmission(long accountId, long promptId) {
        return submissions.getView(accountId, promptId);
    }
    @Override public WritingSubmissionView saveWritingSubmission(long accountId, long promptId,
                                                                 WritingSubmissionRequest request) {
        return submissions.save(accountId, promptId, request);
    }
    @Override public void importLocalProgress(long accountId, Map<String, Object> payload) {
        migration.importLocal(accountId, payload);
    }
    @Override public long claimLegacyProgress(long accountId, String learnerKeyHash) {
        return migration.claimLegacy(accountId, learnerKeyHash);
    }
}
