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
        return works.publishedWorks(1, limits.limitOf(section)).getItems();
    }
}
