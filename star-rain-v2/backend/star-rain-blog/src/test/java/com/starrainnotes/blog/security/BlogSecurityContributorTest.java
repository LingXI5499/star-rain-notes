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
class BlogSecurityContributorTest {

    private final BlogSecurityContributor contributor = new BlogSecurityContributor();

    @Test
    @DisplayName("前台公开读取允许匿名，后台接口只要求已认证")
    void declaresUrlBoundaries() {
        assertThat(contributor.moduleName()).isEqualTo("blog");
        assertThat(contributor.publicPatterns()).containsExactly("/api/public/blog/**");
        assertThat(contributor.authenticatedPatterns()).containsExactly("/api/admin/blog/**");
    }

    @Test
    @DisplayName("后台前缀绝不能出现在公开规则里")
    void adminPatternsAreNotPublic() {
        assertThat(contributor.publicPatterns()).noneMatch(pattern -> pattern.contains("/api/admin"));
    }

    @Test
    @DisplayName("排序在 account(10) 与 media(20) 之后，具体权限交给方法级注解")
    void orderAndDeniedPatterns() {
        assertThat(contributor.order()).isEqualTo(40);
        assertThat(contributor.deniedPatterns()).isEmpty();
    }
}
