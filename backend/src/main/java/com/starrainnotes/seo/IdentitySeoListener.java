package com.starrainnotes.seo;

import com.starrainnotes.profile.ProfileChangedEvent;
import com.starrainnotes.site.SiteSettingsChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IdentitySeoListener {
    private final ApplicationEventPublisher events;
    private final SeoProperties properties;

    public IdentitySeoListener(ApplicationEventPublisher events, SeoProperties properties) {
        this.events = events;
        this.properties = properties;
    }

    @EventListener
    public void siteChanged(SiteSettingsChangedEvent ignored) {
        publishIdentity();
    }

    @EventListener
    public void profileChanged(ProfileChangedEvent ignored) {
        publishIdentity();
    }

    private void publishIdentity() {
        events.publishEvent(new SeoContentChangedEvent(properties.siteOrigin() + "/about"));
        events.publishEvent(new SeoContentChangedEvent(properties.siteOrigin() + "/"));
    }
}
