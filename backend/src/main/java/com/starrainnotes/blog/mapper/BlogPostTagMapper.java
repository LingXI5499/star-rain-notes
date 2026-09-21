package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.entity.BlogTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Persistence boundary for the {@code blog_post_tag} relation. */
@Mapper
public interface BlogPostTagMapper {

    List<Long> selectPostIdsByTagSlug(@Param("tagSlug") String tagSlug);

    int deleteByPostId(@Param("postId") Long postId);

    int insertBatch(@Param("postId") Long postId, @Param("tagIds") List<Long> tagIds);

    List<BlogTag> selectTagsByPostId(@Param("postId") Long postId);

    List<BlogPostTagRow> selectTagsByPostIds(@Param("postIds") List<Long> postIds);

    long countByTagId(@Param("tagId") Long tagId);

    record BlogPostTagRow(Long postId, Long tagId, String name, String slug) {
    }
}
