/*
 * TutorialSeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoSourceProvider、SeoSourceDocument、PageResult、TutorialChapterDocument、TutorialDocumentSourceApi、TutorialPublishedDocument。
 * 阅读入口：supports（判断是否支持教程SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）；publishedTree（已发布Tree处理入口）。
 */

package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.seo.provider.SeoSourceProvider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.api.TutorialChapterDocument;
import com.starrainnotes.tutorial.content.api.TutorialDocumentSourceApi;
import com.starrainnotes.tutorial.content.api.TutorialPublishedDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 教程SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class TutorialSeoSourceProvider implements SeoSourceProvider {
    /**
     * tutorials。
     * 协作类型：<code>TutorialDocumentSourceApi</code>。
     */
    private final TutorialDocumentSourceApi tutorials;

    /**
     * 判断是否支持教程SEO来源提供方。
     * 关键处理步骤调用 <code>path.matches</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String path) {
        return path != null && path.matches("/tutorials/[a-zA-Z0-9_-]{1,120}(/[a-zA-Z0-9_-]{1,120})?");
    }

    /**
     * 加载依据路由。
     * 主要协作调用：<code>tutorials.bySlug</code>、<code>tutorials.chapter</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        String[] parts = path.split("/");
        if (parts.length == 3) return tutorials.bySlug(parts[2]).map(this::tutorial);
        return tutorials.chapter(parts[2], parts[3]).map(chapter -> chapter(parts[2], chapter));
    }

    /**
     * 列出已发布。
     * 主要协作调用：<code>tutorials.page</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageResult<TutorialPublishedDocument> rows = tutorials.page(page, 100);
            for (TutorialPublishedDocument item : rows.getItems()) result.addAll(publishedTree(item));
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    /**
     * 已发布Tree处理入口。
     * 主要协作调用：<code>tutorials.bySlug</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    public List<SeoSourceDocument> publishedTree(String slug) {
        return tutorials.bySlug(slug).map(this::publishedTree).orElseGet(List::of);
    }

    /**
     * 已发布Tree处理入口。
     * 关键处理步骤调用 <code>result.add</code>、<code>tutorial</code>、<code>detail.getChapters</code>、<code>chapter</code>。
     *
     * @param detail 详情；类型为 <code>TutorialPublishedDocument</code>
     * @return 本次处理得到的条目集合
     */
    private List<SeoSourceDocument> publishedTree(TutorialPublishedDocument detail) {
        List<SeoSourceDocument> result = new ArrayList<>();
        result.add(tutorial(detail));
        detail.getChapters().forEach(chapter -> result.add(chapter(detail.getSlug(), chapter)));
        return result;
    }

    /**
     * 教程处理入口。
     * 关键处理步骤调用 <code>detail.getSummary</code>、<code>detail.getChapters</code>、<code>body.append</code>、<code>chapter.getTitle</code>。
     *
     * @param detail 详情；类型为 <code>TutorialPublishedDocument</code>
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
    private SeoSourceDocument tutorial(TutorialPublishedDocument detail) {
        StringBuilder body = new StringBuilder(detail.getSummary() == null ? "" : detail.getSummary());
        detail.getChapters().forEach(chapter -> body.append('\n').append(chapter.getTitle())
            .append(' ').append(chapter.getSummary()));
        return SeoSourceDocument.builder().routePath("/tutorials/" + detail.getSlug())
            .contentType("TUTORIAL").contentId(detail.getId()).title(detail.getTitle())
            .summary(detail.getSummary()).bodyMarkdown(body.toString()).updatedAt(detail.getUpdatedAt()).build();
    }

    /**
     * 章节处理入口。
     * 关键处理步骤调用 <code>SeoSourceDocument.builder</code>、<code>detail.getSlug</code>、<code>detail.getId</code>、<code>detail.getTitle</code>。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param detail 详情；类型为 <code>TutorialChapterDocument</code>
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
    private SeoSourceDocument chapter(String tutorialSlug, TutorialChapterDocument detail) {
        return SeoSourceDocument.builder().routePath("/tutorials/" + tutorialSlug + "/" + detail.getSlug())
            .contentType("CHAPTER").contentId(detail.getId()).title(detail.getTitle())
            .summary(detail.getSummary()).bodyMarkdown(detail.getSearchableText()).updatedAt(detail.getUpdatedAt()).build();
    }
}
