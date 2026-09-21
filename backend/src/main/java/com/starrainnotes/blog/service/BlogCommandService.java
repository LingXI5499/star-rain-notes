package com.starrainnotes.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.entity.BlogPost;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogPostTagMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.seo.SeoContentChange;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Write-side blog workflows and their transactional boundaries. */
@Service
@RequiredArgsConstructor
public class BlogCommandService {
    public static final String PUBLISHED = "PUBLISHED";
    public static final String DRAFT = "DRAFT";
    public static final String WITHDRAWN = "WITHDRAWN";

    private final BlogPostMapper postMapper;
    private final BlogTagMapper tagMapper;
    private final BlogPostTagMapper postTagMapper;
    private final BlogTagService tagService;
    private final BlogQueryService queryService;

    @Transactional
    public BlogPostAdminDetailVO create(CreatePostRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        List<Long> tagIds = resolveTagIds(request.tagIds(), request.tagNames());
        BlogPost post = BlogPost.builder().title(request.title()).slug(slug).summary(request.summary())
                .bodyMarkdown(request.bodyMarkdown()).coverMediaId(request.coverMediaId()).publishStatus(DRAFT).build();
        postMapper.insert(post);
        replaceTags(post.getId(), tagIds);
        return queryService.adminDetail(post.getId());
    }

    @Transactional
    @SeoContentChange(table = "blog_post", pathPrefix = "/blog/")
    public BlogPostAdminDetailVO update(Long postId, UpdatePostRequest request) {
        BlogPost post = requirePost(postId);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), post.getSlug());
        assertSlugFree(slug, postId);
        List<Long> tagIds = resolveTagIds(request.tagIds(), request.tagNames());
        post.setTitle(request.title());
        post.setSlug(slug);
        post.setSummary(request.summary());
        post.setBodyMarkdown(request.bodyMarkdown());
        post.setCoverMediaId(request.coverMediaId());
        postMapper.updateById(post);
        replaceTags(post.getId(), tagIds);
        return queryService.adminDetail(post.getId());
    }

    @Transactional
    @SeoContentChange(table = "blog_post", pathPrefix = "/blog/")
    public void delete(Long postId) {
        requirePost(postId);
        postMapper.deleteById(postId);
    }

    @Transactional
    @SeoContentChange(table = "blog_post", pathPrefix = "/blog/")
    public BlogPostAdminDetailVO publish(Long postId) {
        BlogPost post = requirePost(postId);
        if (!PUBLISHED.equals(post.getPublishStatus())) {
            if (post.getPublishedAt() == null) post.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            post.setPublishStatus(PUBLISHED);
            postMapper.updateById(post);
        }
        return queryService.adminDetail(postId);
    }

    @Transactional
    @SeoContentChange(table = "blog_post", pathPrefix = "/blog/")
    public BlogPostAdminDetailVO withdraw(Long postId) {
        BlogPost post = requirePost(postId);
        if (DRAFT.equals(post.getPublishStatus())) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_PUBLISH_TRANSITION", "Cannot withdraw a draft", "Only published posts can be withdrawn.");
        if (!WITHDRAWN.equals(post.getPublishStatus())) {
            post.setPublishStatus(WITHDRAWN);
            postMapper.updateById(post);
        }
        return queryService.adminDetail(postId);
    }

    private BlogPost requirePost(Long postId) {
        BlogPost post = postMapper.selectById(postId);
        if (post == null) throw new ApiException(HttpStatus.NOT_FOUND, "BLOG_POST_NOT_FOUND", "Post not found", "The blog post does not exist.");
        return post;
    }

    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        LinkedHashSet<Long> resolved = new LinkedHashSet<>();
        if (tagIds != null) tagIds.stream().filter(java.util.Objects::nonNull).forEach(resolved::add);
        validateTags(new ArrayList<>(resolved));
        resolved.addAll(tagService.resolveNames(tagNames));
        if (resolved.size() > 20) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TOO_MANY_TAGS", "Too many tags",
                "每篇文章最多选择 20 个标签。");
        return new ArrayList<>(resolved);
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        List<Long> distinct = tagIds.stream().distinct().toList();
        List<BlogTag> found = tagMapper.selectBatchIds(distinct);
        if (found.size() != distinct.size()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TAG_NOT_FOUND",
                "Tag not found", "One or more referenced tags do not exist.");
    }

    private void replaceTags(Long postId, List<Long> tagIds) {
        postTagMapper.deleteByPostId(postId);
        if (tagIds != null && !tagIds.isEmpty()) postTagMapper.insertBatch(postId, tagIds.stream().distinct().toList());
    }

    private void assertSlugFree(String slug, Long excludeId) {
        if (slugExists(slug, excludeId)) throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT", "Slug already exists",
                "A post with this slug already exists.");
    }

    private boolean slugExists(String slug, Long excludeId) {
        LambdaQueryWrapper<BlogPost> query = new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getSlug, slug);
        if (excludeId != null) query.ne(BlogPost::getId, excludeId);
        Long count = postMapper.selectCount(query);
        return count != null && count > 0;
    }
}
