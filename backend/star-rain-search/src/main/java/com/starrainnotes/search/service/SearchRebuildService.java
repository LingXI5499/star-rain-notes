/*
 * SearchRebuildService：搜索模块的业务契约文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogPostDocument、PortfolioPublishedWork。
 * 阅读入口：rebuild（重建搜索Rebuild）；rebuildAll（重建全部）；rebuildType（重建类型）；indexBlog（索引博客处理入口）。
 */

package com.starrainnotes.search.service;

import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;

// 搜索索引重建业务入口：全量重建、按类型重建与单文档索引。
/**
 * 搜索Rebuild业务服务，属于搜索模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SearchRebuildService {

    /**
     * 重建搜索Rebuild。
     *
     * @param type 类型；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    long rebuild(String type);

    /**
     * 重建全部。
     */
    void rebuildAll();

    /**
     * 重建类型。
     *
     * @param type 类型；类型为 <code>String</code>
     */
    void rebuildType(String type);

    /**
     * 索引博客处理入口。
     *
     * @param post 文章；类型为 <code>BlogPostDocument</code>
     */
    void indexBlog(BlogPostDocument post);

    /**
     * 索引作品处理入口。
     *
     * @param work 作品；类型为 <code>PortfolioPublishedWork</code>
     */
    void indexWork(PortfolioPublishedWork work);

    /**
     * 同步英语。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     */
    void syncEnglish(String contentType, Long contentId);
}
