/*
 * HomeSectionProvider：站点模块的模块支持文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SitePublicConfigVO、HomeSectionEntity。
 * 阅读入口：sectionCode（区块代码处理入口）；load（加载首页区块提供方）。
 */

package com.starrainnotes.site.provider;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;

/**
 * 首页区块提供方，属于站点模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
public interface HomeSectionProvider {
    /**
     * 区块代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String sectionCode();
    /**
     * 加载首页区块提供方。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @param config 配置；类型为 <code>SitePublicConfigVO</code>
     * @return 类型为 <code>Object</code> 的处理结果
     */
    Object load(HomeSectionEntity section, SitePublicConfigVO config);
}
