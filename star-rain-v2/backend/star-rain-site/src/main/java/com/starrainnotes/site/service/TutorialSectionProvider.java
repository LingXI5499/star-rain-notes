package com.starrainnotes.site.service;

import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.tutorial.content.api.TutorialPublicApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialSectionProvider implements HomeSectionProvider {
    private final TutorialPublicApi tutorials;
    private final HomeSectionLimit limits;
    @Override public String sectionCode() { return "TUTORIALS"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) {
        return tutorials.latestPublished(limits.of(section));
    }
}
