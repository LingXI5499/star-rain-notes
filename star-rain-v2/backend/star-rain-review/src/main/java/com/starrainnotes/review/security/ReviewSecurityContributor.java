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
 * /api/review/** 留给业务模块将来暴露的申请人侧入口（例如 /api/review/mine），
 * 它同样只要求已认证，对象级授权由 Service 的 ReviewViewer 判定。
 */
@Component
public class ReviewSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleName() {
        return "review";
    }

    // Review 依赖 Account 的认证上下文，排在 Account(10) 与 Media(20) 之后
    @Override
    public int order() {
        return 30;
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/reviews/**", "/api/review/**");
    }

    // 审核请求一律不公开，显式声明拒绝以免将来别的模块把 /api/** 放开
    @Override
    public List<String> deniedPatterns() {
        return List.of("/api/reviews/**");
    }
}
