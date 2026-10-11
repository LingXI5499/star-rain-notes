/*
 * BlogTextRulesTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：derivesReadableSummary（\u6458\u8981\u4ece\u6b63\u6587\u6d3e\u751f\uff1a\u53bb\u6389 Markdown \u6807\u8bb0\uff0c\u53ea\u7559\u53ef\u8bfb\u6587\u5b57）；skipsCodeFences（\u4ee3\u7801\u5757\u5185\u5bb9\u4e0d\u8fdb\u6458\u8981\uff0c\u907f\u514d\u628a\u793a\u4f8b\u4ee3\u7801\u5f53\u6587\u7ae0\u7b80\u4ecb）；mathDoesNotLeakIntoSummary（\u542b\u516c\u5f0f\u7684\u6b63\u6587\u6d3e\u751f\u7eaf\u6587\u5b57\u6458\u8981\uff0c\u65e7\u6458\u8981\u5373\u4f7f\u622a\u65ad\u5728\u516c\u5f0f\u4e2d\u4e5f\u80fd\u5b89\u5168\u5c55\u793a）；mathSummaryEndsAtSentenceBoundary（\u516c\u5f0f\u6587\u7ae0\u7684\u957f\u6458\u8981\u4f18\u5148\u5728\u5b8c\u6574\u53e5\u5b50\u5904\u7ed3\u675f）。
 */

package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 文本规则与摘要派生测试。
 */
/**
 * 博客文本规则测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogTextRulesTest {

    /**
     * \u6458\u8981\u4ece\u6b63\u6587\u6d3e\u751f\uff1a\u53bb\u6389 Markdown \u6807\u8bb0\uff0c\u53ea\u7559\u53ef\u8bfb\u6587\u5b57。
     * 关键处理步骤调用 <code>BlogTextRules.deriveSummary</code>。
     */
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

    /**
     * \u4ee3\u7801\u5757\u5185\u5bb9\u4e0d\u8fdb\u6458\u8981\uff0c\u907f\u514d\u628a\u793a\u4f8b\u4ee3\u7801\u5f53\u6587\u7ae0\u7b80\u4ecb。
     * 关键处理步骤调用 <code>BlogTextRules.deriveSummary</code>。
     */
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

    /**
     * \u542b\u516c\u5f0f\u7684\u6b63\u6587\u6d3e\u751f\u7eaf\u6587\u5b57\u6458\u8981\uff0c\u65e7\u6458\u8981\u5373\u4f7f\u622a\u65ad\u5728\u516c\u5f0f\u4e2d\u4e5f\u80fd\u5b89\u5168\u5c55\u793a。
     * 关键处理步骤调用 <code>BlogTextRules.deriveSummary</code>、<code>BlogTextRules.publicSummary</code>。
     */
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

    /**
     * \u516c\u5f0f\u6587\u7ae0\u7684\u957f\u6458\u8981\u4f18\u5148\u5728\u5b8c\u6574\u53e5\u5b50\u5904\u7ed3\u675f。
     * 关键处理步骤调用 <code>BlogTextRules.deriveSummary</code>。
     */
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

    /**
     * \u6458\u8981\u957f\u5ea6\u6536\u655b\u5230 200 \u5b57\u7b26\uff0c\u7b26\u5408\u6570\u636e\u5e93\u5217\u4e4b\u5916\u7684\u4ea7\u54c1\u7ea6\u5b9a。
     * 关键处理步骤调用 <code>BlogTextRules.deriveSummary</code>。
     */
    @Test
    @DisplayName("摘要长度收敛到 200 字符，符合数据库列之外的产品约定")
    void truncatesLongSummary() {
        String body = "字".repeat(500);

        assertThat(BlogTextRules.deriveSummary(body)).hasSize(200);
    }

    /**
     * \u7a7a\u6b63\u6587\u6d3e\u751f\u4e0d\u51fa\u6458\u8981\uff0c\u8fd4\u56de null \u800c\u4e0d\u662f\u7a7a\u5b57\u7b26\u4e32。
     * 关键处理步骤调用 <code>BlogTextRules.deriveSummary</code>。
     */
    @Test
    @DisplayName("空正文派生不出摘要，返回 null 而不是空字符串")
    void returnsNullForEmptyBody() {
        assertThat(BlogTextRules.deriveSummary(null)).isNull();
        assertThat(BlogTextRules.deriveSummary("   ")).isNull();
        assertThat(BlogTextRules.deriveSummary("###")).isNull();
    }

    /**
     * hasText / length / normalize \u5bf9\u5168\u7a7a\u767d\u8f93\u5165\u7684\u5904\u7406\u4e00\u81f4。
     * 关键处理步骤调用 <code>BlogTextRules.hasText</code>、<code>BlogTextRules.length</code>、<code>BlogTextRules.normalizeName</code>、<code>BlogTextRules.normalizeDescription</code>。
     */
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
