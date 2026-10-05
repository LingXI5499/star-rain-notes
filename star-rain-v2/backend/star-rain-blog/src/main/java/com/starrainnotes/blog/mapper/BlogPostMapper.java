package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostNeighborVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BlogPostMapper {

    void insertPost(BlogPostEntity post);

    BlogPostEntity postById(@Param("id") Long id);

    /*
     * 加行锁读取。状态切换（publish / withdraw / delete）与媒体引用替换都先走这里：
     * 同一篇文章的两个并发请求会串行化，避免“撤回后又发布”这类交错结果。
     */
    BlogPostEntity postByIdForUpdate(@Param("id") Long id);

    // 公开详情只认 PUBLISHED：草稿与已撤回文章在 SQL 层就被排除
    BlogPostEntity publishedPostBySlug(@Param("slug") String slug);

    BlogPostNeighborVO publishedPrevious(@Param("publishedAt") LocalDateTime publishedAt,
                                         @Param("id") Long id);

    BlogPostNeighborVO publishedNext(@Param("publishedAt") LocalDateTime publishedAt,
                                     @Param("id") Long id);

    // slug 唯一性检查；excludeId 用于“改自己不算冲突”
    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    long adminPageCount(@Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("tagId") Long tagId,
                        @Param("topicId") Long topicId);

    List<BlogPostEntity> adminPage(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   @Param("tagId") Long tagId,
                                   @Param("topicId") Long topicId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    // 元数据局部更新：只有非 null 字段进入 SET，PATCH 语义靠这里实现
    int updatePostMeta(@Param("id") Long id,
                       @Param("slug") String slug,
                       @Param("title") String title,
                       @Param("summary") String summary,
                       @Param("updatedByAccountId") Long updatedByAccountId);

    // 封面单独更新：取消封面与保留封面是两种不同意图，不能靠“传 null”混淆
    int updatePostCover(@Param("id") Long id,
                        @Param("coverMediaAssetId") Long coverMediaAssetId,
                        @Param("updatedByAccountId") Long updatedByAccountId);

    int updatePostBody(@Param("id") Long id,
                       @Param("bodyMarkdown") String bodyMarkdown,
                       @Param("updatedByAccountId") Long updatedByAccountId);

    /*
     * 条件发布：只有 DRAFT / WITHDRAWN 才会成功，影响 0 行即表示状态已被并发改变。
     * published_at 用 COALESCE 保留首次发布时间，恢复已撤回文章不会被改写成“今天发布”。
     */
    int publishPost(@Param("id") Long id,
                    @Param("publishedAt") LocalDateTime publishedAt,
                    @Param("updatedByAccountId") Long updatedByAccountId);

    int withdrawPost(@Param("id") Long id,
                     @Param("withdrawnAt") LocalDateTime withdrawnAt,
                     @Param("updatedByAccountId") Long updatedByAccountId);

    // 物理删除：WHERE 里带状态条件，公开内容无法被直接删除
    int deletePost(@Param("id") Long id);

    long publishedPageCount(@Param("tagSlug") String tagSlug,
                            @Param("topicSlug") String topicSlug,
                            @Param("publishedFrom") LocalDateTime publishedFrom,
                            @Param("publishedTo") LocalDateTime publishedTo);

    List<BlogPostEntity> publishedPage(@Param("tagSlug") String tagSlug,
                                      @Param("topicSlug") String topicSlug,
                                      @Param("publishedFrom") LocalDateTime publishedFrom,
                                      @Param("publishedTo") LocalDateTime publishedTo,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /*
     * 前台专题页的成员分页：按 sr_blog_topic_post.sort_order 排，而不是时间倒序。
     * 顺序是专题的意义所在（人工策展），按时间排会把它降级成一个标签。
     */
    long publishedTopicPageCount(@Param("topicSlug") String topicSlug);

    List<BlogPostEntity> publishedTopicPage(@Param("topicSlug") String topicSlug,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    // 归档月份桶：给前台时间归档的侧栏，依据 published_at 分组
    List<BlogArchiveMonthVO> archiveMonths();

    List<BlogArchiveDayVO> archiveDays(@Param("publishedFrom") LocalDateTime publishedFrom,
                                       @Param("publishedTo") LocalDateTime publishedTo);

    // 模块间读取：Site 首页用
    List<BlogPostEntity> latestPublished(@Param("limit") int limit);

    List<BlogPostEntity> publishedByTopicSlug(@Param("topicSlug") String topicSlug,
                                              @Param("limit") int limit);

    // 模块间读取：Search / SEO 按 ID 游标增量重建索引
    List<BlogPostEntity> publishedAfterCursor(@Param("cursorId") Long cursorId,
                                              @Param("limit") int limit);

    BlogPostEntity publishedPostById(@Param("id") Long id);
}
