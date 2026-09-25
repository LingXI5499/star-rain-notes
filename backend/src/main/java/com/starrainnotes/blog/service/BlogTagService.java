package com.starrainnotes.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.blog.dto.CreateTagRequest;
import com.starrainnotes.blog.dto.UpdateTagRequest;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.mapper.BlogPostTagMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.vo.BlogTagWithPostCountVO;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Blog tag business rules; relation SQL stays in dedicated mappers. */
@Service
@RequiredArgsConstructor
public class BlogTagService {
    private final BlogTagMapper tagMapper;
    private final BlogPostTagMapper postTagMapper;

    public List<BlogTagWithPostCountVO> listAll() {
        return tagMapper.selectAllWithPostCount().stream().map(this::toCountVO).toList();
    }

    @Transactional
    public BlogTagWithPostCountVO create(CreateTagRequest request) {
        String name = normalizeName(request.name());
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null)).toLowerCase(Locale.ROOT);
        assertUnique(name, slug, null);
        BlogTag tag = BlogTag.builder().name(name).slug(slug).build();
        tagMapper.insert(tag);
        return new BlogTagWithPostCountVO(tag.getId(), tag.getName(), tag.getSlug(), 0);
    }

    @Transactional
    public BlogTagWithPostCountVO update(Long tagId, UpdateTagRequest request) {
        BlogTag tag = requireTag(tagId);
        String name = normalizeName(request.name());
        String slug = NumericSlugGenerator.forUpdate(request.slug(), tag.getSlug()).toLowerCase(Locale.ROOT);
        assertUnique(name, slug, tagId);
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.updateById(tag);
        return new BlogTagWithPostCountVO(tag.getId(), tag.getName(), tag.getSlug(), postTagMapper.countByTagId(tagId));
    }

    @Transactional
    public void delete(Long tagId, boolean force) {
        requireTag(tagId);
        long postCount = postTagMapper.countByTagId(tagId);
        if (postCount > 0 && !force) throw new ApiException(HttpStatus.CONFLICT, "TAG_IN_USE", "Tag is in use",
                "该标签仍被 " + postCount + " 篇文章使用，请确认后强制删除。");
        tagMapper.deleteById(tagId);
    }

    /** Resolves normalized names in the caller's transaction. */
    public List<Long> resolveNames(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : names) {
            String name = normalizeName(value);
            if (!name.isBlank()) unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        List<Long> ids = new ArrayList<>();
        for (String name : unique.values()) ids.add(resolveName(name));
        return ids;
    }

    public List<BlogTagWithPostCountVO> publicTags() {
        return tagMapper.selectPublishedWithPostCount().stream().map(this::toCountVO).toList();
    }

    private BlogTagWithPostCountVO toCountVO(BlogTagMapper.BlogTagWithCountRow row) {
        return new BlogTagWithPostCountVO(row.id(), row.name(), row.slug(), row.postCount());
    }

    private BlogTag requireTag(Long tagId) {
        BlogTag tag = tagMapper.selectById(tagId);
        if (tag == null) throw new ApiException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", "Tag not found", "The blog tag does not exist.");
        return tag;
    }

    private Long resolveName(String name) {
        BlogTag existing = tagMapper.selectOne(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getName, name).last("LIMIT 1"));
        if (existing != null) return existing.getId();
        String base = automaticSlug(name);
        for (int suffix = 1; suffix < 10_000; suffix++) {
            String candidate = suffix == 1 ? base : withSuffix(base, suffix);
            tagMapper.insertIgnore(BlogTag.builder().name(name).slug(candidate).build());
            BlogTag resolved = tagMapper.selectOne(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getName, name).last("LIMIT 1"));
            if (resolved != null) return resolved.getId();
        }
        throw new ApiException(HttpStatus.CONFLICT, "TAG_SLUG_CONFLICT", "Unable to create tag", "无法为新标签生成唯一 slug，请在标签管理中手动创建。");
    }

    private static String normalizeName(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
    }

    private static String automaticSlug(String name) {
        String ascii = normalizeName(name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (ascii.isBlank()) ascii = "tag-" + sha256(name).substring(0, 12);
        return ascii.length() <= 60 ? ascii : ascii.substring(0, 60).replaceAll("-+$", "");
    }

    private static String withSuffix(String base, int suffix) {
        String tail = "-" + suffix;
        String head = base.length() + tail.length() <= 60 ? base : base.substring(0, 60 - tail.length());
        return head.replaceAll("-+$", "") + tail;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalizeName(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private void assertUnique(String name, String slug, Long excludeId) {
        LambdaQueryWrapper<BlogTag> nameQuery = new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getName, name);
        LambdaQueryWrapper<BlogTag> slugQuery = new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getSlug, slug);
        if (excludeId != null) { nameQuery.ne(BlogTag::getId, excludeId); slugQuery.ne(BlogTag::getId, excludeId); }
        if (tagMapper.selectCount(nameQuery) > 0) throw new ApiException(HttpStatus.CONFLICT, "TAG_NAME_CONFLICT", "Tag name already exists", "A tag with this name already exists.");
        if (tagMapper.selectCount(slugQuery) > 0) throw new ApiException(HttpStatus.CONFLICT, "SLUG_CONFLICT", "Slug already exists", "A tag with this slug already exists.");
    }

    private boolean slugExists(String slug, Long excludeId) {
        LambdaQueryWrapper<BlogTag> query = new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getSlug, slug);
        if (excludeId != null) query.ne(BlogTag::getId, excludeId);
        Long count = tagMapper.selectCount(query);
        return count != null && count > 0;
    }
}
