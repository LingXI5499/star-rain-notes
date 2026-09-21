package com.starrainnotes.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.starrainnotes.blog.assembler.BlogPostAssembler;
import com.starrainnotes.blog.entity.BlogPost;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogPostTagMapper;
import com.starrainnotes.blog.mapper.BlogPostTagMapper.BlogPostTagRow;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveYearVO;
import com.starrainnotes.blog.vo.BlogCalendarDayVO;
import com.starrainnotes.blog.vo.BlogCalendarVO;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminPageVO;
import com.starrainnotes.blog.vo.BlogPostAdminSummaryVO;
import com.starrainnotes.blog.vo.BlogPostNeighborVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicPageVO;
import com.starrainnotes.blog.vo.BlogPostPublicSummaryVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-side blog queries, pagination and response assembly. */
@Service
@RequiredArgsConstructor
public class BlogQueryService {
    private final BlogPostMapper postMapper;
    private final BlogPostTagMapper postTagMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final BlogPostAssembler assembler;
    private final SiteSettingsTimezone timezone;

    public BlogPostAdminPageVO adminList(int page, int pageSize, String status, String tag, String query) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<BlogPost> condition = new LambdaQueryWrapper<BlogPost>()
                .eq(status != null && !status.isBlank(), BlogPost::getPublishStatus, status)
                .and(query != null && !query.isBlank(), w -> w.like(BlogPost::getTitle, query).or().like(BlogPost::getSlug, query))
                .orderByDesc(BlogPost::getUpdatedAt).orderByDesc(BlogPost::getId);
        restrictToTag(condition, tag);
        condition.select(BlogPost::getId, BlogPost::getTitle, BlogPost::getSlug, BlogPost::getSummary,
                BlogPost::getCoverMediaId, BlogPost::getPublishStatus, BlogPost::getPublishedAt, BlogPost::getUpdatedAt);
        Page<BlogPost> result = postMapper.selectPage(new Page<>(safePage, safeSize), condition);
        Map<Long, List<BlogTagVO>> tagsByPost = tagsForPosts(result.getRecords().stream().map(BlogPost::getId).toList());
        Map<Long, String> covers = coverUrls(result.getRecords().stream().map(BlogPost::getCoverMediaId).toList());
        List<BlogPostAdminSummaryVO> items = result.getRecords().stream()
                .map(post -> assembler.toAdminSummary(post, tagsByPost.getOrDefault(post.getId(), List.of()), cover(post, covers))).toList();
        return new BlogPostAdminPageVO(items, safePage, safeSize, result.getTotal(), pages(result.getTotal(), safeSize));
    }

    public BlogPostAdminDetailVO adminDetail(Long postId) {
        BlogPost post = requirePost(postId);
        return assembler.toAdminDetail(post, postTagMapper.selectTagsByPostId(postId), coverUrl(post.getCoverMediaId()));
    }

    public String publishStatus(Long postId) {
        return requirePost(postId).getPublishStatus();
    }

    public BlogPostPublicPageVO publicList(String tag, String date, String month, int page, int pageSize) {
        if (date != null && !date.isBlank() && month != null && !month.isBlank()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_FILTER_COMBINATION", "Invalid filter combination", "date and month filters cannot be combined.");
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<BlogPost> condition = new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getPublishStatus, BlogCommandService.PUBLISHED);
        restrictToTag(condition, tag);
        if (date != null && !date.isBlank()) {
            LocalDateTime[] bounds = dayBounds(parseDate(date));
            condition.ge(BlogPost::getPublishedAt, bounds[0]).lt(BlogPost::getPublishedAt, bounds[1]);
        }
        if (month != null && !month.isBlank()) {
            LocalDateTime[] bounds = monthBounds(parseMonth(month));
            condition.ge(BlogPost::getPublishedAt, bounds[0]).lt(BlogPost::getPublishedAt, bounds[1]);
        }
        condition.orderByDesc(BlogPost::getPublishedAt).orderByDesc(BlogPost::getId)
                .select(BlogPost::getId, BlogPost::getTitle, BlogPost::getSlug, BlogPost::getSummary,
                        BlogPost::getCoverMediaId, BlogPost::getPublishedAt, BlogPost::getUpdatedAt);
        Page<BlogPost> result = postMapper.selectPage(new Page<>(safePage, safeSize), condition);
        Map<Long, List<BlogTagVO>> tagsByPost = tagsForPosts(result.getRecords().stream().map(BlogPost::getId).toList());
        Map<Long, String> covers = coverUrls(result.getRecords().stream().map(BlogPost::getCoverMediaId).toList());
        List<BlogPostPublicSummaryVO> items = result.getRecords().stream()
                .map(post -> assembler.toPublicSummary(post, tagsByPost.getOrDefault(post.getId(), List.of()), cover(post, covers))).toList();
        return new BlogPostPublicPageVO(items, safePage, safeSize, result.getTotal(), pages(result.getTotal(), safeSize));
    }

    public BlogPostPublicDetailVO publicDetail(String slug) {
        BlogPost post = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getSlug, slug));
        if (post == null || !BlogCommandService.PUBLISHED.equals(post.getPublishStatus())) throw new ApiException(HttpStatus.NOT_FOUND,
                "BLOG_POST_NOT_FOUND", "Post not found", "The post does not exist or is not public.");
        return assembler.toPublicDetail(post, postTagMapper.selectTagsByPostId(post.getId()), coverUrl(post.getCoverMediaId()),
                assembler.toNeighborVO(previousOf(post)), assembler.toNeighborVO(nextOf(post)));
    }

    public BlogCalendarVO calendar(String month) {
        YearMonth yearMonth = parseMonth(month);
        LocalDateTime[] bounds = monthBounds(yearMonth);
        List<BlogPost> posts = postMapper.selectList(new LambdaQueryWrapper<BlogPost>().select(BlogPost::getPublishedAt)
                .eq(BlogPost::getPublishStatus, BlogCommandService.PUBLISHED).ge(BlogPost::getPublishedAt, bounds[0])
                .lt(BlogPost::getPublishedAt, bounds[1]));
        List<BlogCalendarDayVO> days = posts.stream().collect(Collectors.groupingBy(post -> timezone.atSite(post.getPublishedAt()).toLocalDate(), Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> new BlogCalendarDayVO(entry.getKey().toString(), entry.getValue())).toList();
        return new BlogCalendarVO(month, days);
    }

    public List<BlogArchiveYearVO> archive() {
        List<BlogPost> posts = postMapper.selectList(new LambdaQueryWrapper<BlogPost>().select(BlogPost::getPublishedAt)
                .eq(BlogPost::getPublishStatus, BlogCommandService.PUBLISHED));
        Map<Integer, Map<YearMonth, Long>> byYear = new java.util.TreeMap<>(Comparator.reverseOrder());
        for (BlogPost post : posts) {
            var site = timezone.atSite(post.getPublishedAt());
            YearMonth yearMonth = YearMonth.of(site.getYear(), site.getMonthValue());
            byYear.computeIfAbsent(site.getYear(), ignored -> new LinkedHashMap<>()).merge(yearMonth, 1L, Long::sum);
        }
        List<BlogArchiveYearVO> result = new ArrayList<>();
        for (var entry : byYear.entrySet()) {
            List<BlogArchiveMonthVO> months = entry.getValue().entrySet().stream().sorted(Map.Entry.<YearMonth, Long>comparingByKey().reversed())
                    .map(month -> new BlogArchiveMonthVO(month.getKey().toString(), month.getValue())).toList();
            result.add(new BlogArchiveYearVO(entry.getKey(), months));
        }
        return result;
    }

    private void restrictToTag(LambdaQueryWrapper<BlogPost> condition, String tag) {
        if (tag == null || tag.isBlank()) return;
        List<Long> ids = postTagMapper.selectPostIdsByTagSlug(tag);
        condition.in(BlogPost::getId, ids.isEmpty() ? List.of(-1L) : ids);
    }

    private Map<Long, List<BlogTagVO>> tagsForPosts(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        Map<Long, List<BlogTagVO>> result = new HashMap<>();
        for (BlogPostTagRow row : postTagMapper.selectTagsByPostIds(postIds)) {
            result.computeIfAbsent(row.postId(), ignored -> new ArrayList<>()).add(new BlogTagVO(row.tagId(), row.name(), row.slug()));
        }
        return result;
    }

    private Map<Long, String> coverUrls(List<Long> mediaIds) {
        List<Long> distinct = mediaIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) return Map.of();
        return mediaAssetMapper.selectBatchIds(distinct).stream().collect(Collectors.toMap(MediaAsset::getId, MediaAsset::getPublicUrl));
    }

    private String cover(BlogPost post, Map<Long, String> covers) {
        return post.getCoverMediaId() == null ? null : covers.get(post.getCoverMediaId());
    }

    private String coverUrl(Long mediaId) {
        if (mediaId == null) return null;
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? null : media.getPublicUrl();
    }

    private BlogPost previousOf(BlogPost post) {
        return postMapper.selectOne(new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getPublishStatus, BlogCommandService.PUBLISHED)
                .and(query -> query.lt(BlogPost::getPublishedAt, post.getPublishedAt()).or(or -> or.eq(BlogPost::getPublishedAt, post.getPublishedAt()).lt(BlogPost::getId, post.getId())))
                .orderByDesc(BlogPost::getPublishedAt).orderByDesc(BlogPost::getId).last("LIMIT 1"));
    }

    private BlogPost nextOf(BlogPost post) {
        return postMapper.selectOne(new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getPublishStatus, BlogCommandService.PUBLISHED)
                .and(query -> query.gt(BlogPost::getPublishedAt, post.getPublishedAt()).or(or -> or.eq(BlogPost::getPublishedAt, post.getPublishedAt()).gt(BlogPost::getId, post.getId())))
                .orderByAsc(BlogPost::getPublishedAt).orderByAsc(BlogPost::getId).last("LIMIT 1"));
    }

    private BlogPost requirePost(Long postId) {
        BlogPost post = postMapper.selectById(postId);
        if (post == null) throw new ApiException(HttpStatus.NOT_FOUND, "BLOG_POST_NOT_FOUND", "Post not found", "The blog post does not exist.");
        return post;
    }

    private LocalDate parseDate(String date) {
        try { return LocalDate.parse(date); }
        catch (Exception ignored) { throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "Invalid date", "The date filter must use the YYYY-MM-DD format."); }
    }

    private YearMonth parseMonth(String month) {
        try { return YearMonth.parse(month); }
        catch (Exception ignored) { throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MONTH", "Invalid month", "The month must use the YYYY-MM format."); }
    }

    private LocalDateTime[] dayBounds(LocalDate date) {
        var start = date.atStartOfDay(timezone.zone());
        var end = date.plusDays(1).atStartOfDay(timezone.zone());
        return new LocalDateTime[] { start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(), end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime() };
    }

    private LocalDateTime[] monthBounds(YearMonth month) {
        var start = month.atDay(1).atStartOfDay(timezone.zone());
        var end = month.plusMonths(1).atDay(1).atStartOfDay(timezone.zone());
        return new LocalDateTime[] { start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(), end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime() };
    }

    private int pages(long total, int size) { return total == 0 ? 0 : (int) ((total + size - 1) / size); }
}
