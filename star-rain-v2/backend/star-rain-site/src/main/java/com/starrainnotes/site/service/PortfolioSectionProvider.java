package com.starrainnotes.site.service;

import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.entity.HomeSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioSectionProvider implements HomeSectionProvider {
    private final PortfolioPublicApi works;
    private final HomeSectionLimit limits;
    @Override public String sectionCode() { return "PORTFOLIO"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) {
        return works.publishedWorks(1, limits.of(section)).getItems();
    }
}
