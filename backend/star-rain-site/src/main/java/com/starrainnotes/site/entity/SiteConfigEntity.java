/*
 * SiteConfigEntity：站点模块的持久化实体文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.site.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 站点配置实体，属于站点模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class SiteConfigEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 配置键。
     */
    private String configKey;
    /**
     * 站点名称。
     */
    private String siteName;
    /**
     * 站点标题。
     */
    private String siteTitle;
    /**
     * tagline。
     */
    private String tagline;
    /**
     * 站点描述。
     */
    private String siteDescription;
    /**
     * 首页Intro。
     */
    private String homeIntro;
    /**
     * footer文本。
     */
    private String footerText;
    /**
     * logo媒体资产标识。
     */
    private Long logoMediaAssetId;
    /**
     * favicon媒体资产标识。
     */
    private Long faviconMediaAssetId;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
