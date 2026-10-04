package com.starrainnotes.site.home.provider;

import com.starrainnotes.site.config.SitePublicConfig;
import com.starrainnotes.site.section.HomeSectionEntity;
import org.springframework.stereotype.Component;

@Component
public class HeroSectionProvider implements HomeSectionProvider {
    @Override public String sectionCode() { return "HERO"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) { return config; }
}
