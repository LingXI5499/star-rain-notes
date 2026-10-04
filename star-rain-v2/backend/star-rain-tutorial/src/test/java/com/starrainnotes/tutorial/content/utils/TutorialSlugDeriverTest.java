package com.starrainnotes.tutorial.content.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TutorialSlugDeriverTest {
    @Test
    void derivesReadableAsciiAndStableChineseFallback() {
        assertEquals("java-learning-path", TutorialSlugDeriver.fromTitle("Java Learning Path", "tutorial"));
        assertEquals("java-21", TutorialSlugDeriver.fromTitle("Java 21 教程", "tutorial"));
        String chinese = TutorialSlugDeriver.fromTitle("并发编程基础", "tutorial");
        assertTrue(chinese.matches("tutorial-[0-9a-f]{16}"));
        assertEquals(chinese, TutorialSlugDeriver.fromTitle("并发编程基础", "tutorial"));
    }

    @Test
    void normalizesFullWidthAndLimitsLength() {
        assertEquals("java", TutorialSlugDeriver.fromTitle("Ｊａｖａ", "tutorial"));
        String longSlug = TutorialSlugDeriver.fromTitle("a".repeat(200), "tutorial");
        assertEquals(140, longSlug.length());
    }
}
