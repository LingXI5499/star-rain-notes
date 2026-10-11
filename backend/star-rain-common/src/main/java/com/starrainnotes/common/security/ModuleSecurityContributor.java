/*
 * ModuleSecurityContributor：公共基础模块的安全策略文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 阅读入口：moduleName（module名称处理入口）；order（顺序处理入口）；publicPatterns（公开Patterns处理入口）；authenticatedPatterns（已认证Patterns处理入口）。
 */

package com.starrainnotes.common.security;

import java.util.List;

/*
 * 业务模块向全局安全链声明自身 URL 访问边界的唯一契约。
 *
 * V2 只有一条 SecurityFilterChain，由 Account 模块持有：
 * CSRF、会话策略、会话有效性校验与统一 401/403 输出只实现一次，
 * 任何模块都不会因为自建过滤器链而漏掉账户停用校验。
 *
 * 规则优先级固定为 denied > public > authenticated，最后统一 anyRequest().denyAll()。
 * 模块只声明“能不能匿名访问”，对象级与权限级判断仍由方法级 @PreAuthorize
 * 或服务内部完成，浏览器无法通过提交参数伪造内部模块调用。
 *
 * 本接口只使用字符串，避免 star-rain-common 依赖 Spring Security 类型。
 */
/**
 * Module安全贡献者，属于公共基础模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
public interface ModuleSecurityContributor {

    // 模块名，只用于诊断与规则冲突排查
    /**
     * module名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String moduleName();

    // 声明顺序，数值小者先声明，便于更具体的规则排在更宽泛的规则之前
    /**
     * 顺序处理入口。
     * @return 类型为 <code>int</code> 的处理结果
     */
    default int order() {
        return 100;
    }

    // 允许匿名访问的 URL 模式
    /**
     * 公开Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    default List<String> publicPatterns() {
        return List.of();
    }

    // 只要求已认证、不在此处要求具体权限的 URL 模式
    /**
     * 已认证Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    default List<String> authenticatedPatterns() {
        return List.of();
    }

    // 显式拒绝的 URL 模式，优先于本接口的其它规则
    /**
     * 被拒绝Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    default List<String> deniedPatterns() {
        return List.of();
    }

    // 允许同源框架嵌入的页面；默认仍为 DENY，不改变匿名访问或认证规则。
    /**
     * same来源框架Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    default List<String> sameOriginFramePatterns() {
        return List.of();
    }
}










