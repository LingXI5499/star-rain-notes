/*
 * AnalyticsSecurityContributor：访问统计模块的安全策略文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：moduleName（module名称处理入口）；order（顺序处理入口）；publicPatterns（公开Patterns处理入口）；authenticatedPatterns（已认证Patterns处理入口）。
 */

package com.starrainnotes.analytics.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 统计安全贡献者，属于访问统计模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class AnalyticsSecurityContributor implements ModuleSecurityContributor {
    /**
     * module名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleName() { return "analytics"; }
    /**
     * 顺序处理入口。
     * @return 类型为 <code>int</code> 的处理结果
     */
    @Override public int order() { return 48; }
    /**
     * 公开Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override public List<String> publicPatterns() { return List.of("/api/public/analytics/**"); }
    /**
     * 已认证Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override public List<String> authenticatedPatterns() { return List.of("/api/admin/analytics/**"); }
}
