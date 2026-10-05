package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaticPageSeoSourceProvider implements SeoSourceProvider {
    private final SitePublicApi site;
    private static final List<SeoSourceDocument> PAGES = List.of(
        page("/blog", "博客", "技术实践、判断、方法与复盘。", "按时间浏览已公开的博客文章。"),
        page("/blog/archive", "博客归档", "按时间浏览公开文章。", "浏览已公开文章的归档。"),
        page("/tutorials", "教程", "从知识体系进入系统课程。", "浏览已公开的教程与章节。"),
        page("/portfolio", "作品", "用真实项目验证学习。", "浏览已公开的项目作品。"),
        page("/messages", "留言", "交流与留言。", "浏览站点公开留言。")
    );

    @Override
    public boolean supports(String routePath) {
        return "/".equals(routePath) || PAGES.stream().anyMatch(page -> page.getRoutePath().equals(routePath));
    }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String routePath) {
        if ("/".equals(routePath)) return Optional.of(home());
        return PAGES.stream().filter(page -> page.getRoutePath().equals(routePath)).findFirst();
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> pages = new java.util.ArrayList<>();
        pages.add(home());
        pages.addAll(PAGES);
        return pages;
    }

    private SeoSourceDocument home() {
        SitePublicConfigVO config = site.config();
        return page("/", config.getSiteTitle(), config.getSiteDescription(), config.getHomeIntro());
    }

    private static SeoSourceDocument page(String path, String title, String summary, String body) {
        return SeoSourceDocument.builder().routePath(path).contentType("SITE")
            .title(title).summary(summary).bodyMarkdown(body).build();
    }
}
