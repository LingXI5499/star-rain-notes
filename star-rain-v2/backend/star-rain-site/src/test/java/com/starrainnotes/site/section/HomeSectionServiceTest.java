package com.starrainnotes.site.section;

import com.starrainnotes.site.dto.HomeSectionOrderDTO;
import com.starrainnotes.site.dto.HomeSectionPatchDTO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.mapper.HomeSectionMapper;
import com.starrainnotes.site.service.impl.HomeSectionServiceImpl;

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
        when(mapper.all()).thenReturn(List.of(section("HERO"), section("BLOG")));
        HomeSectionOrderDTO order = new HomeSectionOrderDTO();
        order.setSectionCodes(List.of("HERO", "HERO"));

        SiteConfigException error = assertThrows(SiteConfigException.class,
                () -> new HomeSectionServiceImpl(mapper, new ObjectMapper()).reorder(order));

        assertEquals("SITE_SECTION_CONFIG_INVALID", error.getCode());
        verify(mapper, never()).update(any(HomeSectionEntity.class));
    }

    @Test
    void overflowingLimitCannotBecomeValidThroughIntegerConversion() throws Exception {
        HomeSectionMapper mapper = mock(HomeSectionMapper.class);
        when(mapper.byCode("BLOG")).thenReturn(section("BLOG"));
        HomeSectionPatchDTO patch = new HomeSectionPatchDTO();
        patch.setConfig(new ObjectMapper().readTree("{\"limit\":4294967302}"));

        SiteConfigException error = assertThrows(SiteConfigException.class,
                () -> new HomeSectionServiceImpl(mapper, new ObjectMapper()).patch("BLOG", patch));

        assertEquals("SITE_SECTION_CONFIG_INVALID", error.getCode());
        verify(mapper, never()).update(any(HomeSectionEntity.class));
    }

    private HomeSectionEntity section(String code) {
        HomeSectionEntity row = new HomeSectionEntity();
        row.setSectionCode(code);
        row.setEnabled(true);
        return row;
    }
}
