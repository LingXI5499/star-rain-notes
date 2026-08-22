package com.starrainnotes.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.blog.dto.CreateTagRequest;
import com.starrainnotes.blog.dto.UpdateTagRequest;
import com.starrainnotes.blog.dto.BlogTagView;
import com.starrainnotes.blog.dto.PublicTagViewWithCount;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Flat blog tag management (04 §11). Public tags only include tags linked to
 * at least one PUBLISHED post, with the published post count.
 */
@Service
public class BlogTagService {

    private final BlogTagMapper tagMapper;
    private final JdbcTemplate jdbc;

    public BlogTagService(BlogTagMapper tagMapper, JdbcTemplate jdbc) {
        this.tagMapper = tagMapper;
        this.jdbc = jdbc;
    }

    public List<BlogTagView> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<BlogTag>().orderByAsc(BlogTag::getName))
                .stream()
                .map(t -> new BlogTagView(t.getId(), t.getName(), t.getSlug()))
                .toList();
    }

    public BlogTagView create(CreateTagRequest request) {
        assertUnique(request.name(), request.slug(), null);
        BlogTag tag = new BlogTag();
        tag.setName(request.name());
        tag.setSlug(request.slug());
        tagMapper.insert(tag);
        return new BlogTagView(tag.getId(), tag.getName(), tag.getSlug());
    }

    public BlogTagView update(Long tagId, UpdateTagRequest request) {
        BlogTag tag = requireTag(tagId);
        assertUnique(request.name(), request.slug(), tagId);
        tag.setName(request.name());
        tag.setSlug(request.slug());
        tagMapper.updateById(tag);
        return new BlogTagView(tag.getId(), tag.getName(), tag.getSlug());
    }

    public void delete(Long tagId) {
        requireTag(tagId);
        // blog_post_tag relations cascade on delete (frozen FK)
        tagMapper.deleteById(tagId);
    }

    public List<PublicTagViewWithCount> publicTags() {
        return jdbc.query("""
                SELECT t.id, t.name, t.slug, COUNT(*) AS post_count
                FROM blog_tag t
                JOIN blog_post_tag bt ON bt.blog_tag_id = t.id
                JOIN blog_post p ON p.id = bt.blog_post_id
                WHERE p.publish_status = 'PUBLISHED'
                GROUP BY t.id, t.name, t.slug
                ORDER BY post_count DESC, t.name ASC
                """, (rs, rowNum) -> new PublicTagViewWithCount(
                rs.getLong("id"), rs.getString("name"), rs.getString("slug"), rs.getLong("post_count")));
    }

    private BlogTag requireTag(Long tagId) {
        BlogTag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND",
                    "Tag not found", "The blog tag does not exist.");
        }
        return tag;
    }

    private void assertUnique(String name, String slug, Long excludeId) {
        LambdaQueryWrapper<BlogTag> nameWrapper = new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getName, name);
        LambdaQueryWrapper<BlogTag> slugWrapper = new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getSlug, slug);
        if (excludeId != null) {
            nameWrapper.ne(BlogTag::getId, excludeId);
            slugWrapper.ne(BlogTag::getId, excludeId);
        }
        if (tagMapper.selectCount(nameWrapper) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "TAG_NAME_CONFLICT",
                    "Tag name already exists", "A tag with this name already exists.");
        }
        if (tagMapper.selectCount(slugWrapper) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT",
                    "Slug already exists", "A tag with this slug already exists.");
        }
    }
}
