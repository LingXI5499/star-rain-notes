package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.dto.*;
import com.starrainnotes.english.vocabulary.learning.*;
import java.util.List;
import java.util.Map;

/** Account-facing English learning use cases; controllers depend only on this boundary. */
public interface EnglishLearningFacade {
    List<Map<String,Object>> records(long accountId,int page,int pageSize);
    Map<String,LearningRecordView> batchRecords(long accountId,List<String> refs);
    LearningSummaryView summary(long accountId);
    LearningInsightsView insights(long accountId);
    Map<String,Object> record(long accountId,String type,long contentId);
    void saveRecord(long accountId,String type,long contentId,String status,Integer seconds);
    List<Map<String,Object>> vocabularyMemory(long accountId);
    void putVocabularyMemory(long accountId,long wordId,int memoryCount);
    VocabularyStudySettingsView vocabularySettings(long accountId);
    VocabularyStudySettingsView updateVocabularySettings(long accountId,VocabularyStudySettingsRequest request);
    List<VocabularyMemoryView> vocabularyStates(long accountId,List<Long> wordIds);
    VocabularyQueueView vocabularyReviewQueue(long accountId,Long themeId);
    VocabularyMemoryView startVocabularyWord(long accountId,long wordId);
    VocabularyReviewResultView reviewVocabularyWord(long accountId,long wordId,VocabularyReviewRequest request);
    void resetVocabularyWord(long accountId,long wordId);
    VocabularyMemoryView setVocabularyDisplay(long accountId,long wordId,VocabularyDisplayRequest request);
    void clearVocabularyDisplay(long accountId,long wordId);
    VocabularyProgressView vocabularyStatistics(long accountId);
    void importLocalVocabulary(long accountId,Map<String,Object> payload);
    Map<String,Object> writingSubmission(long accountId,long promptId);
    void saveWritingSubmission(long accountId,long promptId,WritingSubmissionRequest request);
    void importLocalProgress(long accountId,Map<String,Object> payload);
    long claimLegacyProgress(long accountId,String learnerKeyHash);
}
