package com.starrainnotes.vocabulary.controller;

import com.starrainnotes.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.vocabulary.service.VocabularyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin vocabulary management: word correction, example sentences and the
 * memory counter correction.
 */
@RestController
@RequestMapping("/api/v1/admin/vocabulary")
public class VocabularyAdminController {

    private final VocabularyService vocabularyService;

    public VocabularyAdminController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @GetMapping("/words")
    public VocabularyPageView words(@RequestParam(required = false) Long themeId,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        return vocabularyService.adminListWords(themeId, q, page, pageSize);
    }

    @PutMapping("/words/{wordId}")
    public VocabularyWordView updateWord(@PathVariable long wordId,
                                         @Valid @RequestBody UpdateVocabularyWordRequest request) {
        return vocabularyService.updateWord(wordId, request);
    }

    @PostMapping("/words/{wordId}/examples")
    public VocabularyWordView addExample(@PathVariable long wordId,
                                         @Valid @RequestBody AddExampleRequest request) {
        return vocabularyService.addExample(wordId, request);
    }

    @DeleteMapping("/words/{wordId}/examples/{index}")
    public VocabularyWordView removeExample(@PathVariable long wordId, @PathVariable int index) {
        return vocabularyService.removeExample(wordId, index);
    }

    @PutMapping("/words/{wordId}/memory")
    public VocabularyWordView setMemory(@PathVariable long wordId,
                                        @Valid @RequestBody SetMemoryRequest request) {
        return vocabularyService.setMemory(wordId, request);
    }
}
