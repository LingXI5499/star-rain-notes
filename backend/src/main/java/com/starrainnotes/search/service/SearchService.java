package com.starrainnotes.search.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.search.dto.SearchCountsView;
import com.starrainnotes.search.dto.SearchItemView;
import com.starrainnotes.search.dto.SearchPageView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Global search over the four published content sources (04 §15):
 * TUTORIAL, CHAPTER, BLOG, PORTFOLIO.
 *
 * <p>No ES / Redis / search table — plain indexed LIKE with parameter binding
 * and escaped %/_ wildcards. Ranking: title exact +100, title prefix +80,
 * title contains +60, summary +30, body +10; ties break by activityAt DESC
 * then id DESC. activityAt: Tutorial/Chapter/Portfolio = updatedAt,
 * Blog = publishedAt. `counts` ignores the type filter; `total` applies it.
 * type=tutorial includes Tutorial + Chapter.</p>
 */
@Service
public class SearchService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 50;

    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public SearchService(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public SearchPageView search(String rawQuery, String type, int page, int pageSize) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2 || query.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Invalid search query", "q must be between 2 and 100 characters after trimming.");
        }
        String pattern = "%" + escapeLike(query) + "%";

        List<Candidate> tutorials = searchTutorials(pattern, query);
        List<Candidate> chapters = searchChapters(pattern, query);
        List<Candidate> blogs = searchBlogs(pattern, query);
        List<Candidate> portfolios = searchPortfolios(pattern, query);

        SearchCountsView counts = new SearchCountsView(
                tutorials.size(), chapters.size(), blogs.size(), portfolios.size());

        boolean includeTutorial = type == null || type.isBlank() || "tutorial".equals(type);
        boolean includeBlog = type == null || type.isBlank() || "blog".equals(type);
        boolean includePortfolio = type == null || type.isBlank() || "portfolio".equals(type);

        List<Candidate> all = new ArrayList<>();
        if (includeTutorial) {
            all.addAll(tutorials);
            all.addAll(chapters);
        }
        if (includeBlog) {
            all.addAll(blogs);
        }
        if (includePortfolio) {
            all.addAll(portfolios);
        }

        all.sort(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparing(Candidate::activityAt, Comparator.reverseOrder())
                .thenComparing(Candidate::id, Comparator.reverseOrder()));

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int total = all.size();
        int from = (safePage - 1) * safeSize;
        List<SearchItemView> items = all.stream()
                .skip(from)
                .limit(safeSize)
                .map(this::toView)
                .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new SearchPageView(items, counts, total, safePage, safeSize, totalPages);
    }

    // ---------------------------------------------------------------
    // sources (parameter-bound LIKE with escaped wildcards)
    // ---------------------------------------------------------------

    private List<Candidate> searchTutorials(String pattern, String query) {
        return jdbc.query("""
                SELECT id, title, summary, slug, updated_at
                FROM tutorial
                WHERE publish_status = 'PUBLISHED'
                  AND (title LIKE ? OR summary LIKE ?)
                """, (rs, rowNum) -> {
            String title = rs.getString("title");
            String summary = rs.getString("summary");
            return new Candidate("TUTORIAL", rs.getLong("id"), title, summary, rs.getString("slug"),
                    null, null, rs.getTimestamp("updated_at").toLocalDateTime(),
                    score(query, title, summary, null));
        }, pattern, pattern);
    }

    private List<Candidate> searchChapters(String pattern, String query) {
        return jdbc.query("""
                SELECT n.id, n.title, n.summary, n.body_markdown, n.slug AS chapter_slug,
                       t.slug AS tutorial_slug, n.updated_at
                FROM tutorial_node n
                JOIN tutorial t ON t.id = n.tutorial_id
                WHERE t.publish_status = 'PUBLISHED'
                  AND n.node_type = 'CHAPTER'
                  AND n.publish_status = 'PUBLISHED'
                  AND (n.title LIKE ? OR n.summary LIKE ? OR n.body_markdown LIKE ?)
                """, (rs, rowNum) -> {
            String title = rs.getString("title");
            String summary = rs.getString("summary");
            String body = rs.getString("body_markdown");
            return new Candidate("CHAPTER", rs.getLong("id"), title, summary, null,
                    rs.getString("tutorial_slug"), rs.getString("chapter_slug"),
                    rs.getTimestamp("updated_at").toLocalDateTime(),
                    score(query, title, summary, body));
        }, pattern, pattern, pattern);
    }

    private List<Candidate> searchBlogs(String pattern, String query) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, published_at
                FROM blog_post
                WHERE publish_status = 'PUBLISHED'
                  AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> {
            String title = rs.getString("title");
            String summary = rs.getString("summary");
            String body = rs.getString("body_markdown");
            return new Candidate("BLOG", rs.getLong("id"), title, summary, rs.getString("slug"),
                    null, null, rs.getTimestamp("published_at").toLocalDateTime(),
                    score(query, title, summary, body));
        }, pattern, pattern, pattern);
    }

    private List<Candidate> searchPortfolios(String pattern, String query) {
        return jdbc.query("""
                SELECT id, title, summary, body_markdown, slug, updated_at
                FROM portfolio_project
                WHERE publish_status = 'PUBLISHED'
                  AND (title LIKE ? OR summary LIKE ? OR body_markdown LIKE ?)
                """, (rs, rowNum) -> {
            String title = rs.getString("title");
            String summary = rs.getString("summary");
            String body = rs.getString("body_markdown");
            return new Candidate("PORTFOLIO", rs.getLong("id"), title, summary, rs.getString("slug"),
                    null, null, rs.getTimestamp("updated_at").toLocalDateTime(),
                    score(query, title, summary, body));
        }, pattern, pattern, pattern);
    }

    // ---------------------------------------------------------------
    // ranking / helpers
    // ---------------------------------------------------------------

    private int score(String query, String title, String summary, String body) {
        String q = query.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        String s = summary == null ? "" : summary.toLowerCase(Locale.ROOT);
        String b = body == null ? "" : body.toLowerCase(Locale.ROOT);
        int score = 0;
        if (t.equals(q)) {
            score += 100;
        } else if (t.startsWith(q)) {
            score += 80;
        } else if (t.contains(q)) {
            score += 60;
        }
        if (s.contains(q)) {
            score += 30;
        }
        if (b.contains(q)) {
            score += 10;
        }
        return score;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private SearchItemView toView(Candidate c) {
        return new SearchItemView(c.type(), c.id(), c.title(), c.summary(), c.slug(),
                c.tutorialSlug(), c.chapterSlug(), formatUtc(c.activityAt()), c.score());
    }

    private String formatUtc(LocalDateTime utc) {
        return timezone.atSite(utc).format(ISO_OFFSET);
    }

    private record Candidate(String type, Long id, String title, String summary, String slug,
                             String tutorialSlug, String chapterSlug, LocalDateTime activityAt, int score) {
    }
}
