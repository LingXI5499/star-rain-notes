/*
 * BlogTopicMapper：博客模块的数据访问文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：BlogPostTopicRow、BlogTopicMemberRow、BlogTopicOrderItem、BlogTopicEntity、BlogTopicVO。
 * 阅读入口：insertTopic（写入专题）；topicById（按标识读取专题）；topicByIdForUpdate（按标识加行锁读取专题）；countBySlug（统计依据固定链接标识）。
 */

package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.dto.BlogPostTopicRow;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicOrderItem;
import com.starrainnotes.blog.entity.BlogTopicEntity;
import com.starrainnotes.blog.vo.BlogTopicVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 博客专题数据映射，属于博客模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface BlogTopicMapper {

    /**
     * 写入专题。
     *
     * 持久化操作：新增，涉及 <code>sr_blog_topic</code>。
     *
     * @param topic 专题；类型为 <code>BlogTopicEntity</code>
     */
    void insertTopic(BlogTopicEntity topic);

    /**
     * 按标识读取专题。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogTopicEntity</code> 的处理结果
     */
    BlogTopicEntity topicById(@Param("id") Long id);

    // 加行锁读取：加入成员、调整顺序、停用都要求专题状态在本事务内稳定
    /**
     * 按标识加行锁读取专题。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogTopicEntity</code> 的处理结果
     */
    BlogTopicEntity topicByIdForUpdate(@Param("id") Long id);

    /**
     * 统计依据固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("excludeId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    /**
     * 更新专题。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_topic</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param name 名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("name")</code>
     * @param description 描述；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("description")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateTopic(@Param("id") Long id,
                    @Param("slug") String slug,
                    @Param("name") String name,
                    @Param("description") String description);

    /**
     * 更新状态。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_topic</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 只删专题本身：成员关系由调用方先确认已清空（见 BlogTopicServiceImpl#delete）
    /**
     * 删除专题。
     *
     * 持久化操作：删除，涉及 <code>sr_blog_topic</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteTopic(@Param("id") Long id);

    /**
     * minimum排序顺序处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>。
     * @return 类型为 <code>Integer</code> 的处理结果
     */
    Integer minimumSortOrder();

    /**
     * 更新Presentation。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_topic</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @param featured 精选；类型为 <code>boolean</code>；对应 MyBatis 命名参数 <code>@Param("featured")</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePresentation(@Param("id") Long id, @Param("sortOrder") int sortOrder,
                           @Param("featured") boolean featured);

    /**
     * 后台分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long adminPageCount(@Param("keyword") String keyword, @Param("status") String status);

    /**
     * 后台分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>、<code>sr_blog_topic_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<BlogTopicVO> adminPage(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    // 前台专题入口：只展示 ENABLED 且至少有一篇已发布文章的专题
    /**
     * 已发布专题列表处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>、<code>sr_blog_topic_post</code>、<code>sr_blog_post</code>。
     * @return 符合 SQL 条件的记录数量
     */
    List<BlogTopicVO> publishedTopics();

    /*
     * 前台专题页的专题本身。
     *
     * 与 publishedTopics 同一套可见性规则（ENABLED + 至少一篇已发布成员），
     * 因此“侧栏里点得到”与“页面打得开”是同一个集合，不会一边列着一边 404。
     */
    /**
     * 按固定链接标识读取已发布专题。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic</code>、<code>sr_blog_topic_post</code>、<code>sr_blog_post</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 符合 SQL 条件的记录数量
     */
    BlogTopicVO publishedTopicBySlug(@Param("slug") String slug);

    /*
     * 公开页面上的专题标记：停用专题不出现在文章卡片与详情页上。
     * 与 topicsByPostId 分开，是因为后台还要看到停用专题才能重新启用它。
     */
    /**
     * 按文章标识读取公开专题列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicVO> publicTopicsByPostId(@Param("postId") Long postId);

    /**
     * 按文章标识集合读取公开专题列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     *
     * @param postIds 文章标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("postIds")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostTopicRow> publicTopicsByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 按文章标识读取专题列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicVO> topicsByPostId(@Param("postId") Long postId);

    /**
     * 按文章标识集合读取专题列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     *
     * @param postIds 文章标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("postIds")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostTopicRow> topicsByPostIds(@Param("postIds") List<Long> postIds);

    // 专题成员按 sort_order 升序：这是 Topic 与 Tag 的分水岭，顺序必须稳定可复现
    /**
     * 专题Members处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>、<code>sr_blog_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicMemberRow> topicMembers(@Param("topicId") Long topicId);

    /**
     * 统计专题文章列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countTopicPosts(@Param("topicId") Long topicId);

    // 当前最大序号，null 表示专题还没有成员
    /**
     * 最大排序顺序处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @return 类型为 <code>Integer</code> 的处理结果
     */
    Integer maxSortOrder(@Param("topicId") Long topicId);

    /**
     * 写入专题文章。
     *
     * 持久化操作：新增，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertTopicPost(@Param("topicId") Long topicId,
                        @Param("postId") Long postId,
                        @Param("sortOrder") int sortOrder);

    /**
     * 删除专题文章。
     *
     * 持久化操作：删除，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteTopicPost(@Param("topicId") Long topicId, @Param("postId") Long postId);

    // 删文章时清理它的全部专题关系
    /**
     * 删除专题文章列表依据文章标识。
     *
     * 持久化操作：删除，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteTopicPostsByPostId(@Param("postId") Long postId);

    /**
     * 统计专题文章。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countTopicPost(@Param("topicId") Long topicId, @Param("postId") Long postId);

    /*
     * BLOG-007 排序：批量写入最终序号。
     *
     * 移除成员后的“序号前移”也复用它 —— 先按顺序读回成员，再按下标重写 1..n，
     * 比在 SQL 里用窗口函数自我更新更直白，也更容易在单测里断言写入的序号。
     */

    /**
     * 更新专题顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_topic_post</code>。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @param items 条目集合；类型为 <code>List&lt;BlogTopicOrderItem&gt;</code>；对应 MyBatis 命名参数 <code>@Param("items")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateTopicOrder(@Param("topicId") Long topicId,
                         @Param("items") List<BlogTopicOrderItem> items);
}
