/*
 * ReviewSecurityContributor：审核模块的安全策略文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：moduleName（module名称处理入口）；order（顺序处理入口）；authenticatedPatterns（已认证Patterns处理入口）；deniedPatterns（被拒绝Patterns处理入口）。
 */

package com.starrainnotes.review.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * Review 模块的 URL 访问边界。
 *
 * Review 没有任何匿名可访问的资源：审核请求里带着未发布的业务内容与申请人信息，
 * 全部端点都要求已认证，具体权限由 Controller 方法上的 @PreAuthorize 声明。
 *
 * 申请人侧若增加入口，应在实际路由出现时显式登记访问规则。
 */
/**
 * 审核安全贡献者，属于审核模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class ReviewSecurityContributor implements ModuleSecurityContributor {

    /**
     * module名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String moduleName() {
        return "review";
    }

    // Review 依赖 Account 的认证上下文，排在 Account(10) 与 Media(20) 之后
    /**
     * 顺序处理入口。
     * @return 类型为 <code>int</code> 的处理结果
     */
    @Override
    public int order() {
        return 30;
    }

    /**
     * 已认证Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/reviews/**");
    }

    // 审核请求一律不公开，显式声明拒绝以免将来别的模块把 /api/** 放开
    /**
     * 被拒绝Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<String> deniedPatterns() {
        return List.of("/api/reviews/**");
    }
}
