/*
 * BlogSeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoSourceProvider、SeoSourceDocument、BlogSearchSourceApi、BlogDocumentPage、BlogPostDocument。
 * 阅读入口：supports（判断是否支持博客SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）。
 */

package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.seo.provider.SeoSourceProvider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 博客SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class BlogSeoSourceProvider implements SeoSourceProvider {
    /**
     * blogs。
     * 协作类型：<code>BlogSearchSourceApi</code>。
     */
    private final BlogSearchSourceApi blogs;

    /**
     * 判断是否支持博客SEO来源提供方。
     * 关键处理步骤调用 <code>path.matches</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String path) { return path != null && path.matches("/blog/posts/[a-zA-Z0-9_-]{1,120}"); }

    /**
     * 加载依据路由。
     * 主要协作调用：<code>blogs.getPublishedDocumentBySlug</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        return Optional.ofNullable(blogs.getPublishedDocumentBySlug(path.substring("/blog/posts/".length())))
            .map(this::document);
    }

    /**
     * 列出已发布。
     * 主要协作调用：<code>blogs.listPublishedDocuments</code>。
     * @return 本次处理得到的条目集合
     */
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

    /**
     * 文档处理入口。
     * 关键处理步骤调用 <code>SeoSourceDocument.builder</code>、<code>post.getSlug</code>、<code>post.getId</code>、<code>post.getTitle</code>。
     *
     * @param post 文章；类型为 <code>BlogPostDocument</code>
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
    private SeoSourceDocument document(BlogPostDocument post) {
        return SeoSourceDocument.builder().routePath("/blog/posts/" + post.getSlug())
            .contentType("BLOG").contentId(post.getId()).title(post.getTitle())
            .summary(post.getSummary()).bodyMarkdown(post.getBodyMarkdown())
            .updatedAt(post.getUpdatedAt()).build();
    }
}
