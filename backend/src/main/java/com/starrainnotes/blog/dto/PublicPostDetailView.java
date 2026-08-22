package com.starrainnotes.blog.dto;

import java.util.List;

/**
 * Public blog post detail — body markdown, tags, cover, Prev/Next on the
 * global timeline. Never returns TOC / Calendar / Archive.
 */
public record PublicPostDetailView(
        Long id,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        String coverUrl,
        List<PublicTagView> tags,
        String seoTitle,
        String seoDescription,
        String publishedAt,
        String updatedAt,
        PrevNextPostView previous,
        PrevNextPostView next) {
}
