/*
 * EnglishSearchSourceApiTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：EnglishSearchDocument、EnglishSearchSourceApiAdapter、EnglishSearchSourceMapper。
 * 阅读入口：longVocabularyQueriesFitTheIndexUrlWithoutSplittingUnicode（long词汇QueriesFitThe索引地址不含SplittingUnicode处理入口）；invalidSourceRequestsAreRejectedBeforeScanning（无效来源RequestsAre已拒绝之前Scanning处理入口）。
 */

package com.starrainnotes.english.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import com.starrainnotes.english.api.impl.EnglishSearchSourceApiAdapter;
import com.starrainnotes.english.knowledge.mapper.EnglishSearchSourceMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * 英语搜索来源接口测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EnglishSearchSourceApiTest {
    /**
     * long词汇QueriesFitThe索引地址不含SplittingUnicode处理入口。
     * 关键处理步骤调用 <code>document.setContentType</code>、<code>document.setId</code>、<code>document.setThemeId</code>、<code>document.setTitle</code>。
     */
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
        String query = URLDecoder.decode(java.util.Arrays.stream(java.net.URI.create("https://example.test" + path).getRawQuery().split("&")).filter(value -> value.startsWith("q=")).findFirst().orElseThrow().substring(2), StandardCharsets.UTF_8);
        assertFalse(query.isBlank());
        assertTrue(document.getTitle().startsWith(query));
        assertEquals(0, query.length() % 2);
    }

    /**
     * 无效来源RequestsAre已拒绝之前Scanning处理入口。
     * 关键处理步骤调用 <code>source.page</code>、<code>source.findPublished</code>。
     */
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
