package com.starrainnotes.seo.event;

import com.starrainnotes.blog.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.event.BlogPostWithdrawnEvent;
import com.starrainnotes.portfolio.event.WorkPublicationChangedEvent;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.event.ProfileChangedEvent;
import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.service.SeoTutorialRefreshService;
import com.starrainnotes.site.event.SiteConfigChangedEvent;
import com.starrainnotes.tutorial.content.event.TutorialPublicationChangedEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoContentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(SeoContentEventConsumer.class);
    private final SeoRefreshApi refresh;
    private final SeoTutorialRefreshService tutorialRefresh;
    private final SeoPageMapper pages;
    private final SeoNotificationService notifications;
    private final ProfilePublicApi profiles;

    @EventListener
    public void onSiteConfigChanged(SiteConfigChangedEvent event) {
        safe("SITE", null, () -> upsert("/"));
    }

    @EventListener
    public void onBlogPublished(BlogPostPublishedEvent event) {
        safe("BLOG", event.getPostId(), () -> upsert("/blog/posts/" + event.getSlug()));
    }

    @EventListener
    public void onBlogWithdrawn(BlogPostWithdrawnEvent event) {
        safe("BLOG", event.getPostId(), () -> remove("/blog/posts/" + event.getSlug()));
    }

    @EventListener
    public void onWorkChanged(WorkPublicationChangedEvent event) {
        safe("PORTFOLIO", event.getWorkId(), () -> {
            String path = "/portfolio/" + event.getSlug();
            if (event.isPublished()) upsert(path);
            else remove(path);
        });
    }

    @EventListener
    public void onProfileChanged(ProfileChangedEvent event) {
        safe("PROFILE", event.getProfileId(), () -> {
            if (profiles.publicProfileId() == null) remove("/about");
            else upsert("/about");
        });
    }

    @EventListener
    public void onTutorialChanged(TutorialPublicationChangedEvent event) {
        safe("TUTORIAL", event.getTutorialId(), () -> {
            String tutorialPath = "/tutorials/" + event.getSlug();
            String chapterPrefix = tutorialPath + "/";
            Set<String> before = new HashSet<>(pages.activePathsByPrefix(chapterPrefix));
            if ("WITHDRAWN".equals(event.getAction())) {
                tutorialRefresh.remove(event.getSlug());
                notifications.enqueue(tutorialPath, "DELETE");
                before.forEach(path -> notifications.enqueue(path, "DELETE"));
            } else if ("PUBLISHED".equals(event.getAction()) || "RESTORED".equals(event.getAction())) {
                tutorialRefresh.refresh(event.getSlug());
                notifications.enqueue(tutorialPath, "UPSERT");
                List<String> after = pages.activePathsByPrefix(chapterPrefix);
                after.forEach(path -> notifications.enqueue(path, "UPSERT"));
                before.removeAll(after);
                before.forEach(path -> notifications.enqueue(path, "DELETE"));
            }
        });
    }

    private void upsert(String path) {
        refresh.refreshRoute(path);
        notifications.enqueue(path, "UPSERT");
    }

    private void remove(String path) {
        refresh.removeRoute(path);
        notifications.enqueue(path, "DELETE");
    }

    private void safe(String type, Long id, Runnable action) {
        try { action.run(); }
        catch (RuntimeException exception) {
            log.error("SEO refresh failed for {} {}; run rebuild", type, id, exception);
        }
    }
}
