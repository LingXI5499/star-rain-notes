package com.starrainnotes.seo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.util.HtmlUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SeoContentRepository {
    private final JdbcTemplate jdbc;
    private final SeoMarkdownRenderer markdown;
    private final BoundedSeoCache<SiteIdentity> identity = new BoundedSeoCache<>(1, 300_000L, System::currentTimeMillis);
    private final BoundedSeoCache<String> author = new BoundedSeoCache<>(1, 300_000L, System::currentTimeMillis);

    public SeoContentRepository(JdbcTemplate jdbc, SeoMarkdownRenderer markdown) {
        this.jdbc = jdbc;
        this.markdown = markdown;
    }

    public SiteIdentity site() {
        return identity.get("site", () -> jdbc.query("SELECT site_name,tagline,default_seo_description,github_url FROM site_setting WHERE id=1",
                rs -> rs.next() ? new SiteIdentity(rs.getString(1), first(rs.getString(2), rs.getString(3), "建立自己的知识世界"), rs.getString(4))
                        : new SiteIdentity("星雨笔录", "建立自己的知识世界", null)));
    }

    public String authorName() {
        return author.get("author", () -> jdbc.query("SELECT display_name FROM profile WHERE id=1", rs -> rs.next() ? first(rs.getString(1), "零燨") : "零燨"));
    }

    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @org.springframework.core.annotation.Order(-100)
    public void contentChanged(SeoContentChangedEvent ignored) {
        identity.clear();
        author.clear();
    }

    public SeoPage resolve(String rawPath) {
        String path = normalize(rawPath);
        return switch (path) {
            case "/" -> home();
            case "/tutorials" -> listing(path, "Java 全栈教程", "系统化整理 Java 全栈、计算机基础与软件工程学习路径。", "tutorial", "/tutorials/", tutorials());
            case "/blog" -> listing(path, "技术博客", "记录 Java、软件工程与智能体开发中的实践、思考与复盘。", "blog_post", "/blog/", rows("SELECT title,slug,summary,updated_at FROM blog_post WHERE publish_status='PUBLISHED' ORDER BY published_at DESC,id DESC"));
            case "/portfolio" -> listing(path, "项目作品", "从问题、设计、实现到部署复盘的真实项目案例。", "portfolio_project", "/portfolio/", rows("SELECT title,slug,summary,updated_at FROM portfolio_project WHERE publish_status='PUBLISHED' ORDER BY featured DESC,sort_order,id"));
            case "/english" -> englishHome();
            case "/english/grammar" -> listing(path, "英语语法完整教程", "从词法到复杂句法，系统建立英语语法知识框架。", "english_grammar_lesson", "/english/grammar/", rows("SELECT l.title,l.slug,l.summary,l.updated_at FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id=l.course_id WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED' ORDER BY l.sort_order,l.id"));
            case "/english/reading" -> listing(path, "英语阅读中心", "按 CEFR 与能力层级组织的原创分级精读内容。", "english_reading_article", "/english/reading/", rows("SELECT title,slug,summary,updated_at FROM english_reading_article WHERE publish_status='PUBLISHED' ORDER BY sort_order,id"));
            case "/english/listening" -> listing(path, "英语听力中心", "完整音频、逐句片段与练习组成的分层精听内容。", "english_listening_item", "/english/listening/", rows("SELECT title,slug,summary,updated_at FROM english_listening_item WHERE publish_status='PUBLISHED' ORDER BY sort_order,id"));
            case "/english/listening/pronunciation" -> listing(path, "英语语音规则", "连读、弱读、同化、省音、重音与语调的系统训练。", "english_listening_pronunciation_rule", "/english/listening/pronunciation/", rows("SELECT title,slug,summary,updated_at FROM english_listening_pronunciation_rule WHERE publish_status='PUBLISHED' ORDER BY sort_order,id"));
            case "/english/writing" -> writingHome();
            case "/english/bundles" -> listing(path, "英语学习组合", "串联阅读、听力与写作的主题学习路径。", "english_learning_bundle", "/english/bundles/", rows("SELECT title,slug,summary,updated_at FROM english_learning_bundle WHERE publish_status='PUBLISHED' ORDER BY sort_order,id"));
            case "/about" -> about();
            case "/search" -> staticPage(path, "站内搜索", "搜索星雨笔录中的教程、博客与作品。", "noindex,follow", links("教程", "/tutorials", "博客", "/blog", "作品", "/portfolio"));
            case "/english/progress" -> staticPage(path, "学习进度", "个人英语学习进度与复习建议。", "noindex,nofollow", "<p>此页面包含个人学习状态，需要登录后使用。</p>");
            case "/english/vocabulary" -> staticPage(path, "英语词汇", "按主题整理的英语词汇库。", "noindex,follow", "<p>词汇主题页面当前不作为独立搜索入口。</p>");
            default -> dynamic(path);
        };
    }

    private SeoPage dynamic(String path) {
        String[] parts = path.substring(1).split("/");
        if (parts.length == 2 && parts[0].equals("blog")) return article(path, "blog_post", parts[1], "博客", "/blog", "Article");
        if (parts.length == 2 && parts[0].equals("portfolio")) return project(path, parts[1]);
        if (parts.length == 2 && parts[0].equals("tutorials")) return tutorial(path, parts[1]);
        if (parts.length == 3 && parts[0].equals("tutorials")) return chapter(path, parts[1], parts[2]);
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("grammar")) return article(path, "english_grammar_lesson", parts[2], "英语语法", "/english/grammar", "Article");
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("reading")) return article(path, "english_reading_article", parts[2], "英语阅读", "/english/reading", "Article");
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("listening") && !parts[2].equals("pronunciation")) return listening(path, parts[2]);
        if (parts.length == 4 && parts[0].equals("english") && parts[1].equals("listening") && parts[2].equals("pronunciation")) return article(path, "english_listening_pronunciation_rule", parts[3], "语音规则", "/english/listening/pronunciation", "Article");
        if (parts.length == 3 && parts[0].equals("english") && parts[1].equals("bundles")) return simpleContent(path, "english_learning_bundle", parts[2], "学习组合", "/english/bundles", "CollectionPage");
        if (parts.length == 4 && parts[0].equals("english") && parts[1].equals("writing") && parts[2].equals("resources")) return article(path, "english_writing_resource", parts[3], "写作资源", "/english/writing", "Article");
        if (parts.length == 4 && parts[0].equals("english") && parts[1].equals("writing") && parts[2].equals("practice")) return writingPrompt(path, parts[3]);
        if (path.startsWith("/english/vocabulary/")) return staticPage(path, "词汇主题", "英语主题词汇。", "noindex,follow", "<p>该主题页暂不纳入搜索索引。</p>");
        return null;
    }

    private SeoPage home() {
        String body = "<section><h2>系统学习与真实实践</h2>" + links("教程", "/tutorials", "博客", "/blog", "作品", "/portfolio", "英语", "/english") + "</section>";
        return staticPage("/", "星雨笔录 | Java 全栈、计算机基础与技术学习笔记", site().tagline(), "index,follow", body, "WebSite");
    }

    private SeoPage englishHome() {
        return staticPage("/english", "英语学习", "从语法、词汇到阅读、听力与写作的长期学习路线。", "index,follow",
                links("语法", "/english/grammar", "阅读", "/english/reading", "听力", "/english/listening", "写作", "/english/writing", "学习组合", "/english/bundles"));
    }

    private SeoPage writingHome() {
        List<ContentRow> resources = rows("SELECT title,slug,summary,updated_at FROM english_writing_resource WHERE publish_status='PUBLISHED' ORDER BY sort_order,id");
        List<ContentRow> prompts = rows("SELECT title,slug,summary,updated_at FROM english_writing_prompt WHERE publish_status='PUBLISHED' ORDER BY sort_order,id");
        String body = cards("/english/writing/resources/", resources) + cards("/english/writing/practice/", prompts);
        return page("/english/writing", "英语写作中心", "表达训练、范文、模板与结构化写作任务。", null, null, null, body,
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("英语", "/english")), "CollectionPage", Map.of());
    }

    private SeoPage about() {
        return jdbc.query("SELECT display_name,headline,bio,github_url,technical_direction_markdown,journey_markdown,updated_at FROM profile WHERE id=1", rs -> {
            if (!rs.next()) return staticPage("/about", "关于我", "关于作者与星雨笔录。", "index,follow", "<p>作者资料尚未公开。</p>");
            String name = first(rs.getString("display_name"), "零燨");
            String description = first(rs.getString("headline"), rs.getString("bio"), "关于作者与星雨笔录。");
            String body = "<p>" + esc(rs.getString("bio")) + "</p>" + markdown.render(rs.getString("technical_direction_markdown")) + markdown.render(rs.getString("journey_markdown"));
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("name", name);
            if (rs.getString("github_url") != null) extras.put("sameAs", List.of(rs.getString("github_url")));
            return page("/about", "关于我", description, null, rs.getTimestamp("updated_at"), null, body,
                    List.of(new SeoBreadcrumb("首页", "/")), "ProfilePage", extras);
        });
    }

    private SeoPage tutorial(String path, String slug) {
        String sql = """
                SELECT t.title,t.summary,t.published_at,t.updated_at,m.public_url,c.name category_name
                FROM tutorial t JOIN tutorial_category c ON c.id=t.category_id
                LEFT JOIN media_asset m ON m.id=t.cover_media_id
                WHERE t.slug=? AND t.publish_status='PUBLISHED'
                """;
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return null;
            List<ContentRow> chapters = jdbc.query("SELECT title,slug,summary,updated_at FROM tutorial_node WHERE tutorial_id=(SELECT id FROM tutorial WHERE slug=?) AND node_type='CHAPTER' AND publish_status='PUBLISHED' ORDER BY sort_order,id", this::mapRows, slug);
            return page(path, rs.getString("title") + "教程", rs.getString("summary"), rs.getTimestamp("published_at"), rs.getTimestamp("updated_at"), rs.getString("public_url"), cards(path + "/", chapters),
                    List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("教程", "/tutorials")), "CollectionPage", Map.of("about", rs.getString("category_name")));
        }, slug);
    }

    private SeoPage chapter(String path, String tutorialSlug, String chapterSlug) {
        String sql = """
                SELECT n.title,n.summary,n.body_markdown,n.published_at,n.updated_at,t.title tutorial_title
                FROM tutorial_node n JOIN tutorial t ON t.id=n.tutorial_id
                WHERE t.slug=? AND t.publish_status='PUBLISHED' AND n.slug=? AND n.node_type='CHAPTER' AND n.publish_status='PUBLISHED'
                """;
        return jdbc.query(sql, rs -> rs.next() ? page(path, rs.getString("title") + " | " + rs.getString("tutorial_title"), first(rs.getString("summary"), rs.getString("title")), rs.getTimestamp("published_at"), rs.getTimestamp("updated_at"), null,
                markdown.render(rs.getString("body_markdown")), List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("教程", "/tutorials"), new SeoBreadcrumb(rs.getString("tutorial_title"), "/tutorials/" + tutorialSlug)), "Article", Map.of()) : null, tutorialSlug, chapterSlug);
    }

    private SeoPage article(String path, String table, String slug, String section, String sectionPath, String schema) {
        String bodyColumn = table.equals("english_listening_pronunciation_rule") || table.equals("english_grammar_lesson") || table.equals("english_reading_article") || table.equals("english_writing_resource") || table.equals("blog_post") ? "body_markdown" : "NULL";
        String coverJoin = switch (table) {
            case "blog_post", "english_reading_article", "english_writing_resource" -> " LEFT JOIN media_asset m ON m.id=x.cover_media_id ";
            default -> "";
        };
        String cover = coverJoin.isBlank() ? "NULL" : "m.public_url";
        String sql = "SELECT x.title,x.summary," + bodyColumn + " body,x.published_at,x.updated_at," + cover + " image FROM " + table + " x" + coverJoin + " WHERE x.slug=? AND x.publish_status='PUBLISHED'";
        return jdbc.query(sql, rs -> rs.next() ? page(path, rs.getString("title"), first(rs.getString("summary"), rs.getString("title")), rs.getTimestamp("published_at"), rs.getTimestamp("updated_at"), rs.getString("image"), markdown.render(rs.getString("body")),
                List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb(section, sectionPath)), schema, Map.of()) : null, slug);
    }

    private SeoPage simpleContent(String path, String table, String slug, String section, String sectionPath, String schema) {
        return jdbc.query("SELECT title,summary,published_at,updated_at FROM " + table + " WHERE slug=? AND publish_status='PUBLISHED'", rs -> rs.next() ? page(path, rs.getString(1), rs.getString(2), rs.getTimestamp(3), rs.getTimestamp(4), null, "<p>" + esc(rs.getString(2)) + "</p>", List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb(section, sectionPath)), schema, Map.of()) : null, slug);
    }

    private SeoPage listening(String path, String slug) {
        String sql = "SELECT i.title,i.summary,i.transcript_markdown,i.published_at,i.updated_at,m.public_url FROM english_listening_item i LEFT JOIN media_asset m ON m.id=i.cover_media_id WHERE i.slug=? AND i.publish_status='PUBLISHED'";
        return jdbc.query(sql, rs -> rs.next() ? page(path, rs.getString(1), rs.getString(2), rs.getTimestamp(4), rs.getTimestamp(5), rs.getString(6), markdown.render(rs.getString(3)), List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("英语听力", "/english/listening")), "Article", Map.of()) : null, slug);
    }

    private SeoPage project(String path, String slug) {
        String sql = "SELECT p.title,p.summary,p.body_markdown,p.published_at,p.updated_at,m.public_url,p.repository_url,p.demo_url FROM portfolio_project p LEFT JOIN media_asset m ON m.id=p.cover_media_id WHERE p.slug=? AND p.publish_status='PUBLISHED'";
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return null;
            Map<String, Object> extra = new LinkedHashMap<>();
            if (rs.getString("repository_url") != null) extra.put("codeRepository", rs.getString("repository_url"));
            if (rs.getString("demo_url") != null) extra.put("url", rs.getString("demo_url"));
            return page(path, rs.getString("title") + " | 项目作品", rs.getString("summary"), rs.getTimestamp("published_at"), rs.getTimestamp("updated_at"), rs.getString("public_url"), markdown.render(rs.getString("body_markdown")), List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("作品", "/portfolio")), "SoftwareSourceCode", extra);
        }, slug);
    }

    private SeoPage writingPrompt(String path, String slug) {
        String sql = "SELECT p.title,p.summary,p.background_markdown,p.requirements_markdown,p.published_at,p.updated_at,m.public_url FROM english_writing_prompt p LEFT JOIN media_asset m ON m.id=p.cover_media_id WHERE p.slug=? AND p.publish_status='PUBLISHED'";
        return jdbc.query(sql, rs -> rs.next() ? page(path, rs.getString(1), rs.getString(2), rs.getTimestamp(5), rs.getTimestamp(6), rs.getString(7), markdown.render(rs.getString(3)) + markdown.render(rs.getString(4)), List.of(new SeoBreadcrumb("首页", "/"), new SeoBreadcrumb("英语写作", "/english/writing")), "Article", Map.of()) : null, slug);
    }

    private SeoPage listing(String path, String title, String description, String table, String prefix, List<ContentRow> rows) {
        return new SeoPage(path, title, description, "index,follow", "website", "CollectionPage", null,
                null, newest(rows), cards(prefix, rows), List.of(new SeoBreadcrumb("首页", "/")), Map.of());
    }

    private SeoPage staticPage(String path, String title, String description, String robots, String body) {
        return staticPage(path, title, description, robots, body, "WebPage");
    }

    private SeoPage staticPage(String path, String title, String description, String robots, String body, String schema) {
        return new SeoPage(path, title, description, robots, "website", schema, null, null, null, body, List.of(), Map.of());
    }

    private SeoPage page(String path, String title, String description, Timestamp published, Timestamp updated, String image, String body, List<SeoBreadcrumb> crumbs, String schema, Map<String, Object> extras) {
        return new SeoPage(path, title, description, "index,follow", schema.equals("Article") ? "article" : "website", schema, image, time(published), time(updated), body, crumbs, extras);
    }

    private List<ContentRow> tutorials() {
        return rows("SELECT t.title,t.slug,t.summary,t.updated_at FROM tutorial t WHERE t.publish_status='PUBLISHED' ORDER BY t.sort_order,t.id");
    }

    private List<ContentRow> rows(String sql) { return jdbc.query(sql, this::mapRows); }
    private List<ContentRow> mapRows(ResultSet rs) throws SQLException {
        List<ContentRow> result = new ArrayList<>();
        while (rs.next()) result.add(new ContentRow(rs.getString(1), rs.getString(2), rs.getString(3), time(rs.getTimestamp(4))));
        return result;
    }

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
    private LocalDateTime time(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }
    private String esc(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
    private String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return ""; }
    private String normalize(String value) { if (value == null || value.isBlank()) return "/"; String path = value.startsWith("/") ? value : "/" + value; return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path; }

    public record SiteIdentity(String name, String tagline, String githubUrl) {}
    private record ContentRow(String title, String slug, String summary, LocalDateTime updatedAt) {}
}
