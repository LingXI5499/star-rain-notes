package com.starrainnotes.portfolio.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.portfolio.exception.WorkInvalidException;
import org.junit.jupiter.api.Test;

class WorkRulesTest {
    @Test
    void generatedSlugIsStableForChineseAndAsciiTitles() {
        assertThat(WorkSlugRules.fromTitle("Java Study 2026")).isEqualTo("java-study-2026");
        String chinese = WorkSlugRules.fromTitle("星雨笔录项目");
        assertThat(chinese).startsWith("work-").isEqualTo(WorkSlugRules.fromTitle("星雨笔录项目"));
        assertThat(WorkSlugRules.valid(chinese)).isTrue();
        assertThat(WorkSlugRules.valid(WorkSlugRules.fromTitle("!!!"))).isTrue();
    }

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
