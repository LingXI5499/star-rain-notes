package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.english.vocabulary.dto.*;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyRepository;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyThemeRepository;
import org.springframework.stereotype.Service;

@Service
public class VocabularyCommandService {
    private final VocabularyRepository repository;
    private final VocabularyThemeRepository themes;
    public VocabularyCommandService(VocabularyRepository repository,VocabularyThemeRepository themes) { this.repository = repository; this.themes=themes; }
    public VocabularyThemeView createTheme(VocabularyThemeRequest request) { return themes.create(request); }
    public VocabularyThemeView updateTheme(long id,VocabularyThemeRequest request) { return themes.update(id,request); }
    public void deleteTheme(long id) { themes.delete(id); }
    public VocabularyWordView createWord(CreateVocabularyWordRequest request) { return repository.createWord(request); }
    public VocabularyWordView updateWord(long id,UpdateVocabularyWordRequest request) { return repository.updateWord(id,request); }
    public void deleteWord(long id) { repository.deleteWord(id); }
    public VocabularyWordView addExample(long id,AddExampleRequest request) { return repository.addExample(id,request); }
    public VocabularyWordView removeExample(long id,int index) { return repository.removeExample(id,index); }
}
