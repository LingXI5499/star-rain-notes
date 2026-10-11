/*
 * ModuleFrameHeaders：账号模块的安全策略文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：writer（writer处理入口）。
 */

package com.starrainnotes.account.security;

import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Keep DENY as the site default; only module-declared static previews allow their own frame. */
public final class ModuleFrameHeaders {
    /**
     * 构造ModuleFrameHeaders，接收本类使用的依赖或初始化参数。
     */
    private ModuleFrameHeaders() { }

    /**
     * writer处理入口。
     * 关键处理步骤调用 <code>contributors.stream</code>、<code>module.sameOriginFramePatterns</code>、<code>PathPatternRequestMatcher.withDefaults</code>、<code>response.setHeader</code>。
     *
     * @param contributors contributors；类型为 <code>List&lt;ModuleSecurityContributor&gt;</code>
     * @return 类型为 <code>HeaderWriter</code> 的处理结果
     */
    public static HeaderWriter writer(List<ModuleSecurityContributor> contributors) {
        List<RequestMatcher> previews = contributors.stream()
                .flatMap(module -> module.sameOriginFramePatterns().stream())
                .map(pattern -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(pattern))
                .toList();
        return (request, response) -> response.setHeader("X-Frame-Options",
                previews.stream().anyMatch(matcher -> matcher.matches(request)) ? "SAMEORIGIN" : "DENY");
    }
}
