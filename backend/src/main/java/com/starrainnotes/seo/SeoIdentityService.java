package com.starrainnotes.seo;

import com.starrainnotes.profile.api.ProfileSeoPort;
import com.starrainnotes.site.api.SiteSeoPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Site and author identity shared by page assembly and HTML rendering. */
@Service
public class SeoIdentityService {
    private final SiteSeoPort siteSettings;
    private final ProfileSeoPort profiles;
    private final BoundedSeoCache<SiteIdentity> sites = new BoundedSeoCache<>(1, 300_000L, System::currentTimeMillis);
    private final BoundedSeoCache<String> authors = new BoundedSeoCache<>(1, 300_000L, System::currentTimeMillis);

    public SeoIdentityService(SiteSeoPort siteSettings, ProfileSeoPort profiles) {
        this.siteSettings = siteSettings;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public SiteIdentity site() {
        return sites.get("site", () -> {
            SiteSeoPort.Identity row = siteSettings.identity();
            return row == null ? new SiteIdentity("星雨笔录", "建立自己的知识世界", null)
                    : new SiteIdentity(row.name(), first(row.tagline(), row.defaultSeoDescription(), "建立自己的知识世界"), row.githubUrl());
        });
    }

    @Transactional(readOnly = true)
    public String authorName() {
        return authors.get("author", () -> first(profiles.displayName(), "零燨"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Order(-100)
    public void contentChanged(SeoContentChangedEvent ignored) {
        sites.clear();
        authors.clear();
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    public record SiteIdentity(String name, String tagline, String githubUrl) {}
}
