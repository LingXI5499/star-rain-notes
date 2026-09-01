package com.starrainnotes.site.service;

import com.starrainnotes.site.entity.SiteSetting;
import com.starrainnotes.site.mapper.SiteSettingMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Resolves the current site timezone from the frozen singleton
 * (site_setting id = 1, default Asia/Shanghai) and converts UTC timestamps
 * (03 §1: database stores UTC).
 */
@Component
public class SiteSettingsTimezone {

    private final SiteSettingMapper siteSettingMapper;

    public SiteSettingsTimezone(SiteSettingMapper siteSettingMapper) {
        this.siteSettingMapper = siteSettingMapper;
    }

    public String get() {
        SiteSetting setting = siteSettingMapper.selectById(1);
        String timezone = setting == null ? null : setting.getTimezone();
        return timezone == null || timezone.isBlank() ? "Asia/Shanghai" : timezone;
    }

    public ZoneId zone() {
        return ZoneId.of(get());
    }

    /**
     * Converts a UTC database timestamp into the site timezone.
     */
    public ZonedDateTime atSite(LocalDateTime utc) {
        return ZonedDateTime.of(utc, ZoneOffset.UTC).withZoneSameInstant(zone());
    }
}
