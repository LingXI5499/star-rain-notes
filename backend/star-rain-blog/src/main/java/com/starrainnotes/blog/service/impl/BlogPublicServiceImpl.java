/*
 * BlogPublicServiceImpl：博客模块的业务实现文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：BlogLimits、BlogPublicQueryDTO、BlogPostEntity、BlogPostNotFoundException、BlogQueryInvalidException、BlogTopicNotFoundException、BlogPostMapper。
 * 阅读入口：listPosts（列出文章列表）；postBySlug（按固定链接标识读取文章）；archive（archive处理入口）；topicBySlug（按固定链接标识读取专题）。
 */

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
import java.util.ArrayList;
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
/**
 * 博客公开业务服务实现，属于博客模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class BlogPublicServiceImpl implements BlogPublicService {

    // 标签筛选的个数上限：再多就不是筛选，而是把 SQL 的 IN 列表撑大
    /**
     * 最大。
     * 初始值为 <code>20</code>。
     */
    private static final int TAG_FILTER_MAX = 20;

    /**
     * 文章数据映射。
     * 协作类型：<code>BlogPostMapper</code>。
     */
    private final BlogPostMapper postMapper;
    /**
     * 标签数据映射。
     * 协作类型：<code>BlogTagMapper</code>。
     */
    private final BlogTagMapper tagMapper;
    /**
     * 专题数据映射。
     * 协作类型：<code>BlogTopicMapper</code>。
     */
    private final BlogTopicMapper topicMapper;
    /**
     * 组装器。
     */
    private final BlogViewAssembler assembler;

    /**
     * 构造BlogPublicServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param postMapper 文章数据映射；类型为 <code>BlogPostMapper</code>
     * @param tagMapper 标签数据映射；类型为 <code>BlogTagMapper</code>
     * @param topicMapper 专题数据映射；类型为 <code>BlogTopicMapper</code>
     * @param assembler 组装器；类型为 <code>BlogViewAssembler</code>
     */
    public BlogPublicServiceImpl(BlogPostMapper postMapper,
                                 BlogTagMapper tagMapper,
                                 BlogTopicMapper topicMapper,
                                 BlogViewAssembler assembler) {
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.topicMapper = topicMapper;
        this.assembler = assembler;
    }

    /**
     * 列出文章列表。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>BlogQueryRules.validatePage</code>、<code>query.getPage</code>、<code>query.getPageSize</code>、<code>tagSlugs</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogPostPublicVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogPostPublicVO> listPosts(BlogPublicQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        List<String> tagSlugs = tagSlugs(query);
        String topicSlug = publicSlug(query.getTopic(), "topic", BlogLimits.TOPIC_SLUG_MAX_LENGTH);
        return page(query, tagSlugs, topicSlug, null, null);
    }

    /**
     * 按固定链接标识读取文章。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.publishedPostBySlug</code>、<code>assembler.toPublicDetailVO</code>、<code>postMapper.publishedPrevious</code>、<code>postMapper.publishedNext</code>。
     * 实现中的显式失败类型包括 <code>BlogPostNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostPublicDetailVO</code> 的处理结果
     */
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

    /**
     * archive处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>BlogQueryRules.validatePage</code>、<code>query.getPage</code>、<code>query.getPageSize</code>、<code>tagSlugs</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogPostPublicVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogPostPublicVO> archive(BlogPublicQueryDTO query) {
        BlogQueryRules.validatePage(query.getPage(), query.getPageSize());
        List<String> tagSlugs = tagSlugs(query);
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
        return page(query, tagSlugs, topicSlug, from, to);
    }

    /**
     * 按固定链接标识读取专题。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>topicMapper.publishedTopicBySlug</code>、<code>postMapper.publishedTopicPageCount</code>、<code>assembler.toPublicVOs</code>、<code>postMapper.publishedTopicPage</code>。
     * 实现中的显式失败类型包括 <code>BlogTopicNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>BlogTopicDetailVO</code> 的处理结果
     */
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

    /**
     * archiveMonths处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.archiveMonths</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<BlogArchiveMonthVO> archiveMonths() {
        return postMapper.archiveMonths();
    }

    /**
     * 统计字段处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.publishedStats</code>。
     * @return 类型为 <code>BlogPublicStatsVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public BlogPublicStatsVO stats() {
        // 只有已发布文章参与统计；published_at 为空的行（撤回后未再发布）不计入最早时间
        return postMapper.publishedStats();
    }

    /**
     * archiveDays处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>postMapper.archiveDays</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param year 年份；类型为 <code>Integer</code>
     * @param month 月份；类型为 <code>Integer</code>
     * @return 本次处理得到的条目集合
     */
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

    /**
     * 列出已发布标签列表。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tagMapper.publishedTags</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<BlogTagVO> listPublishedTags() {
        return tagMapper.publishedTags();
    }

    /**
     * 列出已发布专题列表。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>topicMapper.publishedTopics</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<BlogTopicVO> listPublishedTopics() {
        return topicMapper.publishedTopics();
    }

    /**
     * 分页读取博客公开。
     * 主要协作调用：<code>postMapper.publishedPageCount</code>、<code>assembler.toPublicVOs</code>、<code>postMapper.publishedPage</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @param tagSlugs 标签Slugs；类型为 <code>List&lt;String&gt;</code>
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>
     * @param publishedFrom 已发布来源；类型为 <code>LocalDateTime</code>
     * @param publishedTo 已发布目标；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>PageResult&lt;BlogPostPublicVO&gt;</code> 的处理结果
     */
    private PageResult<BlogPostPublicVO> page(BlogPublicQueryDTO query, List<String> tagSlugs, String topicSlug,
                                              LocalDateTime publishedFrom, LocalDateTime publishedTo) {
        long total = postMapper.publishedPageCount(tagSlugs, topicSlug, publishedFrom, publishedTo);
        List<BlogPostPublicVO> items = total == 0
                ? List.of()
                : assembler.toPublicVOs(postMapper.publishedPage(tagSlugs, topicSlug, publishedFrom, publishedTo,
                        BlogQueryRules.offset(query.getPage(), query.getPageSize()), query.getPageSize()));
        return PageResult.<BlogPostPublicVO>builder()
                .items(items)
                .total(total)
                .page(query.getPage())
                .pageSize(query.getPageSize())
                .build();
    }

    /*
     * 标签筛选条件：单标签参数 tag 与多标签参数 tags（逗号分隔）合并去重。
     *
     * 两个都给时不报错，因为「分享出来的链接里 tag 与 tags 同时存在」是很自然的事，
     * 为此报 400 只会让人困惑。数量上限 20：再多就不是筛选，而是把 SQL 的 IN 列表撑大。
     */
    /**
     * 标签Slugs处理入口。
     * 关键处理步骤调用 <code>publicSlugs</code>、<code>query.getTag</code>、<code>query.getTags</code>、<code>slugs.contains</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 本次处理得到的条目集合
     */
    private List<String> tagSlugs(BlogPublicQueryDTO query) {
        List<String> slugs = new ArrayList<>(publicSlugs(query.getTag(), "tag", BlogLimits.TAG_SLUG_MAX_LENGTH));
        for (String slug : publicSlugs(query.getTags(), "tags", BlogLimits.TAG_SLUG_MAX_LENGTH)) {
            if (!slugs.contains(slug)) {
                slugs.add(slug);
            }
        }
        return slugs.size() > TAG_FILTER_MAX ? slugs.subList(0, TAG_FILTER_MAX) : slugs;
    }

    /**
     * 公开Slugs处理入口。
     * 关键处理步骤调用 <code>raw.isBlank</code>、<code>List.of</code>、<code>raw.split</code>、<code>publicSlug</code>。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @param fieldName 字段名称；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    private List<String> publicSlugs(String raw, String fieldName, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> slugs = new ArrayList<>();
        for (String part : raw.split(",")) {
            String slug = publicSlug(part, fieldName, maxLength);
            if (slug != null && !slugs.contains(slug)) {
                slugs.add(slug);
            }
        }
        return slugs;
    }

    /*
     * 公开筛选用的 slug 校验。
     *
     * 非法值明确报 BLOG_QUERY_INVALID 而不是当作“查不到”：
     * 归档页的筛选条件来自 URL query，静默返回空列表会让人以为是“这个分类下没有文章”。
     */
    /**
     * 公开固定链接标识处理入口。
     * 关键处理步骤调用 <code>raw.isBlank</code>、<code>BlogSlugRules.normalize</code>、<code>BlogSlugRules.isValid</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @param fieldName 字段名称；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
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
