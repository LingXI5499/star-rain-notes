package com.starrainnotes.seo;

import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogCommandService;
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.service.ProfileCommandService;
import com.starrainnotes.profile.service.ProfileQueryService;
import com.starrainnotes.site.dto.UpdateSiteSettingsRequest;
import com.starrainnotes.site.service.SiteCommandService;
import com.starrainnotes.site.service.SiteQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SeoCacheLifecycleTest {
    @Autowired BlogCommandService blogs;
    @Autowired SiteCommandService siteCommands;
    @Autowired SiteQueryService siteQueries;
    @Autowired ProfileCommandService profileCommands;
    @Autowired ProfileQueryService profileQueries;
    @Autowired SeoDocumentCache pages;
    @Autowired SeoPageService content;
    @Autowired SeoIdentityService identity;
    @Autowired SeoSitemapService sitemap;
    @Autowired PlatformTransactionManager transactions;

    private SeoPage page(String path) { return pages.page(path, () -> content.resolve(path)); }

    @Test void committedPublishEditWithdrawRepublishAndDeleteRefreshAllPublicDocuments() {
        var post = blogs.create(new CreatePostRequest("cache fixture", null, "summary", "## Old body", null, null, null, List.of(), List.of()));
        String path = "/blog/" + post.slug();
        try {
            assertThat(page(path)).isNull();
            assertThat(sitemap.sitemap()).doesNotContain(path + "</loc>");
            blogs.publish(post.id());
            assertThat(page(path).bodyHtml()).contains("Old body");
            assertThat(sitemap.sitemap()).contains(path + "</loc>");
            blogs.update(post.id(), new UpdatePostRequest("new title", post.slug(), "summary", "## New body", null, null, null, List.of(), List.of()));
            assertThat(page(path).bodyHtml()).contains("New body");
            blogs.withdraw(post.id());
            assertThat(page(path)).isNull();
            assertThat(sitemap.sitemap()).doesNotContain(path + "</loc>");
            blogs.publish(post.id());
            assertThat(page(path)).isNotNull();
        } finally { blogs.delete(post.id()); }
        assertThat(page(path)).isNull();
        assertThat(sitemap.sitemap()).doesNotContain(path + "</loc>");
    }

    @Test void rolledBackPublicationDoesNotPopulatePublicCache() {
        var post = blogs.create(new CreatePostRequest("rollback fixture", null, "summary", "body", null, null, null, List.of(), List.of()));
        try {
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                blogs.publish(post.id());
                status.setRollbackOnly();
            });
            assertThat(page("/blog/" + post.slug())).isNull();
        } finally { blogs.delete(post.id()); }
    }

    @Test void siteAndAuthorEditsInvalidateCachedIdentity() {
        var site = siteQueries.getAdminSettings();
        var profile = profileQueries.getAdmin();
        try {
            identity.site(); identity.authorName(); page("/about");
            siteCommands.updateAdminSettings(new UpdateSiteSettingsRequest("Updated identity", site.tagline(), site.siteUrl(), site.footerText(), site.githubUrl(), site.defaultSeoDescription(), site.timezone(), site.logoMediaId(), site.faviconMediaId()));
            assertThat(identity.site().name()).isEqualTo("Updated identity");
            profileCommands.update(new UpdateAboutRequest("Updated author", profile.headline(), profile.bio(), profile.avatarMediaId(), profile.githubUrl(), profile.publicEmail(), profile.resumeMediaId(), profile.currentFocus(), profile.technicalDirectionMarkdown(), profile.journeyMarkdown()));
            assertThat(identity.authorName()).isEqualTo("Updated author");
        } finally {
            siteCommands.updateAdminSettings(new UpdateSiteSettingsRequest(site.siteName(), site.tagline(), site.siteUrl(), site.footerText(), site.githubUrl(), site.defaultSeoDescription(), site.timezone(), site.logoMediaId(), site.faviconMediaId()));
            profileCommands.update(new UpdateAboutRequest(profile.displayName(), profile.headline(), profile.bio(), profile.avatarMediaId(), profile.githubUrl(), profile.publicEmail(), profile.resumeMediaId(), profile.currentFocus(), profile.technicalDirectionMarkdown(), profile.journeyMarkdown()));
        }
    }
}
