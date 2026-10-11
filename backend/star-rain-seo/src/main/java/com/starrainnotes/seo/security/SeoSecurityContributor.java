/*
 * SeoSecurityContributor：公开页面与 SEO模块的安全策略文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：moduleName（module名称处理入口）；order（顺序处理入口）；publicPatterns（公开Patterns处理入口）；authenticatedPatterns（已认证Patterns处理入口）。
 */

package com.starrainnotes.seo.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * SEO安全贡献者，属于公开页面与 SEO模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class SeoSecurityContributor implements ModuleSecurityContributor {
    /**
     * module名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleName() { return "seo"; }
    /**
     * 顺序处理入口。
     * @return 类型为 <code>int</code> 的处理结果
     */
    @Override public int order() { return 48; }
    /**
     * 公开Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override public List<String> publicPatterns() {
        return List.of("/sitemap.xml", "/robots.txt", "/api/public/seo/**", "/",
            "/blog", "/blog/**", "/tutorials", "/tutorials/**",
            "/portfolio", "/portfolio/**", "/messages", "/about");
    }
    /**
     * 已认证Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/seo/**"); }
}
