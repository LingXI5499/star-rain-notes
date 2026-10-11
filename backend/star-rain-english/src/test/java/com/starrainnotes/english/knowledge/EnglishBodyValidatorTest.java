package com.starrainnotes.english.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import org.junit.jupiter.api.Test;

class EnglishBodyValidatorTest {
    @Test void draftTitleIsOptionalAndEmptyMarkupIsNotACompletedArticle() {
        assertEquals("Untitled Article", EnglishBodyValidator.title("  "));
        for (String empty : new String[]{"", " \n ", "# ** **", "---", "![cover](https://example.test/a.png)", "<script>alert('x')</script>", "&nbsp;"}) {
            assertFalse(EnglishBodyValidator.hasMeaningfulText(empty), empty);
            assertThrows(ApiException.class, () -> EnglishBodyValidator.requireBody(empty));
        }
        assertTrue(EnglishBodyValidator.hasMeaningfulText("# An idea\n\nA real **paragraph**."));
        assertTrue(EnglishBodyValidator.hasMeaningfulText("[Actual words](https://example.test)"));
    }

    @Test void hashesUseUtf8AndPreserveWhitespaceAndMarkdownChanges() {
        assertEquals(64, EnglishBodyValidator.hash("中文😀").length());
        assertNotEquals(EnglishBodyValidator.hash("a"), EnglishBodyValidator.hash("a "));
        assertNotEquals(EnglishBodyValidator.hash("word"), EnglishBodyValidator.hash("**word**"));
        assertEquals(EnglishBodyValidator.hash(null), EnglishBodyValidator.hash(""));
    }
}
