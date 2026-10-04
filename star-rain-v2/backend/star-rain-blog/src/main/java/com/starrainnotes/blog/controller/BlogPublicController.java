package com.starrainnotes.blog.controller;

import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;

import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
@RestController
@RequestMapping("/api/public/blog")
public class BlogPublicController {

    @Autowired(required = false)
    private AnalyticsRecordApi analytics;

    private final BlogPublicService publicService;

    public BlogPublicController(BlogPublicService publicService) {
        this.publicService = publicService;
    }

    // BLOG-001 浏览博客列表
    @GetMapping("/posts")
    public ApiResponse<PageResult<BlogPostPublicVO>> posts(BlogPublicQueryDTO query) {
        return ApiResponse.ok(publicService.listPosts(query));
    }

    // BLOG-002 阅读文章：按 slug 而不是 ID，公开 URL 不暴露内部主键
    @GetMapping("/posts/{slug}")
    public ApiResponse<BlogPostPublicDetailVO> post(@PathVariable String slug) {
        BlogPostPublicDetailVO detail = publicService.postBySlug(slug);
        if (analytics != null) analytics.recordContentView(new ContentViewEvent("BLOG", detail.getId(), "/blog/posts/:slug"));
        return ApiResponse.ok(detail);
    }

    // BLOG-010 归档浏览：tag / topic / year / month 可组合
    @GetMapping("/archive")
    public ApiResponse<PageResult<BlogPostPublicVO>> archive(BlogPublicQueryDTO query) {
        return ApiResponse.ok(publicService.archive(query));
    }

    // 归档侧栏的月份桶，只统计已发布文章
    @GetMapping("/archive/months")
    public ApiResponse<List<BlogArchiveMonthVO>> archiveMonths() {
        return ApiResponse.ok(publicService.archiveMonths());
    }

    @GetMapping("/archive/days")
    public ApiResponse<List<BlogArchiveDayVO>> archiveDays(@RequestParam Integer year,
                                                            @RequestParam Integer month) {
        return ApiResponse.ok(publicService.archiveDays(year, month));
    }

    // 前台筛选项：禁用标签与停用专题、以及 0 篇已发布文章的项都不会出现
    @GetMapping("/tags")
    public ApiResponse<List<BlogTagVO>> tags() {
        return ApiResponse.ok(publicService.listPublishedTags());
    }

    @GetMapping("/topics")
    public ApiResponse<List<BlogTopicVO>> topics() {
        return ApiResponse.ok(publicService.listPublishedTopics());
    }
}
