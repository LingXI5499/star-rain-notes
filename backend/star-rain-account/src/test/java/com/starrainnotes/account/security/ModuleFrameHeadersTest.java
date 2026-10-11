/*
 * ModuleFrameHeadersTest：账号模块的测试文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ModuleSecurityContributor。
 * 阅读入口：moduleName（module名称处理入口）；sameOriginFramePatterns（same来源框架Patterns处理入口）。
 */

package com.starrainnotes.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Module框架响应头测试，属于账号模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class ModuleFrameHeadersTest {
    /**
     * onlyTheDeclaredPrototype路径CanBeFramed处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>ModuleFrameHeaders.writer</code>、<code>writer.writeHeaders</code>、<code>response.getHeader</code>。
     */
    @Test
    void onlyTheDeclaredPrototypePathCanBeFramed() {
        var module = new ModuleSecurityContributor()
    /**
     * 当前调用使用的匿名协作实现，属于账号模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    {
            /**
             * module名称处理入口。
             * @return 类型为 <code>String</code> 的处理结果
             */
            public String moduleName() { return "portfolio"; }
            /**
             * same来源框架Patterns处理入口。
             * 关键处理步骤调用 <code>List.of</code>。
             * @return 本次处理得到的条目集合
             */
            public List<String> sameOriginFramePatterns() { return List.of("/api/public/portfolio/works/*/live/**"); }
        };
        var writer = ModuleFrameHeaders.writer(List.of(module));
        for (String path : List.of("/api/public/portfolio/works/demo/live/index.html",
                "/api/public/portfolio/works/demo/live/assets/app.js")) {
            var response = new MockHttpServletResponse();
            writer.writeHeaders(new MockHttpServletRequest("GET", path), response);
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        }
        for (String path : List.of("/api/admin/accounts", "/api/public/portfolio/works/demo",
                "/api/public/portfolio/works/demo/live-other/index.html", "/useradmin/login")) {
            var response = new MockHttpServletResponse();
            writer.writeHeaders(new MockHttpServletRequest("GET", path), response);
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        }
    }

    /**
     * 不含贡献者保留DenyEverywhere处理入口。
     * 关键处理步骤调用 <code>ModuleFrameHeaders.writer</code>、<code>List.of</code>、<code>response.getHeader</code>。
     */
    @Test
    void noContributorKeepsDenyEverywhere() {
        var response = new MockHttpServletResponse();
        ModuleFrameHeaders.writer(List.of()).writeHeaders(new MockHttpServletRequest("GET", "/index.html"), response);
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }
}
