package com.starrainnotes.site.service.impl;

import com.starrainnotes.site.dto.SiteConfigPatchDTO;
import com.starrainnotes.site.dto.SiteMediaRequestDTO;
import com.starrainnotes.site.entity.SiteConfigEntity;
import com.starrainnotes.site.mapper.SiteConfigMapper;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;

import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import com.starrainnotes.site.api.event.SiteConfigChangedEvent;
import com.starrainnotes.site.exception.SiteConfigException;
import com.starrainnotes.site.service.SiteConfigService;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {
    private static final Set<String> FAVICON_TYPES = Set.of("image/png", "image/x-icon", "image/vnd.microsoft.icon", "image/svg+xml");
    private final SiteConfigMapper mapper;
    private final MediaAssetApi media;
    private final MediaReferenceApi references;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public SitePublicConfigVO config() {
        SiteConfigEntity row = primary();
        return SitePublicConfigVO.builder()
                .siteName(row.getSiteName()).siteTitle(row.getSiteTitle()).tagline(row.getTagline())
                .siteDescription(row.getSiteDescription()).homeIntro(row.getHomeIntro())
                .footerText(row.getFooterText()).logoUrl(url(row.getLogoMediaAssetId()))
                .faviconUrl(url(row.getFaviconMediaAssetId())).build();
    }

    @Override
    @Transactional
    public SitePublicConfigVO patch(SiteConfigPatchDTO patch) {
        if (patch == null) throw invalid("站点配置不能为空");
        SiteConfigEntity row = primary();
        if (patch.getSiteName() != null) row.setSiteName(required(patch.getSiteName(), 120, "站点名称"));
        if (patch.getSiteTitle() != null) row.setSiteTitle(required(patch.getSiteTitle(), 255, "站点标题"));
        if (patch.getTagline() != null) row.setTagline(optional(patch.getTagline(), 255, "首页副标题"));
        if (patch.getSiteDescription() != null) row.setSiteDescription(optional(patch.getSiteDescription(), 1000, "站点简介"));
        if (patch.getHomeIntro() != null) row.setHomeIntro(optional(patch.getHomeIntro(), 2000, "首页介绍"));
        if (patch.getFooterText() != null) row.setFooterText(optional(patch.getFooterText(), 1000, "页脚文字"));
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        afterCommit();
        return config();
    }

    @Override
    @Transactional
    public SitePublicConfigVO setMediaRequired(String kind, SiteMediaRequestDTO request) {
        if (request == null || request.getMediaAssetId() == null || request.getMediaAssetId() <= 0)
            throw new SiteConfigException("SITE_MEDIA_INVALID", "请选择有效的站点图片", 400);
        return setMedia(kind, request.getMediaAssetId());
    }

    @Override
    @Transactional
    public SitePublicConfigVO setMedia(String kind, Long assetId) {
        boolean logo = "logo".equals(kind);
        if (!logo && !"favicon".equals(kind)) throw invalid("站点媒体用途无效");
        SiteConfigEntity row = primary();
        Long oldId = logo ? row.getLogoMediaAssetId() : row.getFaviconMediaAssetId();
        if (oldId != null && oldId.equals(assetId)) return config();
        if (assetId != null) validateMedia(assetId, logo);
        if (logo) row.setLogoMediaAssetId(assetId);
        else row.setFaviconMediaAssetId(assetId);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        String usage = logo ? MediaUsageCodes.SITE_LOGO : MediaUsageCodes.SITE_FAVICON;
        if (assetId != null) references.attach(reference(row.getId(), assetId, usage));
        if (oldId != null) references.detach(reference(row.getId(), oldId, usage));
        afterCommit();
        return config();
    }

    private SiteConfigEntity primary() {
        SiteConfigEntity row = mapper.primary();
        if (row == null) throw new SiteConfigException("SITE_CONFIG_NOT_FOUND", "站点配置不存在", 500);
        return row;
    }

    private String url(Long assetId) {
        if (assetId == null) return null;
        MediaAssetSummary asset = media.get(assetId);
        return asset != null && "ACTIVE".equals(asset.getStatus())
                && "PUBLIC".equals(asset.getAccessLevel()) ? asset.getContentUrl() : null;
    }

    private void validateMedia(Long assetId, boolean logo) {
        if (assetId <= 0) throw invalid("媒体 ID 无效");
        MediaAssetSummary asset = media.get(assetId);
        if (asset == null || !"ACTIVE".equals(asset.getStatus()) || !"IMAGE".equals(asset.getMediaType()))
            throw new SiteConfigException("SITE_MEDIA_INVALID", "请选择已启用的图片", 400);
        if (!"PUBLIC".equals(asset.getAccessLevel()))
            throw new SiteConfigException("SITE_MEDIA_NOT_PUBLIC", "站点图片必须公开", 400);
        if (!logo && (!FAVICON_TYPES.contains(asset.getMimeType())
                || asset.getWidth() != null && asset.getHeight() != null
                && (!asset.getWidth().equals(asset.getHeight()) || asset.getWidth() > 512)))
            throw new SiteConfigException("SITE_MEDIA_INVALID", "图标须为方形且不超过 512 像素的 PNG、ICO 或 SVG", 400);
    }

    private MediaReferenceCommand reference(Long id, Long assetId, String usage) {
        return MediaReferenceCommand.builder().mediaAssetId(assetId).sourceModule("SITE")
                .sourceType("CONFIG").sourceId(id).usageCode(usage).build();
    }

    private String required(String value, int max, String field) {
        String clean = value.trim();
        if (clean.isEmpty() || clean.length() > max) throw invalid(field + "长度无效");
        return clean;
    }

    private String optional(String value, int max, String field) {
        String clean = value.trim();
        if (clean.length() > max) throw invalid(field + "过长");
        return clean.isEmpty() ? null : clean;
    }

    private SiteConfigException invalid(String message) {
        return new SiteConfigException("SITE_CONFIG_INVALID", message, 400);
    }

    private void afterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(new SiteConfigChangedEvent());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(new SiteConfigChangedEvent()); }
        });
    }
}
