package com.starrainnotes.search.event;

import com.starrainnotes.blog.api.event.BlogPostPublishedEvent;
import com.starrainnotes.blog.api.event.BlogPostWithdrawnEvent;
import com.starrainnotes.blog.api.event.BlogPostChangedEvent;
import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.profile.api.event.ProfileChangedEvent;
import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.search.service.SearchIndexService;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SearchEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(SearchEventConsumer.class);
    private final SearchRebuildService rebuild;
    private final SearchIndexService index;
    private final BlogSearchSourceApi blogs;
    private final PortfolioReferenceApi works;

    @TransactionalEventListener(fallbackExecution = true)
    public void onEnglishChanged(EnglishSearchContentChangedEvent event) {
        safe(event.getContentType(), event.getContentId(), () -> {
            if (event.getContentId() == null) rebuild.rebuildType(event.getContentType());
            else rebuild.syncEnglish(event.getContentType(), event.getContentId());
        });
    }

    @EventListener
    public void onBlogPublished(BlogPostPublishedEvent event) {
        safe("BLOG", event.getPostId(), () -> {
            var post = blogs.getPublishedDocument(event.getPostId());
            if (post != null) rebuild.indexBlog(post);
        });
    }

    @EventListener
    public void onBlogWithdrawn(BlogPostWithdrawnEvent event) {
        safe("BLOG", event.getPostId(), () -> index.removeByContent("BLOG", event.getPostId()));
    }

    @EventListener
    public void onBlogChanged(BlogPostChangedEvent event) {
        safe("BLOG", event.postId(), () -> {
            var post = blogs.getPublishedDocument(event.postId());
            if (post != null) rebuild.indexBlog(post);
        });
    }

    @EventListener
    public void onTutorialChanged(TutorialPublicationChangedEvent event) {
        safe("TUTORIAL", event.getTutorialId(), () -> rebuild.rebuildType("TUTORIAL"));
    }

    @EventListener
    public void onWorkChanged(WorkPublicationChangedEvent event) {
        safe("PORTFOLIO", event.getWorkId(), () -> {
            if (event.isPublished()) works.publishedWork(event.getWorkId()).ifPresent(rebuild::indexWork);
            else index.removeByContent("PORTFOLIO", event.getWorkId());
        });
    }

    @EventListener
    public void onProfileChanged(ProfileChangedEvent event) {
        safe("PROFILE", event.getProfileId(), () -> rebuild.rebuildType("PROFILE"));
    }

    private void safe(String type, Long id, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // Index is derived state: a failed consumer must not turn a committed publication into a failed HTTP response.
            log.error("Search index sync failed for {} {}; run rebuild", type, id, exception);
        }
    }
}
