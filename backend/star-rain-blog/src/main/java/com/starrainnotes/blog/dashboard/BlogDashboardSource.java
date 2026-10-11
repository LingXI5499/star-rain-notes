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

@Component
@RequiredArgsConstructor
public class BlogDashboardSource implements DashboardSource {
    private final BlogPostService posts;

    @Override public String moduleCode() { return "BLOG"; }

    @Override public DashboardModuleData load() {
        PageResult<BlogPostAdminVO> recent = posts.page(query(null, 8));
        long drafts = posts.page(query("DRAFT", 1)).getTotal();
        List<DashboardRecentItem> items = recent.getItems().stream()
                .map(post -> new DashboardRecentItem(String.valueOf(post.getId()), "BLOG",
                        post.getTitle(), post.getStatus(), post.getUpdatedAt())).toList();
        return new DashboardModuleData(Map.of("total", recent.getTotal(), "drafts", drafts), items);
    }

    private BlogPostQueryDTO query(String status, int pageSize) {
        BlogPostQueryDTO query = new BlogPostQueryDTO();
        query.setPageSize(pageSize);
        query.setStatus(status);
        return query;
    }
}
