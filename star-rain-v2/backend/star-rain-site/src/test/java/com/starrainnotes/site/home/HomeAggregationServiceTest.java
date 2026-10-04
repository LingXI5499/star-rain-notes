package com.starrainnotes.site.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.config.SitePublicConfig;
import com.starrainnotes.site.home.provider.HomeSectionProvider;
import com.starrainnotes.site.home.provider.HomeSectionLimit;
import com.starrainnotes.site.section.HomeSectionEntity;
import com.starrainnotes.site.section.HomeSectionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HomeAggregationServiceTest {
    private HomeAggregationService service;

    @AfterEach
    void close() { if (service != null) service.shutdown(); }

    @Test
    void optionalSectionFailureKeepsOtherSectionsAndOrder() {
        SitePublicApi site = mock(SitePublicApi.class);
        HomeSectionService sections = mock(HomeSectionService.class);
        HomeSectionProvider blog = mock(HomeSectionProvider.class);
        HomeSectionProvider hot = mock(HomeSectionProvider.class);
        HomeSectionLimit display = mock(HomeSectionLimit.class);
        SitePublicConfig config = SitePublicConfig.builder().siteName("星雨笔录").build();
        when(site.config()).thenReturn(config);
        when(sections.enabled()).thenReturn(List.of(section("HERO"), section("HOT_CONTENT"), section("BLOG")));
        when(blog.sectionCode()).thenReturn("BLOG");
        when(blog.load(any(), any())).thenReturn(List.of("published"));
        when(hot.sectionCode()).thenReturn("HOT_CONTENT");
        when(hot.load(any(), any())).thenThrow(new IllegalStateException("analytics unavailable"));
        service = new HomeAggregationService(site, sections, display, List.of(blog, hot));

        SiteHomeView result = service.home();

        assertSame(config, result.getConfig());
        assertEquals(List.of("HERO", "HOT_CONTENT", "BLOG"),
                result.getSections().stream().map(HomeSectionData::getCode).toList());
        assertEquals("READY", result.getSections().get(0).getStatus());
        assertEquals("DEGRADED", result.getSections().get(1).getStatus());
        assertEquals("READY", result.getSections().get(2).getStatus());
        assertEquals(List.of("published"), result.getSections().get(2).getData());
    }

    private HomeSectionEntity section(String code) {
        HomeSectionEntity row = new HomeSectionEntity();
        row.setSectionCode(code);
        row.setDisplayName(code);
        row.setEnabled(true);
        return row;
    }
}
