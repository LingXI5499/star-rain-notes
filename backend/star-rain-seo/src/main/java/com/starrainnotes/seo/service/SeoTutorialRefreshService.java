/*
 * SeoTutorialRefreshService：公开页面与 SEO模块的业务契约文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：publicationChanged（发布Changed处理入口）。
 */

package com.starrainnotes.seo.service;

/* 教程发布状态变化后的 SEO 派生数据同步入口。 */
/**
 * SEO教程刷新业务服务，属于公开页面与 SEO模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SeoTutorialRefreshService {
    /**
     * 发布Changed处理入口。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param action 动作；类型为 <code>String</code>
     */
    void publicationChanged(String slug, String action);
}
