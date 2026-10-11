/*
 * BlogContentMediaParserTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：extractsContentUrlsInOrder（\u53ea\u8bc6\u522b Media \u6a21\u5757\u7684\u516c\u5f00\u5185\u5bb9\u5730\u5740\uff0c\u5e76\u4fdd\u6301\u51fa\u73b0\u987a\u5e8f）；deduplicatesRepeatedAssets（\u540c\u4e00\u5a92\u4f53\u91cd\u590d\u51fa\u73b0\u53ea\u767b\u8bb0\u4e00\u6761\u5f15\u7528）；ignoresExternalUrls（\u5916\u90e8\u56fe\u5e8a\u5730\u5740\u4e0d\u6784\u6210\u5bf9\u672c\u7ad9\u5a92\u4f53\u7684\u5f15\u7528）；handlesEmptyBody（\u7a7a\u6b63\u6587\u4e0e null \u90fd\u8fd4\u56de\u7a7a\u96c6\u5408\uff0c\u4e0d\u629b\u5f02\u5e38）。
 */

package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 正文媒体引用解析测试。
 *
 * 这是 blog.content 引用的唯一来源，解析错了会导致 Media 侧归档保护失效：
 * 引用少解析一条，作者就能归档一张还在正文里用的图。
 */
/**
 * 博客内容媒体Parser测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogContentMediaParserTest {

    /**
     * \u53ea\u8bc6\u522b Media \u6a21\u5757\u7684\u516c\u5f00\u5185\u5bb9\u5730\u5740\uff0c\u5e76\u4fdd\u6301\u51fa\u73b0\u987a\u5e8f。
     * 关键处理步骤调用 <code>BlogContentMediaParser.extractMediaAssetIds</code>。
     */
    @Test
    @DisplayName("只识别 Media 模块的公开内容地址，并保持出现顺序")
    void extractsContentUrlsInOrder() {
        String body = """
                # 标题

                ![封面](/api/media/assets/12/content)

                正文中间夹一段

                <img src="/api/media/assets/7/content" />
                """;

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).containsExactly(12L, 7L);
    }

    /**
     * \u540c\u4e00\u5a92\u4f53\u91cd\u590d\u51fa\u73b0\u53ea\u767b\u8bb0\u4e00\u6761\u5f15\u7528。
     * 关键处理步骤调用 <code>BlogContentMediaParser.extractMediaAssetIds</code>。
     */
    @Test
    @DisplayName("同一媒体重复出现只登记一条引用")
    void deduplicatesRepeatedAssets() {
        String body = "![](/api/media/assets/5/content)\n![](/api/media/assets/5/content)";

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).containsExactly(5L);
    }

    /**
     * \u5916\u90e8\u56fe\u5e8a\u5730\u5740\u4e0d\u6784\u6210\u5bf9\u672c\u7ad9\u5a92\u4f53\u7684\u5f15\u7528。
     * 关键处理步骤调用 <code>BlogContentMediaParser.extractMediaAssetIds</code>。
     */
    @Test
    @DisplayName("外部图床地址不构成对本站媒体的引用")
    void ignoresExternalUrls() {
        String body = "![](https://cdn.example.com/api/media/assets/9/content.png)\n![](https://img.example.com/a.png)";

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).isEmpty();
    }

    /**
     * \u7a7a\u6b63\u6587\u4e0e null \u90fd\u8fd4\u56de\u7a7a\u96c6\u5408\uff0c\u4e0d\u629b\u5f02\u5e38。
     * 关键处理步骤调用 <code>BlogContentMediaParser.extractMediaAssetIds</code>。
     */
    @Test
    @DisplayName("空正文与 null 都返回空集合，不抛异常")
    void handlesEmptyBody() {
        assertThat(BlogContentMediaParser.extractMediaAssetIds(null)).isEmpty();
        assertThat(BlogContentMediaParser.extractMediaAssetIds("")).isEmpty();
        assertThat(BlogContentMediaParser.extractMediaAssetIds("   ")).isEmpty();
    }

    /**
     * \u8d85\u51fa long \u8303\u56f4\u7684 ID \u88ab\u5ffd\u7565\uff0c\u4e0d\u8ba9\u6574\u7bc7\u4fdd\u5b58\u5931\u8d25。
     * 关键处理步骤调用 <code>BlogContentMediaParser.extractMediaAssetIds</code>。
     */
    @Test
    @DisplayName("超出 long 范围的 ID 被忽略，不让整篇保存失败")
    void ignoresOverflowingIds() {
        String body = "![](/api/media/assets/99999999999999999999999/content)\n![](/api/media/assets/3/content)";

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).containsExactly(3L);
    }

    /**
     * \u63a5\u53e3\u8fd4\u56de\u7684\u96c6\u5408\u4e0d\u53ef\u53d8\uff0c\u8c03\u7528\u65b9\u65e0\u6cd5\u5728\u522b\u5904\u7be1\u6539\u5f15\u7528\u96c6\u5408。
     * 关键处理步骤调用 <code>BlogContentMediaParser.extractMediaAssetIds</code>。
     */
    @Test
    @DisplayName("接口返回的集合不可变，调用方无法在别处篡改引用集合")
    void returnsIndependentList() {
        List<Long> first = BlogContentMediaParser.extractMediaAssetIds("![](/api/media/assets/1/content)");
        List<Long> second = BlogContentMediaParser.extractMediaAssetIds("![](/api/media/assets/2/content)");

        assertThat(first).containsExactly(1L);
        assertThat(second).containsExactly(2L);
    }
}
