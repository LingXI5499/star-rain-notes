package com.starrainnotes.vocabulary.controller;

import com.starrainnotes.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.vocabulary.dto.CreateVocabularyWordRequest;
import com.starrainnotes.vocabulary.dto.SetMemoryRequest;
import com.starrainnotes.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.vocabulary.dto.VocabularyPageView;
import com.starrainnotes.vocabulary.dto.VocabularyThemeRequest;
import com.starrainnotes.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.vocabulary.service.VocabularyService;
import com.starrainnotes.vocabulary.service.VocabularyAdminService;
import com.starrainnotes.vocabulary.provider.DictionaryPreview;
import com.starrainnotes.vocabulary.provider.DictionaryProvider;
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

    private final VocabularyService vocabularyService;
    private final VocabularyAdminService vocabularyAdminService;
    private final DictionaryProvider dictionaryProvider;

    public VocabularyAdminController(VocabularyService vocabularyService,
                                     VocabularyAdminService vocabularyAdminService,
                                     DictionaryProvider dictionaryProvider) {
        this.vocabularyService = vocabularyService;
        this.vocabularyAdminService = vocabularyAdminService;
        this.dictionaryProvider = dictionaryProvider;
    }

    @GetMapping("/words/{wordId}/dictionary-preview")
    public DictionaryPreview dictionaryPreview(@PathVariable long wordId) {
        return dictionaryProvider.preview(vocabularyService.getWord(wordId).word());
    }

    @GetMapping("/words")
    public VocabularyPageView words(@RequestParam(required = false) Long themeId,
                                    @RequestParam(required = false) Integer layerOrder,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        return vocabularyAdminService.listWords(themeId, layerOrder, q, page, pageSize);
    }

    @PostMapping("/themes")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyThemeView createTheme(@Valid @RequestBody VocabularyThemeRequest request) {
        return vocabularyAdminService.createTheme(request);
    }

    @PutMapping("/themes/{themeId}")
    public VocabularyThemeView updateTheme(@PathVariable long themeId,
                                           @Valid @RequestBody VocabularyThemeRequest request) {
        return vocabularyAdminService.updateTheme(themeId, request);
    }

    @DeleteMapping("/themes/{themeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTheme(@PathVariable long themeId) {
        vocabularyAdminService.deleteTheme(themeId);
    }

    @PostMapping("/words")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyWordView createWord(@Valid @RequestBody CreateVocabularyWordRequest request) {
        return vocabularyAdminService.createWord(request);
    }

    @PutMapping("/words/{wordId}")
    public VocabularyWordView updateWord(@PathVariable long wordId,
                                         @Valid @RequestBody UpdateVocabularyWordRequest request) {
        return vocabularyAdminService.updateWord(wordId, request);
    }

    @DeleteMapping("/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWord(@PathVariable long wordId) {
        vocabularyAdminService.deleteWord(wordId);
    }

    @PostMapping("/words/{wordId}/examples")
    public VocabularyWordView addExample(@PathVariable long wordId,
                                         @Valid @RequestBody AddExampleRequest request) {
        return vocabularyAdminService.addExample(wordId, request);
    }

    @DeleteMapping("/words/{wordId}/examples/{index}")
    public VocabularyWordView removeExample(@PathVariable long wordId, @PathVariable int index) {
        return vocabularyAdminService.removeExample(wordId, index);
    }

    @PutMapping("/words/{wordId}/memory")
    public VocabularyWordView setMemory(@PathVariable long wordId,
                                        @Valid @RequestBody SetMemoryRequest request) {
        return vocabularyAdminService.setMemory(wordId, request);
    }

    @PostMapping("/words/{wordId}/audio")
    public VocabularyAudioView addAudio(@PathVariable long wordId,
                                        @Valid @RequestBody VocabularyAudioRequest request) {
        return vocabularyAdminService.addAudio(wordId, request);
    }

    @DeleteMapping("/words/{wordId}/audio/{audioId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteAudio(@PathVariable long wordId, @PathVariable long audioId) {
        vocabularyAdminService.deleteAudio(wordId, audioId);
    }

    @PutMapping("/words/{wordId}/audio/{audioId}/primary")
    public VocabularyAudioView setPrimaryAudio(@PathVariable long wordId, @PathVariable long audioId) {
        return vocabularyAdminService.setPrimaryAudio(wordId, audioId);
    }
}
