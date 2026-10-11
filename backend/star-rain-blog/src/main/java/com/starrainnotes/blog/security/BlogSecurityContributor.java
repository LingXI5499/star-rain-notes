/*
 * BlogSecurityContributor：博客模块的安全策略文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：moduleName（module名称处理入口）；order（顺序处理入口）；publicPatterns（公开Patterns处理入口）；authenticatedPatterns（已认证Patterns处理入口）。
 */

package com.starrainnotes.blog.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * Blog 模块的 URL 边界。
 *
 * 前台公开读取必须允许匿名到达 Controller：/api/public/blog/** 只返回 status = PUBLISHED 的内容，
 * “草稿与已撤回文章不可见”是每个资源自己的状态属性，URL 层无法预判，
 * 因此放行到 Controller，由 Service 在查询条件里强制 status = PUBLISHED。
 *
 * 后台接口只要求已认证，具体权限由方法级 @PreAuthorize 执行：
 * 这里若写成 permitAll，博客就会变成任何人可写的公开内容域。
 */
/**
 * 博客安全贡献者，属于博客模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class BlogSecurityContributor implements ModuleSecurityContributor {

    /**
     * module名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String moduleName() {
        return "blog";
    }

    // Blog 依赖 Account 的认证上下文与 Media 的公开内容地址，排在它们之后
    /**
     * 顺序处理入口。
     * @return 类型为 <code>int</code> 的处理结果
     */
    @Override
    public int order() {
        return 40;
    }

    /**
     * 公开Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<String> publicPatterns() {
        return List.of("/api/public/blog/**");
    }

    /**
     * 已认证Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/blog/**");
    }
}
