/*
 * SeoNotificationService：公开页面与 SEO模块的业务契约文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：enqueue（enqueue处理入口）；processPending（处理待处理）。
 */

package com.starrainnotes.seo.service;

/**
 * SEO通知业务服务，属于公开页面与 SEO模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SeoNotificationService {
    /**
     * enqueue处理入口。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @param changeType change类型；类型为 <code>String</code>
     */
    void enqueue(String routePath, String changeType);

    /**
     * 处理待处理。
     */
    void processPending();
}
