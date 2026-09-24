package com.starrainnotes.account.english.controller;

import com.starrainnotes.account.service.AccountSessionService;
import com.starrainnotes.english.api.EnglishLearningFacade;
import com.starrainnotes.english.api.EnglishVocabularyFacade;
import com.starrainnotes.english.vocabulary.learning.VocabularyDisplayRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyMemoryView;
import com.starrainnotes.english.vocabulary.learning.VocabularyProgressView;
import com.starrainnotes.english.vocabulary.learning.VocabularyQueueView;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyReviewResultView;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.learning.VocabularyStudySettingsView;
import com.starrainnotes.english.learning.dto.WritingSubmissionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account/english")
public class AccountEnglishController {

    private final EnglishLearningFacade english;
    private final EnglishVocabularyFacade vocabulary;
    private final AccountSessionService sessions;

    public AccountEnglishController(EnglishLearningFacade english, EnglishVocabularyFacade vocabulary,
                                    AccountSessionService sessions) {
        this.english = english;
        this.vocabulary = vocabulary;
        this.sessions = sessions;
    }

    private long accountId() {
        return sessions.currentAccountId();
    }

    @GetMapping("/learning/records")
    public List<Map<String, Object>> records(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return english.records(accountId(), page, pageSize);
    }

    @GetMapping("/learning/records/batch")
    public java.util.Map<String, com.starrainnotes.english.learning.dto.LearningRecordView> batchRecords(
            @RequestParam("ref") java.util.List<String> refs) {
        return english.batchRecords(accountId(), refs);
    }

    @GetMapping("/learning/summary")
    public com.starrainnotes.english.learning.dto.LearningSummaryView summary() {
        return english.summary(accountId());
    }

    @GetMapping("/learning/insights")
    public com.starrainnotes.english.learning.dto.LearningInsightsView insights() {
        return english.insights(accountId());
    }

    @GetMapping("/learning/records/{type}/{contentId}")
    public Map<String, Object> record(@PathVariable String type, @PathVariable Long contentId) {
        return english.record(accountId(), type, contentId);
    }

    @PutMapping("/learning/records/{type}/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putRecord(@PathVariable String type, @PathVariable Long contentId,
                          @RequestBody Map<String, Object> body) {
        english.saveRecord(accountId(), type, contentId,
                body.get("completionStatus") == null ? "IN_PROGRESS" : String.valueOf(body.get("completionStatus")),
                body.get("timeSpentSeconds") == null ? null : ((Number) body.get("timeSpentSeconds")).intValue());
    }

    @GetMapping("/vocabulary/memory")
    public List<Map<String, Object>> vocabularyMemory() {
        return vocabulary.memorySnapshot(accountId());
    }

    @PutMapping("/vocabulary/words/{wordId}/memory")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putMemory(@PathVariable Long wordId, @RequestBody Map<String, Object> body) {
        vocabulary.putMemoryCount(accountId(), wordId,
                body.get("memoryCount") == null ? 1 : ((Number) body.get("memoryCount")).intValue());
    }

    @GetMapping("/vocabulary/settings")
    public VocabularyStudySettingsView vocabularySettings() {
        return vocabulary.settings(accountId());
    }

    @PutMapping("/vocabulary/settings")
    public VocabularyStudySettingsView updateVocabularySettings(
            @Valid @RequestBody VocabularyStudySettingsRequest request) {
        return vocabulary.updateSettings(accountId(), request);
    }

    @GetMapping("/vocabulary/states")
    public List<VocabularyMemoryView> vocabularyStates(@RequestParam("wordId") List<Long> wordIds) {
        return vocabulary.memories(accountId(), wordIds);
    }

    @GetMapping("/vocabulary/review-queue")
    public VocabularyQueueView vocabularyReviewQueue(@RequestParam(required = false) Long themeId) {
        return vocabulary.queue(accountId(), themeId);
    }

    @PostMapping("/vocabulary/words/{wordId}/start")
    public VocabularyMemoryView startVocabularyWord(@PathVariable long wordId) {
        return vocabulary.start(accountId(), wordId);
    }

    @PostMapping("/vocabulary/words/{wordId}/reviews")
    public VocabularyReviewResultView reviewVocabularyWord(
            @PathVariable long wordId, @Valid @RequestBody VocabularyReviewRequest request) {
        return vocabulary.completeReview(accountId(), wordId, request);
    }

    @DeleteMapping("/vocabulary/words/{wordId}/progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetVocabularyWord(@PathVariable long wordId) {
        vocabulary.reset(accountId(), wordId);
    }

    @PutMapping("/vocabulary/words/{wordId}/display")
    public VocabularyMemoryView setVocabularyDisplay(
            @PathVariable long wordId, @Valid @RequestBody VocabularyDisplayRequest request) {
        return vocabulary.setDisplay(accountId(), wordId, request);
    }

    @DeleteMapping("/vocabulary/words/{wordId}/display")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearVocabularyDisplay(@PathVariable long wordId) {
        vocabulary.clearDisplay(accountId(), wordId);
    }

    @GetMapping("/vocabulary/statistics")
    public VocabularyProgressView vocabularyStatistics() {
        return vocabulary.progress(accountId());
    }

    @PostMapping("/vocabulary/import-local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocalVocabulary(@RequestBody Map<String, Object> body) {
        vocabulary.importLocal(accountId(), body);
    }

    @GetMapping("/writing-submissions/{promptId}")
    public Map<String, Object> writingSubmission(@PathVariable Long promptId) {
        return english.writingSubmission(accountId(), promptId);
    }

    @PutMapping("/writing-submissions/{promptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveWritingSubmission(@PathVariable Long promptId,
                                      @Valid @RequestBody WritingSubmissionRequest request) {
        english.saveWritingSubmission(accountId(), promptId, request);
    }

    @PostMapping("/import-local-progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocal(@RequestBody Map<String, Object> body) {
        english.importLocalProgress(accountId(), body);
    }

    @PostMapping("/claim-legacy-progress")
    public void claimLegacy(@RequestBody Map<String, Object> body) {
        english.claimLegacyProgress(accountId(), String.valueOf(body.get("learnerKeyHash")));
    }
}
