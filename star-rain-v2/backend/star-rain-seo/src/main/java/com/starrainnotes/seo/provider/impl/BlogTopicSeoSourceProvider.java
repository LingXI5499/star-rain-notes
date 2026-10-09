package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.blog.api.dto.BlogTopicSummary;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import com.starrainnotes.seo.provider.SeoSourceProvider;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlogTopicSeoSourceProvider implements SeoSourceProvider {
    private static final String PREFIX = "/blog/topics/";
    private final BlogPublicApi blogs;

    @Override
    public boolean supports(String routePath) {
        return routePath != null && routePath.matches("/blog/topics/[a-zA-Z0-9_-]{1,120}");
    }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String routePath) {
        if (!supports(routePath)) {
            return Optional.empty();
        }
        return Optional.ofNullable(blogs.publishedTopicBySlug(routePath.substring(PREFIX.length())))
                .map(this::document);
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        return blogs.publishedTopics().stream().map(this::document).toList();
    }

    private SeoSourceDocument document(BlogTopicSummary topic) {
        String summary = topic.description() == null || topic.description().isBlank()
                ? "浏览「" + topic.name() + "」专题的公开文章。" : topic.description();
        return SeoSourceDocument.builder()
                .routePath(PREFIX + topic.slug())
                .contentType("BLOG_TOPIC")
                .contentId(topic.id())
                .title(topic.name())
                .summary(summary)
                .bodyMarkdown(summary)
                .updatedAt(topic.updatedAt())
                .build();
    }
}
