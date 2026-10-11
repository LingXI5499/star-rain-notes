/*
 * BlogPostService：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogPostBodyDTO、BlogPostCreateDTO、BlogPostQueryDTO、BlogPostUpdateDTO、BlogPostAdminDetailVO、BlogPostAdminVO、PageResult。
 * 阅读入口：page（分页读取博客文章）；detail（读取详情博客文章）；preview（预览博客文章）；create（创建博客文章）。
 */

package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogPostBodyDTO;
import com.starrainnotes.blog.dto.BlogPostCreateDTO;
import com.starrainnotes.blog.dto.BlogPostQueryDTO;
import com.starrainnotes.blog.dto.BlogPostUpdateDTO;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.common.result.PageResult;

/*
 * BLOG-003 文章的模块内服务契约。
 *
 * 状态切换（发布 / 撤回 / 恢复）不在这里，由 BlogPublishService 承担：
 * 两件事的失败模式完全不同 —— 编辑是“参数不合法”，发布是“内容还不满足公开条件”。
 */
/**
 * 博客文章业务服务，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogPostService {

    /**
     * 分页读取博客文章。
     *
     * @param query 查询参数；类型为 <code>BlogPostQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogPostAdminVO&gt;</code> 的处理结果
     */
    PageResult<BlogPostAdminVO> page(BlogPostQueryDTO query);

    /**
     * 读取详情博客文章。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostAdminDetailVO</code> 的处理结果
     */
    BlogPostAdminDetailVO detail(Long postId);

    // 预览与详情数据相同，但语义是“发布前看效果”，因此单独暴露端点
    /**
     * 预览博客文章。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostAdminDetailVO</code> 的处理结果
     */
    BlogPostAdminDetailVO preview(Long postId);

    /**
     * 创建博客文章。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogPostCreateDTO</code>
     * @return 类型为 <code>BlogPostAdminVO</code> 的处理结果
     */
    BlogPostAdminVO create(BlogPostCreateDTO request);

    /**
     * 更新博客文章。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogPostUpdateDTO</code>
     * @return 类型为 <code>BlogPostAdminVO</code> 的处理结果
     */
    BlogPostAdminVO update(Long postId, BlogPostUpdateDTO request);

    // 正文单独更新，避免小改一次标题就重传整篇 Markdown
    /**
     * 更新正文。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogPostBodyDTO</code>
     * @return 类型为 <code>BlogPostAdminDetailVO</code> 的处理结果
     */
    BlogPostAdminDetailVO updateBody(Long postId, BlogPostBodyDTO request);

    // 物理删除：只允许 DRAFT / WITHDRAWN，并同步清理标签、专题与媒体引用
    /**
     * 删除博客文章。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
    void delete(Long postId);
}
