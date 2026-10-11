/*
 * BlogTagMapper：博客模块的数据访问文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：BlogPostTagRow、BlogTagEntity、BlogTagVO。
 * 阅读入口：insertTag（写入标签）；tagById（按标识读取标签）；tagByIdForUpdate（按标识加行锁读取标签）；countBySlug（统计依据固定链接标识）。
 */

package com.starrainnotes.blog.mapper;

import com.starrainnotes.blog.dto.BlogPostTagRow;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.vo.BlogTagVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 博客标签数据映射，属于博客模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface BlogTagMapper {

    /**
     * 写入标签。
     *
     * 持久化操作：新增，涉及 <code>sr_blog_tag</code>。
     *
     * @param tag 标签；类型为 <code>BlogTagEntity</code>
     */
    void insertTag(BlogTagEntity tag);

    /**
     * 按标识读取标签。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogTagEntity</code> 的处理结果
     */
    BlogTagEntity tagById(@Param("id") Long id);

    // 加行锁读取：绑定、停用、改名都需要先锁住标签行，避免与并发绑定交错
    /**
     * 按标识加行锁读取标签。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>BlogTagEntity</code> 的处理结果
     */
    BlogTagEntity tagByIdForUpdate(@Param("id") Long id);

    /**
     * 统计依据固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("excludeId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    /**
     * 统计依据名称。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>。
     *
     * @param name 名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("name")</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("excludeId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 更新标签。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_tag</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param name 名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("name")</code>
     * @param description 描述；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("description")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateTag(@Param("id") Long id,
                  @Param("slug") String slug,
                  @Param("name") String name,
                  @Param("description") String description);

    /**
     * 更新状态。
     *
     * 持久化操作：更新，涉及 <code>sr_blog_tag</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 后台分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long adminPageCount(@Param("keyword") String keyword, @Param("status") String status);

    // postCount 是绑定总数（含草稿与已撤回），用于判断标签是否还在被使用
    /**
     * 后台分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>、<code>sr_blog_post_tag</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 符合 SQL 条件的记录数量
     */
    List<BlogTagVO> adminPage(@Param("keyword") String keyword,
                              @Param("status") String status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    // 前台筛选项：只统计已发布文章，避免出现“点进去 0 篇”的空标签
    /**
     * 已发布标签列表处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>、<code>sr_blog_post_tag</code>、<code>sr_blog_post</code>。
     * @return 符合 SQL 条件的记录数量
     */
    List<BlogTagVO> publishedTags();

    /**
     * 按文章标识读取标签列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post_tag</code>、<code>sr_blog_tag</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTagVO> tagsByPostId(@Param("postId") Long postId);

    // 批量取标签，供列表接口一次性组装，避免每篇文章一次查询
    /**
     * 按文章标识集合读取标签列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post_tag</code>、<code>sr_blog_tag</code>。
     *
     * @param postIds 文章标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("postIds")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostTagRow> tagsByPostIds(@Param("postIds") List<Long> postIds);

    // 绑定前校验标签状态用：返回 id 在给定集合内的标签实体
    /**
     * 按标识集合读取标签列表。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_tag</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("ids")</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTagEntity> tagsByIds(@Param("ids") List<Long> ids);

    /**
     * 写入文章标签。
     *
     * 持久化操作：新增，涉及 <code>sr_blog_post_tag</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @param tagId 标签标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tagId")</code>
     * @return SQL 写入影响的记录行数
     */
    int insertPostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    /**
     * 删除文章标签。
     *
     * 持久化操作：删除，涉及 <code>sr_blog_post_tag</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @param tagId 标签标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tagId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deletePostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    /**
     * 删除文章标签列表依据文章标识。
     *
     * 持久化操作：删除，涉及 <code>sr_blog_post_tag</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deletePostTagsByPostId(@Param("postId") Long postId);

    /**
     * 统计文章标签。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post_tag</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @param tagId 标签标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tagId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countPostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    // 发布前校验：正文已绑定的标签里是否有被停用的
    /**
     * 统计已禁用标签列表依据文章标识。
     *
     * 持久化操作：读取，涉及 <code>sr_blog_post_tag</code>、<code>sr_blog_tag</code>。
     *
     * @param postId 文章标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("postId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countDisabledTagsByPostId(@Param("postId") Long postId);
}
