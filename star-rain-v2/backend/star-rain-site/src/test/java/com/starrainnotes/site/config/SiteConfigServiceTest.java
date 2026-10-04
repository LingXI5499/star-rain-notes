package com.starrainnotes.site.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.site.exception.SiteConfigException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SiteConfigServiceTest {
    @Test
    void privateLogoCannotBeExposedOrReferenced() {
        SiteConfigMapper mapper = mock(SiteConfigMapper.class);
        MediaAssetApi media = mock(MediaAssetApi.class);
        MediaReferenceApi references = mock(MediaReferenceApi.class);
        SiteConfigEntity row = new SiteConfigEntity();
        row.setId(1L);
        row.setConfigKey("PRIMARY");
        when(mapper.selectOne(any())).thenReturn(row);
        when(media.get(7L)).thenReturn(MediaAssetSummary.builder().id(7L).status("ACTIVE")
                .mediaType("IMAGE").accessLevel("PRIVATE").build());
        SiteConfigService service = new SiteConfigService(mapper, media, references,
                mock(ApplicationEventPublisher.class));

        SiteConfigException error = assertThrows(SiteConfigException.class, () -> service.setMedia("logo", 7L));

        assertEquals("SITE_MEDIA_NOT_PUBLIC", error.getCode());
        verify(mapper, never()).updateById(any(SiteConfigEntity.class));
        verify(references, never()).attach(any());
    }
}
