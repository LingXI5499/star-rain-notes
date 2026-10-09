package com.starrainnotes.site.provider;

import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioSectionProvider implements HomeSectionProvider {
    private final PortfolioPublicApi works;
    private final HomeSectionDisplayOptions limits;
    @Override public String sectionCode() { return "PORTFOLIO"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfigVO config) {
        var selected = limits.selectedIdsOf(section);
        if (selected.isEmpty()) return works.publishedWorks(1, 3).getItems();
        var available = works.publishedWorks(1, 100).getItems();
        return selected.stream().map(id -> available.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull).toList();
    }
}
