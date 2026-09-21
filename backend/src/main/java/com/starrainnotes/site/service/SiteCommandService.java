package com.starrainnotes.site.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.site.dto.AdminSiteSettingsView;
import com.starrainnotes.site.dto.UpdateSiteSettingsRequest;
import com.starrainnotes.site.entity.SiteSetting;
import com.starrainnotes.site.mapper.SiteSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

/** Write-side singleton site-settings validation and persistence. */
@Service
@RequiredArgsConstructor
public class SiteCommandService {

    private final SiteSettingMapper siteSettingMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final SiteQueryService queryService;

    @Transactional
    public AdminSiteSettingsView updateAdminSettings(UpdateSiteSettingsRequest request) {
        validateTimezone(request.timezone());
        validateImageMedia(request.logoMediaId(), "logo");
        validateImageMedia(request.faviconMediaId(), "favicon");
        SiteSetting setting = requireSetting();
        setting.setSiteName(request.siteName());
        setting.setTagline(request.tagline());
        setting.setSiteUrl(request.siteUrl());
        setting.setFooterText(request.footerText());
        setting.setGithubUrl(request.githubUrl());
        if (request.defaultSeoDescription() != null) setting.setDefaultSeoDescription(request.defaultSeoDescription());
        setting.setTimezone(request.timezone());
        setting.setLogoMediaId(request.logoMediaId());
        setting.setFaviconMediaId(request.faviconMediaId());
        siteSettingMapper.updateById(setting);
        return queryService.getAdminSettings();
    }

    private SiteSetting requireSetting() {
        SiteSetting setting = siteSettingMapper.selectById(1);
        if (setting == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SITE_SETTING_NOT_FOUND",
                    "Site settings not found", "The singleton site settings row is missing.");
        }
        return setting;
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TIMEZONE",
                    "Invalid timezone", "The timezone must be a valid IANA zone id.");
        }
    }

    private void validateImageMedia(Long mediaId, String field) {
        if (mediaId == null) return;
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_NOT_FOUND",
                    "Media not found", "The referenced media asset does not exist.");
        }
        if (!"IMAGE".equals(media.getAssetType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID", "Only IMAGE media allowed",
                    "The " + field + " media id must reference an IMAGE asset, got " + media.getAssetType() + ".");
        }
    }
}
