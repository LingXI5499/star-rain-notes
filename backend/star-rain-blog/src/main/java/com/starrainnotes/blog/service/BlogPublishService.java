package com.starrainnotes.blog.service;

/*
 * BLOG-008 / BLOG-009 发布状态机。
 *
 * 三个动作都是条件 UPDATE：判断与写入在同一条 SQL 里完成，
 * 重复点击或并发请求只会有一个成功，另一个得到 BLOG_POST_STATE_INVALID，
 * 不会出现“两个请求都以为自己发布成功”。
 */
public interface BlogPublishService {

    // DRAFT / WITHDRAWN → PUBLISHED
    void publish(Long postId);

    // PUBLISHED → WITHDRAWN
    void withdraw(Long postId);

    // WITHDRAWN → PUBLISHED，保留首次发布时间
    void restore(Long postId);
}
