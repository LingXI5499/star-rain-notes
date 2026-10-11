/*
 * HomeAggregationService：站点模块的业务契约文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：SiteHomeVO。
 * 阅读入口：home（首页处理入口）。
 */

package com.starrainnotes.site.service;

import com.starrainnotes.site.vo.SiteHomeVO;

/** Aggregates enabled homepage sections into the public view. */
public interface HomeAggregationService {
    /** Returns enabled homepage sections, degrading failed sources independently. */
    SiteHomeVO home();
}
