package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.english.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.english.vocabulary.dto.CreateVocabularyWordRequest;
import com.starrainnotes.english.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.english.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.english.vocabulary.application.LegacyVocabularyMemoryService;
import com.starrainnotes.english.vocabulary.application.VocabularyAudioService;
import com.starrainnotes.english.vocabulary.application.VocabularyCommandService;
import com.starrainnotes.english.vocabulary.application.VocabularyQueryService;
import com.starrainnotes.english.vocabulary.provider.DictionaryPreview;
import com.starrainnotes.english.vocabulary.provider.DictionaryProvider;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Admin vocabulary management: word correction, example sentences and the
 * memory counter correction.
 */
@RestController
@RequestMapping("/api/v1/admin/vocabulary")
public class VocabularyAdminController {

    private final VocabularyQueryService vocabularyQueryService;
    private final VocabularyCommandService vocabularyCommandService;
    private final VocabularyAudioService vocabularyAudioService;
    private final LegacyVocabularyMemoryService legacyMemoryService;
    private final DictionaryProvider dictionaryProvider;

    public VocabularyAdminController(VocabularyQueryService vocabularyQueryService,
                                     VocabularyCommandService vocabularyCommandService,
                                     VocabularyAudioService vocabularyAudioService,
                                     LegacyVocabularyMemoryService legacyMemoryService,
                                     DictionaryProvider dictionaryProvider) {
        this.vocabularyQueryService = vocabularyQueryService;
        this.vocabularyCommandService = vocabularyCommandService;
        this.vocabularyAudioService = vocabularyAudioService;
        this.legacyMemoryService = legacyMemoryService;
        this.dictionaryProvider = dictionaryProvider;
    }

    @GetMapping("/words/{wordId}/dictionary-preview")
    public DictionaryPreview dictionaryPreview(@PathVariable long wordId) {
        return dictionaryProvider.preview(vocabularyQueryService.getWord(wordId).word());
    }

    @GetMapping("/words")
    public VocabularyPageView words(@RequestParam(required = false) Long themeId,
                                    @RequestParam(required = false) Integer layerOrder,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        return vocabularyQueryService.listWords(themeId, layerOrder, q, page, pageSize);
    }

    @PostMapping("/themes")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyThemeView createTheme(@Valid @RequestBody VocabularyThemeRequest request) {
        return vocabularyCommandService.createTheme(request);
    }

    @PutMapping("/themes/{themeId}")
    public VocabularyThemeView updateTheme(@PathVariable long themeId,
                                           @Valid @RequestBody VocabularyThemeRequest request) {
        return vocabularyCommandService.updateTheme(themeId, request);
    }

    @DeleteMapping("/themes/{themeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTheme(@PathVariable long themeId) {
        vocabularyCommandService.deleteTheme(themeId);
    }

    @PostMapping("/words")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyWordView createWord(@Valid @RequestBody CreateVocabularyWordRequest request) {
        return vocabularyCommandService.createWord(request);
    }

    @PutMapping("/words/{wordId}")
    public VocabularyWordView updateWord(@PathVariable long wordId,
                                         @Valid @RequestBody UpdateVocabularyWordRequest request) {
        return vocabularyCommandService.updateWord(wordId, request);
    }

    @DeleteMapping("/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWord(@PathVariable long wordId) {
        vocabularyCommandService.deleteWord(wordId);
    }

    @PostMapping("/words/{wordId}/examples")
    public VocabularyWordView addExample(@PathVariable long wordId,
                                         @Valid @RequestBody AddExampleRequest request) {
        return vocabularyCommandService.addExample(wordId, request);
    }

    @DeleteMapping("/words/{wordId}/examples/{index}")
    public VocabularyWordView removeExample(@PathVariable long wordId, @PathVariable int index) {
        return vocabularyCommandService.removeExample(wordId, index);
    }

    @PutMapping("/words/{wordId}/memory")
    public VocabularyWordView setMemory(@PathVariable long wordId,
                                        @Valid @RequestBody SetMemoryRequest request) {
        return legacyMemoryService.setLegacyMemory(wordId, request);
    }

    @PostMapping("/words/{wordId}/audio")
    public VocabularyAudioView addAudio(@PathVariable long wordId,
                                        @Valid @RequestBody VocabularyAudioRequest request) {
        return vocabularyAudioService.addAudio(wordId, request);
    }

    @DeleteMapping("/words/{wordId}/audio/{audioId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteAudio(@PathVariable long wordId, @PathVariable long audioId) {
        vocabularyAudioService.deleteAudio(wordId, audioId);
    }

    @PutMapping("/words/{wordId}/audio/{audioId}/primary")
    public VocabularyAudioView setPrimaryAudio(@PathVariable long wordId, @PathVariable long audioId) {
        return vocabularyAudioService.setPrimaryAudio(wordId, audioId);
    }
}
