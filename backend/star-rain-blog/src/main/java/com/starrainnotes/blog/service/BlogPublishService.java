/*
 * BlogPublishService：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：publish（发布博客发布）；withdraw（撤回博客发布）；restore（恢复博客发布）。
 */

package com.starrainnotes.blog.service;

/*
 * BLOG-008 / BLOG-009 发布状态机。
 *
 * 三个动作都是条件 UPDATE：判断与写入在同一条 SQL 里完成，
 * 重复点击或并发请求只会有一个成功，另一个得到 BLOG_POST_STATE_INVALID，
 * 不会出现“两个请求都以为自己发布成功”。
 */
/**
 * 博客发布业务服务，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogPublishService {

    // DRAFT / WITHDRAWN → PUBLISHED
    /**
     * 发布博客发布。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
    void publish(Long postId);

    // PUBLISHED → WITHDRAWN
    /**
     * 撤回博客发布。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
    void withdraw(Long postId);

    // WITHDRAWN → PUBLISHED，保留首次发布时间
    /**
     * 恢复博客发布。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     */
    void restore(Long postId);
}
