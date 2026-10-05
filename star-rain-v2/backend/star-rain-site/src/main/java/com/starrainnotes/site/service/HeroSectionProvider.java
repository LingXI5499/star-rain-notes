package com.starrainnotes.site.service;

import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.entity.HomeSectionEntity;
import org.springframework.stereotype.Component;

@Component
public class HeroSectionProvider implements HomeSectionProvider {
    @Override public String sectionCode() { return "HERO"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) { return config; }
}
