package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.dto.AddExampleRequest;
import com.starrainnotes.english.vocabulary.dto.CreateVocabularyWordRequest;
import com.starrainnotes.english.vocabulary.dto.UpdateVocabularyWordRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyThemeView;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyRepository;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyThemeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyCommandService {
    private final VocabularyRepository repository;
    private final VocabularyThemeRepository themes;

    public VocabularyCommandService(VocabularyRepository repository, VocabularyThemeRepository themes) {
        this.repository = repository;
        this.themes = themes;
    }

    @Transactional
    public VocabularyThemeView createTheme(VocabularyThemeRequest request) {
        return themes.create(request);
    }

    @Transactional
    public VocabularyThemeView updateTheme(long id, VocabularyThemeRequest request) {
        return themes.update(id, request);
    }

    @Transactional
    public void deleteTheme(long id) {
        themes.requireTheme(id);
        if (themes.wordCount(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "VOCABULARY_THEME_NOT_EMPTY", "Theme is not empty",
                    "Move or delete the theme's vocabulary words before deleting this category.");
        }
        themes.delete(id);
    }

    @Transactional
    public VocabularyWordView createWord(CreateVocabularyWordRequest request) {
        return repository.createWord(request);
    }

    @Transactional
    public VocabularyWordView updateWord(long id, UpdateVocabularyWordRequest request) {
        if (request.word() != null && request.word().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VOCABULARY_WORD_REQUIRED", "Word required",
                    "The English word cannot be blank.");
        }
        return repository.updateWord(id, request);
    }

    @Transactional
    public void deleteWord(long id) {
        repository.deleteWord(id);
    }

    @Transactional
    public VocabularyWordView addExample(long id, AddExampleRequest request) {
        return repository.addExample(id, request);
    }

    @Transactional
    public VocabularyWordView removeExample(long id, int index) {
        int size = repository.exampleCount(id);
        if (index < 0 || index >= size) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EXAMPLE_INDEX_OUT_OF_RANGE",
                    "Invalid example index", "Example index must be within the word's example list.");
        }
        return repository.removeExample(id, index);
    }
}
