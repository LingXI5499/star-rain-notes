package com.starrainnotes.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.blog.dto.AdminPostDetailView;
import com.starrainnotes.blog.dto.AdminPostPageView;
import com.starrainnotes.blog.dto.AdminPostSummaryView;
import com.starrainnotes.blog.dto.ArchiveMonthView;
import com.starrainnotes.blog.dto.ArchiveYearView;
import com.starrainnotes.blog.dto.BlogTagView;
import com.starrainnotes.blog.dto.CalendarDayView;
import com.starrainnotes.blog.dto.CalendarView;
import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.PrevNextPostView;
import com.starrainnotes.blog.dto.PublicPostDetailView;
import com.starrainnotes.blog.dto.PublicPostPageView;
import com.starrainnotes.blog.dto.PublicPostSummaryView;
import com.starrainnotes.blog.dto.PublicTagView;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.entity.BlogPost;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Blog vertical slice (04 §11).
 *
 * <p>Time semantics (01 §3.2): created_at = draft creation; published_at =
 * FIRST public publish, immutable across edit/withdraw/republish; updated_at =
 * last modification. Timeline / Calendar / Archive / Prev-Next all use the
 * first publishedAt, in the site timezone.</p>
 *
 * <p>Post update + tag relations are committed in one transaction; relations
 * are replaced (delete old, insert new) as the frozen spec prescribes.</p>
 */
@Service
public class BlogService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";

    private final BlogPostMapper postMapper;
    private final BlogTagMapper tagMapper;
    private final BlogTagService tagService;
    private final MediaAssetMapper mediaAssetMapper;
    private final SiteSettingsTimezone timezone;
    private final JdbcTemplate jdbc;

    public BlogService(BlogPostMapper postMapper, BlogTagMapper tagMapper, BlogTagService tagService,
                       MediaAssetMapper mediaAssetMapper, SiteSettingsTimezone timezone, JdbcTemplate jdbc) {
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.tagService = tagService;
        this.mediaAssetMapper = mediaAssetMapper;
        this.timezone = timezone;
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------
    // admin
    // ---------------------------------------------------------------

    public AdminPostPageView adminList(int page, int pageSize, String status, String tag, String query) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<BlogPost>()
                .eq(status != null && !status.isBlank(), BlogPost::getPublishStatus, status)
                .and(query != null && !query.isBlank(), w -> w
                        .like(BlogPost::getTitle, query).or().like(BlogPost::getSlug, query))
                .orderByDesc(BlogPost::getUpdatedAt)
                .orderByDesc(BlogPost::getId);
        if (tag != null && !tag.isBlank()) {
            List<Long> taggedPostIds = jdbc.query("""
                    SELECT bt.blog_post_id FROM blog_post_tag bt
                    JOIN blog_tag t ON t.id = bt.blog_tag_id
                    WHERE t.slug = ?
                    """, (rs, rowNum) -> rs.getLong(1), tag);
            wrapper.in(BlogPost::getId, taggedPostIds.isEmpty() ? List.of(-1L) : taggedPostIds);
        }

        Long total = postMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<BlogPost> rows = postMapper.selectList(wrapper);
        Map<Long, List<BlogTagView>> tagsByPost = tagsForAdminPosts(rows.stream().map(BlogPost::getId).toList());
        Map<Long, String> covers = coverUrls(rows.stream().map(BlogPost::getCoverMediaId).toList());
        List<AdminPostSummaryView> items = rows.stream()
                .map(p -> new AdminPostSummaryView(
                        p.getId(), p.getTitle(), p.getSlug(), p.getSummary(),
                        p.getCoverMediaId() == null ? null : covers.get(p.getCoverMediaId()),
                        p.getPublishStatus(), formatUtc(p.getPublishedAt()), formatUtc(p.getUpdatedAt()),
                        tagsByPost.getOrDefault(p.getId(), List.of())))
                .toList();

        long safeTotal = total == null ? 0 : total;
        int totalPages = safeTotal == 0 ? 0 : (int) ((safeTotal + safeSize - 1) / safeSize);
        return new AdminPostPageView(items, safePage, safeSize, safeTotal, totalPages);
    }

    public AdminPostDetailView adminDetail(Long postId) {
        BlogPost post = requirePost(postId);
        return toAdminDetail(post);
    }

    @Transactional
    public AdminPostDetailView create(CreatePostRequest request) {
        assertSlugFree(request.slug(), null);
        List<Long> tagIds = resolveTagIds(request.tagIds(), request.tagNames());

        BlogPost post = new BlogPost();
        applyFields(post, request.title(), request.slug(), request.summary(), request.bodyMarkdown(),
                request.coverMediaId(), request.seoTitle(), request.seoDescription());
        post.setPublishStatus(DRAFT);
        post.setPublishedAt(null);
        postMapper.insert(post);
        replaceTags(post.getId(), tagIds);
        return toAdminDetail(post);
    }

    @Transactional
    public AdminPostDetailView update(Long postId, UpdatePostRequest request) {
        BlogPost post = requirePost(postId);
        assertSlugFree(request.slug(), postId);
        List<Long> tagIds = resolveTagIds(request.tagIds(), request.tagNames());

        applyFields(post, request.title(), request.slug(), request.summary(), request.bodyMarkdown(),
                request.coverMediaId(), request.seoTitle(), request.seoDescription());
        // publishStatus / publishedAt are never touched by a plain update
        postMapper.updateById(post);
        replaceTags(post.getId(), tagIds);
        return toAdminDetail(post);
    }

    public void delete(Long postId) {
        requirePost(postId);
        // blog_post_tag relations cascade on delete (frozen FK)
        postMapper.deleteById(postId);
    }

    public AdminPostDetailView publish(Long postId) {
        BlogPost post = requirePost(postId);
        if (!PUBLISHED.equals(post.getPublishStatus())) {
            if (post.getPublishedAt() == null) {
                post.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            }
            post.setPublishStatus(PUBLISHED);
            postMapper.updateById(post);
        }
        return toAdminDetail(post);
    }

    public AdminPostDetailView withdraw(Long postId) {
        BlogPost post = requirePost(postId);
        if (DRAFT.equals(post.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                    "Cannot withdraw a draft", "Only published posts can be withdrawn.");
        }
        if (!WITHDRAWN.equals(post.getPublishStatus())) {
            post.setPublishStatus(WITHDRAWN);
            postMapper.updateById(post);
        }
        return toAdminDetail(post);
    }

    // ---------------------------------------------------------------
    // public timeline / filters
    // ---------------------------------------------------------------

    public PublicPostPageView publicList(String tag, String date, String month, int page, int pageSize) {
        if (date != null && !date.isBlank() && month != null && !month.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FILTER_COMBINATION",
                    "Invalid filter combination", "date and month filters cannot be combined.");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getPublishStatus, PUBLISHED);
        if (tag != null && !tag.isBlank()) {
            List<Long> ids = jdbc.query("""
                    SELECT bt.blog_post_id FROM blog_post_tag bt
                    JOIN blog_tag t ON t.id = bt.blog_tag_id
                    WHERE t.slug = ?
                    """, (rs, rowNum) -> rs.getLong(1), tag);
            wrapper.in(BlogPost::getId, ids.isEmpty() ? List.of(-1L) : ids);
        }
        if (date != null && !date.isBlank()) {
            LocalDateTime[] bounds = dayBounds(parseDate(date));
            wrapper.ge(BlogPost::getPublishedAt, bounds[0]).lt(BlogPost::getPublishedAt, bounds[1]);
        }
        if (month != null && !month.isBlank()) {
            LocalDateTime[] bounds = monthBounds(parseMonth(month));
            wrapper.ge(BlogPost::getPublishedAt, bounds[0]).lt(BlogPost::getPublishedAt, bounds[1]);
        }
        wrapper.orderByDesc(BlogPost::getPublishedAt).orderByDesc(BlogPost::getId);

        Long total = postMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<BlogPost> rows = postMapper.selectList(wrapper);

        Map<Long, List<PublicTagView>> tagsByPost = tagsForPosts(rows.stream().map(BlogPost::getId).toList());
        Map<Long, String> covers = coverUrls(rows.stream().map(BlogPost::getCoverMediaId).toList());

        List<PublicPostSummaryView> items = rows.stream()
                .map(p -> new PublicPostSummaryView(
                        p.getId(), p.getTitle(), p.getSlug(), p.getSummary(),
                        p.getCoverMediaId() == null ? null : covers.get(p.getCoverMediaId()),
                        formatUtc(p.getPublishedAt()), formatUtc(p.getUpdatedAt()),
                        tagsByPost.getOrDefault(p.getId(), List.of())))
                .toList();

        long safeTotal = total == null ? 0 : total;
        int totalPages = safeTotal == 0 ? 0 : (int) ((safeTotal + safeSize - 1) / safeSize);
        return new PublicPostPageView(items, safePage, safeSize, safeTotal, totalPages);
    }

    public PublicPostDetailView publicDetail(String slug) {
        BlogPost post = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getSlug, slug));
        if (post == null || !PUBLISHED.equals(post.getPublishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BLOG_POST_NOT_FOUND",
                    "Post not found", "The post does not exist or is not public.");
        }
        List<PublicTagView> tags = tagsOf(post.getId()).stream()
                .map(t -> new PublicTagView(t.id(), t.name(), t.slug()))
                .toList();
        return new PublicPostDetailView(
                post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), post.getBodyMarkdown(),
                coverUrl(post.getCoverMediaId()), tags,
                post.getSeoTitle(), post.getSeoDescription(),
                formatUtc(post.getPublishedAt()), formatUtc(post.getUpdatedAt()),
                previousOf(post), nextOf(post));
    }

    public CalendarView calendar(String month) {
        YearMonth yearMonth = parseMonth(month);
        LocalDateTime[] bounds = monthBounds(yearMonth);
        List<BlogPost> posts = postMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getPublishStatus, PUBLISHED)
                .ge(BlogPost::getPublishedAt, bounds[0])
                .lt(BlogPost::getPublishedAt, bounds[1]));
        Map<LocalDate, Long> counts = posts.stream()
                .collect(Collectors.groupingBy(p -> timezone.atSite(p.getPublishedAt()).toLocalDate(), Collectors.counting()));
        List<CalendarDayView> days = counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new CalendarDayView(e.getKey().toString(), e.getValue()))
                .toList();
        return new CalendarView(month, days);
    }

    public List<ArchiveYearView> archive() {
        List<BlogPost> posts = postMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getPublishStatus, PUBLISHED));
        Map<Integer, Map<YearMonth, Long>> byYear = new TreeMapDesc();
        for (BlogPost post : posts) {
            var site = timezone.atSite(post.getPublishedAt());
            int year = site.getYear();
            YearMonth ym = YearMonth.of(site.getYear(), site.getMonthValue());
            byYear.computeIfAbsent(year, k -> new LinkedHashMap<>())
                    .merge(ym, 1L, Long::sum);
        }
        List<ArchiveYearView> result = new ArrayList<>();
        for (var entry : byYear.entrySet()) {
            List<ArchiveMonthView> months = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<YearMonth, Long>comparingByKey().reversed())
                    .map(e -> new ArchiveMonthView(e.getKey().toString(), e.getValue()))
                    .toList();
            result.add(new ArchiveYearView(entry.getKey(), months));
        }
        return result;
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void applyFields(BlogPost post, String title, String slug, String summary, String bodyMarkdown,
                             Long coverMediaId, String seoTitle, String seoDescription) {
        post.setTitle(title);
        post.setSlug(slug);
        post.setSummary(summary);
        post.setBodyMarkdown(bodyMarkdown);
        post.setCoverMediaId(coverMediaId);
        post.setSeoTitle(seoTitle);
        post.setSeoDescription(seoDescription);
    }

    private BlogPost requirePost(Long postId) {
        BlogPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BLOG_POST_NOT_FOUND",
                    "Post not found", "The blog post does not exist.");
        }
        return post;
    }

    private void assertSlugFree(String slug, Long excludeId) {
        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(BlogPost::getId, excludeId);
        }
        if (postMapper.selectCount(wrapper) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A post with this slug already exists.");
        }
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> distinct = tagIds.stream().distinct().toList();
        List<BlogTag> found = tagMapper.selectBatchIds(distinct);
        if (found.size() != distinct.size()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TAG_NOT_FOUND",
                    "Tag not found", "One or more referenced tags do not exist.");
        }
    }

    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        LinkedHashSet<Long> resolved = new LinkedHashSet<>();
        if (tagIds != null) {
            tagIds.stream().filter(java.util.Objects::nonNull).forEach(resolved::add);
        }
        validateTags(new ArrayList<>(resolved));
        resolved.addAll(tagService.resolveNames(tagNames));
        if (resolved.size() > 20) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TOO_MANY_TAGS",
                    "Too many tags", "每篇文章最多选择 20 个标签。");
        }
        return new ArrayList<>(resolved);
    }

    private void replaceTags(Long postId, List<Long> tagIds) {
        jdbc.update("DELETE FROM blog_post_tag WHERE blog_post_id = ?", postId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds.stream().distinct().toList()) {
            jdbc.update("INSERT INTO blog_post_tag (blog_post_id, blog_tag_id) VALUES (?, ?)", postId, tagId);
        }
    }

    private List<BlogTagView> tagsOf(Long postId) {
        return jdbc.query("""
                SELECT t.id, t.name, t.slug FROM blog_post_tag bt
                JOIN blog_tag t ON t.id = bt.blog_tag_id
                WHERE bt.blog_post_id = ?
                ORDER BY t.name
                """, (rs, rowNum) -> new BlogTagView(rs.getLong("id"), rs.getString("name"), rs.getString("slug")),
                postId);
    }

    private Map<Long, List<BlogTagView>> tagsForAdminPosts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = postIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Map<Long, List<BlogTagView>> result = new HashMap<>();
        jdbc.query("""
                SELECT bt.blog_post_id, t.id, t.name, t.slug FROM blog_post_tag bt
                JOIN blog_tag t ON t.id = bt.blog_tag_id
                WHERE bt.blog_post_id IN (%s)
                ORDER BY t.name
                """.formatted(placeholders),
                (rs, rowNum) -> Map.entry(
                        rs.getLong("blog_post_id"),
                        new BlogTagView(rs.getLong("id"), rs.getString("name"), rs.getString("slug"))),
                postIds.toArray()).forEach(entry -> result
                        .computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(entry.getValue()));
        return result;
    }

    private Map<Long, List<PublicTagView>> tagsForPosts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = postIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Map<Long, List<PublicTagView>> result = new HashMap<>();
        jdbc.query("""
                SELECT bt.blog_post_id, t.id, t.name, t.slug FROM blog_post_tag bt
                JOIN blog_tag t ON t.id = bt.blog_tag_id
                WHERE bt.blog_post_id IN (%s)
                ORDER BY t.name
                """.formatted(placeholders),
                (rs, rowNum) -> Map.entry(
                        rs.getLong("blog_post_id"),
                        new PublicTagView(rs.getLong("id"), rs.getString("name"), rs.getString("slug"))),
                postIds.toArray()).forEach(entry -> result
                        .computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(entry.getValue()));
        return result;
    }

    private Map<Long, String> coverUrls(List<Long> mediaIds) {
        List<Long> distinct = mediaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return mediaAssetMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MediaAsset::getId, MediaAsset::getPublicUrl));
    }

    private String coverUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? null : media.getPublicUrl();
    }

    private PrevNextPostView previousOf(BlogPost post) {
        BlogPost previous = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getPublishStatus, PUBLISHED)
                .and(w -> w.lt(BlogPost::getPublishedAt, post.getPublishedAt())
                        .or(o -> o.eq(BlogPost::getPublishedAt, post.getPublishedAt())
                                .lt(BlogPost::getId, post.getId())))
                .orderByDesc(BlogPost::getPublishedAt)
                .orderByDesc(BlogPost::getId)
                .last("LIMIT 1"));
        return previous == null ? null : toPrevNext(previous);
    }

    private PrevNextPostView nextOf(BlogPost post) {
        BlogPost next = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getPublishStatus, PUBLISHED)
                .and(w -> w.gt(BlogPost::getPublishedAt, post.getPublishedAt())
                        .or(o -> o.eq(BlogPost::getPublishedAt, post.getPublishedAt())
                                .gt(BlogPost::getId, post.getId())))
                .orderByAsc(BlogPost::getPublishedAt)
                .orderByAsc(BlogPost::getId)
                .last("LIMIT 1"));
        return next == null ? null : toPrevNext(next);
    }

    private PrevNextPostView toPrevNext(BlogPost post) {
        return new PrevNextPostView(post.getId(), post.getSlug(), post.getTitle());
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
                    "Invalid date", "The date filter must use the YYYY-MM-DD format.");
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MONTH",
                    "Invalid month", "The month must use the YYYY-MM format.");
        }
    }

    private LocalDateTime[] dayBounds(LocalDate siteDate) {
        var start = siteDate.atStartOfDay(timezone.zone());
        var end = siteDate.plusDays(1).atStartOfDay(timezone.zone());
        return new LocalDateTime[]{
                start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()};
    }

    private LocalDateTime[] monthBounds(YearMonth yearMonth) {
        var start = yearMonth.atDay(1).atStartOfDay(timezone.zone());
        var end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(timezone.zone());
        return new LocalDateTime[]{
                start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()};
    }

    private AdminPostDetailView toAdminDetail(BlogPost post) {
        return new AdminPostDetailView(
                post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), post.getBodyMarkdown(),
                post.getCoverMediaId(), coverUrl(post.getCoverMediaId()), post.getPublishStatus(),
                post.getSeoTitle(), post.getSeoDescription(),
                formatUtc(post.getPublishedAt()), formatUtc(post.getCreatedAt()), formatUtc(post.getUpdatedAt()),
                tagsOf(post.getId()));
    }

    private String formatUtc(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return timezone.atSite(utc).format(ISO_OFFSET);
    }

    /** Map descending by key (year DESC). */
    private static final class TreeMapDesc extends java.util.TreeMap<Integer, Map<YearMonth, Long>> {
        TreeMapDesc() {
            super(Comparator.reverseOrder());
        }
    }
}
