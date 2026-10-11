/*
 * DashboardModuleData：公共基础模块的请求与传输数据文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.common.dashboard.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 概览ModuleData，属于公共基础模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@AllArgsConstructor
public class DashboardModuleData {
    /**
     * metrics。
     */
    private Map<String, Object> metrics;
    /**
     * recent内容。
     */
    private List<DashboardRecentItem> recentContent;
}
