package com.starrainnotes.site.provider;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.tutorial.content.api.TutorialPublicApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialSectionProvider implements HomeSectionProvider {
    private final TutorialPublicApi tutorials;
    private final HomeSectionDisplayOptions limits;
    @Override public String sectionCode() { return "TUTORIALS"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfigVO config) {
        return tutorials.latestPublished(limits.limitOf(section));
    }
}
