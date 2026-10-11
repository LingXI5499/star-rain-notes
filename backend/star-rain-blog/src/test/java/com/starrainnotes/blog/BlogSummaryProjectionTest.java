/*
 * BlogSummaryProjectionTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogPostEntity、BlogViewAssemblerImpl、MediaAssetApi。
 * 阅读入口：onlyLegacyMathSummariesFetchBodyAndOrdinarySummariesStayUnchanged（onlyLegacyMathSummariesFetch正文及OrdinarySummariesStayUnchanged处理入口）。
 */

package com.starrainnotes.blog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.mapper.*;
import com.starrainnotes.blog.service.impl.BlogViewAssemblerImpl;
import com.starrainnotes.media.api.MediaAssetApi;
import org.junit.jupiter.api.Test;

/**
 * 博客摘要Projection测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogSummaryProjectionTest {
    /**
     * onlyLegacyMathSummariesFetch正文及OrdinarySummariesStayUnchanged处理入口。
     * 关键处理步骤调用 <code>row.setId</code>、<code>row.setSummary</code>、<code>assembler.toPublicVO</code>、<code>detail.setBodyMarkdown</code>。
     */
    @Test void onlyLegacyMathSummariesFetchBodyAndOrdinarySummariesStayUnchanged() {
        var posts = mock(BlogPostMapper.class);
        var assembler = new BlogViewAssemblerImpl(mock(BlogTagMapper.class), mock(BlogTopicMapper.class), mock(MediaAssetApi.class), posts);
        var row = new BlogPostEntity(); row.setId(1L); row.setSummary("Readable summary");
        assertEquals("Readable summary", assembler.toPublicVO(row).getSummary());
        verifyNoInteractions(posts);
        row.setSummary("$$ truncated math");
        var detail = new BlogPostEntity(); detail.setBodyMarkdown("Readable content.\n\n$$\nx^2\n$$");
        when(posts.postById(1L)).thenReturn(detail);
        assertTrue(assembler.toPublicVO(row).getSummary().contains("Readable content"));
        verify(posts).postById(1L);
    }
}
