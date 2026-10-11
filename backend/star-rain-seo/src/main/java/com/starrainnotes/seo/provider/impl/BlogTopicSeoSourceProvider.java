/*
 * BlogTopicSeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：BlogPublicApi、BlogTopicSummary、SeoSourceDocument、SeoSourceProvider。
 * 阅读入口：supports（判断是否支持博客专题SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）。
 */

package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.blog.api.dto.BlogTopicSummary;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import com.starrainnotes.seo.provider.SeoSourceProvider;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 博客专题SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class BlogTopicSeoSourceProvider implements SeoSourceProvider {
    /**
     * PREFIX。
     */
    private static final String PREFIX = "/blog/topics/";
    /**
     * blogs。
     * 协作类型：<code>BlogPublicApi</code>。
     */
    private final BlogPublicApi blogs;

    /**
     * 判断是否支持博客专题SEO来源提供方。
     * 关键处理步骤调用 <code>routePath.matches</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String routePath) {
        return routePath != null && routePath.matches("/blog/topics/[a-zA-Z0-9_-]{1,120}");
    }

    /**
     * 加载依据路由。
     * 主要协作调用：<code>blogs.publishedTopicBySlug</code>、<code>PREFIX.length</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<SeoSourceDocument> loadByRoute(String routePath) {
        if (!supports(routePath)) {
            return Optional.empty();
        }
        return Optional.ofNullable(blogs.publishedTopicBySlug(routePath.substring(PREFIX.length())))
                .map(this::document);
    }

    /**
     * 列出已发布。
     * 主要协作调用：<code>blogs.publishedTopics</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<SeoSourceDocument> listPublished() {
        return blogs.publishedTopics().stream().map(this::document).toList();
    }

    /**
     * 文档处理入口。
     * 关键处理步骤调用 <code>topic.description</code>、<code>topic.name</code>、<code>SeoSourceDocument.builder</code>、<code>topic.slug</code>。
     *
     * @param topic 专题；类型为 <code>BlogTopicSummary</code>
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
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
