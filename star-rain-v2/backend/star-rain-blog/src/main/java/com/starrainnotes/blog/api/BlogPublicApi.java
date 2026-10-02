package com.starrainnotes.blog.api;

import com.starrainnotes.blog.api.dto.BlogPostSummary;
import java.util.List;

/*
 * 博客对外公开读取契约。
 *
 * 给 Site 首页与任何需要“展示最新文章”的模块使用。
 * 只提供已发布内容，调用方无法通过参数拿到草稿。
 */
public interface BlogPublicApi {

    // 首页最新文章；limit 由调用方决定，实现内部会收敛到合理上限
    List<BlogPostSummary> latestPublished(int limit);

    // 某个专题下的文章，按专题内人工顺序之外的发布时间倒序展示
    List<BlogPostSummary> featuredByTopic(String topicSlug, int limit);
}
