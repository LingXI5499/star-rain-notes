package com.starrainnotes.seo.source;

import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.exception.BlogPostNotFoundException;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.common.result.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlogSeoSourceProvider implements SeoSourceProvider {
    private final BlogPublicService blogs;

    @Override
    public boolean supports(String path) { return path != null && path.matches("/blog/posts/[a-zA-Z0-9_-]{1,120}"); }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        try { return Optional.of(document(blogs.postBySlug(path.substring("/blog/posts/".length())))); }
        catch (BlogPostNotFoundException absent) { return Optional.empty(); }
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            BlogPublicQueryDTO query = new BlogPublicQueryDTO();
            query.setPage(page);
            query.setPageSize(100);
            PageResult<com.starrainnotes.blog.vo.BlogPostPublicVO> rows = blogs.listPosts(query);
            rows.getItems().forEach(item -> result.add(document(blogs.postBySlug(item.getSlug()))));
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    private SeoSourceDocument document(BlogPostPublicDetailVO post) {
        return SeoSourceDocument.builder().routePath("/blog/posts/" + post.getSlug())
            .contentType("BLOG").contentId(post.getId()).title(post.getTitle())
            .summary(post.getSummary()).bodyMarkdown(post.getBodyMarkdown())
            .updatedAt(post.getUpdatedAt()).build();
    }
}
