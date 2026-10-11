/*
 * SeoRebuildService：公开页面与 SEO模块的业务契约文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：rebuild（重建SEORebuild）；rebuildAll（重建全部）；rebuildRoute（重建路由）。
 */

package com.starrainnotes.seo.service;

// SEO 快照重建入口：全量重建、按路由重建。
/**
 * SEORebuild业务服务，属于公开页面与 SEO模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SeoRebuildService {

    /**
     * 重建SEORebuild。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    long rebuild(String routePath);

    /**
     * 重建全部。
     */
    void rebuildAll();

    /**
     * 重建路由。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     */
    void rebuildRoute(String routePath);
}
