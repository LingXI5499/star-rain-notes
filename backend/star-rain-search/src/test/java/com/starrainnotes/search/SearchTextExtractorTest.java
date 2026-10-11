/*
 * SearchTextExtractorTest：搜索模块的测试文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：SearchTextExtractor。
 * 阅读入口：keepsChineseAndCodeIdentifiersWithoutMarkdownNoise（验证保留Chinese及代码Identifiers不含MarkdownNoise）。
 */

package com.starrainnotes.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.search.utils.SearchTextExtractor;
import org.junit.jupiter.api.Test;

/**
 * 搜索文本Extractor测试，属于搜索模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SearchTextExtractorTest {
    /**
     * 验证保留Chinese及代码Identifiers不含MarkdownNoise。
     * 关键处理步骤调用 <code>text.contains</code>。
     */
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
