package com.starrainnotes.search.service;

import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;

// 搜索索引重建业务入口：全量重建、按类型重建与单文档索引。
public interface SearchRebuildService {

    long rebuild(String type);

    void rebuildAll();

    void rebuildType(String type);

    void indexBlog(BlogPostDocument post);

    void indexWork(PortfolioPublishedWork work);

    void syncEnglish(String contentType, Long contentId);
}
