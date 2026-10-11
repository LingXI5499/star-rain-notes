/*
 * BlogPublicReadServiceImpl：博客模块的业务实现文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AnalyticsRecordApi、ContentViewEvent、BlogPublicReadService、BlogPublicService、BlogPostPublicDetailVO。
 * 阅读入口：read（读取博客公开读取）。
 */

package com.starrainnotes.blog.service.impl;

import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.blog.service.BlogPublicReadService;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 博客公开读取业务服务实现，属于博客模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class BlogPublicReadServiceImpl implements BlogPublicReadService {
    /**
     * 文章列表。
     * 协作类型：<code>BlogPublicService</code>。
     */
    private final BlogPublicService posts;
    /**
     * 统计。
     */
    private final ObjectProvider<AnalyticsRecordApi> analytics;

    /**
     * 读取博客公开读取。
     * 主要协作调用：<code>posts.postBySlug</code>、<code>analytics.getIfAvailable</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostPublicDetailVO</code> 的处理结果
     */
    @Override
    public BlogPostPublicDetailVO read(String slug) {
        BlogPostPublicDetailVO detail = posts.postBySlug(slug);
        AnalyticsRecordApi recorder = analytics.getIfAvailable();
        if (recorder != null)
            recorder.recordContentView(new ContentViewEvent("BLOG", detail.getId(), "/blog/posts/:slug"));
        return detail;
    }
}
