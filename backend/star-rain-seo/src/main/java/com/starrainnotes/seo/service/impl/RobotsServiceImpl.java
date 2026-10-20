/*
 * RobotsServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：RobotsService、CanonicalUrlResolver。
 * 阅读入口：robotsTxt（robotsTxt处理入口）。
 */

package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.service.RobotsService;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Robots业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class RobotsServiceImpl implements RobotsService {
    /**
     * 规范。
     */
    private final CanonicalUrlResolver canonical;

    /**
     * robotsTxt处理入口。
     * 主要协作调用：<code>canonical.baseUrl</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String robotsTxt() {
        return "User-agent: *\n"
            + "Allow: /\n"
            // Vue 阅读页渲染正文依赖公开 API；私有及管理接口继续由 /api/ 规则排除。
            + "Allow: /api/public/\n"
            + "Disallow: /api/\n"
            + "Disallow: /useradmin/\n"
            + "Disallow: /search\n"
            + "Sitemap: " + canonical.baseUrl() + "/sitemap.xml\n";
    }
}
