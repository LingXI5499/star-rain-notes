package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.exception.BlogPostNotFoundException;
import com.starrainnotes.blog.exception.BlogQueryInvalidException;
import com.starrainnotes.blog.exception.BlogTopicNotFoundException;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.utils.BlogQueryRules;
import com.starrainnotes.blog.utils.BlogSlugRules;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogPublicStatsVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicDetailVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * BLOG-001 / BLOG-002 / BLOG-010 的实现。
 *
 * 这一层唯一的硬约束：只返回 PUBLISHED。它由 SQL 条件保证（见 BlogPostMapper.xml
 * 的 publishedFilters），而不是靠这里记得过滤 —— Service 里少写一个 if 就会泄露草稿，
 * SQL 里的常量不会。
 *
 * 时间归档统一换算成 [from, to) 半开区间去比较 published_at：
 * 用 YEAR()/MONTH() 会让 idx_sr_blog_post_status_published 失去作用，
 * 而且跨年边界容易写错（12 月 + 1 个月是次年 1 月）。
 */
@Service
public class BlogPublicServiceImpl implements BlogPublicService {

    private final BlogPostMapper postMapper;
    private final BlogTagMapper tagMapper;
    private final BlogTopicMapper topicMapper;
    private final BlogViewAssembler assembler;

    public BlogPublicServiceImpl(BlogPostMapper postMapper,
                                 BlogTagMapper tagMapper,
                                 BlogTopicMapper topicMapper,
                                 BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.topicMapper = topicMapper;
        this.assembler = assembler;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogPostPublicVO> listPosts(BlogPublicQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        String tagSlug = publicSlug(query.getTag(), "tag", BlogLimits.TAG_SLUG_MAX_LENGTH);
        String topicSlug = publicSlug(query.getTopic(), "topic", BlogLimits.TOPIC_SLUG_MAX_LENGTH);
        return page(query, tagSlug, topicSlug, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostPublicDetailVO postBySlug(String slug) {
        String normalized = BlogSlugRules.normalize(slug);
        BlogPostEntity post = normalized == null ? null : postMapper.publishedPostBySlug(normalized);
        if (post == null) {
            // 草稿与已撤回文章同样走这里：对外不能区分“不存在”和“未公开”
            throw new BlogPostNotFoundException();
        }
        BlogPostPublicDetailVO detail = assembler.toPublicDetailVO(post);
        if (post.getPublishedAt() != null) {
            detail.setPrevious(postMapper.publishedPrevious(post.getPublishedAt(), post.getId()));
            detail.setNext(postMapper.publishedNext(post.getPublishedAt(), post.getId()));
        }
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogPostPublicVO> archive(BlogPublicQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        String tagSlug = publicSlug(query.getTag(), "tag", BlogLimits.TAG_SLUG_MAX_LENGTH);
        String topicSlug = publicSlug(query.getTopic(), "topic", BlogLimits.TOPIC_SLUG_MAX_LENGTH);

        Integer year = BlogQueryRules.archiveYear(query.getYear());
        Integer month = BlogQueryRules.archiveMonth(year, query.getMonth());
        Integer day = BlogQueryRules.archiveDay(year, month, query.getDay());
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (year != null) {
            from = LocalDateTime.of(year, month == null ? 1 : month, day == null ? 1 : day, 0, 0);
            to = day != null ? from.plusDays(1) : month == null ? from.plusYears(1) : from.plusMonths(1);
        }
        return page(query, tagSlug, topicSlug, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogTopicDetailVO topicBySlug(String slug, int page, int pageSize) {
        BlogQueryRules.validatePage(page, pageSize);
        String topicSlug = publicSlug(slug, "topic", BlogLimits.TOPIC_SLUG_MAX_LENGTH);
        BlogTopicVO topic = topicSlug == null ? null : topicMapper.publishedTopicBySlug(topicSlug);
        if (topic == null) {
            // 不存在、已停用、以及“成员全都没发布”的专题在对外表现上完全一致，不区分
            throw new BlogTopicNotFoundException();
        }
        long total = postMapper.publishedTopicPageCount(topicSlug);
        List<BlogPostPublicVO> items = total == 0
                ? List.of()
                : assembler.toPublicVOs(postMapper.publishedTopicPage(topicSlug,
                        BlogQueryRules.offset(page, pageSize), pageSize));
        return BlogTopicDetailVO.builder()
                .topic(topic)
                .posts(PageResult.<BlogPostPublicVO>builder()
                        .items(items)
                        .total(total)
                        .page(page)
                        .pageSize(pageSize)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogArchiveMonthVO> archiveMonths() {
        return postMapper.archiveMonths();
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPublicStatsVO stats() {
        // 只有已发布文章参与统计；published_at 为空的行（撤回后未再发布）不计入最早时间
        return postMapper.publishedStats();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogArchiveDayVO> archiveDays(Integer year, Integer month) {
        Integer validYear = BlogQueryRules.archiveYear(year);
        Integer validMonth = BlogQueryRules.archiveMonth(validYear, month);
        if (validYear == null || validMonth == null) {
            throw new BlogQueryInvalidException("按天查看日历必须给出 year 和 month");
        }
        LocalDateTime from = LocalDateTime.of(validYear, validMonth, 1, 0, 0);
        return postMapper.archiveDays(from, from.plusMonths(1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogTagVO> listPublishedTags() {
        return tagMapper.publishedTags();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogTopicVO> listPublishedTopics() {
        return topicMapper.publishedTopics();
    }

    private PageResult<BlogPostPublicVO> page(BlogPublicQueryDTO query, String tagSlug, String topicSlug,
                                              LocalDateTime publishedFrom, LocalDateTime publishedTo) {
        long total = postMapper.publishedPageCount(tagSlug, topicSlug, publishedFrom, publishedTo);
        List<BlogPostPublicVO> items = total == 0
                ? List.of()
                : assembler.toPublicVOs(postMapper.publishedPage(tagSlug, topicSlug, publishedFrom, publishedTo,
                        BlogQueryRules.offset(query.getPage(), query.getPageSize()), query.getPageSize()));
        return PageResult.<BlogPostPublicVO>builder()
                .items(items)
                .total(total)
                .page(query.getPage())
                .pageSize(query.getPageSize())
                .build();
    }

    /*
     * 公开筛选用的 slug 校验。
     *
     * 非法值明确报 BLOG_QUERY_INVALID 而不是当作“查不到”：
     * 归档页的筛选条件来自 URL query，静默返回空列表会让人以为是“这个分类下没有文章”。
     */
    private String publicSlug(String raw, String fieldName, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String slug = BlogSlugRules.normalize(raw);
        if (!BlogSlugRules.isValid(slug, maxLength)) {
            throw new BlogQueryInvalidException(fieldName + " 不是合法的 slug");
        }
        return slug;
    }
}
