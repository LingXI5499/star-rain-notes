package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.blog.constant.BlogLimits;
import com.starrainnotes.blog.exception.BlogQueryInvalidException;
import com.starrainnotes.common.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 分页与筛选参数校验测试。
 *
 * 目标：非法参数得到明确的 BLOG_QUERY_INVALID，而不是让数据库先报错再变成 500。
 */
class BlogQueryRulesTest {

    @Test
    @DisplayName("分页越界返回 BLOG_QUERY_INVALID 而不是静默纠正")
    void rejectsInvalidPaging() {
        assertThatThrownBy(() -> BlogQueryRules.validatePage(0, 20))
                .isInstanceOf(BlogQueryInvalidException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
        assertThatThrownBy(() -> BlogQueryRules.validatePage(1, 0))
                .isInstanceOf(BlogQueryInvalidException.class);
        assertThatThrownBy(() -> BlogQueryRules.validatePage(1, BlogQueryRules.PAGE_SIZE_MAX + 1))
                .isInstanceOf(BlogQueryInvalidException.class);
        assertThatThrownBy(() -> BlogQueryRules.validatePage(BlogQueryRules.PAGE_MAX + 1, 20))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("合法分页通过，offset 由页码与页大小算出")
    void acceptsValidPaging() {
        BlogQueryRules.validatePage(1, BlogQueryRules.PAGE_SIZE_MAX);

        assertThat(BlogQueryRules.offset(1, 20)).isZero();
        assertThat(BlogQueryRules.offset(3, 20)).isEqualTo(40);
    }

    @Test
    @DisplayName("状态筛选：空白不筛选，非法值报错，大小写不敏感")
    void parsesPostStatus() {
        assertThat(BlogQueryRules.postStatus(null)).isNull();
        assertThat(BlogQueryRules.postStatus("  ")).isNull();
        assertThat(BlogQueryRules.postStatus("published").name()).isEqualTo("PUBLISHED");
        assertThatThrownBy(() -> BlogQueryRules.postStatus("DELETED"))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("分类状态筛选：非法值报错而不是被忽略")
    void parsesTaxonomyStatus() {
        assertThat(BlogQueryRules.taxonomyStatus("enabled").name()).isEqualTo("ENABLED");
        assertThatThrownBy(() -> BlogQueryRules.taxonomyStatus("ARCHIVED"))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("主键筛选条件必须为正整数")
    void rejectsNonPositiveIds() {
        assertThat(BlogQueryRules.optionalId(null, "tagId")).isNull();
        assertThat(BlogQueryRules.optionalId(3L, "tagId")).isEqualTo(3L);
        assertThatThrownBy(() -> BlogQueryRules.optionalId(0L, "tagId"))
                .isInstanceOf(BlogQueryInvalidException.class);
        assertThatThrownBy(() -> BlogQueryRules.optionalId(-1L, "topicId"))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("归档年份必须在合理区间")
    void validatesArchiveYear() {
        assertThat(BlogQueryRules.archiveYear(null)).isNull();
        assertThat(BlogQueryRules.archiveYear(2026)).isEqualTo(2026);
        assertThatThrownBy(() -> BlogQueryRules.archiveYear(1999))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("月份必须配合年份，且只能在 1 到 12 之间")
    void validatesArchiveMonth() {
        assertThat(BlogQueryRules.archiveMonth(2026, null)).isNull();
        assertThat(BlogQueryRules.archiveMonth(2026, 7)).isEqualTo(7);
        assertThatThrownBy(() -> BlogQueryRules.archiveMonth(null, 7))
                .isInstanceOf(BlogQueryInvalidException.class);
        assertThatThrownBy(() -> BlogQueryRules.archiveMonth(2026, 13))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("归档日期需有效年月，闰年与平年二月按实际天数校验")
    void validatesArchiveDay() {
        assertThat(BlogQueryRules.archiveDay(2026, 2, null)).isNull();
        assertThat(BlogQueryRules.archiveDay(2024, 2, 29)).isEqualTo(29);
        assertThatThrownBy(() -> BlogQueryRules.archiveDay(2026, 2, 29))
                .isInstanceOf(BlogQueryInvalidException.class);
        assertThatThrownBy(() -> BlogQueryRules.archiveDay(null, 2, 1))
                .isInstanceOf(BlogQueryInvalidException.class);
        assertThatThrownBy(() -> BlogQueryRules.archiveDay(2026, null, 1))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    @Test
    @DisplayName("slug 规范化：去空白、转小写，非法字符被识别")
    void slugRules() {
        assertThat(BlogSlugRules.normalize("  Hello-World  ")).isEqualTo("hello-world");
        assertThat(BlogSlugRules.normalize("   ")).isNull();
        assertThat(BlogSlugRules.isValid("hello-world-2", BlogLimits.POST_SLUG_MAX_LENGTH)).isTrue();
        assertThat(BlogSlugRules.isValid("hello--world", BlogLimits.POST_SLUG_MAX_LENGTH)).isFalse();
        assertThat(BlogSlugRules.isValid("-hello", BlogLimits.POST_SLUG_MAX_LENGTH)).isFalse();
        assertThat(BlogSlugRules.isValid("中文", BlogLimits.POST_SLUG_MAX_LENGTH)).isFalse();
        assertThat(BlogSlugRules.isValid("a".repeat(BlogLimits.POST_SLUG_MAX_LENGTH + 1),
                BlogLimits.POST_SLUG_MAX_LENGTH)).isFalse();
    }
}
