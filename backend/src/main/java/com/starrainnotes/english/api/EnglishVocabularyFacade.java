package com.starrainnotes.english.api;

import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.learning.VocabularyMemoryView;
import com.starrainnotes.english.vocabulary.learning.VocabularyProgressView;
import com.starrainnotes.english.vocabulary.learning.VocabularyQueueView;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewResultView;
import com.starrainnotes.english.vocabulary.learning.VocabularyDisplayRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsView;

import java.util.List;
import java.util.Map;

public interface EnglishVocabularyFacade {
    VocabularyWordView getWord(long wordId);
    List<VocabularyWordView> getWords(List<Long> ids);
    VocabularyQueueView queue(long accountId, Long themeId);
    VocabularyProgressView progress(long accountId);
    VocabularyReviewResultView completeReview(long accountId, long wordId, VocabularyReviewRequest request);
    List<VocabularyMemoryView> memories(long accountId, List<Long> wordIds);
    List<Map<String, Object>> memorySnapshot(long accountId);
    void putMemoryCount(long accountId, long wordId, int memoryCount);
    VocabularyStudySettingsView settings(long accountId);
    VocabularyStudySettingsView updateSettings(long accountId, VocabularyStudySettingsRequest request);
    VocabularyMemoryView start(long accountId, long wordId);
    void reset(long accountId, long wordId);
    VocabularyMemoryView setDisplay(long accountId, long wordId, VocabularyDisplayRequest request);
    void clearDisplay(long accountId, long wordId);
    void importLocal(long accountId, Map<String, Object> payload);
}
