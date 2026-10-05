package com.starrainnotes.site.service;

import com.starrainnotes.analytics.api.AnalyticsQueryApi;
import com.starrainnotes.blog.api.BlogReferenceApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.dto.HotContentItem;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialReferenceApi;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotContentSectionProvider implements HomeSectionProvider {
    private final AnalyticsQueryApi analytics;
    private final BlogReferenceApi blogs;
    private final TutorialReferenceApi tutorials;
    private final PortfolioReferenceApi works;
    private final HomeSectionLimit limits;
    @Override public String sectionCode() { return "HOT_CONTENT"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return analytics.hotContent(null, today.minusDays(29), today, limits.of(section) * 2).stream()
                .map(this::publicItem).filter(java.util.Objects::nonNull)
                .limit(limits.of(section)).toList();
    }

    private HotContentItem publicItem(Map<String, Object> row) {
        String type = String.valueOf(row.get("contentType"));
        Long id = ((Number) row.get("contentId")).longValue();
        long count = ((Number) row.get("viewCount")).longValue();
        if ("BLOG".equals(type)) {
            BlogPostSummary post = blogs.summary(id);
            return post == null || !blogs.isPublished(id) ? null
                    : new HotContentItem(type, post.getTitle(), "/blog/posts/" + post.getSlug(), count);
        }
        if ("TUTORIAL".equals(type)) {
            PublishedTutorial tutorial = tutorials.publishedTutorial(id).orElse(null);
            return tutorial == null ? null
                    : new HotContentItem(type, tutorial.getTitle(), "/tutorials/" + tutorial.getSlug(), count);
        }
        if ("PORTFOLIO".equals(type)) {
            PortfolioPublishedWork work = works.publishedWork(id).orElse(null);
            return work == null ? null
                    : new HotContentItem(type, work.getTitle(), "/portfolio/" + work.getSlug(), count);
        }
        return null;
    }
}
