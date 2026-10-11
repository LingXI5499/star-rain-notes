/*
 * BlogPublicController：博客模块的HTTP 接入文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：BlogPublicQueryDTO、BlogPublicService、BlogPublicReadService、BlogArchiveMonthVO、BlogArchiveDayVO、BlogPostPublicDetailVO、BlogPostPublicVO。
 * 阅读入口：posts（文章列表处理入口）；post（文章处理入口）；archive（archive处理入口）；archiveMonths（archiveMonths处理入口）。
 */

package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.service.BlogPublicReadService;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogPublicStatsVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicDetailVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * BLOG-001 / BLOG-002 / BLOG-010 前台公开接口。
 *
 * 整条 URL 前缀在 BlogSecurityContributor 里声明为 public，
 * 因此这里的每个端点都必须假设调用者是匿名的：
 * 不返回 status、不返回内部账户 ID，未发布文章一律 404。
 */
/**
 * 博客公开控制器，属于博客模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/public/blog")
public class BlogPublicController {

    /**
     * 公开业务服务。
     * 协作类型：<code>BlogPublicService</code>。
     */
    private final BlogPublicService publicService;
    /**
     * 阅读。
     * 协作类型：<code>BlogPublicReadService</code>。
     */
    private final BlogPublicReadService reading;

    /**
     * 构造BlogPublicController，接收本类使用的依赖或初始化参数。
     *
     * @param publicService 公开业务服务；类型为 <code>BlogPublicService</code>
     * @param reading 阅读；类型为 <code>BlogPublicReadService</code>
     */
    public BlogPublicController(BlogPublicService publicService, BlogPublicReadService reading) {
        this.publicService = publicService;
        this.reading = reading;
    }

    // BLOG-001 浏览博客列表
    /**
     * 文章列表处理入口。
     * HTTP 映射：<code>@GetMapping("/posts")</code>。
     * 主要协作调用：<code>publicService.listPosts</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;BlogPostPublicVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/posts")
    public ApiResponse<PageResult<BlogPostPublicVO>> posts(BlogPublicQueryDTO query) {
        return ApiResponse.ok(publicService.listPosts(query));
    }

    // BLOG-002 阅读文章：按 slug 而不是 ID，公开 URL 不暴露内部主键
    /**
     * 文章处理入口。
     * HTTP 映射：<code>@GetMapping("/posts/{slug}")</code>。
     * 主要协作调用：<code>reading.read</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;BlogPostPublicDetailVO&gt;</code> 的处理结果
     */
    @GetMapping("/posts/{slug}")
    public ApiResponse<BlogPostPublicDetailVO> post(@PathVariable String slug) {
        return ApiResponse.ok(reading.read(slug));
    }

    // BLOG-010 归档浏览：tag / topic / year / month 可组合
    /**
     * archive处理入口。
     * HTTP 映射：<code>@GetMapping("/archive")</code>。
     * 主要协作调用：<code>publicService.archive</code>。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;BlogPostPublicVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/archive")
    public ApiResponse<PageResult<BlogPostPublicVO>> archive(BlogPublicQueryDTO query) {
        return ApiResponse.ok(publicService.archive(query));
    }

    // 归档侧栏的月份桶，只统计已发布文章
    /**
     * archiveMonths处理入口。
     * HTTP 映射：<code>@GetMapping("/archive/months")</code>。
     * 主要协作调用：<code>publicService.archiveMonths</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;BlogArchiveMonthVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/archive/months")
    public ApiResponse<List<BlogArchiveMonthVO>> archiveMonths() {
        return ApiResponse.ok(publicService.archiveMonths());
    }

    // 博客首页右栏的写作统计：篇数、正文字数合计、开始写作的时间
    /**
     * 统计字段处理入口。
     * HTTP 映射：<code>@GetMapping("/stats")</code>。
     * 主要协作调用：<code>publicService.stats</code>。
     * @return 类型为 <code>ApiResponse&lt;BlogPublicStatsVO&gt;</code> 的处理结果
     */
    @GetMapping("/stats")
    public ApiResponse<BlogPublicStatsVO> stats() {
        return ApiResponse.ok(publicService.stats());
    }

    /**
     * archiveDays处理入口。
     * HTTP 映射：<code>@GetMapping("/archive/days")</code>。
     * 主要协作调用：<code>publicService.archiveDays</code>。
     *
     * @param year 年份；类型为 <code>Integer</code>
     * @param month 月份；类型为 <code>Integer</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;BlogArchiveDayVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/archive/days")
    public ApiResponse<List<BlogArchiveDayVO>> archiveDays(@RequestParam Integer year,
                                                            @RequestParam Integer month) {
        return ApiResponse.ok(publicService.archiveDays(year, month));
    }

    // 前台筛选项：禁用标签与停用专题、以及 0 篇已发布文章的项都不会出现
    /**
     * 标签列表处理入口。
     * HTTP 映射：<code>@GetMapping("/tags")</code>。
     * 主要协作调用：<code>publicService.listPublishedTags</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;BlogTagVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tags")
    public ApiResponse<List<BlogTagVO>> tags() {
        return ApiResponse.ok(publicService.listPublishedTags());
    }

    /**
     * 专题列表处理入口。
     * HTTP 映射：<code>@GetMapping("/topics")</code>。
     * 主要协作调用：<code>publicService.listPublishedTopics</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;BlogTopicVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/topics")
    public ApiResponse<List<BlogTopicVO>> topics() {
        return ApiResponse.ok(publicService.listPublishedTopics());
    }

    // 专题页：专题 + 该专题下的文章，顺序是策展顺序（sort_order），不是发布时间
    /**
     * 专题处理入口。
     * HTTP 映射：<code>@GetMapping("/topics/{slug}")</code>。
     * 主要协作调用：<code>publicService.topicBySlug</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;BlogTopicDetailVO&gt;</code> 的处理结果
     */
    @GetMapping("/topics/{slug}")
    public ApiResponse<BlogTopicDetailVO> topic(@PathVariable String slug,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(publicService.topicBySlug(slug, page, pageSize));
    }
}
