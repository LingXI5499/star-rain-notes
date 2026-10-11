/*
 * BlogTopicService：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogTopicDTO、BlogTopicMemberRow、BlogTopicQueryDTO、BlogTopicVO、PageResult。
 * 阅读入口：page（分页读取博客专题）；listPublished（列出已发布）；create（创建博客专题）；update（更新博客专题）。
 */

package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogTopicDTO;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicQueryDTO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;

/*
 * BLOG-005 / BLOG-006 / BLOG-007 专题管理与专题内排序。
 *
 * Topic 的语义是“人工策展的有序专题”：加入顺序由人决定，且顺序本身是内容的一部分。
 * 因此这里比 Tag 多了成员管理、sortOrder 与整体重排三类操作。
 */
/**
 * 博客专题业务服务，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogTopicService {

    /**
     * 分页读取博客专题。
     *
     * @param query 查询参数；类型为 <code>BlogTopicQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogTopicVO&gt;</code> 的处理结果
     */
    PageResult<BlogTopicVO> page(BlogTopicQueryDTO query);

    // 前台专题入口：只含 ENABLED 且至少有一篇已发布文章的专题
    /**
     * 列出已发布。
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicVO> listPublished();

    /**
     * 创建博客专题。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTopicDTO</code>
     * @return 类型为 <code>BlogTopicVO</code> 的处理结果
     */
    BlogTopicVO create(BlogTopicDTO request);

    /**
     * 更新博客专题。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTopicDTO</code>
     * @return 类型为 <code>BlogTopicVO</code> 的处理结果
     */
    BlogTopicVO update(Long topicId, BlogTopicDTO request);

    /**
     * 调整顺序专题列表。
     *
     * @param topicIds 专题标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderTopics(List<Long> topicIds);

    /*
     * 删除专题：仅空专题（成员数为 0）可以删，有成员报 BLOG_TOPIC_NOT_EMPTY。
     * 需要下架但保留策展结果时用 disable —— 两者的差别就是「要不要留住成员与顺序」。
     */
    /**
     * 删除博客专题。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     */
    void delete(Long topicId);

    // 停用：不在前台展示，但成员与顺序全部保留
    /**
     * 禁用博客专题。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     */
    void disable(Long topicId);

    /**
     * 启用博客专题。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     */
    void enable(Long topicId);

    // 专题成员（按 sortOrder 升序），后台排序界面用
    /**
     * members处理入口。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicMemberRow> members(Long topicId);

    // BLOG-006 加入：追加到末尾
    /**
     * 添加文章。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     */
    void addPost(Long topicId, Long postId);

    // BLOG-006 移出：移除后压缩序号，保持 1..n 连续
    /**
     * 移除文章。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     */
    void removePost(Long topicId, Long postId);

    // BLOG-007 重排：要求 postIds 与当前成员集合完全一致，序号按下标重写为 1..n
    /**
     * 调整顺序博客专题。
     *
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param postIds 文章标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorder(Long topicId, List<Long> postIds);
}
