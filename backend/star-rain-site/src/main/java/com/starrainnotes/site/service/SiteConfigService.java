/*
 * SiteConfigService：站点模块的业务契约文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：SitePublicApi、SitePublicConfigVO、SiteConfigPatchDTO、SiteMediaRequestDTO。
 * 阅读入口：patch（patch处理入口）；setMediaRequired（设置媒体必需）；setMedia（设置媒体）。
 */

package com.starrainnotes.site.service;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.dto.SiteConfigPatchDTO;
import com.starrainnotes.site.dto.SiteMediaRequestDTO;

/** Updates the single public site configuration and its media references. */
public interface SiteConfigService extends SitePublicApi {
    /** Updates the primary site's text fields. */
    SitePublicConfigVO patch(SiteConfigPatchDTO patch);

    /** Sets a logo or favicon from a required request body. */
    SitePublicConfigVO setMediaRequired(String kind, SiteMediaRequestDTO request);

    /** Sets or removes a logo or favicon and updates media references. */
    SitePublicConfigVO setMedia(String kind, Long assetId);
}
