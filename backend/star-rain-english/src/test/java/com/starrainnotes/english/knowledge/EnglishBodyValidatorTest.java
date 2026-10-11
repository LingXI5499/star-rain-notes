/*
 * EnglishBodyValidatorTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、EnglishBodyValidator。
 * 阅读入口：draftTitleIsOptionalAndEmptyMarkupIsNotACompletedArticle（草稿标题是否Optional及空白Markup是否不A已完成Article处理入口）；hashesUseUtf8AndPreserveWhitespaceAndMarkdownChanges（hashesUseUtf8及PreserveWhitespace及MarkdownChanges处理入口）。
 */

package com.starrainnotes.english.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import org.junit.jupiter.api.Test;

/**
 * 英语正文校验器测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EnglishBodyValidatorTest {
    /**
     * 草稿标题是否Optional及空白Markup是否不A已完成Article处理入口。
     * 关键处理步骤调用 <code>EnglishBodyValidator.title</code>、<code>EnglishBodyValidator.hasMeaningfulText</code>、<code>EnglishBodyValidator.requireBody</code>。
     */
    @Test void draftTitleIsOptionalAndEmptyMarkupIsNotACompletedArticle() {
        assertEquals("Untitled Article", EnglishBodyValidator.title("  "));
        for (String empty : new String[]{"", " \n ", "# ** **", "---", "![cover](https://example.test/a.png)", "<script>alert('x')</script>", "&nbsp;"}) {
            assertFalse(EnglishBodyValidator.hasMeaningfulText(empty), empty);
            assertThrows(ApiException.class, () -> EnglishBodyValidator.requireBody(empty));
        }
        assertTrue(EnglishBodyValidator.hasMeaningfulText("# An idea\n\nA real **paragraph**."));
        assertTrue(EnglishBodyValidator.hasMeaningfulText("[Actual words](https://example.test)"));
    }

    /**
     * hashesUseUtf8及PreserveWhitespace及MarkdownChanges处理入口。
     * 关键处理步骤调用 <code>EnglishBodyValidator.hash</code>。
     */
    @Test void hashesUseUtf8AndPreserveWhitespaceAndMarkdownChanges() {
        assertEquals(64, EnglishBodyValidator.hash("中文😀").length());
        assertNotEquals(EnglishBodyValidator.hash("a"), EnglishBodyValidator.hash("a "));
        assertNotEquals(EnglishBodyValidator.hash("word"), EnglishBodyValidator.hash("**word**"));
        assertEquals(EnglishBodyValidator.hash(null), EnglishBodyValidator.hash(""));
    }
}
