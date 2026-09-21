package com.starrainnotes.blog.controller;

import com.starrainnotes.blog.dto.CreateTagRequest;
import com.starrainnotes.blog.dto.UpdateTagRequest;
import com.starrainnotes.blog.service.BlogTagService;
import com.starrainnotes.blog.vo.BlogTagWithPostCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/admin/blog/tags")
@RequiredArgsConstructor
@Tag(name = "后台博客标签管理")
public class BlogTagAdminController {
    private final BlogTagService tagService;

    @GetMapping
    @Operation(summary = "查询博客标签")
    public List<BlogTagWithPostCountVO> list() { return tagService.listAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建博客标签")
    public BlogTagWithPostCountVO create(@Valid @RequestBody CreateTagRequest request) { return tagService.create(request); }

    @PutMapping("/{tagId}")
    @Operation(summary = "更新博客标签")
    public BlogTagWithPostCountVO update(@PathVariable Long tagId, @Valid @RequestBody UpdateTagRequest request) {
        return tagService.update(tagId, request);
    }

    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除博客标签")
    public void delete(@PathVariable Long tagId, @RequestParam(defaultValue = "false") boolean force) { tagService.delete(tagId, force); }
}
