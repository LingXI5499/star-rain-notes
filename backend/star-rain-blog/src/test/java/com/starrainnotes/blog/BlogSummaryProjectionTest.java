package com.starrainnotes.blog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.mapper.*;
import com.starrainnotes.blog.service.impl.BlogViewAssemblerImpl;
import com.starrainnotes.media.api.MediaAssetApi;
import org.junit.jupiter.api.Test;

class BlogSummaryProjectionTest {
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
