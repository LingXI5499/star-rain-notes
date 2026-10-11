/*
 * SeoRefreshApi：公开页面与 SEO模块的跨模块接口文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：refreshRoute（刷新路由）；removeRoute（移除路由）。
 */

package com.starrainnotes.seo.api;

/**
 * SEO刷新接口，属于公开页面与 SEO模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface SeoRefreshApi {
    /**
     * 刷新路由。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     */
    void refreshRoute(String routePath);
    /**
     * 移除路由。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     */
    void removeRoute(String routePath);
}
