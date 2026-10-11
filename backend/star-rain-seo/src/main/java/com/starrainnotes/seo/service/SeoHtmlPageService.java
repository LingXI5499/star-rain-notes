/*
 * SeoHtmlPageService：公开页面与 SEO模块的业务契约文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：html（HTML处理入口）。
 */

package com.starrainnotes.seo.service;

import java.util.Optional;

// SEO 静态 HTML 页面读取入口。
/**
 * SEOHTML分页业务服务，属于公开页面与 SEO模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SeoHtmlPageService {

    /**
     * HTML处理入口。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<String> html(String path);
}
