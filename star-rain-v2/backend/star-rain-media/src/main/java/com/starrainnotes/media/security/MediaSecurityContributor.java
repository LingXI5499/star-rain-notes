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
@Component
public class MediaSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleName() {
        return "media";
    }

    // Media 依赖 Account 的认证上下文，排在 Account 之后
    @Override
    public int order() {
        return 20;
    }

    @Override
    public List<String> publicPatterns() {
        return List.of("/api/media/assets/*/content");
    }

    @Override
    public List<String> authenticatedPatterns() {
        return List.of("/api/admin/media/**");
    }
}
