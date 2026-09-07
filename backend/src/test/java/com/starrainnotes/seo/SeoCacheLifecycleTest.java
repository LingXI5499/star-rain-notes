package com.starrainnotes.seo;

import com.starrainnotes.blog.dto.CreatePostRequest;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.blog.service.BlogService;
import com.starrainnotes.profile.dto.UpdateAboutRequest;
import com.starrainnotes.profile.service.ProfileService;
import com.starrainnotes.site.dto.UpdateSiteSettingsRequest;
import com.starrainnotes.site.service.SiteService;
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
    @Autowired BlogService blogs;
    @Autowired SiteService sites;
    @Autowired ProfileService profiles;
    @Autowired SeoDocumentCache pages;
    @Autowired SeoContentRepository content;
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
        var site = sites.getAdminSettings();
        var profile = profiles.getAdmin();
        try {
            content.site(); content.authorName(); page("/about");
            sites.updateAdminSettings(new UpdateSiteSettingsRequest("Updated identity", site.tagline(), site.siteUrl(), site.footerText(), site.githubUrl(), site.defaultSeoDescription(), site.timezone(), site.logoMediaId(), site.faviconMediaId()));
            assertThat(content.site().name()).isEqualTo("Updated identity");
            profiles.update(new UpdateAboutRequest("Updated author", profile.headline(), profile.bio(), profile.avatarMediaId(), profile.githubUrl(), profile.publicEmail(), profile.resumeMediaId(), profile.currentFocus(), profile.technicalDirectionMarkdown(), profile.journeyMarkdown()));
            assertThat(content.authorName()).isEqualTo("Updated author");
        } finally {
            sites.updateAdminSettings(new UpdateSiteSettingsRequest(site.siteName(), site.tagline(), site.siteUrl(), site.footerText(), site.githubUrl(), site.defaultSeoDescription(), site.timezone(), site.logoMediaId(), site.faviconMediaId()));
            profiles.update(new UpdateAboutRequest(profile.displayName(), profile.headline(), profile.bio(), profile.avatarMediaId(), profile.githubUrl(), profile.publicEmail(), profile.resumeMediaId(), profile.currentFocus(), profile.technicalDirectionMarkdown(), profile.journeyMarkdown()));
        }
    }
}
