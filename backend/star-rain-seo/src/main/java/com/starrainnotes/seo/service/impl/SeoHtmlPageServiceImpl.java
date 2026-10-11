/*
 * SeoHtmlPageServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：SeoPageApi、SeoPageSnapshot、SeoHtmlPageService、SeoInteractiveAssetRenderer。
 * 阅读入口：html（HTML处理入口）。
 */

package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.api.SeoPageApi;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.service.SeoHtmlPageService;
import com.starrainnotes.seo.renderer.SeoInteractiveAssetRenderer;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SEOHTML分页业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SeoHtmlPageServiceImpl implements SeoHtmlPageService {
    /**
     * pages。
     * 协作类型：<code>SeoPageApi</code>。
     */
    private final SeoPageApi pages;
    /**
     * assets。
     */
    private final SeoInteractiveAssetRenderer assets;

    /**
     * HTML处理入口。
     * 主要协作调用：<code>pages.getByRoute</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<String> html(String path) {
        return pages.getByRoute(path).map(SeoPageSnapshot::getHtmlSnapshot).map(assets::include);
    }
}
