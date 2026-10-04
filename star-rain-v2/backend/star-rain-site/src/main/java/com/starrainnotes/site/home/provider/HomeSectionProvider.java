package com.starrainnotes.site.home.provider;

import com.starrainnotes.site.config.SitePublicConfig;
import com.starrainnotes.site.section.HomeSectionEntity;

public interface HomeSectionProvider {
    String sectionCode();
    Object load(HomeSectionEntity section, SitePublicConfig config);
}
