/*
 * WorkRulesTest：作品模块的测试文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：WorkInvalidException。
 * 阅读入口：generatedSlugIsStableForChineseAndAsciiTitles（generated固定链接标识是否Stable面向Chinese及AsciiTitles处理入口）；externalLinksRejectUnsafeSchemesAndUserInfo（externalLinksReject不安全Schemes及用户Info处理入口）。
 */

package com.starrainnotes.portfolio.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.portfolio.exception.WorkInvalidException;
import org.junit.jupiter.api.Test;

/**
 * 作品规则测试，属于作品模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class WorkRulesTest {
    /**
     * generated固定链接标识是否Stable面向Chinese及AsciiTitles处理入口。
     * 关键处理步骤调用 <code>WorkSlugRules.fromTitle</code>、<code>WorkSlugRules.valid</code>。
     */
    @Test
    void generatedSlugIsStableForChineseAndAsciiTitles() {
        assertThat(WorkSlugRules.fromTitle("Java Study 2026")).isEqualTo("java-study-2026");
        String chinese = WorkSlugRules.fromTitle("星雨笔录项目");
        assertThat(chinese).startsWith("work-").isEqualTo(WorkSlugRules.fromTitle("星雨笔录项目"));
        assertThat(WorkSlugRules.valid(chinese)).isTrue();
        assertThat(WorkSlugRules.valid(WorkSlugRules.fromTitle("!!!"))).isTrue();
    }

    /**
     * externalLinksReject不安全Schemes及用户Info处理入口。
     * 关键处理步骤调用 <code>WorkLinkRules.requireSafeUrl</code>。
     */
    @Test
    void externalLinksRejectUnsafeSchemesAndUserInfo() {
        assertThat(WorkLinkRules.requireSafeUrl("https://example.com/project"))
                .isEqualTo("https://example.com/project");
        assertThatThrownBy(() -> WorkLinkRules.requireSafeUrl("javascript:alert(1)"))
                .isInstanceOf(WorkInvalidException.class);
        assertThatThrownBy(() -> WorkLinkRules.requireSafeUrl("https://user:pass@example.com"))
                .isInstanceOf(WorkInvalidException.class);
    }
}
