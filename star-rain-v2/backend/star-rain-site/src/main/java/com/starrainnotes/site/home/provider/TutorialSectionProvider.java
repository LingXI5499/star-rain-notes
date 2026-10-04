package com.starrainnotes.site.home.provider;

import com.starrainnotes.site.config.SitePublicConfig;
import com.starrainnotes.site.section.HomeSectionEntity;
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
