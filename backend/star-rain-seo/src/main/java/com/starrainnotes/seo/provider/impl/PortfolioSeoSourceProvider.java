/*
 * PortfolioSeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoSourceProvider、SeoSourceDocument、PageResult、PortfolioPublicApi、PortfolioPublishedWork。
 * 阅读入口：supports（判断是否支持作品SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）。
 */

package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.seo.provider.SeoSourceProvider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 作品SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class PortfolioSeoSourceProvider implements SeoSourceProvider {
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioPublicApi</code>。
     */
    private final PortfolioPublicApi works;

    /**
     * 判断是否支持作品SEO来源提供方。
     * 关键处理步骤调用 <code>path.matches</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String path) { return path != null && path.matches("/portfolio/[a-zA-Z0-9_-]{1,180}"); }

    /**
     * 加载依据路由。
     * 主要协作调用：<code>works.publishedWorkBySlug</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        return works.publishedWorkBySlug(path.substring("/portfolio/".length())).map(this::document);
    }

    /**
     * 列出已发布。
     * 主要协作调用：<code>works.publishedWorks</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageResult<PortfolioPublishedWork> rows = works.publishedWorks(page, 100);
            rows.getItems().forEach(item -> result.add(document(item)));
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    /**
     * 文档处理入口。
     * 关键处理步骤调用 <code>SeoSourceDocument.builder</code>、<code>work.getSlug</code>、<code>work.getId</code>、<code>work.getSeoTitle</code>。
     *
     * @param work 作品；类型为 <code>PortfolioPublishedWork</code>
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
    private SeoSourceDocument document(PortfolioPublishedWork work) {
        return SeoSourceDocument.builder().routePath("/portfolio/" + work.getSlug())
            .contentType("PORTFOLIO").contentId(work.getId())
            .title(work.getSeoTitle() == null || work.getSeoTitle().isBlank() ? work.getTitle() : work.getSeoTitle())
            .summary(work.getSeoDescription() == null || work.getSeoDescription().isBlank() ? work.getSummary() : work.getSeoDescription()).bodyMarkdown(work.getBodyMarkdown())
            .coverUrl(work.getCoverUrl()).updatedAt(work.getUpdatedAt()).build();
    }
}
