/*
 * SiteConfigServiceImpl：站点模块的业务实现文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：SiteConfigPatchDTO、SiteMediaRequestDTO、SiteConfigEntity、SiteConfigMapper、SitePublicConfigVO、MediaAssetApi、MediaReferenceApi。
 * 阅读入口：config（配置处理入口）；patch（patch处理入口）；setMediaRequired（设置媒体必需）；setMedia（设置媒体）。
 */

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

/**
 * 站点配置业务服务实现，属于站点模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {
    /**
     * TYPES。
     */
    private static final Set<String> FAVICON_TYPES = Set.of("image/png", "image/x-icon", "image/vnd.microsoft.icon", "image/svg+xml");
    /**
     * 数据映射。
     * 协作类型：<code>SiteConfigMapper</code>。
     */
    private final SiteConfigMapper mapper;
    /**
     * 媒体。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi media;
    /**
     * references。
     * 协作类型：<code>MediaReferenceApi</code>。
     */
    private final MediaReferenceApi references;
    /**
     * 事件集合。
     */
    private final ApplicationEventPublisher events;

    /**
     * 配置处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>primary</code>、<code>SitePublicConfigVO.builder</code>、<code>row.getSiteName</code>、<code>row.getSiteTitle</code>。
     * @return 类型为 <code>SitePublicConfigVO</code> 的处理结果
     */
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

    /**
     * patch处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.update</code>。
     *
     * @param patch patch；类型为 <code>SiteConfigPatchDTO</code>
     * @return 类型为 <code>SitePublicConfigVO</code> 的处理结果
     */
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
        publishChange();
        return config();
    }

    /**
     * 设置媒体必需。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>request.getMediaAssetId</code>、<code>setMedia</code>。
     * 实现中的显式失败类型包括 <code>SiteConfigException</code>；具体触发条件见对应分支。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SiteMediaRequestDTO</code>
     * @return 类型为 <code>SitePublicConfigVO</code> 的处理结果
     */
    @Override
    @Transactional
    public SitePublicConfigVO setMediaRequired(String kind, SiteMediaRequestDTO request) {
        if (request == null || request.getMediaAssetId() == null || request.getMediaAssetId() <= 0)
            throw new SiteConfigException("SITE_MEDIA_INVALID", "请选择有效的站点图片", 400);
        return setMedia(kind, request.getMediaAssetId());
    }

    /**
     * 设置媒体。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.update</code>、<code>references.attach</code>、<code>references.detach</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>SitePublicConfigVO</code> 的处理结果
     */
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
        publishChange();
        return config();
    }

    /**
     * primary处理入口。
     * 主要协作调用：<code>mapper.primary</code>。
     * 实现中的显式失败类型包括 <code>SiteConfigException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>SiteConfigEntity</code> 的处理结果
     */
    private SiteConfigEntity primary() {
        SiteConfigEntity row = mapper.primary();
        if (row == null) throw new SiteConfigException("SITE_CONFIG_NOT_FOUND", "站点配置不存在", 500);
        return row;
    }

    /**
     * 地址处理入口。
     * 主要协作调用：<code>media.get</code>。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String url(Long assetId) {
        if (assetId == null) return null;
        MediaAssetSummary asset = media.get(assetId);
        return asset != null && "ACTIVE".equals(asset.getStatus())
                && "PUBLIC".equals(asset.getAccessLevel()) ? asset.getContentUrl() : null;
    }

    /**
     * 校验媒体。
     * 主要协作调用：<code>media.get</code>、<code>FAVICON_TYPES.contains</code>。
     * 实现中的显式失败类型包括 <code>SiteConfigException</code>；具体触发条件见对应分支。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param logo logo；类型为 <code>boolean</code>
     */
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

    /**
     * 引用处理入口。
     * 关键处理步骤调用 <code>MediaReferenceCommand.builder</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param usage usage；类型为 <code>String</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    private MediaReferenceCommand reference(Long id, Long assetId, String usage) {
        return MediaReferenceCommand.builder().mediaAssetId(assetId).sourceModule("SITE")
                .sourceType("CONFIG").sourceId(id).usageCode(usage).build();
    }

    /**
     * 必需处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>clean.isEmpty</code>、<code>clean.length</code>、<code>invalid</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @param field 字段；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String required(String value, int max, String field) {
        String clean = value.trim();
        if (clean.isEmpty() || clean.length() > max) throw invalid(field + "长度无效");
        return clean;
    }

    /**
     * optional处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>clean.length</code>、<code>invalid</code>、<code>clean.isEmpty</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @param field 字段；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String optional(String value, int max, String field) {
        String clean = value.trim();
        if (clean.length() > max) throw invalid(field + "过长");
        return clean.isEmpty() ? null : clean;
    }

    /**
     * 无效处理入口。
     *
     * @param message 留言；类型为 <code>String</code>
     * @return 类型为 <code>SiteConfigException</code> 的处理结果
     */
    private SiteConfigException invalid(String message) {
        return new SiteConfigException("SITE_CONFIG_INVALID", message, 400);
    }

    /**
     * 之后Commit处理入口。
     * 主要协作调用：<code>events.publishEvent</code>。
     */
    private void publishChange() {
        events.publishEvent(new SiteConfigChangedEvent());
    }
}
