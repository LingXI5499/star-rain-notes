package com.starrainnotes.site.service;

import com.starrainnotes.site.dto.ContentCountsView;
import com.starrainnotes.site.dto.DashboardView;
import com.starrainnotes.site.dto.DraftCountsView;
import com.starrainnotes.site.dto.RecentContentView;
import com.starrainnotes.site.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Administrative dashboard query orchestration and site-timezone formatting. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final DashboardRepository repository;
    private final SiteSettingsTimezone timezone;

    public DashboardView getDashboard() {
        DashboardRepository.DashboardCounts counts = repository.counts();
        List<RecentContentView> recent = repository.recentContent().stream()
                .map(row -> new RecentContentView(row.type(), row.id(), row.title(), row.publishStatus(),
                        timezone.atSite(row.updatedAt()).format(ISO_OFFSET)))
                .toList();
        return new DashboardView(
                new ContentCountsView(counts.tutorials(), counts.chapters(), counts.blogPosts(), counts.portfolioProjects()),
                new DraftCountsView(counts.draftTutorials(), counts.draftChapters(), counts.draftBlogPosts(),
                        counts.draftPortfolioProjects()), recent);
    }
}
