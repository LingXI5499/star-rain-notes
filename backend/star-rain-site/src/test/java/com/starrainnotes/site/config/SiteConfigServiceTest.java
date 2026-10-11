/*
 * SiteConfigServiceTest：站点模块的测试文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：SiteConfigEntity、SiteConfigMapper、SiteConfigServiceImpl、MediaAssetApi、MediaReferenceApi、MediaAssetSummary、SiteConfigException。
 * 阅读入口：privateLogoCannotBeExposedOrReferenced（privateLogoCannotBeExposed或Referenced处理入口）。
 */

package com.starrainnotes.site.config;

import com.starrainnotes.site.entity.SiteConfigEntity;
import com.starrainnotes.site.mapper.SiteConfigMapper;
import com.starrainnotes.site.service.impl.SiteConfigServiceImpl;

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

/**
 * 站点配置业务服务测试，属于站点模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SiteConfigServiceTest {
    /**
     * privateLogoCannotBeExposed或Referenced处理入口。
     * 关键处理步骤调用 <code>row.setId</code>、<code>row.setConfigKey</code>、<code>mapper.primary</code>、<code>media.get</code>。
     */
    @Test
    void privateLogoCannotBeExposedOrReferenced() {
        SiteConfigMapper mapper = mock(SiteConfigMapper.class);
        MediaAssetApi media = mock(MediaAssetApi.class);
        MediaReferenceApi references = mock(MediaReferenceApi.class);
        SiteConfigEntity row = new SiteConfigEntity();
        row.setId(1L);
        row.setConfigKey("PRIMARY");
        when(mapper.primary()).thenReturn(row);
        when(media.get(7L)).thenReturn(MediaAssetSummary.builder().id(7L).status("ACTIVE")
                .mediaType("IMAGE").accessLevel("PRIVATE").build());
        SiteConfigServiceImpl service = new SiteConfigServiceImpl(mapper, media, references,
                mock(ApplicationEventPublisher.class));

        SiteConfigException error = assertThrows(SiteConfigException.class, () -> service.setMedia("logo", 7L));

        assertEquals("SITE_MEDIA_NOT_PUBLIC", error.getCode());
        verify(mapper, never()).update(any(SiteConfigEntity.class));
        verify(references, never()).attach(any());
    }
}
