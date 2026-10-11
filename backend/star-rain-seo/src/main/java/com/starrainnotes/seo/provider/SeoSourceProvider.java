/*
 * SeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoSourceDocument。
 * 阅读入口：supports（判断是否支持SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）。
 */

package com.starrainnotes.seo.provider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import java.util.List;
import java.util.Optional;

/**
 * SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
public interface SeoSourceProvider {
    /**
     * 判断是否支持SEO来源提供方。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean supports(String routePath);
    /**
     * 加载依据路由。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<SeoSourceDocument> loadByRoute(String routePath);
    /**
     * 列出已发布。
     * @return 本次处理得到的条目集合
     */
    List<SeoSourceDocument> listPublished();
}
