package com.starrainnotes.site.service;

import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.entity.HomeSectionEntity;

public interface HomeSectionProvider {
    String sectionCode();
    Object load(HomeSectionEntity section, SitePublicConfig config);
}
