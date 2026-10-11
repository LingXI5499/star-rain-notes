/*
 * SeoSnapshotService：公开页面与 SEO模块的业务契约文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：SeoSourceDocument。
 * 阅读入口：upsert（新增或更新SEO快照）。
 */

package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

/**
 * SEO快照业务服务，属于公开页面与 SEO模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SeoSnapshotService {
    /**
     * 新增或更新SEO快照。
     *
     * @param source 来源；类型为 <code>SeoSourceDocument</code>
     */
    void upsert(SeoSourceDocument source);
}
