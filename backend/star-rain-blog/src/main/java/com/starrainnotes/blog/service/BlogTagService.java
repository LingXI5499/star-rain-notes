/*
 * BlogTagService：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogTagDTO、BlogTagQueryDTO、BlogTagVO、PageResult。
 * 阅读入口：page（分页读取博客标签）；listPublished（列出已发布）；create（创建博客标签）；update（更新博客标签）。
 */

package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogTagDTO;
import com.starrainnotes.blog.dto.BlogTagQueryDTO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;

/*
 * BLOG-004 标签管理。
 *
 * Tag 的语义是“多维分类”：一篇文章可以有多个，彼此无序，靠名字检索。
 * 需要顺序的策展场景请用 Topic，不要给 Tag 加排序字段。
 */
/**
 * 博客标签业务服务，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogTagService {
    /** Delete an unused tag; referenced tags must be disabled instead. */
    void delete(Long tagId);

    /**
     * 分页读取博客标签。
     *
     * @param query 查询参数；类型为 <code>BlogTagQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogTagVO&gt;</code> 的处理结果
     */
    PageResult<BlogTagVO> page(BlogTagQueryDTO query);

    // 前台可用的标签集合：只含 ENABLED 且至少有一篇已发布文章的标签
    /**
     * 列出已发布。
     * @return 本次处理得到的条目集合
     */
    List<BlogTagVO> listPublished();

    /**
     * 创建博客标签。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTagDTO</code>
     * @return 类型为 <code>BlogTagVO</code> 的处理结果
     */
    BlogTagVO create(BlogTagDTO request);

    /**
     * 更新博客标签。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>BlogTagDTO</code>
     * @return 类型为 <code>BlogTagVO</code> 的处理结果
     */
    BlogTagVO update(Long tagId, BlogTagDTO request);

    // 停用：不再接受新绑定，但保留已有绑定关系
    /**
     * 禁用博客标签。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     */
    void disable(Long tagId);

    /**
     * 启用博客标签。
     *
     * @param tagId 标签标识；类型为 <code>Long</code>
     */
    void enable(Long tagId);
}
