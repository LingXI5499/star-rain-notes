/*
 * AnalyticsAggregateService：访问统计模块的业务契约文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：aggregateDay（aggregate日处理入口）；deleteRawBefore（删除原始之前）。
 */

package com.starrainnotes.analytics.service;

import java.time.LocalDate;

/**
 * 统计Aggregate业务服务，属于访问统计模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AnalyticsAggregateService {
    /**
     * aggregate日处理入口。
     *
     * @param date 日期；类型为 <code>LocalDate</code>
     */
    void aggregateDay(LocalDate date);

    /**
     * 删除原始之前。
     *
     * @param date 日期；类型为 <code>LocalDate</code>
     */
    void deleteRawBefore(LocalDate date);
}
