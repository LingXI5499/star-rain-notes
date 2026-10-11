/*
 * SitePublicApi：站点模块的跨模块接口文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：SitePublicConfigVO。
 * 阅读入口：config（配置处理入口）。
 */

package com.starrainnotes.site.api;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;

/**
 * 站点公开接口，属于站点模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface SitePublicApi {
    /**
     * 配置处理入口。
     * @return 类型为 <code>SitePublicConfigVO</code> 的处理结果
     */
    SitePublicConfigVO config();
}
