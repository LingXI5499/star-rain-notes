package com.starrainnotes.site.repository;

import com.starrainnotes.site.entity.SiteSetting;
import com.starrainnotes.site.mapper.SiteSettingMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SiteSettingRepository {
    private final SiteSettingMapper mapper;

    public SiteSettingRepository(SiteSettingMapper mapper) {
        this.mapper = mapper;
    }

    public SiteSetting findSingleton() {
        return mapper.selectById(1);
    }
}
