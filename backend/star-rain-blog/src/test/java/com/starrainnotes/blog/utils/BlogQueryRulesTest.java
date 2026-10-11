/*
 * BlogQueryRulesTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogLimits、BlogQueryInvalidException、ApiException。
 * 阅读入口：rejectsInvalidPaging（\u5206\u9875\u8d8a\u754c\u8fd4\u56de BLOG_QUERY_INVALID \u800c\u4e0d\u662f\u9759\u9ed8\u7ea0\u6b63）；acceptsValidPaging（\u5408\u6cd5\u5206\u9875\u901a\u8fc7\uff0coffset \u7531\u9875\u7801\u4e0e\u9875\u5927\u5c0f\u7b97\u51fa）；parsesPostStatus（\u72b6\u6001\u7b5b\u9009\uff1a\u7a7a\u767d\u4e0d\u7b5b\u9009\uff0c\u975e\u6cd5\u503c\u62a5\u9519\uff0c\u5927\u5c0f\u5199\u4e0d\u654f\u611f）；parsesTaxonomyStatus（\u5206\u7c7b\u72b6\u6001\u7b5b\u9009\uff1a\u975e\u6cd5\u503c\u62a5\u9519\u800c\u4e0d\u662f\u88ab\u5ffd\u7565）。
 */

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
/**
 * 博客查询参数规则测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogQueryRulesTest {

    /**
     * \u5206\u9875\u8d8a\u754c\u8fd4\u56de BLOG_QUERY_INVALID \u800c\u4e0d\u662f\u9759\u9ed8\u7ea0\u6b63。
     * 关键处理步骤调用 <code>BlogQueryRules.validatePage</code>。
     */
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

    /**
     * \u5408\u6cd5\u5206\u9875\u901a\u8fc7\uff0coffset \u7531\u9875\u7801\u4e0e\u9875\u5927\u5c0f\u7b97\u51fa。
     * 关键处理步骤调用 <code>BlogQueryRules.validatePage</code>、<code>BlogQueryRules.offset</code>。
     */
    @Test
    @DisplayName("合法分页通过，offset 由页码与页大小算出")
    void acceptsValidPaging() {
        BlogQueryRules.validatePage(1, BlogQueryRules.PAGE_SIZE_MAX);

        assertThat(BlogQueryRules.offset(1, 20)).isZero();
        assertThat(BlogQueryRules.offset(3, 20)).isEqualTo(40);
    }

    /**
     * \u72b6\u6001\u7b5b\u9009\uff1a\u7a7a\u767d\u4e0d\u7b5b\u9009\uff0c\u975e\u6cd5\u503c\u62a5\u9519\uff0c\u5927\u5c0f\u5199\u4e0d\u654f\u611f。
     * 关键处理步骤调用 <code>BlogQueryRules.postStatus</code>。
     */
    @Test
    @DisplayName("状态筛选：空白不筛选，非法值报错，大小写不敏感")
    void parsesPostStatus() {
        assertThat(BlogQueryRules.postStatus(null)).isNull();
        assertThat(BlogQueryRules.postStatus("  ")).isNull();
        assertThat(BlogQueryRules.postStatus("published").name()).isEqualTo("PUBLISHED");
        assertThatThrownBy(() -> BlogQueryRules.postStatus("DELETED"))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    /**
     * \u5206\u7c7b\u72b6\u6001\u7b5b\u9009\uff1a\u975e\u6cd5\u503c\u62a5\u9519\u800c\u4e0d\u662f\u88ab\u5ffd\u7565。
     * 关键处理步骤调用 <code>BlogQueryRules.taxonomyStatus</code>。
     */
    @Test
    @DisplayName("分类状态筛选：非法值报错而不是被忽略")
    void parsesTaxonomyStatus() {
        assertThat(BlogQueryRules.taxonomyStatus("enabled").name()).isEqualTo("ENABLED");
        assertThatThrownBy(() -> BlogQueryRules.taxonomyStatus("ARCHIVED"))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    /**
     * \u4e3b\u952e\u7b5b\u9009\u6761\u4ef6\u5fc5\u987b\u4e3a\u6b63\u6574\u6570。
     * 关键处理步骤调用 <code>BlogQueryRules.optionalId</code>。
     */
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

    /**
     * \u5f52\u6863\u5e74\u4efd\u5fc5\u987b\u5728\u5408\u7406\u533a\u95f4。
     * 关键处理步骤调用 <code>BlogQueryRules.archiveYear</code>。
     */
    @Test
    @DisplayName("归档年份必须在合理区间")
    void validatesArchiveYear() {
        assertThat(BlogQueryRules.archiveYear(null)).isNull();
        assertThat(BlogQueryRules.archiveYear(2026)).isEqualTo(2026);
        assertThatThrownBy(() -> BlogQueryRules.archiveYear(1999))
                .isInstanceOf(BlogQueryInvalidException.class);
    }

    /**
     * \u6708\u4efd\u5fc5\u987b\u914d\u5408\u5e74\u4efd\uff0c\u4e14\u53ea\u80fd\u5728 1 \u5230 12 \u4e4b\u95f4。
     * 关键处理步骤调用 <code>BlogQueryRules.archiveMonth</code>。
     */
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

    /**
     * \u5f52\u6863\u65e5\u671f\u9700\u6709\u6548\u5e74\u6708\uff0c\u95f0\u5e74\u4e0e\u5e73\u5e74\u4e8c\u6708\u6309\u5b9e\u9645\u5929\u6570\u6821\u9a8c。
     * 关键处理步骤调用 <code>BlogQueryRules.archiveDay</code>。
     */
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

    /**
     * slug \u89c4\u8303\u5316\uff1a\u53bb\u7a7a\u767d\u3001\u8f6c\u5c0f\u5199\uff0c\u975e\u6cd5\u5b57\u7b26\u88ab\u8bc6\u522b。
     * 关键处理步骤调用 <code>BlogSlugRules.normalize</code>、<code>BlogSlugRules.isValid</code>。
     */
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
