package com.starrainnotes.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.blog.dto.AdminBlogTagView;
import com.starrainnotes.blog.dto.CreateTagRequest;
import com.starrainnotes.blog.dto.UpdateTagRequest;
import com.starrainnotes.blog.dto.PublicTagViewWithCount;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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

    public List<AdminBlogTagView> listAll() {
        return jdbc.query("""
                SELECT t.id, t.name, t.slug, COUNT(bt.blog_post_id) AS post_count
                FROM blog_tag t
                LEFT JOIN blog_post_tag bt ON bt.blog_tag_id = t.id
                GROUP BY t.id, t.name, t.slug
                ORDER BY t.name ASC
                """, (rs, rowNum) -> new AdminBlogTagView(
                rs.getLong("id"), rs.getString("name"), rs.getString("slug"), rs.getLong("post_count")));
    }

    public AdminBlogTagView create(CreateTagRequest request) {
        String name = normalizeName(request.name());
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        slug = slug.toLowerCase(Locale.ROOT);
        assertUnique(name, slug, null);
        BlogTag tag = new BlogTag();
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.insert(tag);
        return new AdminBlogTagView(tag.getId(), tag.getName(), tag.getSlug(), 0);
    }

    public AdminBlogTagView update(Long tagId, UpdateTagRequest request) {
        BlogTag tag = requireTag(tagId);
        String name = normalizeName(request.name());
        String slug = NumericSlugGenerator.forUpdate(request.slug(), tag.getSlug()).toLowerCase(Locale.ROOT);
        assertUnique(name, slug, tagId);
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.updateById(tag);
        return new AdminBlogTagView(tag.getId(), tag.getName(), tag.getSlug(), usageCount(tagId));
    }

    public void delete(Long tagId, boolean force) {
        requireTag(tagId);
        long postCount = usageCount(tagId);
        if (postCount > 0 && !force) {
            throw new ApiException(HttpStatus.CONFLICT, "TAG_IN_USE",
                    "Tag is in use", "该标签仍被 " + postCount + " 篇文章使用，请确认后强制删除。");
        }
        // blog_post_tag relations cascade on delete (frozen FK)
        tagMapper.deleteById(tagId);
    }

    /** Resolve normalized names to existing tags or create them inside the caller transaction. */
    public List<Long> resolveNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : names) {
            String name = normalizeName(value);
            if (!name.isBlank()) {
                unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
            }
        }
        List<Long> ids = new ArrayList<>();
        for (String name : unique.values()) {
            ids.add(resolveName(name));
        }
        return ids;
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

    private Long resolveName(String name) {
        BlogTag existing = tagMapper.selectOne(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getName, name).last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        String base = automaticSlug(name);
        for (int suffix = 1; suffix < 10_000; suffix++) {
            String candidate = suffix == 1 ? base : withSuffix(base, suffix);
            int inserted = jdbc.update("INSERT IGNORE INTO blog_tag (name, slug) VALUES (?, ?)", name, candidate);
            BlogTag resolved = tagMapper.selectOne(new LambdaQueryWrapper<BlogTag>()
                    .eq(BlogTag::getName, name).last("LIMIT 1"));
            if (resolved != null) {
                return resolved.getId();
            }
            if (inserted == 0) {
                continue;
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, "TAG_SLUG_CONFLICT",
                "Unable to create tag", "无法为新标签生成唯一 slug，请在标签管理中手动创建。");
    }

    private long usageCount(Long tagId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blog_post_tag WHERE blog_tag_id = ?", Long.class, tagId);
        return count == null ? 0 : count;
    }

    private static String normalizeName(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
    }

    private static String automaticSlug(String name) {
        String ascii = normalizeName(name).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (ascii.isBlank()) {
            ascii = "tag-" + sha256(name).substring(0, 12);
        }
        return ascii.length() <= 60 ? ascii : ascii.substring(0, 60).replaceAll("-+$", "");
    }

    private static String withSuffix(String base, int suffix) {
        String tail = "-" + suffix;
        String head = base.length() + tail.length() <= 60 ? base : base.substring(0, 60 - tail.length());
        return head.replaceAll("-+$", "") + tail;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizeName(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
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

    private boolean slugExists(String slug, Long excludeId) {
        LambdaQueryWrapper<BlogTag> wrapper = new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getSlug, slug);
        if (excludeId != null) wrapper.ne(BlogTag::getId, excludeId);
        Long count = tagMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
