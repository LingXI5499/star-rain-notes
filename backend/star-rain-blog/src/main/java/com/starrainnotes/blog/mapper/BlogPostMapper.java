/*
 * BlogPostMapper：博客模块的数据访问文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：BlogPostEntity、BlogArchiveMonthVO、BlogArchiveDayVO、BlogPostNeighborVO、BlogPublicStatsVO。
 * 阅读入口：insertPost（写入文章）；postById（按标识读取文章）；postByIdForUpdate（按标识加行锁读取文章）；publishedPostBySlug（按固定链接标识读取已发布文章）。
 */

package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostNeighborVO;
import com.starrainnotes.blog.vo.BlogPublicStatsVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 博客文章数据映射，属于博客模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface BlogPostMapper {

    /**
     * 写入文章。
     *
     * 持久化操作：新增，涉及 <code>sr_blog_post</code>。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     */
    void insertPost(BlogPostEntity post);

    /**
     * 按标识读取文章。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
    BlogPostEntity postById(@Param("id") Long id);

    /*
     * 加行锁读取。状态切换（publish / withdraw / delete）与媒体引用替换都先走这里：
     * 同一篇文章的两个并发请求会串行化，避免“撤回后又发布”这类交错结果。
     */
    /**
     * 按标识加行锁读取文章。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
    BlogPostEntity postByIdForUpdate(@Param("id") Long id);

    // 公开详情只认 PUBLISHED：草稿与已撤回文章在 SQL 层就被排除
    /**
     * 按固定链接标识读取已发布文章。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
    BlogPostEntity publishedPostBySlug(@Param("slug") String slug);

    /**
     * 读取前一个已发布条目。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param publishedAt 已发布时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedAt")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogPostNeighborVO</code> 的处理结果
     */
    BlogPostNeighborVO publishedPrevious(@Param("publishedAt") LocalDateTime publishedAt,
                                         @Param("id") Long id);

    /**
     * 读取后一个已发布条目。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param publishedAt 已发布时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedAt")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogPostNeighborVO</code> 的处理结果
     */
    BlogPostNeighborVO publishedNext(@Param("publishedAt") LocalDateTime publishedAt,
                                     @Param("id") Long id);

    // slug 唯一性检查；excludeId 用于“改自己不算冲突”
    /**
     * 统计依据固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("excludeId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    /**
     * 后台分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param tagId 标签标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tagId")</code>
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long adminPageCount(@Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("tagId") Long tagId,
                        @Param("topicId") Long topicId);

    /**
     * 后台分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param tagId 标签标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tagId")</code>
     * @param topicId 专题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("topicId")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostEntity> adminPage(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   @Param("tagId") Long tagId,
                                   @Param("topicId") Long topicId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    // 元数据局部更新：只有非 null 字段进入 SET，PATCH 语义靠这里实现
    /**
     * 更新文章Meta。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param title 标题；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("title")</code>
     * @param summary 摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("summary")</code>
     * @param updatedByAccountId 最近一次更新该记录的账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("updatedByAccountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePostMeta(@Param("id") Long id,
                       @Param("slug") String slug,
                       @Param("title") String title,
                       @Param("summary") String summary,
                       @Param("updatedByAccountId") Long updatedByAccountId);

    // 封面单独更新：取消封面与保留封面是两种不同意图，不能靠“传 null”混淆
    /**
     * 更新文章封面。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param coverMediaAssetId 封面媒体资产标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("coverMediaAssetId")</code>
     * @param updatedByAccountId 最近一次更新该记录的账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("updatedByAccountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePostCover(@Param("id") Long id,
                        @Param("coverMediaAssetId") Long coverMediaAssetId,
                        @Param("updatedByAccountId") Long updatedByAccountId);

    /**
     * 更新文章正文。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param bodyMarkdown Markdown 格式的正文；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("bodyMarkdown")</code>
     * @param updatedByAccountId 最近一次更新该记录的账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("updatedByAccountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePostBody(@Param("id") Long id,
                       @Param("bodyMarkdown") String bodyMarkdown,
                       @Param("updatedByAccountId") Long updatedByAccountId);

    /*
     * 条件发布：只有 DRAFT / WITHDRAWN 才会成功，影响 0 行即表示状态已被并发改变。
     * published_at 用 COALESCE 保留首次发布时间，恢复已撤回文章不会被改写成“今天发布”。
     */
    /**
     * 发布文章。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param publishedAt 已发布时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedAt")</code>
     * @param updatedByAccountId 最近一次更新该记录的账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("updatedByAccountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int publishPost(@Param("id") Long id,
                    @Param("publishedAt") LocalDateTime publishedAt,
                    @Param("updatedByAccountId") Long updatedByAccountId);

    /**
     * 撤回文章。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param withdrawnAt 已撤回时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("withdrawnAt")</code>
     * @param updatedByAccountId 最近一次更新该记录的账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("updatedByAccountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int withdrawPost(@Param("id") Long id,
                     @Param("withdrawnAt") LocalDateTime withdrawnAt,
                     @Param("updatedByAccountId") Long updatedByAccountId);

    // 物理删除：WHERE 里带状态条件，公开内容无法被直接删除
    /**
     * 删除文章。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deletePost(@Param("id") Long id);

    /*
     * 公开列表 / 归档的分页查询。
     * tagSlugs 是「命中任一」的集合（空集合表示不按标签筛选），
     * 用集合而不是单个 slug：多标签筛选必须在 SQL 里完成，否则分页的 total 就是错的。
     */
    /**
     * 已发布分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param tagSlugs 标签Slugs；类型为 <code>List&lt;String&gt;</code>；对应 MyBatis 命名参数 <code>@Param("tagSlugs")</code>
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("topicSlug")</code>
     * @param publishedFrom 已发布来源；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedFrom")</code>
     * @param publishedTo 已发布目标；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedTo")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long publishedPageCount(@Param("tagSlugs") List<String> tagSlugs,
                            @Param("topicSlug") String topicSlug,
                            @Param("publishedFrom") LocalDateTime publishedFrom,
                            @Param("publishedTo") LocalDateTime publishedTo);

    /**
     * 已发布分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param tagSlugs 标签Slugs；类型为 <code>List&lt;String&gt;</code>；对应 MyBatis 命名参数 <code>@Param("tagSlugs")</code>
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("topicSlug")</code>
     * @param publishedFrom 已发布来源；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedFrom")</code>
     * @param publishedTo 已发布目标；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedTo")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostEntity> publishedPage(@Param("tagSlugs") List<String> tagSlugs,
                                      @Param("topicSlug") String topicSlug,
                                      @Param("publishedFrom") LocalDateTime publishedFrom,
                                      @Param("publishedTo") LocalDateTime publishedTo,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /*
     * 前台专题页的成员分页：按 sr_blog_topic_post.sort_order 排，而不是时间倒序。
     * 顺序是专题的意义所在（人工策展），按时间排会把它降级成一个标签。
     */
    /**
     * 已发布专题分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>、<code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     *
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("topicSlug")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long publishedTopicPageCount(@Param("topicSlug") String topicSlug);

    /**
     * 已发布专题分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>、<code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("topicSlug")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostEntity> publishedTopicPage(@Param("topicSlug") String topicSlug,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    // 归档月份桶：给前台时间归档的侧栏，依据 published_at 分组
    /**
     * archiveMonths处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * @return 符合 SQL 条件的记录数量
     */
    List<BlogArchiveMonthVO> archiveMonths();

    // 博客首页右栏的写作统计：已发布篇数、正文字符数合计、最早发布时间
    /**
     * 已发布统计字段处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * @return 符合 SQL 条件的记录数量
     */
    BlogPublicStatsVO publishedStats();

    /**
     * archiveDays处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param publishedFrom 已发布来源；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedFrom")</code>
     * @param publishedTo 已发布目标；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("publishedTo")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<BlogArchiveDayVO> archiveDays(@Param("publishedFrom") LocalDateTime publishedFrom,
                                       @Param("publishedTo") LocalDateTime publishedTo);

    // 模块间读取：Site 首页用
    /**
     * 最新已发布处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostEntity> latestPublished(@Param("limit") int limit);

    /**
     * 按专题固定链接标识读取已发布。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>、<code>sr_blog_topic_post</code>、<code>sr_blog_topic</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("topicSlug")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostEntity> publishedByTopicSlug(@Param("topicSlug") String topicSlug,
                                              @Param("limit") int limit);

    // 模块间读取：Search / SEO 按 ID 游标增量重建索引
    /**
     * 已发布之后Cursor处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param cursorId cursor标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cursorId")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostEntity> publishedAfterCursor(@Param("cursorId") Long cursorId,
                                              @Param("limit") int limit);

    /**
     * 按标识读取已发布文章。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogPostEntity</code> 的处理结果
     */
    BlogPostEntity publishedPostById(@Param("id") Long id);
}
