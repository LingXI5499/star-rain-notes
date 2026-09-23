package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.english.vocabulary.dto.VocabularyLayerView;
import com.starrainnotes.english.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.service.VocabularyPronunciationService;
import com.starrainnotes.english.vocabulary.application.LegacyVocabularyMemoryService;
import com.starrainnotes.english.vocabulary.application.VocabularyQueryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Arrays;

/**
 * Public vocabulary (approved English vocabulary module).
 */
@RestController
@RequestMapping("/api/v1/public/vocabulary")
public class VocabularyPublicController {

    private final VocabularyQueryService vocabularyService;
    private final VocabularyPronunciationService pronunciationService;
    private final LegacyVocabularyMemoryService legacyMemoryService;

    public VocabularyPublicController(VocabularyQueryService vocabularyService,
                                      VocabularyPronunciationService pronunciationService,
                                      LegacyVocabularyMemoryService legacyMemoryService) {
        this.vocabularyService = vocabularyService;
        this.pronunciationService = pronunciationService;
        this.legacyMemoryService = legacyMemoryService;
    }

    @GetMapping({"/themes", "/layers"})
    public List<VocabularyLayerView> themes() {
        return vocabularyService.listLayers();
    }

    @GetMapping("/themes/{themeId}/words")
    public VocabularyPageView themeWords(@PathVariable long themeId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize,
                                         @RequestParam(defaultValue = "false") boolean remembered) {
        return vocabularyService.listThemeWords(themeId, page, pageSize, remembered);
    }

    @GetMapping({"/words/{wordId}/study", "/words/{wordId}"})
    public VocabularyWordView studyWord(@PathVariable long wordId) {
        return vocabularyService.getWord(wordId);
    }

    /**
     * Serves a professional pronunciation audio clip proxied from the
     * configured provider (e.g. Youdao dictvoice). Bytes are cached on disk and
     * long-cached by the browser. Returns 404 when the provider is disabled or
     * the upstream is unavailable, so the client falls back to browser speech.
     */
    @GetMapping("/pronunciation")
    public ResponseEntity<byte[]> pronunciation(@RequestParam String word,
                                                @RequestParam(required = false) String accent) {
        byte[] bytes = pronunciationService.audio(word, accent);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000, immutable")
                .body(bytes);
    }

    @GetMapping("/words/batch")
    public List<VocabularyWordView> wordsByIds(@RequestParam String ids) {
        List<Long> parsed;
        try {
            parsed = Arrays.stream(ids.split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).map(Long::valueOf).distinct().limit(100).toList();
        } catch (NumberFormatException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "ids must contain numeric word ids");
        }
        return vocabularyService.getWords(parsed);
    }

    @GetMapping("/words")
    public List<VocabularyWordView> wordsByIdsCompatibility(@RequestParam String ids) {
        return wordsByIds(ids);
    }

    /**
     * Personal memory +1 from the public card (single-admin site; the owner
     * reviews vocabulary without logging in).
     */
    @PostMapping("/words/{wordId}/memory")
    @Deprecated
    public VocabularyWordView incrementMemory(@PathVariable long wordId) {
        return legacyMemoryService.incrementLegacyMemory(wordId);
    }
}
