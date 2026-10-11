/*
 * StaticPageSeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoSourceProvider、SeoSourceDocument、SitePublicApi、SitePublicConfigVO。
 * 阅读入口：supports（判断是否支持Static分页SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）。
 */

package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.seo.provider.SeoSourceProvider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Static分页SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class StaticPageSeoSourceProvider implements SeoSourceProvider {
    /**
     * 站点。
     * 协作类型：<code>SitePublicApi</code>。
     */
    private final SitePublicApi site;
    /**
     * PAGES。
     */
    private static final List<SeoSourceDocument> PAGES = List.of(
        page("/blog", "博客", "技术实践、判断、方法与复盘。", "按时间浏览已公开的博客文章。"),
        page("/blog/archive", "博客归档", "按时间浏览公开文章。", "浏览已公开文章的归档。"),
        page("/tutorials", "教程", "从知识体系进入系统课程。", "浏览已公开的教程与章节。"),
        page("/portfolio", "作品", "用真实项目验证学习。", "浏览已公开的项目作品。"),
        page("/messages", "留言", "交流与留言。", "浏览站点公开留言。")
    );

    /**
     * 判断是否支持Static分页SEO来源提供方。
     * 主要协作调用：<code>PAGES.stream</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String routePath) {
        return "/".equals(routePath) || PAGES.stream().anyMatch(page -> page.getRoutePath().equals(routePath));
    }

    /**
     * 加载依据路由。
     * 主要协作调用：<code>PAGES.stream</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<SeoSourceDocument> loadByRoute(String routePath) {
        if ("/".equals(routePath)) return Optional.of(home());
        return PAGES.stream().filter(page -> page.getRoutePath().equals(routePath)).findFirst();
    }

    /**
     * 列出已发布。
     * 关键处理步骤调用 <code>pages.add</code>、<code>home</code>、<code>pages.addAll</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> pages = new java.util.ArrayList<>();
        pages.add(home());
        pages.addAll(PAGES);
        return pages;
    }

    /**
     * 首页处理入口。
     * 主要协作调用：<code>site.config</code>。
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
    private SeoSourceDocument home() {
        SitePublicConfigVO config = site.config();
        return page("/", config.getSiteTitle(), config.getSiteDescription(), config.getHomeIntro());
    }

    /**
     * 分页读取Static分页SEO来源提供方。
     * 关键处理步骤调用 <code>SeoSourceDocument.builder</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @param title 标题；类型为 <code>String</code>
     * @param summary 摘要；类型为 <code>String</code>
     * @param body 正文；类型为 <code>String</code>
     * @return 类型为 <code>SeoSourceDocument</code> 的处理结果
     */
    private static SeoSourceDocument page(String path, String title, String summary, String body) {
        return SeoSourceDocument.builder().routePath(path).contentType("SITE")
            .title(title).summary(summary).bodyMarkdown(body).build();
    }
}
