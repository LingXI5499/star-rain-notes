/*
 * TutorialSectionProvider：站点模块的模块支持文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SitePublicConfigVO、HomeSectionEntity、TutorialPublicApi。
 * 阅读入口：sectionCode（区块代码处理入口）；load（加载教程区块提供方）。
 */

package com.starrainnotes.site.provider;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.tutorial.content.api.TutorialPublicApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 教程区块提供方，属于站点模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class TutorialSectionProvider implements HomeSectionProvider {
    /**
     * tutorials。
     * 协作类型：<code>TutorialPublicApi</code>。
     */
    private final TutorialPublicApi tutorials;
    /**
     * limits。
     */
    private final HomeSectionDisplayOptions limits;
    /**
     * 区块代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String sectionCode() { return "TUTORIALS"; }
    /**
     * 加载教程区块提供方。
     * 主要协作调用：<code>limits.selectedIdsOf</code>、<code>tutorials.latestPublished</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @param config 配置；类型为 <code>SitePublicConfigVO</code>
     * @return 类型为 <code>Object</code> 的处理结果
     */
    @Override public Object load(HomeSectionEntity section, SitePublicConfigVO config) {
        var selected = limits.selectedIdsOf(section);
        if (selected.isEmpty()) return tutorials.latestPublished(3);
        var available = tutorials.latestPublished(100);
        return selected.stream().map(id -> available.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull).toList();
    }
}
