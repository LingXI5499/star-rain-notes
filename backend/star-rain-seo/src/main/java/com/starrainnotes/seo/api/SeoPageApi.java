/*
 * SeoPageApi：公开页面与 SEO模块的跨模块接口文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：SeoPageSnapshot。
 * 阅读入口：getByRoute（读取依据路由）；getMetaByRoute（读取Meta依据路由）。
 */

package com.starrainnotes.seo.api;

import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.util.Optional;

/**
 * SEO分页接口，属于公开页面与 SEO模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface SeoPageApi {
    /**
     * 读取依据路由。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<SeoPageSnapshot> getByRoute(String routePath);
    /**
     * 读取Meta依据路由。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<SeoPageSnapshot> getMetaByRoute(String routePath);
}
