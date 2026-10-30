package com.starrainnotes.english.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.library.dto.LibraryDto;
import com.starrainnotes.english.library.mapper.LibraryMapper;
import com.starrainnotes.english.library.service.EnglishLibraryService;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LibraryWordCreateTest {
    private final LibraryMapper mapper = mock(LibraryMapper.class);
    private final TaxonomyService taxonomy = mock(TaxonomyService.class);
    private final VocabularyService vocabulary = mock(VocabularyService.class);
    private final EnglishLibraryService service = new EnglishLibraryService(mapper, taxonomy, vocabulary, new ObjectMapper());
    private LibraryDto.NewWord request() {
        var value = new LibraryDto.NewWord(); value.setCategory("T01"); value.setTopic("T01-01");
        var word = new VocabularyDto.WordRequest(); word.setWord("physical"); value.setWord(word); return value;
    }
    private void catalog() {
        Node parent = new Node(); parent.setCode("T01"); parent.setLibraryModule("vocabulary");
        Node child = new Node(); child.setId(102L); child.setCode("T01-01"); child.setName("健康");
        parent.setChildren(List.of(child)); when(taxonomy.tree(false)).thenReturn(List.of(parent));
    }
    @Test void createsAWordWithCurrentTaxonomyAndKeepsItsLearningIdentity() {
        catalog(); when(mapper.lockTopic("T01-01")).thenReturn(102L); when(mapper.managedTheme("library:T01-01")).thenReturn(9L);
        VocabularyDto.Word saved = new VocabularyDto.Word(); saved.setId(77L); when(vocabulary.createWord(any())).thenReturn(saved);
        LibraryDto.Word locked = new LibraryDto.Word(); locked.setId(77L); locked.setThemeId(9L); locked.setRowVersion(1L);
        when(mapper.lock(77L)).thenReturn(locked); when(mapper.updateMapping(eq(77L), any())).thenReturn(1);
        var value = request(); assertThat(service.createWord(value).getId()).isEqualTo(77L);
        assertThat(value.getWord().getThemeId()).isEqualTo("9"); assertThat(value.getWord().getTranslation()).isNull();
        verify(mapper).addTopic(77L, 102L); verify(vocabulary, never()).deleteWord(any());
    }
    @Test void invalidTopicDoesNotCreateBackingContentOrChangeExistingWords() {
        catalog(); var value = request(); value.setTopic("T02-01");
        assertThatThrownBy(() -> service.createWord(value)).isInstanceOf(ApiException.class);
        verifyNoInteractions(vocabulary);
    }
}
