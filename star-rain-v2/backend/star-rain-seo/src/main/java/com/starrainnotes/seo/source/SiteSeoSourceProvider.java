package com.starrainnotes.seo.source;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SiteSeoSourceProvider implements SeoSourceProvider {
    private static final List<SeoSourceDocument> PAGES = List.of(
        page("/", "星雨笔录", "沿时间沉淀思考，让经验持续生长。",
            "星雨笔录记录技术实践、学习路径与系统复盘。浏览教程、博客、作品与作者资料。"),
        page("/blog", "博客", "技术实践、判断、方法与复盘。", "按时间浏览已公开的博客文章。"),
        page("/blog/archive", "博客归档", "按时间浏览公开文章。", "浏览已公开文章的归档。"),
        page("/tutorials", "教程", "从知识体系进入系统课程。", "浏览已公开的教程与章节。"),
        page("/portfolio", "作品", "用真实项目验证学习。", "浏览已公开的项目作品。"),
        page("/messages", "留言", "交流与留言。", "浏览站点公开留言。")
    );

    @Override
    public boolean supports(String routePath) {
        return PAGES.stream().anyMatch(page -> page.getRoutePath().equals(routePath));
    }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String routePath) {
        return PAGES.stream().filter(page -> page.getRoutePath().equals(routePath)).findFirst();
    }

    @Override
    public List<SeoSourceDocument> listPublished() { return PAGES; }

    private static SeoSourceDocument page(String path, String title, String summary, String body) {
        return SeoSourceDocument.builder().routePath(path).contentType("SITE")
            .title(title).summary(summary).bodyMarkdown(body).build();
    }
}
