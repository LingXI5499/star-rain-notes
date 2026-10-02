package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.dto.BlogPostTopicRow;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicOrderItem;
import com.starrainnotes.blog.entity.BlogTopicEntity;
import com.starrainnotes.blog.vo.BlogTopicVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BlogTopicMapper {

    void insertTopic(BlogTopicEntity topic);

    BlogTopicEntity topicById(@Param("id") Long id);

    // 加行锁读取：加入成员、调整顺序、停用都要求专题状态在本事务内稳定
    BlogTopicEntity topicByIdForUpdate(@Param("id") Long id);

    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    int updateTopic(@Param("id") Long id,
                    @Param("slug") String slug,
                    @Param("name") String name,
                    @Param("description") String description);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    long adminPageCount(@Param("keyword") String keyword, @Param("status") String status);

    List<BlogTopicVO> adminPage(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    // 前台专题入口：只展示 ENABLED 且至少有一篇已发布文章的专题
    List<BlogTopicVO> publishedTopics();

    List<BlogTopicVO> topicsByPostId(@Param("postId") Long postId);

    List<BlogPostTopicRow> topicsByPostIds(@Param("postIds") List<Long> postIds);

    // 专题成员按 sort_order 升序：这是 Topic 与 Tag 的分水岭，顺序必须稳定可复现
    List<BlogTopicMemberRow> topicMembers(@Param("topicId") Long topicId);

    long countTopicPosts(@Param("topicId") Long topicId);

    // 当前最大序号，null 表示专题还没有成员
    Integer maxSortOrder(@Param("topicId") Long topicId);

    int insertTopicPost(@Param("topicId") Long topicId,
                        @Param("postId") Long postId,
                        @Param("sortOrder") int sortOrder);

    int deleteTopicPost(@Param("topicId") Long topicId, @Param("postId") Long postId);

    // 删文章时清理它的全部专题关系
    int deleteTopicPostsByPostId(@Param("postId") Long postId);

    long countTopicPost(@Param("topicId") Long topicId, @Param("postId") Long postId);

    /*
     * BLOG-007 排序：批量写入最终序号。
     *
     * 移除成员后的“序号前移”也复用它 —— 先按顺序读回成员，再按下标重写 1..n，
     * 比在 SQL 里用窗口函数自我更新更直白，也更容易在单测里断言写入的序号。
     */

    int updateTopicOrder(@Param("topicId") Long topicId,
                         @Param("items") List<BlogTopicOrderItem> items);
}
