package com.starrainnotes.english.api;

import com.starrainnotes.english.learning.dto.*;
import java.util.List;
import java.util.Map;

/** Account-facing English learning use cases; controllers depend only on this boundary. */
public interface EnglishLearningFacade {
    List<Map<String,Object>> records(long accountId,int page,int pageSize);
    Map<String,LearningRecordView> batchRecords(long accountId,List<String> refs);
    LearningSummaryView summary(long accountId);
    LearningInsightsView insights(long accountId);
    Map<String,Object> record(long accountId,String type,long contentId);
    LearningRecordView getRecord(long accountId, String type, long contentId);
    void saveRecord(long accountId,String type,long contentId,String status,Integer seconds);
    Map<String,Object> writingSubmission(long accountId,long promptId);
    WritingSubmissionView getWritingSubmission(long accountId, long promptId);
    WritingSubmissionView saveWritingSubmission(long accountId,long promptId,WritingSubmissionRequest request);
    void importLocalProgress(long accountId,Map<String,Object> payload);
    long claimLegacyProgress(long accountId,String learnerKeyHash);
}
