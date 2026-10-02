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
public interface BlogPostService {

    PageResult<BlogPostAdminVO> page(BlogPostQueryDTO query);

    BlogPostAdminDetailVO detail(Long postId);

    // 预览与详情数据相同，但语义是“发布前看效果”，因此单独暴露端点
    BlogPostAdminDetailVO preview(Long postId);

    BlogPostAdminVO create(BlogPostCreateDTO request);

    BlogPostAdminVO update(Long postId, BlogPostUpdateDTO request);

    // 正文单独更新，避免小改一次标题就重传整篇 Markdown
    BlogPostAdminDetailVO updateBody(Long postId, BlogPostBodyDTO request);

    // 物理删除：只允许 DRAFT / WITHDRAWN，并同步清理标签、专题与媒体引用
    void delete(Long postId);
}
