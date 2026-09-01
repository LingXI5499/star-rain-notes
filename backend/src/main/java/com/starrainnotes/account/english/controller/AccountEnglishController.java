package com.starrainnotes.account.english.controller;

import com.starrainnotes.account.english.AccountEnglishService;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.learning.dto.WritingSubmissionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final AccountEnglishService service;

    public AccountEnglishController(AccountEnglishService service) { this.service = service; }

    private long accountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = auth == null ? null : auth.getPrincipal();
        if (p instanceof com.starrainnotes.account.security.AccountPrincipal ap) {
            return ap.getId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Not authenticated", "Account login required.");
    }

    @GetMapping("/learning/records")
    public List<Map<String, Object>> records(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return service.records(accountId(), page, pageSize);
    }

    @GetMapping("/learning/records/batch")
    public java.util.Map<String, com.starrainnotes.english.shared.learning.dto.LearningRecordView> batchRecords(
            @RequestParam("ref") java.util.List<String> refs) {
        return service.batchRecords(accountId(), refs);
    }

    @GetMapping("/learning/summary")
    public com.starrainnotes.english.shared.learning.dto.LearningSummaryView summary() {
        return service.summary(accountId());
    }

    @GetMapping("/learning/insights")
    public com.starrainnotes.english.shared.learning.dto.LearningInsightsView insights() {
        return service.insights(accountId());
    }

    @GetMapping("/learning/records/{type}/{contentId}")
    public Map<String, Object> record(@PathVariable String type, @PathVariable Long contentId) {
        return service.record(accountId(), type, contentId);
    }

    @PutMapping("/learning/records/{type}/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putRecord(@PathVariable String type, @PathVariable Long contentId,
                          @RequestBody Map<String, Object> body) {
        service.putRecord(accountId(), type, contentId,
                body.get("completionStatus") == null ? "IN_PROGRESS" : String.valueOf(body.get("completionStatus")),
                body.get("timeSpentSeconds") == null ? null : ((Number) body.get("timeSpentSeconds")).intValue());
    }

    @GetMapping("/vocabulary/memory")
    public List<Map<String, Object>> vocabularyMemory() {
        return service.vocabularyMemory(accountId());
    }

    @PutMapping("/vocabulary/words/{wordId}/memory")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putMemory(@PathVariable Long wordId, @RequestBody Map<String, Object> body) {
        service.putVocabularyMemory(accountId(), wordId,
                body.get("memoryCount") == null ? 1 : ((Number) body.get("memoryCount")).intValue());
    }

    @GetMapping("/writing-submissions/{promptId}")
    public Map<String, Object> writingSubmission(@PathVariable Long promptId) {
        return service.writingSubmission(accountId(), promptId);
    }

    @PutMapping("/writing-submissions/{promptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveWritingSubmission(@PathVariable Long promptId,
                                      @Valid @RequestBody WritingSubmissionRequest request) {
        service.saveSubmission(accountId(), promptId, request);
    }

    @PostMapping("/import-local-progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocal(@RequestBody Map<String, Object> body) {
        service.importLocalProgress(accountId(), body);
    }

    @PostMapping("/claim-legacy-progress")
    public void claimLegacy(@RequestBody Map<String, Object> body) {
        service.claimLegacyProgress(accountId(), String.valueOf(body.get("learnerKeyHash")));
    }
}