package com.starrainnotes.site.section;

import com.starrainnotes.site.dto.HomeSectionOrder;
import com.starrainnotes.site.dto.HomeSectionPatch;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.mapper.HomeSectionMapper;
import com.starrainnotes.site.service.HomeSectionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.site.exception.SiteConfigException;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeSectionServiceTest {
    @Test
    void malformedOrderDoesNotChangeAnySection() {
        HomeSectionMapper mapper = mock(HomeSectionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(section("HERO"), section("BLOG")));
        HomeSectionOrder order = new HomeSectionOrder();
        order.setSectionCodes(List.of("HERO", "HERO"));

        SiteConfigException error = assertThrows(SiteConfigException.class,
                () -> new HomeSectionService(mapper, new ObjectMapper()).reorder(order));

        assertEquals("SITE_SECTION_CONFIG_INVALID", error.getCode());
        verify(mapper, never()).updateById(any(HomeSectionEntity.class));
    }

    @Test
    void overflowingLimitCannotBecomeValidThroughIntegerConversion() throws Exception {
        HomeSectionMapper mapper = mock(HomeSectionMapper.class);
        when(mapper.selectOne(any())).thenReturn(section("BLOG"));
        HomeSectionPatch patch = new HomeSectionPatch();
        patch.setConfig(new ObjectMapper().readTree("{\"limit\":4294967302}"));

        SiteConfigException error = assertThrows(SiteConfigException.class,
                () -> new HomeSectionService(mapper, new ObjectMapper()).patch("BLOG", patch));

        assertEquals("SITE_SECTION_CONFIG_INVALID", error.getCode());
        verify(mapper, never()).updateById(any(HomeSectionEntity.class));
    }

    private HomeSectionEntity section(String code) {
        HomeSectionEntity row = new HomeSectionEntity();
        row.setSectionCode(code);
        row.setEnabled(true);
        return row;
    }
}
