package com.starrainnotes.tutorial.content.dashboard;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialDashboardSource implements DashboardSource {
    private final TutorialContentService tutorials;

    @Override public String moduleCode() { return "TUTORIAL"; }

    @Override public DashboardModuleData load() {
        PageResult<TutorialAdminVO> first = tutorials.tutorials(1, 100);
        List<TutorialAdminVO> rows = new ArrayList<>(first.getItems());
        int pages = (int) Math.ceil(first.getTotal() / 100.0);
        for (int page = 2; page <= pages; page++) rows.addAll(tutorials.tutorials(page, 100).getItems());
        long chapters = rows.stream().mapToLong(TutorialAdminVO::getChapterCount).sum();
        long drafts = rows.stream().filter(row -> "NEVER_PUBLISHED".equals(row.getPublicationStatus())).count();
        long draftChapters = rows.stream().filter(row -> "NEVER_PUBLISHED".equals(row.getPublicationStatus()))
                .mapToLong(TutorialAdminVO::getChapterCount).sum();
        List<DashboardRecentItem> recent = rows.stream()
                .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                .limit(8).map(row -> new DashboardRecentItem(row.getId(), "TUTORIAL", row.getTitle(),
                        "IN_REVIEW".equals(row.getEditingStatus()) ? "IN_REVIEW" : row.getPublicationStatus(),
                        row.getUpdatedAt())).toList();
        return new DashboardModuleData(Map.of("total", first.getTotal(), "drafts", drafts,
                "chapters", chapters, "draftChapters", draftChapters), recent);
    }
}
