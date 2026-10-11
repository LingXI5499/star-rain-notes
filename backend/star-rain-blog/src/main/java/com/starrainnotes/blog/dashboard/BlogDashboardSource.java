/*
 * BlogDashboardSource：博客模块的模块支持文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：BlogPostQueryDTO、BlogPostService、BlogPostAdminVO、DashboardSource、DashboardModuleData、DashboardRecentItem、PageResult。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载博客概览来源）。
 */

package com.starrainnotes.blog.dashboard;

import com.starrainnotes.blog.dto.BlogPostQueryDTO;
import com.starrainnotes.blog.service.BlogPostService;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import com.starrainnotes.common.result.PageResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 博客概览来源，属于博客模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class BlogDashboardSource implements DashboardSource {
    /**
     * 文章列表。
     * 协作类型：<code>BlogPostService</code>。
     */
    private final BlogPostService posts;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "BLOG"; }

    /**
     * 加载博客概览来源。
     * 主要协作调用：<code>posts.page</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        PageResult<BlogPostAdminVO> recent = posts.page(query(null, 8));
        long drafts = posts.page(query("DRAFT", 1)).getTotal();
        List<DashboardRecentItem> items = recent.getItems().stream()
                .map(post -> new DashboardRecentItem(String.valueOf(post.getId()), "BLOG",
                        post.getTitle(), post.getStatus(), post.getUpdatedAt())).toList();
        return new DashboardModuleData(Map.of("total", recent.getTotal(), "drafts", drafts), items);
    }

    /**
     * 查询参数处理入口。
     * 关键处理步骤调用 <code>query.setPageSize</code>、<code>query.setStatus</code>。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>BlogPostQueryDTO</code> 的处理结果
     */
    private BlogPostQueryDTO query(String status, int pageSize) {
        BlogPostQueryDTO query = new BlogPostQueryDTO();
        query.setPageSize(pageSize);
        query.setStatus(status);
        return query;
    }
}
