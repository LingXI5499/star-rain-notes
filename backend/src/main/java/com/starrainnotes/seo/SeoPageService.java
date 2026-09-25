package com.starrainnotes.seo;

import com.starrainnotes.blog.api.BlogSeoPort;
import com.starrainnotes.english.grammar.api.GrammarSeoPort;
import com.starrainnotes.english.listening.api.ListeningSeoPort;
import com.starrainnotes.english.reading.api.ReadingSeoPort;
import com.starrainnotes.english.shared.bundle.api.BundleSeoPort;
import com.starrainnotes.english.writing.api.WritingSeoPort;
import com.starrainnotes.media.api.MediaAssetPort;
import com.starrainnotes.portfolio.api.PortfolioSeoPort;
import com.starrainnotes.profile.api.ProfileSeoPort;
import com.starrainnotes.tutorial.api.TutorialSeoPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeoPageService {
    private final SeoMarkdownRenderer markdown;
    private final MediaAssetPort media;
    private final SeoIdentityService identity;
    private final ProfileSeoPort profiles;
    private final TutorialSeoPort tutorials;
    private final BlogSeoPort blogs;
    private final PortfolioSeoPort projects;
    private final GrammarSeoPort grammar;
    private final ReadingSeoPort reading;
    private final ListeningSeoPort listening;
    private final WritingSeoPort writing;
    private final BundleSeoPort bundles;
    public SeoPageService(SeoMarkdownRenderer markdown, MediaAssetPort media, SeoIdentityService identity, ProfileSeoPort profiles,
                          TutorialSeoPort tutorials, BlogSeoPort blogs, PortfolioSeoPort projects, GrammarSeoPort grammar,
                          ReadingSeoPort reading, ListeningSeoPort listening, WritingSeoPort writing, BundleSeoPort bundles) {
        this.markdown = markdown;
        this.media = media;
        this.identity = identity;
        this.profiles = profiles;
        this.tutorials = tutorials;
        this.blogs = blogs;
        this.projects = projects;
        this.grammar = grammar;
        this.reading = reading;
        this.listening = listening;
        this.writing = writing;
        this.bundles = bundles;
    }

    @Transactional(readOnly = true)
    public SeoPage resolve(String rawPath) {
        String path = normalize(rawPath);
        return switch (path) {
            case "/" -> home();
            case "/tutorials" -> listing(path, "Java 全栈教程", "系统化整理 Java 全栈、计算机基础与软件工程学习路径。", "/tutorials/", cards(tutorials.publishedTutorials()));
            case "/blog" -> listing(path, "技术博客", "记录 Java、软件工程与智能体开发中的实践、思考与复盘。", "/blog/", blogCards(blogs.published()));
            case "/portfolio" -> listing(path, "项目作品", "从问题、设计、实现到部署复盘的真实项目案例。", "/portfolio/", projectCards(projects.published()));
            case "/english" -> englishHome();
            case "/english/grammar" -> listing(path, "英语语法完整教程", "从词法到复杂句法，系统建立英语语法知识框架。", "/english/grammar/", grammarCards(grammar.published()));
            case "/english/reading" -> listing(path, "英语阅读中心", "按 CEFR 与能力层级组织的原创分级精读内容。", "/english/reading/", readingCards(reading.published()));
            case "/english/listening" -> listing(path, "英语听力中心", "完整音频、逐句片段与练习组成的分层精听内容。", "/english/listening/", listeningCards(listening.publishedMaterials()));
            case "/english/listening/pronunciation" -> listing(path, "英语语音规则", "连读、弱读、同化、省音、重音与语调的系统训练。", "/english/listening/pronunciation/", listeningCards(listening.publishedRules()));
            case "/english/writing" -> writingHome();
            case "/english/bundles" -> listing(path, "英语学习组合", "串联阅读、听力与写作的主题学习路径。", "/english/bundles/", bundleCards(bundles.published()));
            case "/about" -> about();
            case "/search" -> staticPage(path, "站内搜索", "搜索星雨笔录中的教程、博客与作品。", "noindex,follow", links("教程", "/tutorials", "博客", "/blog", "作品", "/portfolio"));
            case "/english/progress" -> staticPage(path, "学习进度", "个人英语学习进度与复习建议。", "noindex,nofollow", "<p>此页面包含个人学习状态，需要登录后使用。</p>");
            case "/english/vocabulary" -> staticPage(path, "英语词汇", "按主题整理的英语词汇库。", "noindex,follow", "<p>词汇主题页面当前不作为独立搜索入口。</p>");
            default -> dynamic(path);
        };
    }

    private SeoPage dynamic(String path) {
        String[] parts = path.substring(1).split("/");
        if (parts.length == 2 && parts[0].equals("blog")) return blog(path, parts[1]);
        if (parts.length == 2 && parts[0].equals("portfolio")) return project(path, parts[1]);
        if (parts.length == 2 && parts[0].equals("tutorials")) return tutorial(path, parts[1]);
        if (parts.length == 3 && parts[0].equals("tutorials")) return chapter(path, parts[1], parts[2]);
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("grammar")) return grammar(path, parts[2]);
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("reading")) return reading(path, parts[2]);
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("listening") && !parts[2].equals("pronunciation")) return listening(path, parts[2]);
        if (parts.length == 4 && parts[0].equals("english") && parts[1].equals("listening") && parts[2].equals("pronunciation")) return pronunciation(path, parts[3]);
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("bundles")) return bundle(path, parts[2]);
        if (parts.length == 4 && parts[0].equals("english") && parts[1].equals("writing") && parts[2].equals("resources")) return writingResource(path, parts[3]);
        if (parts.length == 4 && parts[0].equals("english") && parts[1].equals("writing") && parts[2].equals("practice")) return writingPrompt(path, parts[3]);
        if (path.startsWith("/english/vocabulary/")) return staticPage(path, "词汇主题", "英语主题词汇。", "noindex,follow", "<p>该主题页暂不纳入搜索索引。</p>");
        return null;
    }

    private SeoPage home() {
        String body = "<section><h2>系统学习与真实实践</h2>" + links("教程", "/tutorials", "博客", "/blog", "作品", "/portfolio", "英语", "/english") + "</section>";
        return staticPage("/", "星雨笔录 | Java 全栈、计算机基础与技术学习笔记", identity.site().tagline(), "index,follow", body, "WebSite");
    }

    private SeoPage englishHome() {
        return staticPage("/english", "英语学习", "从语法、词汇到阅读、听力与写作的长期学习路线。", "index,follow",
                links("语法", "/english/grammar", "阅读", "/english/reading", "听力", "/english/listening", "写作", "/english/writing", "学习组合", "/english/bundles"));
    }

    private SeoPage writingHome() {
        String body = cards("/english/writing/resources/", writingCards(writing.publishedResources()))
                + cards("/english/writing/practice/", writingCards(writing.publishedPrompts()));
        return page("/english/writing", "英语写作中心", "表达训练、范文、模板与结构化写作任务。", null, null, null, body,
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("英语", "/english")), "CollectionPage", Map.of());
    }

    private SeoPage about() {
        ProfileSeoPort.About row = profiles.about();
        if (row == null) return staticPage("/about", "关于我", "关于作者与星雨笔录。", "index,follow", "<p>作者资料尚未公开。</p>");
        String name = first(row.displayName(), "零燨");
        String description = first(row.headline(), row.bio(), "关于作者与星雨笔录。");
        String body = "<p>" + esc(row.bio()) + "</p>" + markdown.render(row.technicalDirection()) + markdown.render(row.journey());
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("name", name);
        if (row.githubUrl() != null) extras.put("sameAs", List.of(row.githubUrl()));
        return page("/about", "关于我", description, null, row.updatedAt(), null, body, List.of(new SeoBreadcrumb("首页", "/")), "ProfilePage", extras);
    }

    private SeoPage tutorial(String path, String slug) {
        TutorialSeoPort.TutorialPage row = tutorials.publishedTutorial(slug);
        if (row == null) return null;
        return page(path, row.title() + "教程", row.summary(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()),
                cards(path + "/", cards(tutorials.publishedChapters(slug))),
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("教程", "/tutorials")), "CollectionPage", Map.of("about", row.categoryName()));
    }

    private SeoPage chapter(String path, String tutorialSlug, String chapterSlug) {
        TutorialSeoPort.ChapterPage row = tutorials.publishedChapter(tutorialSlug, chapterSlug);
        if (row == null) return null;
        return page(path, row.title() + " | " + row.tutorialTitle(), first(row.summary(), row.title()), row.publishedAt(), row.updatedAt(), null,
                markdown.render(row.body()), List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("教程", "/tutorials"), new SeoBreadcrumb(row.tutorialTitle(), "/tutorials/" + tutorialSlug)), "Article", Map.of());
    }

    private SeoPage blog(String path, String slug) {
        BlogSeoPort.Article row = blogs.publishedArticle(slug);
        if (row == null) return null;
        return articlePage(path, row.title(), row.summary(), row.body(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()), "博客", "/blog", "Article");
    }

    private SeoPage grammar(String path, String slug) {
        GrammarSeoPort.Article row = grammar.publishedArticle(slug);
        if (row == null) return null;
        return articlePage(path, row.title(), row.summary(), row.body(), row.publishedAt(), row.updatedAt(), null, "英语语法", "/english/grammar", "Article");
    }

    private SeoPage reading(String path, String slug) {
        ReadingSeoPort.Article row = reading.publishedArticle(slug);
        if (row == null) return null;
        return articlePage(path, row.title(), row.summary(), row.body(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()), "英语阅读", "/english/reading", "Article");
    }

    private SeoPage listening(String path, String slug) {
        ListeningSeoPort.Material row = listening.publishedMaterial(slug);
        if (row == null) return null;
        return page(path, row.title(), row.summary(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()), markdown.render(row.transcript()),
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("英语听力", "/english/listening")), "Article", Map.of());
    }

    private SeoPage pronunciation(String path, String slug) {
        ListeningSeoPort.Rule row = listening.publishedRule(slug);
        if (row == null) return null;
        return articlePage(path, row.title(), row.summary(), row.body(), row.publishedAt(), row.updatedAt(), null, "语音规则", "/english/listening/pronunciation", "Article");
    }

    private SeoPage bundle(String path, String slug) {
        BundleSeoPort.Page row = bundles.publishedBundle(slug);
        if (row == null) return null;
        return page(path, row.title(), row.summary(), row.publishedAt(), row.updatedAt(), null, "<p>" + esc(row.summary()) + "</p>",
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("学习组合", "/english/bundles")), "CollectionPage", Map.of());
    }

    private SeoPage writingResource(String path, String slug) {
        WritingSeoPort.Resource row = writing.publishedResource(slug);
        if (row == null) return null;
        return articlePage(path, row.title(), row.summary(), row.body(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()), "写作资源", "/english/writing", "Article");
    }

    private SeoPage writingPrompt(String path, String slug) {
        WritingSeoPort.Prompt row = writing.publishedPrompt(slug);
        if (row == null) return null;
        return page(path, row.title(), row.summary(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()),
                markdown.render(row.background()) + markdown.render(row.requirements()),
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("英语写作", "/english/writing")), "Article", Map.of());
    }

    private SeoPage project(String path, String slug) {
        PortfolioSeoPort.Project row = projects.publishedProject(slug);
        if (row == null) return null;
        Map<String, Object> extra = new LinkedHashMap<>();
        if (row.repositoryUrl() != null) extra.put("codeRepository", row.repositoryUrl());
        if (row.demoUrl() != null) extra.put("url", row.demoUrl());
        return page(path, row.title() + " | 项目作品", row.summary(), row.publishedAt(), row.updatedAt(), media.publicUrl(row.coverMediaId()),
                markdown.render(row.body()), List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("作品", "/portfolio")), "SoftwareSourceCode", extra);
    }

    private SeoPage articlePage(String path, String title, String summary, String body, LocalDateTime published, LocalDateTime updated, String image, String section, String sectionPath, String schema) {
        return page(path, title, first(summary, title), published, updated, image, markdown.render(body),
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb(section, sectionPath)), schema, Map.of());
    }

    private SeoPage listing(String path, String title, String description, String prefix, List<ContentRow> rows) {
        return new SeoPage(path, title, description, "index,follow", "website", "CollectionPage", null, null, newest(rows), cards(prefix, rows), List.of(new SeoBreadcrumb("首页", "/")), Map.of());
    }

    private SeoPage staticPage(String path, String title, String description, String robots, String body) {
        return staticPage(path, title, description, robots, body, "WebPage");
    }

    private SeoPage staticPage(String path, String title, String description, String robots, String body, String schema) {
        return new SeoPage(path, title, description, robots, "website", schema, null, null, null, body, List.of(), Map.of());
    }

    private SeoPage page(String path, String title, String description, LocalDateTime published, LocalDateTime updated, String image, String body, List<SeoBreadcrumb> crumbs, String schema, Map<String, Object> extras) {
        return new SeoPage(path, title, description, "index,follow", schema.equals("Article") ? "article" : "website", schema, image, published, updated, body, crumbs, extras);
    }

    private List<ContentRow> cards(List<TutorialSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> blogCards(List<BlogSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> projectCards(List<PortfolioSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> grammarCards(List<GrammarSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> readingCards(List<ReadingSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> listeningCards(List<ListeningSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> writingCards(List<WritingSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }
    private List<ContentRow> bundleCards(List<BundleSeoPort.Card> rows) { return rows.stream().map(row -> new ContentRow(row.title(), row.slug(), row.summary(), row.updatedAt())).toList(); }

    private String cards(String prefix, List<ContentRow> rows) {
        StringBuilder html = new StringBuilder("<section class=\"seo-list\">");
        for (ContentRow row : rows) html.append("<article><h2><a href=\"").append(esc(prefix + row.slug())).append("\">").append(esc(row.title())).append("</a></h2><p>").append(esc(row.summary())).append("</p></article>");
        return html.append("</section>").toString();
    }

    private String links(String... values) {
        StringBuilder html = new StringBuilder("<nav aria-label=\"主要内容\"><ul>");
        for (int i = 0; i + 1 < values.length; i += 2) html.append("<li><a href=\"").append(esc(values[i + 1])).append("\">").append(esc(values[i])).append("</a></li>");
        return html.append("</ul></nav>").toString();
    }

    private LocalDateTime newest(List<ContentRow> rows) { return rows.stream().map(ContentRow::updatedAt).filter(v -> v != null).max(LocalDateTime::compareTo).orElse(null); }
    private String esc(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
    private String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return ""; }
    private String normalize(String value) { if (value == null || value.isBlank()) return "/"; String path = value.startsWith("/") ? value : "/" + value; return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path; }

    private record ContentRow(String title, String slug, String summary, LocalDateTime updatedAt) {}
}
