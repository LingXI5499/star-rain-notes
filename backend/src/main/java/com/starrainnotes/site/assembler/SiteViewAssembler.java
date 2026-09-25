package com.starrainnotes.site.assembler;

import com.starrainnotes.site.dto.AboutPreviewView;
import com.starrainnotes.site.dto.AdminSiteSettingsView;
import com.starrainnotes.site.dto.FeaturedProjectView;
import com.starrainnotes.site.dto.LatestUpdateView;
import com.starrainnotes.site.dto.PublicHomeView;
import com.starrainnotes.site.dto.PublicSiteView;
import com.starrainnotes.site.entity.SiteSetting;

import java.util.List;

public final class SiteViewAssembler {
    private SiteViewAssembler() {
    }

    public static PublicSiteView publicSite(SiteSetting setting, String logoUrl, String faviconUrl) {
        return new PublicSiteView(setting.getSiteName(), setting.getTagline(), setting.getSiteUrl(), setting.getFooterText(),
                setting.getGithubUrl(), setting.getDefaultSeoDescription(), setting.getTimezone(), logoUrl, faviconUrl);
    }

    public static PublicHomeView home(List<LatestUpdateView> latest, List<FeaturedProjectView> featured, AboutPreviewView about) {
        return new PublicHomeView(latest, featured, about);
    }

    public static AdminSiteSettingsView admin(SiteSetting setting) {
        return new AdminSiteSettingsView(setting.getId(), setting.getSiteName(), setting.getTagline(), setting.getSiteUrl(),
                setting.getFooterText(), setting.getGithubUrl(), setting.getDefaultSeoDescription(), setting.getTimezone(),
                setting.getLogoMediaId(), setting.getFaviconMediaId());
    }
}
