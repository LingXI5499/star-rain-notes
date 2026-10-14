/*
 * MediaSecurityContributor：媒体模块的安全策略文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：moduleName（module名称处理入口）；order（顺序处理入口）；publicPatterns（公开Patterns处理入口）；authenticatedPatterns（已认证Patterns处理入口）。
 */

package com.starrainnotes.media.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * Media 模块的 URL 边界。
 *
 * /api/media/assets/*&#47;content 必须允许匿名到达 Controller：
 * 某个媒体是 PUBLIC 还是 PROTECTED 是“每个资源自己的属性”，URL 层无法预判。
 * URL 层放行，真正的读取授权由 MediaAccessService 按资源的 accessLevel 判定。
 *
 * 后台媒体接口只要求已认证，具体权限由方法级 @PreAuthorize 执行。
 */
/**
 * 媒体安全贡献者，属于媒体模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class MediaSecurityContributor implements ModuleSecurityContributor {

    /**
     * module名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String moduleName() {
        return "media";
    }

    // Media 依赖 Account 的认证上下文，排在 Account 之后
    /**
     * 顺序处理入口。
     * @return 类型为 <code>int</code> 的处理结果
     */
    @Override
    public int order() {
        return 20;
    }

    /**
     * 公开Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<String> publicPatterns() {
        return List.of("/api/media/assets/*/content", "/api/media/assets/*/thumbnail");
    }

    /**
     * 已认证Patterns处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/media/**");
    }
}
