/*
 * SitePublicConfigVO：站点模块的响应数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.site.api.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 站点公开配置响应对象，属于站点模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
public class SitePublicConfigVO {
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
     * logo地址。
     */
    private String logoUrl;
    /**
     * favicon地址。
     */
    private String faviconUrl;
}
