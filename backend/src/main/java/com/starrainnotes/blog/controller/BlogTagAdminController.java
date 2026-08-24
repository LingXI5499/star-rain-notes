package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.AdminBlogTagView;
import com.starrainnotes.blog.dto.CreateTagRequest;
import com.starrainnotes.blog.dto.UpdateTagRequest;
import com.starrainnotes.blog.service.BlogTagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin flat blog tag management (04 §11).
 */
@RestController
@RequestMapping("/api/v1/admin/blog/tags")
public class BlogTagAdminController {

    private final BlogTagService tagService;

    public BlogTagAdminController(BlogTagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<AdminBlogTagView> list() {
        return tagService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBlogTagView create(@Valid @RequestBody CreateTagRequest request) {
        return tagService.create(request);
    }

    @PutMapping("/{tagId}")
    public AdminBlogTagView update(@PathVariable Long tagId, @Valid @RequestBody UpdateTagRequest request) {
        return tagService.update(tagId, request);
    }

    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long tagId,
                       @RequestParam(defaultValue = "false") boolean force) {
        tagService.delete(tagId, force);
    }
}
