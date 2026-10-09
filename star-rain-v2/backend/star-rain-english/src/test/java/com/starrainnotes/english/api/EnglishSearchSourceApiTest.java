package com.starrainnotes.english.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import com.starrainnotes.english.api.impl.EnglishSearchSourceApiAdapter;
import com.starrainnotes.english.knowledge.mapper.EnglishSearchSourceMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EnglishSearchSourceApiTest {
    @Test
    void longVocabularyQueriesFitTheIndexUrlWithoutSplittingUnicode() {
        var mapper = mock(EnglishSearchSourceMapper.class);
        var document = new EnglishSearchDocument();
        document.setContentType("ENGLISH_VOCABULARY_WORD");
        document.setId(12L);
        document.setThemeId(4L);
        document.setTitle("𠮷".repeat(100));
        when(mapper.findPublished("ENGLISH_VOCABULARY_WORD", 12L)).thenReturn(document);
        var source = new EnglishSearchSourceApiAdapter(mapper);
        String path = source.findPublished("ENGLISH_VOCABULARY_WORD", 12L).getRoutePath();
        assertTrue(path.length() <= 500);
        String query = URLDecoder.decode(path.substring(path.indexOf("?q=") + 3), StandardCharsets.UTF_8);
        assertFalse(query.isBlank());
        assertTrue(document.getTitle().startsWith(query));
        assertEquals(0, query.length() % 2);
    }

    @Test
    void invalidSourceRequestsAreRejectedBeforeScanning() {
        var mapper = mock(EnglishSearchSourceMapper.class);
        var source = new EnglishSearchSourceApiAdapter(mapper);
        assertThrows(IllegalArgumentException.class, () -> source.page("ACCOUNT", null, 100));
        assertThrows(IllegalArgumentException.class, () -> source.page("ENGLISH_READING", null, 101));
        assertThrows(IllegalArgumentException.class, () -> source.findPublished("ENGLISH_WRITING", -1L));
        verifyNoInteractions(mapper);
    }
}
