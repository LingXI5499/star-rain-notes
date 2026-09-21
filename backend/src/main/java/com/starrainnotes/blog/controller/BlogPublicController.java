package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.service.BlogQueryService;
import com.starrainnotes.blog.service.BlogTagService;
import com.starrainnotes.blog.vo.BlogArchiveYearVO;
import com.starrainnotes.blog.vo.BlogCalendarVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicPageVO;
import com.starrainnotes.blog.vo.BlogTagWithPostCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public blog endpoints; draft and withdrawn posts stay invisible. */
@RestController
@RequestMapping("/api/v1/public/blog")
@RequiredArgsConstructor
@Tag(name = "公开博客")
public class BlogPublicController {
    private final BlogQueryService queryService;
    private final BlogTagService tagService;

    @GetMapping("/posts")
    @Operation(summary = "查询公开博客文章")
    public BlogPostPublicPageVO posts(@RequestParam(required = false) String tag, @RequestParam(required = false) String date,
                                      @RequestParam(required = false) String month, @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return queryService.publicList(tag, date, month, page, pageSize);
    }

    @GetMapping("/posts/{slug}")
    @Operation(summary = "查询公开博客文章详情")
    public BlogPostPublicDetailVO postDetail(@PathVariable String slug) { return queryService.publicDetail(slug); }

    @GetMapping("/tags")
    @Operation(summary = "查询公开博客标签")
    public List<BlogTagWithPostCountVO> tags() { return tagService.publicTags(); }

    @GetMapping("/calendar")
    @Operation(summary = "查询博客日历")
    public BlogCalendarVO calendar(@RequestParam String month) { return queryService.calendar(month); }

    @GetMapping("/archive")
    @Operation(summary = "查询博客归档")
    public List<BlogArchiveYearVO> archive() { return queryService.archive(); }
}
