package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.*;
import com.starrainnotes.english.vocabulary.mapper.VocabularyMapper;
import com.starrainnotes.english.vocabulary.service.impl.VocabularyServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VocabularyMinimalCreateTest {
    @Test void savesOnlyAWordAndThemeWithoutRequiringDefinitionsOrPhonetics() {
        VocabularyMapper mapper = mock(VocabularyMapper.class);
        var service = new VocabularyServiceImpl(mapper, mock(ApplicationEventPublisher.class));
        WordRequest request = new WordRequest(); request.setThemeId("1"); request.setWord(" physical ");
        doAnswer(call -> { Word word = call.getArgument(0); word.setId(5L); assertThat(word.getWord()).isEqualTo("physical"); assertThat(word.getTranslation()).isEmpty(); assertThat(word.getExamples()).isEqualTo("[]"); return 1; }).when(mapper).insertWord(any());
        Word saved = new Word(); saved.setId(5L); when(mapper.word(5L)).thenReturn(saved);
        assertThat(service.createWord(request).getId()).isEqualTo(5L);
        verify(mapper).initializeLibraryMapping(any());
    }
}
