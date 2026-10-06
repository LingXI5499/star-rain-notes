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
        var selected = limits.selectedIdsOf(section);
        if (selected.isEmpty()) return tutorials.latestPublished(3);
        var available = tutorials.latestPublished(100);
        return selected.stream().map(id -> available.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull).toList();
    }
}
