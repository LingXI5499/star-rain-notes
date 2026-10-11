/*
 * DashboardSource：公共基础模块的跨模块接口文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：DashboardModuleData。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载概览来源）。
 */

package com.starrainnotes.common.dashboard.api;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;

/**
 * 概览来源，属于公共基础模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface DashboardSource {
    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String moduleCode();
    /**
     * 加载概览来源。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    DashboardModuleData load();
}
