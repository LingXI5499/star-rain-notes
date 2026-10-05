package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlogSeoSourceProvider implements SeoSourceProvider {
    private final BlogSearchSourceApi blogs;

    @Override
    public boolean supports(String path) { return path != null && path.matches("/blog/posts/[a-zA-Z0-9_-]{1,120}"); }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        return Optional.ofNullable(blogs.getPublishedDocumentBySlug(path.substring("/blog/posts/".length())))
            .map(this::document);
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        Long cursor = null;
        do {
            BlogDocumentPage page = blogs.listPublishedDocuments(cursor, 100);
            page.getItems().forEach(item -> result.add(document(item)));
            cursor = page.getNextCursor();
        } while (cursor != null);
        return result;
    }

    private SeoSourceDocument document(BlogPostDocument post) {
        return SeoSourceDocument.builder().routePath("/blog/posts/" + post.getSlug())
            .contentType("BLOG").contentId(post.getId()).title(post.getTitle())
            .summary(post.getSummary()).bodyMarkdown(post.getBodyMarkdown())
            .updatedAt(post.getUpdatedAt()).build();
    }
}
