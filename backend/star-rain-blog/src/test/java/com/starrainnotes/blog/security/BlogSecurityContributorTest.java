/*
 * BlogSecurityContributorTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：declaresUrlBoundaries（\u524d\u53f0\u516c\u5f00\u8bfb\u53d6\u5141\u8bb8\u533f\u540d\uff0c\u540e\u53f0\u63a5\u53e3\u53ea\u8981\u6c42\u5df2\u8ba4\u8bc1）；adminPatternsAreNotPublic（\u540e\u53f0\u524d\u7f00\u7edd\u4e0d\u80fd\u51fa\u73b0\u5728\u516c\u5f00\u89c4\u5219\u91cc）；orderAndDeniedPatterns（\u6392\u5e8f\u5728 account(10) \u4e0e media(20) \u4e4b\u540e\uff0c\u5177\u4f53\u6743\u9650\u4ea4\u7ed9\u65b9\u6cd5\u7ea7\u6ce8\u89e3）。
 */

package com.starrainnotes.blog.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * Blog 模块 URL 边界测试。
 *
 * 这三条断言对应三类真实事故：
 * 1. 后台前缀被写成 permitAll —— 任何人都能写别人的博客；
 * 2. 前台前缀没放行 —— 匿名读者全部 403，博客等于不存在；
 * 3. order() 与其他模块撞车 —— 规则声明顺序变化会悄悄改变谁先匹配。
 */
/**
 * 博客安全贡献者测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogSecurityContributorTest {

    /**
     * 贡献者。
     */
    private final BlogSecurityContributor contributor = new BlogSecurityContributor();

    /**
     * \u524d\u53f0\u516c\u5f00\u8bfb\u53d6\u5141\u8bb8\u533f\u540d\uff0c\u540e\u53f0\u63a5\u53e3\u53ea\u8981\u6c42\u5df2\u8ba4\u8bc1。
     * 主要协作调用：<code>contributor.moduleName</code>、<code>contributor.publicPatterns</code>、<code>contributor.authenticatedPatterns</code>。
     */
    @Test
    @DisplayName("前台公开读取允许匿名，后台接口只要求已认证")
    void declaresUrlBoundaries() {
        assertThat(contributor.moduleName()).isEqualTo("blog");
        assertThat(contributor.publicPatterns()).containsExactly("/api/public/blog/**");
        assertThat(contributor.authenticatedPatterns()).containsExactly("/api/admin/blog/**");
    }

    /**
     * \u540e\u53f0\u524d\u7f00\u7edd\u4e0d\u80fd\u51fa\u73b0\u5728\u516c\u5f00\u89c4\u5219\u91cc。
     * 主要协作调用：<code>contributor.publicPatterns</code>。
     */
    @Test
    @DisplayName("后台前缀绝不能出现在公开规则里")
    void adminPatternsAreNotPublic() {
        assertThat(contributor.publicPatterns()).noneMatch(pattern -> pattern.contains("/api/admin"));
    }

    /**
     * \u6392\u5e8f\u5728 account(10) \u4e0e media(20) \u4e4b\u540e\uff0c\u5177\u4f53\u6743\u9650\u4ea4\u7ed9\u65b9\u6cd5\u7ea7\u6ce8\u89e3。
     * 主要协作调用：<code>contributor.order</code>、<code>contributor.deniedPatterns</code>。
     */
    @Test
    @DisplayName("排序在 account(10) 与 media(20) 之后，具体权限交给方法级注解")
    void orderAndDeniedPatterns() {
        assertThat(contributor.order()).isEqualTo(40);
        assertThat(contributor.deniedPatterns()).isEmpty();
    }
}
