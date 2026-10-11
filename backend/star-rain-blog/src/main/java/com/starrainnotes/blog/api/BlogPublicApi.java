/*
 * BlogPublicApi：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：BlogPostSummary、BlogTopicSummary。
 * 阅读入口：latestPublished（最新已发布处理入口）；featuredByTopic（按专题读取精选）；publishedTopics（已发布专题列表处理入口）；publishedTopicBySlug（按固定链接标识读取已发布专题）。
 */

package com.starrainnotes.blog.api;

import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.blog.api.dto.BlogTopicSummary;
import java.util.List;

/*
 * 博客对外公开读取契约。
 *
 * 给 Site 首页与任何需要“展示最新文章”的模块使用。
 * 只提供已发布内容，调用方无法通过参数拿到草稿。
 */
/**
 * 博客公开接口，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface BlogPublicApi {

    // 首页最新文章；limit 由调用方决定，实现内部会收敛到合理上限
    /**
     * 最新已发布处理入口。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostSummary> latestPublished(int limit);

    // 某个专题下的文章，按专题内人工顺序之外的发布时间倒序展示
    /**
     * 按专题读取精选。
     *
     * @param topicSlug 专题固定链接标识；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostSummary> featuredByTopic(String topicSlug, int limit);

    /** Enabled topics that have public pages, in navigation order. */
    List<BlogTopicSummary> publishedTopics();

    /** An enabled topic by slug, or null when the page is not public. */
    BlogTopicSummary publishedTopicBySlug(String slug);
}
