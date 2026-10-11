/*
 * AnalyticsService：访问统计模块的业务契约文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：AnalyticsQueryApi、AnalyticsRecordApi。
 * 阅读入口：recordPageView（记录分页View）。
 */

package com.starrainnotes.analytics.service;

import com.starrainnotes.analytics.api.AnalyticsQueryApi;
import com.starrainnotes.analytics.api.AnalyticsRecordApi;

/**
 * 统计业务服务，属于访问统计模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AnalyticsService extends AnalyticsRecordApi, AnalyticsQueryApi {
    /**
     * 记录分页View。
     *
     * @param routeKey 路由键；类型为 <code>String</code>
     * @param referrer referrer；类型为 <code>String</code>
     */
    void recordPageView(String routeKey, String referrer);
}
