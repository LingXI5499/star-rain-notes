package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.ArchiveYearView;
import com.starrainnotes.blog.dto.CalendarView;
import com.starrainnotes.blog.dto.PublicPostDetailView;
import com.starrainnotes.blog.dto.PublicPostPageView;
import com.starrainnotes.blog.dto.PublicTagViewWithCount;
import com.starrainnotes.blog.service.BlogService;
import com.starrainnotes.blog.service.BlogTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public blog endpoints (04 §11). Draft/Withdrawn posts return 404.
 */
@RestController
@RequestMapping("/api/v1/public/blog")
public class BlogPublicController {

    private final BlogService blogService;
    private final BlogTagService tagService;

    public BlogPublicController(BlogService blogService, BlogTagService tagService) {
        this.blogService = blogService;
        this.tagService = tagService;
    }

    @GetMapping("/posts")
    public PublicPostPageView posts(@RequestParam(required = false) String tag,
                                    @RequestParam(required = false) String date,
                                    @RequestParam(required = false) String month,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        return blogService.publicList(tag, date, month, page, pageSize);
    }

    @GetMapping("/posts/{slug}")
    public PublicPostDetailView postDetail(@PathVariable String slug) {
        return blogService.publicDetail(slug);
    }

    @GetMapping("/tags")
    public List<PublicTagViewWithCount> tags() {
        return tagService.publicTags();
    }

    @GetMapping("/calendar")
    public CalendarView calendar(@RequestParam String month) {
        return blogService.calendar(month);
    }

    @GetMapping("/archive")
    public List<ArchiveYearView> archive() {
        return blogService.archive();
    }
}
