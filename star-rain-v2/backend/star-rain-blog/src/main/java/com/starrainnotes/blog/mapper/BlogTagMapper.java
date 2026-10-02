package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.dto.BlogPostTagRow;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.vo.BlogTagVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BlogTagMapper {

    void insertTag(BlogTagEntity tag);

    BlogTagEntity tagById(@Param("id") Long id);

    // 加行锁读取：绑定、停用、改名都需要先锁住标签行，避免与并发绑定交错
    BlogTagEntity tagByIdForUpdate(@Param("id") Long id);

    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    int updateTag(@Param("id") Long id,
                  @Param("slug") String slug,
                  @Param("name") String name,
                  @Param("description") String description);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    long adminPageCount(@Param("keyword") String keyword, @Param("status") String status);

    // postCount 是绑定总数（含草稿与已撤回），用于判断标签是否还在被使用
    List<BlogTagVO> adminPage(@Param("keyword") String keyword,
                              @Param("status") String status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    // 前台筛选项：只统计已发布文章，避免出现“点进去 0 篇”的空标签
    List<BlogTagVO> publishedTags();

    List<BlogTagVO> tagsByPostId(@Param("postId") Long postId);

    // 批量取标签，供列表接口一次性组装，避免每篇文章一次查询
    List<BlogPostTagRow> tagsByPostIds(@Param("postIds") List<Long> postIds);

    // 绑定前校验标签状态用：返回 id 在给定集合内的标签实体
    List<BlogTagEntity> tagsByIds(@Param("ids") List<Long> ids);

    int insertPostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    int deletePostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    int deletePostTagsByPostId(@Param("postId") Long postId);

    long countPostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    // 发布前校验：正文已绑定的标签里是否有被停用的
    long countDisabledTagsByPostId(@Param("postId") Long postId);
}
