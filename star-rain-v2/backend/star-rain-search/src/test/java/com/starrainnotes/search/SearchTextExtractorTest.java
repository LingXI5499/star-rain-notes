package com.starrainnotes.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.search.text.SearchTextExtractor;
import org.junit.jupiter.api.Test;

class SearchTextExtractorTest {
    @Test
    void keepsChineseAndCodeIdentifiersWithoutMarkdownNoise() {
        String text = new SearchTextExtractor().plainText("# 排序算法\n![流程图](image.png)\n"
            + "[稳定排序](https://example.test)\n```java\nmergeSort(list);\n```");
        assertTrue(text.contains("排序算法"));
        assertTrue(text.contains("稳定排序"));
        assertTrue(text.contains("mergeSort(list)"));
        assertFalse(text.contains("image.png"));
        assertFalse(text.contains("example.test"));
    }
}
