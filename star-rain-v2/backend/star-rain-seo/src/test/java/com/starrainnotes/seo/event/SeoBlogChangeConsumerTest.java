package com.starrainnotes.seo.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.starrainnotes.blog.api.event.BlogPostChangedEvent;
import com.starrainnotes.blog.api.event.BlogTopicChangedEvent;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.service.SeoTutorialRefreshService;
import org.junit.jupiter.api.Test;

class SeoBlogChangeConsumerTest {
    private final SeoRefreshApi refresh = mock(SeoRefreshApi.class);
    private final SeoNotificationService notifications = mock(SeoNotificationService.class);
    private final SeoContentEventConsumer consumer = new SeoContentEventConsumer(refresh,
            mock(SeoTutorialRefreshService.class), notifications, mock(ProfilePublicApi.class));

    @Test
    void renamedPublishedPostRemovesOldUrlAndRefreshesNewUrl() {
        consumer.onBlogChanged(new BlogPostChangedEvent(9L, "old-post", "new-post"));

        verify(refresh).removeRoute("/blog/posts/old-post");
        verify(refresh).refreshRoute("/blog/posts/new-post");
        verify(notifications).enqueue("/blog/posts/old-post", "DELETE");
        verify(notifications).enqueue("/blog/posts/new-post", "UPSERT");
    }

    @Test
    void disabledTopicIsRemovedFromSitemap() {
        consumer.onBlogTopicChanged(new BlogTopicChangedEvent(3L, "java", "java", false));

        verify(refresh).removeRoute("/blog/topics/java");
        verify(notifications).enqueue("/blog/topics/java", "DELETE");
    }
}
