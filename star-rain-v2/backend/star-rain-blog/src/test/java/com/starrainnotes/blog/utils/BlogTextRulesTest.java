package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 文本规则与摘要派生测试。
 */
class BlogTextRulesTest {

    @Test
    @DisplayName("摘要从正文派生：去掉 Markdown 标记，只留可读文字")
    void derivesReadableSummary() {
        String body = """
                # 星雨笔录 V2

                > 引用段落

                - 列表项
                - 第二项

                **加粗**与`代码`以及[链接](https://example.com/a)
                """;

        String summary = BlogTextRules.deriveSummary(body);

        assertThat(summary).isEqualTo("星雨笔录 V2 引用段落 列表项 第二项 加粗与代码以及链接");
    }

    @Test
    @DisplayName("代码块内容不进摘要，避免把示例代码当文章简介")
    void skipsCodeFences() {
        String body = """
                正文第一行

                ```
                SELECT * FROM sr_blog_post;
                ```
                """;

        assertThat(BlogTextRules.deriveSummary(body)).isEqualTo("正文第一行");
    }

    @Test
    @DisplayName("含公式的正文派生纯文字摘要，旧摘要即使截断在公式中也能安全展示")
    void mathDoesNotLeakIntoSummary() {
        String body = """
                ## 排序

                给定长度为 \\(n\\) 的序列：
                \\[
                A = (a_0, a_1, \\ldots)
                \\]
                第二个式子保证排序只改变位置、不增加元素。
                这也适用于其他排序算法。
                """;

        String plain = "第二个式子保证排序只改变位置、不增加元素。 这也适用于其他排序算法。";
        assertThat(BlogTextRules.deriveSummary(body)).isEqualTo(plain);
        assertThat(BlogTextRules.publicSummary("给定 \\(n\\) 且 \\[ A =", body)).isEqualTo(plain);
        assertThat(BlogTextRules.publicSummary("作者手写的普通摘要", body)).isEqualTo("作者手写的普通摘要");
    }

    @Test
    @DisplayName("公式文章的长摘要优先在完整句子处结束")
    void mathSummaryEndsAtSentenceBoundary() {
        String body = """
                公式是 \\(x\\)。
                %s。
                %s。
                """.formatted("甲".repeat(110), "乙".repeat(110));

        String summary = BlogTextRules.deriveSummary(body);

        assertThat(summary).isEqualTo("甲".repeat(110) + "。");
    }

    @Test
    @DisplayName("摘要长度收敛到 200 字符，符合数据库列之外的产品约定")
    void truncatesLongSummary() {
        String body = "字".repeat(500);

        assertThat(BlogTextRules.deriveSummary(body)).hasSize(200);
    }

    @Test
    @DisplayName("空正文派生不出摘要，返回 null 而不是空字符串")
    void returnsNullForEmptyBody() {
        assertThat(BlogTextRules.deriveSummary(null)).isNull();
        assertThat(BlogTextRules.deriveSummary("   ")).isNull();
        assertThat(BlogTextRules.deriveSummary("###")).isNull();
    }

    @Test
    @DisplayName("hasText / length / normalize 对全空白输入的处理一致")
    void normalizers() {
        assertThat(BlogTextRules.hasText("  ")).isFalse();
        assertThat(BlogTextRules.length("  abc  ")).isEqualTo(3);
        assertThat(BlogTextRules.normalizeName("  Java  ")).isEqualTo("Java");
        assertThat(BlogTextRules.normalizeName("   ")).isNull();
        assertThat(BlogTextRules.normalizeDescription("")).isNull();
        assertThat(BlogTextRules.normalizeKeyword("  Kafka  ")).isEqualTo("kafka");
    }
}
